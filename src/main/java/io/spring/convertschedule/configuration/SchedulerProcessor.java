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

import java.util.Map;

import io.pivotal.scheduler.SchedulerClient;
import io.pivotal.scheduler.v1.jobs.ListJobsRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.GetApplicationEnvironmentsRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.spaces.SpaceSummary;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;

public class SchedulerProcessor<T> implements ItemProcessor {

	private CloudFoundryOperations cloudFoundryOperations;

	private SchedulerClient schedulerClient;

	private CloudFoundryConnectionProperties properties;

	public SchedulerProcessor(CloudFoundryOperations cloudFoundryOperations,
			SchedulerClient schedulerClient,
			CloudFoundryConnectionProperties properties) {
		this.cloudFoundryOperations = cloudFoundryOperations;
		this.schedulerClient = schedulerClient;
		this.properties = properties;
		System.out.println(this.cloudFoundryOperations.applications().get(GetApplicationRequest.builder().name("grenfro-scdf-server").build()).block());
	}

	@Override
	public Object process(Object o){
		ScheduleInfo scheduleInfo = (ScheduleInfo ) o;
		ApplicationEnvironments environment = cloudFoundryOperations.applications().
				getEnvironments(GetApplicationEnvironmentsRequest.builder().
						name(scheduleInfo.getTaskDefinitionName()).
						build()).
				block();
		for (Map.Entry<String, Object> var : environment.getUserProvided().entrySet()) {
			scheduleInfo.getScheduleProperties().put(var.getKey(), (String) var.getValue());
		}

		this.getSpace(properties.getSpace()).flatMap(requestSummary -> {
			return this.schedulerClient.jobs().list(ListJobsRequest.builder()
					.spaceId(requestSummary.getId())
					.page(1)
					.detailed(true).build());}).block().getResources().stream().forEach(job -> {
						int locationOfArgs = job.getCommand().indexOf("org.springframework.boot.loader.JarLauncher") + "org.springframework.boot.loader.JarLauncher".length();
			System.out.println("******" + job.getName() + "<<<<>>>>>>" + job.getCommand().substring(locationOfArgs));
			System.out.println(">>>>>>" + job);
		});
		return scheduleInfo;
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
}
