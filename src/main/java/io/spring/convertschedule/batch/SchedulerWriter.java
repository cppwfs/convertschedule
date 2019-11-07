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

	@Autowired
	public ConverterProperties converterProperties;

	private static final Logger logger = LoggerFactory.getLogger(SchedulerWriter.class);

	private Scheduler scheduler;

	private String schedulePrefix = "scdf-";

	@Override
	public void write(List list) throws Exception {
		list.stream().forEach(item -> {
			ConvertScheduleInfo scheduleInfo = ((ConvertScheduleInfo) item);
			logger.info(item + "<<>>" + scheduleInfo.getCommandLineArgs());
			String scheduleName = scheduleInfo.getScheduleName() + "-" + getSchedulePrefix(scheduleInfo.getTaskDefinitionName());
			AppDefinition appDefinition = new AppDefinition(scheduleName, scheduleInfo.getScheduleProperties());
			Map<String, String> schedulerProperties = extractAndQualifySchedulerProperties(scheduleInfo.getScheduleProperties());
			List<String>  revisedCommandLineArgs = new ArrayList<String>();//TODO need to add command line args
			revisedCommandLineArgs.add("--spring.cloud.scheduler.task.launcher.taskName=" + scheduleInfo.getTaskDefinitionName());
			ScheduleRequest scheduleRequest = new ScheduleRequest(appDefinition, schedulerProperties, new HashMap<>(), revisedCommandLineArgs, scheduleName, getTaskLauncherResource());
			scheduler.schedule(scheduleRequest);
			scheduler.unschedule(scheduleInfo.getScheduleName());
		});
	}

	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	private String getSchedulePrefix(String taskDefinitionName) {
		return schedulePrefix + taskDefinitionName;
	}

	/**
	 * Retain only properties that are meant for the <em>scheduler</em> of a given task(those
	 * that start with {@code scheduler.}and qualify all
	 * property values with the {@code spring.cloud.scheduler.} prefix.
	 *
	 * @param input the scheduler properties
	 * @return scheduler properties for the task
	 */
	private static Map<String, String> extractAndQualifySchedulerProperties(Map<String, String> input) {
		final String prefix = "spring.cloud.scheduler.";

		Map<String, String> result = new TreeMap<>(input).entrySet().stream()
				.filter(kv -> kv.getKey().startsWith(prefix))
				.collect(Collectors.toMap(kv -> kv.getKey(), kv -> kv.getValue(),
						(fromWildcard, fromApp) -> fromApp));

		return result;
	}

	protected Resource getTaskLauncherResource() {
		final URI url;
		try {
			url = new URI(this.converterProperties.getSchedulerTaskLauncherUrl());
		}
		catch (URISyntaxException urise) {
			throw new IllegalStateException(urise);
		}
		AppResourceCommon appResourceCommon = new AppResourceCommon(new MavenProperties(), new DefaultResourceLoader());
		return appResourceCommon.getResource(this.converterProperties.getSchedulerTaskLauncherUrl());
	}
}
