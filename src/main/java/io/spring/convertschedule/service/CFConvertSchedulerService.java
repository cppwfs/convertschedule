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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pivotal.scheduler.SchedulerClient;
import io.pivotal.scheduler.v1.jobs.ListJobsRequest;
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
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.deployer.spi.scheduler.SchedulerPropertyKeys;

public class CFConvertSchedulerService extends AbstractConvertService {

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
		Flux<ApplicationSummary> applicationSummaries = cacheAppSummaries();
		return this.getSpace(this.properties.getSpace()).flatMap(requestSummary -> {
			return this.schedulerClient.jobs().list(ListJobsRequest.builder()
					.spaceId(requestSummary.getId())
					.page(1)
					.detailed(true).build());})
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
								scheduleInfo.setCommandLineArgs(job.getCommand().substring(locationOfArgs));
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
		for (Map.Entry<String, Object> var : environment.getUserProvided().entrySet()) {
			scheduleInfo.getScheduleProperties().put(var.getKey(), (String) var.getValue());
		}
		return scheduleInfo;
	}

	@Override
	public void migrateSchedule(Scheduler scheduler, ConvertScheduleInfo scheduleInfo) {
		String scheduleName = scheduleInfo.getScheduleName() + "-" + getSchedulePrefix(scheduleInfo.getTaskDefinitionName());
		Map<String, String> appProperties;
		try {
			appProperties = getSpringAppProperties(scheduleInfo.getScheduleProperties());
			TaskDefinition taskDefinition = findTaskDefinitionByName(appProperties.get("spring.cloud.task.name"));
			System.out.println(">>>>>" + taskDefinition.getRegisteredAppName());
			appProperties = tagProperties(taskDefinition.getRegisteredAppName(), appProperties, APP_PREFIX);
		}
		catch (Exception e) {
			appProperties = new HashMap<>();
		}
		appProperties = addSchedulerAppProps(appProperties);
		AppDefinition appDefinition = new AppDefinition(scheduleName, appProperties);
		Map<String, String> schedulerProperties = extractAndQualifySchedulerProperties(scheduleInfo.getScheduleProperties());
		List<String>  revisedCommandLineArgs = null;
		try {
			revisedCommandLineArgs = new ArrayList<>(Arrays.asList(CommandLineUtils.translateCommandline(scheduleInfo.getCommandLineArgs())));
			revisedCommandLineArgs = tagCommandLineArgs(revisedCommandLineArgs);
		}
		catch (Exception e) {
			revisedCommandLineArgs = new ArrayList<>();
		}
		revisedCommandLineArgs.add("--spring.cloud.scheduler.task.launcher.taskName=" + scheduleInfo.getTaskDefinitionName());
		ScheduleRequest scheduleRequest = new ScheduleRequest(appDefinition, schedulerProperties, new HashMap<>(), revisedCommandLineArgs, scheduleName, getTaskLauncherResource());
		scheduler.schedule(scheduleRequest);
		scheduler.unschedule(scheduleInfo.getScheduleName());
	}

	private Map<String, String> getSpringAppProperties(Map<String, String> properties) throws Exception{
		return  new ObjectMapper()
				.readValue(properties.get("SPRING_APPLICATION_JSON"), Map.class);
	}

	/**
	 * Retrieve a {@link Mono} containing a {@link SpaceSummary} for the specified name.
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
	 * @param applicationSummaries {@link Flux} of {@link ApplicationSummary}s to filter.
	 * @param appId the id of the {@link ApplicationSummary} to search.
	 */
	private Mono<ApplicationSummary> getApplication(Flux<ApplicationSummary> applicationSummaries,
			String appId) {
		return applicationSummaries
				.filter(application -> appId.equals(application.getId()))
				.singleOrEmpty();
	}
}
