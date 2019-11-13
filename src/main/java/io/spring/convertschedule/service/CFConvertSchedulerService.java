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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pivotal.scheduler.SchedulerClient;
import io.pivotal.scheduler.v1.jobs.ListJobsRequest;
import io.pivotal.scheduler.v1.jobs.ListJobsResponse;
import io.spring.convertschedule.batch.ConvertScheduleInfo;
import io.spring.convertschedule.batch.ConverterProperties;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.GetApplicationEnvironmentsRequest;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.deployer.spi.scheduler.SchedulerException;
import org.springframework.cloud.deployer.spi.scheduler.SchedulerPropertyKeys;
import org.springframework.util.SocketUtils;
import org.springframework.util.StringUtils;

public class CFConvertSchedulerService extends AbstractConvertService {

	private final static int PCF_PAGE_START_NUM = 1; //First PageNum for PCFScheduler starts at 1.

	private final static String SCHEDULER_SERVICE_ERROR_MESSAGE = "Scheduler Service returned a null response.";

	private CloudFoundryOperations cloudFoundryOperations;

	private SchedulerClient schedulerClient;

	private CloudFoundryConnectionProperties properties;

	public CFConvertSchedulerService(CloudFoundryOperations cloudFoundryOperations,
			SchedulerClient schedulerClient,
			CloudFoundryConnectionProperties properties, ConverterProperties converterProperties,
			TaskDefinitionRepository taskDefinitionRepository) {
		super(converterProperties, taskDefinitionRepository);
		this.cloudFoundryOperations = cloudFoundryOperations;
		this.schedulerClient = schedulerClient;
		this.properties = properties;
	}

	@Override
	public List<ConvertScheduleInfo> scheduleInfoList() {
		List<ConvertScheduleInfo> result = new ArrayList<>();
		for (int i = PCF_PAGE_START_NUM; i <= getJobPageCount(); i++) {
			System.out.println("********PAGE COUNT -> " + i);
			List<ConvertScheduleInfo> scheduleInfoPage = getSchedules(i);
			if(scheduleInfoPage == null) {
				throw new SchedulerException(SCHEDULER_SERVICE_ERROR_MESSAGE);
			}
			result.addAll(scheduleInfoPage);
		}
		return result;
	}

	public List<ConvertScheduleInfo> getSchedules(int page) {
		Flux<ApplicationSummary> applicationSummaries = cacheAppSummaries();
		return this.getSpace(this.properties.getSpace()).flatMap(requestSummary -> {
			return this.schedulerClient.jobs().list(ListJobsRequest.builder()
					.spaceId(requestSummary.getId())
					.page(page)
					.detailed(true).build());
		})
				.flatMapIterable(jobs -> jobs.getResources())// iterate over the resources returned.
				.flatMap(job -> {
					return getApplication(applicationSummaries,
							job.getApplicationId()) // get the application name for each job.
							.map(optionalApp -> {
								ConvertScheduleInfo scheduleInfo = new ConvertScheduleInfo();
								scheduleInfo.setScheduleProperties(new HashMap<>());
								scheduleInfo.setScheduleName(job.getName());
								scheduleInfo.setTaskDefinitionName(optionalApp.getName());

								int locationOfArgs = job.getCommand().indexOf("org.springframework.boot.loader.JarLauncher") + "org.springframework.boot.loader.JarLauncher".length();
								String commandArgs = job.getCommand().substring(locationOfArgs);
								if (StringUtils.hasText(commandArgs)) {
									try {
										scheduleInfo.setCommandLineArgs(Arrays.asList(CommandLineUtils.translateCommandline(commandArgs)));
									}
									catch (Exception e) {
										throw new IllegalArgumentException(e);
									}
								}
								if (job.getJobSchedules() != null) {
									scheduleInfo.getScheduleProperties().put(SchedulerPropertyKeys.CRON_EXPRESSION,
											job.getJobSchedules().get(0).getExpression());
								}
								else {
									System.out.println(String.format("Job %s does not have an associated schedule", job.getName()));
								}
								return scheduleInfo;
							});
				}).collectList().block();
	}

