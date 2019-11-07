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

import java.util.List;

import io.spring.convertschedule.batch.ConvertScheduleInfo;

import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;

public interface ConvertScheduleService {

	/**
	 * Retrieve all available {@link ScheduleInfo}s.
	 * @return list of available ScheduleInfos
	 */
	List<ConvertScheduleInfo> scheduleInfoList();

	/**
	 * Add properties and commandLine args to the {@link ScheduleInfo}
	 * @return enriched {@link ScheduleInfo}
	 */
	ConvertScheduleInfo enrichScheduleMetadata(ConvertScheduleInfo scheduleInfo);
}