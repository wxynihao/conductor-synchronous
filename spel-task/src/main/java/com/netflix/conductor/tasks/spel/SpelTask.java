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
package com.netflix.conductor.tasks.spel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.core.execution.tasks.WorkflowSystemTask;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import com.alibaba.fastjson2.JSONObject;

import static com.netflix.conductor.common.metadata.tasks.TaskType.TASK_TYPE_SPEL;

@Component(TASK_TYPE_SPEL)
public class SpelTask extends WorkflowSystemTask {

    private static SpelExpressionParser spelExpressionParser;

    private static final Logger LOGGER = LoggerFactory.getLogger(SpelTask.class);

    public static final String TEMPLATE_PARAMETER_NAME = "template";

    public SpelTask() {
        super(TASK_TYPE_SPEL);
        spelExpressionParser = new SpelExpressionParser();
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
            Expression expression =
                    spelExpressionParser.parseExpression(templateStr, TEMPLATE_EXPRESSION);

            if (inputObj instanceof List<?>) {
                List<JSONObject> resultList = dealList(expression, inputObj);
                task.addOutput("result", resultList);
            } else {
                JSONObject result = dealOne(expression, inputObj);
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

    private JSONObject dealOne(Expression expression, Object inputObj) {
        // SPEL 支持直接解析对象，但是此时如果对象值为null将不会抛出异常，所以此处将对象转为Map，去除为null的字段，以便在解析时抛出异常
        StandardEvaluationContext context = new StandardEvaluationContext(convertObj(inputObj));
        context.addPropertyAccessor(new MapAccessor());
        String outStr = expression.getValue(context, String.class);
        return JSONObject.parseObject(outStr);
    }

    private List<JSONObject> dealList(Expression expression, Object inputObj) {
        List<Object> inputList = new ArrayList<>((List<Object>) inputObj);
        return inputList.parallelStream()
                .map(e -> dealOne(expression, e))
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

    private static final ParserContext TEMPLATE_EXPRESSION =
            new ParserContext() {
                @Override
                public boolean isTemplate() {
                    return true;
                }

                @Override
                public String getExpressionPrefix() {
                    return "{{";
                }

                @Override
                public String getExpressionSuffix() {
                    return "}}";
                }
            };

    @Override
    public boolean execute(WorkflowModel workflow, TaskModel task, WorkflowExecutor executor) {
        return true;
    }

    @Override
    public void cancel(WorkflowModel workflow, TaskModel task, WorkflowExecutor executor) {
        task.setStatus(TaskModel.Status.CANCELED);
    }
}