	@Override
	public ConvertScheduleInfo enrichScheduleMetadata(ConvertScheduleInfo scheduleInfo) {
		ApplicationEnvironments environment = this.cloudFoundryOperations.applications().
				getEnvironments(GetApplicationEnvironmentsRequest.builder().
						name(scheduleInfo.getTaskDefinitionName()).
						build()).
				block();
		if (environment != null) {
			for (Map.Entry<String, Object> var : environment.getUserProvided().entrySet()) {
				scheduleInfo.getScheduleProperties().put(var.getKey(), (String) var.getValue());
			}
		}
		List<String> revisedCommandLineArgs = tagCommandLineArgs(scheduleInfo.getCommandLineArgs());
		revisedCommandLineArgs.add("--spring.cloud.scheduler.task.launcher.taskName=" + scheduleInfo.getTaskDefinitionName());
		scheduleInfo.setCommandLineArgs(revisedCommandLineArgs);
		Map<String, String> appProperties = null;
		try {
			appProperties = getSpringAppProperties(scheduleInfo.getScheduleProperties());
		}
		catch (Exception exception) {
			throw new IllegalArgumentException("Unable to parse SPRING_APPLICATION_JSON from USER VARIABLES", exception);
		}
		TaskDefinition taskDefinition = findTaskDefinitionByName(appProperties.get("spring.cloud.task.name"));
		if (appProperties.size() > 0 && taskDefinition == null) {
			throw new IllegalStateException(String.format("The schedule %s contains " +
							"properties but the task definition %s does not exist and thus can't be migrated",
					scheduleInfo.getScheduleName(), scheduleInfo.getTaskDefinitionName()));
		}
		appProperties = tagProperties(taskDefinition.getRegisteredAppName(), appProperties, APP_PREFIX);
		appProperties = addSchedulerAppProps(appProperties);
		scheduleInfo.setAppProperties(appProperties);
		return scheduleInfo;
	}

	@Override
	public void migrateSchedule(Scheduler scheduler, ConvertScheduleInfo scheduleInfo) {
		String scheduleName = scheduleInfo.getScheduleName() + "-" + getSchedulePrefix(scheduleInfo.getTaskDefinitionName());
		AppDefinition appDefinition = new AppDefinition(scheduleName, scheduleInfo.getAppProperties());
		Map<String, String> schedulerProperties = extractAndQualifySchedulerProperties(scheduleInfo.getScheduleProperties());
		ScheduleRequest scheduleRequest = new ScheduleRequest(appDefinition, schedulerProperties, new HashMap<>(), scheduleInfo.getCommandLineArgs(), scheduleName, getTaskLauncherResource());
		scheduler.schedule(scheduleRequest);
		scheduler.unschedule(scheduleInfo.getScheduleName());
	}

	/**
	 * Retrieves the number of pages that can be returned when retrieving a list of jobs.
	 * @return an int containing the number of available pages.
	 */
	private int getJobPageCount() {
		ListJobsResponse response = this.getSpace(this.properties.getSpace()).flatMap(requestSummary -> {
			return this.schedulerClient.jobs().list(ListJobsRequest.builder()
					.spaceId(requestSummary.getId())
					.detailed(false).build());
		}).block();
		if(response == null) {
			throw new SchedulerException(SCHEDULER_SERVICE_ERROR_MESSAGE);
		}
		return response.getPagination().getTotalPages();
	}

	private Map<String, String> getSpringAppProperties(Map<String, String> properties) throws Exception {
		Map<String, String> result;
		if(properties.containsKey("SPRING_APPLICATION_JSON")) {
			result = new ObjectMapper()
					.readValue(properties.get("SPRING_APPLICATION_JSON"), Map.class);
		}
		else {
			result = new HashMap<>();
		}
		return result;
	}

	/**
	 * Retrieve a {@link Mono} containing a {@link SpaceSummary} for the specified name.
	 *
	 * @param spaceName the name of space to search.
	 * @return the {@link SpaceSummary} associated with the spaceName.
	 */
	private Mono<SpaceSummary> getSpace(String spaceName) {
		return requestSpaces()
				.cache() //cache results from first call.
				.filter(space -> spaceName.equals(space.getName()))
				.singleOrEmpty()
				.cast(SpaceSummary.class);
	}

	/**
	 * Retrieve a {@link Flux} containing the available {@link SpaceSummary}s.
	 *
	 * @return {@link Flux} of {@link SpaceSummary}s.
	 */
	private Flux<SpaceSummary> requestSpaces() {
		return this.cloudFoundryOperations.spaces()
				.list();
	}

	/**
	 * Retrieve a cached {@link Flux} of {@link ApplicationSummary}s.
	 */
	private Flux<ApplicationSummary> cacheAppSummaries() {
		return requestListApplications()
				.cache(); //cache results from first call.  No need to re-retrieve each time.
	}

	/**
	 * Retrieve a  {@link Flux} of {@link ApplicationSummary}s.
	 */
	private Flux<ApplicationSummary> requestListApplications() {
		return this.cloudFoundryOperations.applications()
				.list();
	}

	/**
	 * Retrieve a {@link Mono} containing the {@link ApplicationSummary} associated with the appId.
	 *
	 * @param applicationSummaries {@link Flux} of {@link ApplicationSummary}s to filter.
	 * @param appId                the id of the {@link ApplicationSummary} to search.
	 */
	private Mono<ApplicationSummary> getApplication(Flux<ApplicationSummary> applicationSummaries,
			String appId) {
		return applicationSummaries
				.filter(application -> appId.equals(application.getId()))
				.singleOrEmpty();
	}
}
