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

public class ConverterProperties {
	private String schedulerTaskLauncherUrl = "maven://org.springframework.cloud:spring-cloud-dataflow-scheduler-task-launcher:2.3.0.BUILD-SNAPSHOT";

	/**
	 * The prefix for the updated schedules.
	 */
	private String schedulerPrefix = "scdf_";

	/**
	 * The prefix to attach to the application properties to be sent to the schedule task launcher.
	 */
	private String taskLauncherPrefix = "tasklauncher";

	private String dataflowServerUri = "http://localhost:9393";

	public String getSchedulerTaskLauncherUrl() {
		return schedulerTaskLauncherUrl;
	}

	public void setSchedulerTaskLauncherUrl(String schedulerTaskLauncherUrl) {
		this.schedulerTaskLauncherUrl = schedulerTaskLauncherUrl;
	}

	public String getSchedulerPrefix() {
		return schedulerPrefix;
	}

	public void setSchedulerPrefix(String schedulerPrefix) {
		this.schedulerPrefix = schedulerPrefix;
	}

	public String getTaskLauncherPrefix() {
		return taskLauncherPrefix;
	}

	public void setTaskLauncherPrefix(String taskLauncherPrefix) {
		this.taskLauncherPrefix = taskLauncherPrefix;
	}

	public String getDataflowServerUri() {
		return dataflowServerUri;
	}

	public void setDataflowServerUri(String dataflowServerUri) {
		this.dataflowServerUri = dataflowServerUri;
	}
}
