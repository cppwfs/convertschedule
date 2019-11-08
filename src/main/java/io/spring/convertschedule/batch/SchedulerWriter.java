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

package io.spring.convertschedule.batch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.spring.convertschedule.service.ConvertScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

public class SchedulerWriter<T> implements ItemWriter {

	private static final Logger logger = LoggerFactory.getLogger(SchedulerWriter.class);

	private Scheduler scheduler;

	private ConvertScheduleService scheduleService;

	@Override
	public void write(List list) throws Exception {
		list.stream().forEach(item -> {
			ConvertScheduleInfo scheduleInfo = ((ConvertScheduleInfo) item);
			logger.info(">>>>" + item + "<<>>" + scheduleInfo.getCommandLineArgs());
			scheduleService.migrateSchedule(scheduler, scheduleInfo);
		});
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public void setScheduleService(ConvertScheduleService scheduleService) {
		this.scheduleService = scheduleService;
	}
}
