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
package com.netflix.conductor.tasks.groovy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.core.execution.tasks.WorkflowSystemTask;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import static com.netflix.conductor.common.metadata.tasks.TaskType.TASK_TYPE_GROOVY;

@Component(TASK_TYPE_GROOVY)
public class GroovyTask extends WorkflowSystemTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroovyTask.class);

    public GroovyTask() {
        super(TASK_TYPE_GROOVY);
        LOGGER.info("{} initialized...", getTaskType());
    }

    @Override
    public void start(WorkflowModel workflow, TaskModel task, WorkflowExecutor executor) {
        String expression = (String) task.getInputData().get("expression");
        Object inputObj = task.getInputData().get("input");
        try {
            Binding binding = new Binding();
            binding.setVariable("input", inputObj);
            GroovyShell shell = new GroovyShell(binding);
            Object result = shell.evaluate(expression);
            task.addOutput("result", result);
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

    @Override
    public boolean execute(WorkflowModel workflow, TaskModel task, WorkflowExecutor executor) {
        return true;
    }

    @Override
    public void cancel(WorkflowModel workflow, TaskModel task, WorkflowExecutor executor) {
        task.setStatus(TaskModel.Status.CANCELED);
    }
}
