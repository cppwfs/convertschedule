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

package io.spring.convertschedule.configuration;

import io.spring.convertschedule.ConvertScheduleService;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;

public class SchedulerProcessor<T> implements ItemProcessor {

	private ConvertScheduleService convertScheduleService;

	public SchedulerProcessor(ConvertScheduleService convertScheduleService) {
		this.convertScheduleService = convertScheduleService;
	}

	@Override
	public Object process(Object o){
		return this.convertScheduleService.enrichScheduleMetadata((ScheduleInfo ) o);
	}

}
