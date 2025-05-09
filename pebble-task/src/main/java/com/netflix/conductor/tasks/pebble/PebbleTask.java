/*
 * Copyright 2022 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.tasks.pebble;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.core.execution.tasks.WorkflowSystemTask;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import com.alibaba.fastjson2.JSONObject;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import static com.netflix.conductor.common.metadata.tasks.TaskType.TASK_TYPE_PEBBLE;

@Component(TASK_TYPE_PEBBLE)
public class PebbleTask extends WorkflowSystemTask {

    private static PebbleEngine engine;

    private static final Logger LOGGER = LoggerFactory.getLogger(PebbleTask.class);

    public static final String TEMPLATE_PARAMETER_NAME = "template";

    public PebbleTask() {
        super(TASK_TYPE_PEBBLE);
        engine = new PebbleEngine.Builder().build();
        LOGGER.info("{} initialized...", getTaskType());
    }

    @Override
    public void start(WorkflowModel workflow, TaskModel task, WorkflowExecutor executor) {
        // 获取 Input
        Object templateObj = task.getInputData().get(TEMPLATE_PARAMETER_NAME);
        if (!(templateObj instanceof String templateStr)) {
            task.setReasonForIncompletion("missing " + TEMPLATE_PARAMETER_NAME);
            task.setStatus(TaskModel.Status.FAILED);
            return;
        }

        if (templateStr.isBlank()) {
            task.setReasonForIncompletion("Missing template");
            task.setStatus(TaskModel.Status.FAILED);
            return;
        }
        Object inputObj = task.getInputData().get("input");
        try {
            PebbleTemplate pebbleTemplate = engine.getLiteralTemplate(templateStr);
            if (inputObj instanceof List<?>) {
                List<JSONObject> resultList = dealList(pebbleTemplate, inputObj);
                task.addOutput("result", resultList);
            } else {
                JSONObject result = dealOne(pebbleTemplate, inputObj);
                task.addOutput("result", result);
            }
            task.setStatus(TaskModel.Status.COMPLETED);
        } catch (Exception e) {
            LOGGER.error(
                    "Failed to invoke {} task: {} - in workflow: {}",
                    getTaskType(),
                    task.getTaskId(),
                    task.getWorkflowInstanceId(),
                    e);
            task.setStatus(TaskModel.Status.FAILED);
            task.setReasonForIncompletion(
                    "Failed to invoke " + getTaskType() + " task due to: " + e);
            task.addOutput("reason", e.toString());
        }
    }

    private JSONObject dealOne(PebbleTemplate pebbleTemplate, Object inputObj) throws IOException {
        StringWriter writer = new StringWriter();
        pebbleTemplate.evaluate(writer, convertObj(inputObj));
        return JSONObject.parseObject(writer.toString());
    }

    private List<JSONObject> dealList(PebbleTemplate pebbleTemplate, Object inputObj) {
        List<Object> inputList = new ArrayList<>((List<Object>) inputObj);
        return inputList.parallelStream()
                .map(
                        e -> {
                            try {
                                return dealOne(pebbleTemplate, e);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        })
                .collect(Collectors.toList());
    }

    private JSONObject convertObj(Object inputObj) {
        if (inputObj == null) {
            return new JSONObject();
        }
        if (inputObj instanceof JSONObject) {
            return (JSONObject) inputObj;
        }
        return JSONObject.from(inputObj); // 将 Object 转换为 JSONObject
    }

    @Override
    public boolean execute(WorkflowModel workflow, TaskModel task, WorkflowExecutor executor) {
        return true;
    }

    @Override
    public void cancel(WorkflowModel workflow, TaskModel task, WorkflowExecutor executor) {
        task.setStatus(TaskModel.Status.CANCELED);
    }
}
