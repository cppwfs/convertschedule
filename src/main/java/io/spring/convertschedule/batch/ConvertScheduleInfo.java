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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;

public class ConvertScheduleInfo extends ScheduleInfo {

	private List<String> commandLineArgs = new ArrayList<>();

	private String registeredAppName;

	private Map<String, String> appProperties = new HashMap<>();

	public List<String> getCommandLineArgs() {
		return commandLineArgs;
	}

	public void setCommandLineArgs(List<String> commandLineArgs) {
		this.commandLineArgs = commandLineArgs;
	}

	public String getRegisteredAppName() {
		return registeredAppName;
	}

	public void setRegisteredAppName(String registeredAppName) {
		this.registeredAppName = registeredAppName;
	}

	public Map<String, String> getAppProperties() {
		return appProperties;
	}

	public void setAppProperties(Map<String, String> appProperties) {
		this.appProperties = appProperties;
	}
}
