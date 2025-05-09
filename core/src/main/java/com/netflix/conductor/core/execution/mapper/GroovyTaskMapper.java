/*
 * Copyright 2021 Conductor Authors.
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
package com.netflix.conductor.core.execution.mapper;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.core.utils.ParametersUtils;
import com.netflix.conductor.model.TaskModel;

/**
 * An implementation of {@link TaskMapper} to map a {@link WorkflowTask} of type {@link
 * TaskType#JOIN} to a {@link TaskModel} of type {@link TaskType#JOIN}
 */
@Component
public class GroovyTaskMapper implements TaskMapper {

    public static final Logger LOGGER = LoggerFactory.getLogger(GroovyTaskMapper.class);
    private final ParametersUtils parametersUtils;

    public GroovyTaskMapper(ParametersUtils parametersUtils) {
        this.parametersUtils = parametersUtils;
    }

    @Override
    public String getTaskType() {
        return TaskType.GROOVY.name();
    }

    @Override
    public List<TaskModel> getMappedTasks(TaskMapperContext taskMapperContext) {
        LOGGER.debug("TaskMapperContext {} in GroovyTaskMapper", taskMapperContext);

        Map<String, Object> taskInput =
                parametersUtils.getTaskInputV2(
                        taskMapperContext.getWorkflowTask().getInputParameters(),
                        taskMapperContext.getWorkflowModel(),
                        taskMapperContext.getTaskId(),
                        null);

        TaskModel taskModel = taskMapperContext.createTaskModel();
        taskModel.setTaskType(TaskType.TASK_TYPE_GROOVY);
        taskModel.setStartTime(System.currentTimeMillis());
        taskModel.setInputData(taskInput);
        taskModel.setStatus(TaskModel.Status.IN_PROGRESS);
        return List.of(taskModel);
    }
}
