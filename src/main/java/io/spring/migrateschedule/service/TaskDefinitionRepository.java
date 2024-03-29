/*
 * Copyright 2019 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.spring.migrateschedule.service;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository to access {@link org.springframework.cloud.dataflow.core.TaskDefinition}s.
 *
 * @author Michael Minella
 * @author Gunnar Hillert
 */
@Transactional
public interface TaskDefinitionRepository extends PagingAndSortingRepository<org.springframework.cloud.dataflow.core.TaskDefinition, String> {

	Page<org.springframework.cloud.dataflow.core.TaskDefinition> findByTaskNameContains(String taskName, Pageable pageable);

	/**
	 * Performs a findByName query and throws an exception if the name is not found.
	 * @param name the name of the task definition
	 * @return The task definition instance or {@link NoSuchTaskDefinitionException} if not found.
	 */
	TaskDefinition findByTaskName(String name);
}
