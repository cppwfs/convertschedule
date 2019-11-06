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

import java.io.Serializable;
import java.util.List;

import io.jsonwebtoken.lang.Assert;
import io.spring.convertschedule.ConvertScheduleInfo;
import io.spring.convertschedule.ConvertScheduleService;


import org.springframework.batch.item.ItemReader;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;

public class SchedulerReader<T> implements ItemReader {

	private List<ConvertScheduleInfo> scheduleInfoList;

	private int currentOffset;

	private int scheduleCount;

	public SchedulerReader(ConvertScheduleService convertScheduleService) {
		Assert.notNull(convertScheduleService, "convertScheduleService must not be null");
		scheduleInfoList = convertScheduleService.scheduleInfoList();
		scheduleCount = scheduleInfoList.size();
	}

	@Override
	public Object read() throws Exception {
		ScheduleInfo result = null;
		if(currentOffset < scheduleCount) {
			result =  scheduleInfoList.get(currentOffset++);
		}
		return result;
	}
}
