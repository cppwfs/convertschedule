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

package io.spring.migrateschedule.batch;

import java.util.List;

import io.jsonwebtoken.lang.Assert;
import io.spring.migrateschedule.service.ConvertScheduleInfo;
import io.spring.migrateschedule.service.MigrateScheduleService;


import org.springframework.batch.item.ItemReader;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;

public class SchedulerReader<T> implements ItemReader {

	private List<ConvertScheduleInfo> scheduleInfoList;

	private int currentOffset;

	private int scheduleCount;

	public SchedulerReader(MigrateScheduleService migrateScheduleService) {
		Assert.notNull(migrateScheduleService, "convertScheduleService must not be null");
		scheduleInfoList = migrateScheduleService.scheduleInfoList();
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
