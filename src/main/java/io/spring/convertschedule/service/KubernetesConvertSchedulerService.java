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

package io.spring.convertschedule.service;

import java.util.ArrayList;
import java.util.List;

import io.spring.convertschedule.batch.ConvertScheduleInfo;
import io.spring.convertschedule.batch.ConverterProperties;

import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("Kubernetes")
@Configuration
public class KubernetesConvertSchedulerService extends  AbstractConvertService {

	public KubernetesConvertSchedulerService(ConverterProperties converterProperties, TaskDefinitionRepository taskDefinitionRepository) {
		super(converterProperties, taskDefinitionRepository);
	}
	@Override
	public List<ConvertScheduleInfo> scheduleInfoList() {
		return new ArrayList<>();
	}

	@Override
	public ConvertScheduleInfo enrichScheduleMetadata(ConvertScheduleInfo scheduleInfo) {
		return scheduleInfo;
	}

	@Override
	public void migrateSchedule(Scheduler scheduler, ConvertScheduleInfo scheduleInfo) {

	}
}
