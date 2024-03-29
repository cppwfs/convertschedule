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

import io.spring.migrateschedule.service.ConvertScheduleInfo;
import io.spring.migrateschedule.service.MigrateScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.item.ItemWriter;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;

public class SchedulerWriter<T> implements ItemWriter {

	private static final Logger logger = LoggerFactory.getLogger(SchedulerWriter.class);

	private Scheduler scheduler;

	private MigrateScheduleService scheduleService;

	@Override
	public void write(List list) {
		list.stream().forEach(item -> {
			ConvertScheduleInfo scheduleInfo = ((ConvertScheduleInfo) item);
			scheduleService.migrateSchedule(scheduler, scheduleInfo);
			logger.info(String.format("Migrated Schedule %s ", scheduleInfo.getScheduleName()));
		});
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public void setScheduleService(MigrateScheduleService scheduleService) {
		this.scheduleService = scheduleService;
	}
}
