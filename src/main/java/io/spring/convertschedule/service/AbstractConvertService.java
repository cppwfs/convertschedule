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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import io.spring.convertschedule.batch.AppResourceCommon;
import io.spring.convertschedule.batch.ConverterProperties;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

public abstract class AbstractConvertService implements ConvertScheduleService {

	private final static String DATA_FLOW_URI_KEY = "spring.cloud.dataflow.client.serverUri";

	private final static String COMMAND_ARGUMENT_PREFIX = "cmdarg.";

	protected final static String APP_PREFIX = "app.";

	protected final static String DEPLOYER_PREFIX = "deployer.";

	private ConverterProperties converterProperties;

	private TaskDefinitionRepository taskDefinitionRepository;

	public AbstractConvertService(ConverterProperties converterProperties, TaskDefinitionRepository taskDefinitionRepository) {
		this.converterProperties = converterProperties;
		this.taskDefinitionRepository = taskDefinitionRepository;
	}

	public TaskDefinition findTaskDefinitionByName(String taskDefinitionName) {
		return this.taskDefinitionRepository.findByTaskName(taskDefinitionName);
	}

	protected String getSchedulePrefix(String taskDefinitionName) {
		return converterProperties.getSchedulerPrefix() + taskDefinitionName;
	}

	/**
	 * Retain only properties that are meant for the <em>scheduler</em> of a given task(those
	 * that start with {@code scheduler.}and qualify all
	 * property values with the {@code spring.cloud.scheduler.} prefix.
	 *
	 * @param input the scheduler properties
	 * @return scheduler properties for the task
	 */
	protected static Map<String, String> extractAndQualifySchedulerProperties(Map<String, String> input) {
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

	protected List<String> tagCommandLineArgs(List<String> args) {
		List<String> taggedArgs = new ArrayList<>();

		for(String arg : args) {
			if(arg.contains("spring.cloud.task.name")) {
				continue;
			}
			String updatedArg = arg;
			if (!arg.startsWith(DATA_FLOW_URI_KEY) && !"--".concat(arg).startsWith(DATA_FLOW_URI_KEY)) {
				updatedArg = COMMAND_ARGUMENT_PREFIX +
						converterProperties.getTaskLauncherPrefix() + "." + arg;
			}
			taggedArgs.add(updatedArg);
		}
		return taggedArgs;
	}

	protected Map<String, String> tagProperties(String appName, Map<String, String> appProperties, String prefix) {
		Map<String, String> taggedAppProperties = new HashMap<>(appProperties.size());

		for(String key : appProperties.keySet()) {
			if(key.contains("spring.cloud.task.name")) {
				continue;
			}
			String updatedKey = key;
			if (!key.startsWith(DATA_FLOW_URI_KEY)) {
				if (StringUtils.hasText(appName)) {
					updatedKey = converterProperties.getTaskLauncherPrefix() + "." +
							prefix + appName + "." + key;
				}
				else {
					updatedKey = converterProperties.getTaskLauncherPrefix() + "." +
							prefix + key;
				}
			}
			taggedAppProperties.put(updatedKey, appProperties.get(key));
		}
		return taggedAppProperties;
	}

	protected Map<String, String> addSchedulerAppProps(Map<String, String> properties) {
		Map<String, String> appProperties = new HashMap<>(properties);
		appProperties.put("spring.cloud.dataflow.client.serverUri", converterProperties.getDataflowServerUri());
		return appProperties;
	}
}
