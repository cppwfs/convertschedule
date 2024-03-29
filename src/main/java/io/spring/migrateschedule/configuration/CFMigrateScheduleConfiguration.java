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

package io.spring.migrateschedule.configuration;

import io.pivotal.reactor.scheduler.ReactorSchedulerClient;
import io.pivotal.scheduler.SchedulerClient;
import io.spring.migrateschedule.service.ConverterProperties;
import io.spring.migrateschedule.service.CFMigrateSchedulerService;
import io.spring.migrateschedule.service.MigrateScheduleService;
import io.spring.migrateschedule.service.TaskDefinitionRepository;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryTaskLauncher;
import org.springframework.cloud.deployer.spi.scheduler.cloudfoundry.CloudFoundryAppScheduler;
import org.springframework.cloud.deployer.spi.scheduler.cloudfoundry.CloudFoundrySchedulerProperties;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

@Configuration
@Profile("cf")
@EntityScan({
		"org.springframework.cloud.dataflow.core"
})
public class CFMigrateScheduleConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ReactorSchedulerClient reactorSchedulerClient(ConnectionContext context,
			TokenProvider passwordGrantTokenProvider,
			CloudFoundrySchedulerProperties properties) {
		return ReactorSchedulerClient.builder()
				.connectionContext(context)
				.tokenProvider(passwordGrantTokenProvider)
				.root(Mono.just(properties.getSchedulerUrl()))
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public Resource getMavenResource() {
		return new MavenResource.Builder(new MavenProperties())
				.groupId("org.springframework.cloud")
				.artifactId("spring-cloud-scheduler-spi-test-app")
				.classifier("exec")
				.version("1.0.0.RELEASE")
				.extension("jar")
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public CloudFoundrySchedulerProperties cloudFoundrySchedulerProperties() {
		return new CloudFoundrySchedulerProperties();
	}

	@Bean
	MigrateScheduleService scheduleService(CloudFoundryOperations cloudFoundryOperations,
			SchedulerClient schedulerClient,
			CloudFoundryConnectionProperties properties, ConverterProperties converterProperties,
			TaskDefinitionRepository taskDefinitionRepository) {
		return new CFMigrateSchedulerService(cloudFoundryOperations,
				schedulerClient, properties, converterProperties, taskDefinitionRepository);
	}
	@Bean
	public CloudFoundryAppScheduler scheduler(SchedulerClient client, CloudFoundryOperations operations,
			CloudFoundryConnectionProperties properties, TaskLauncher taskLauncher,
			CloudFoundrySchedulerProperties schedulerProperties) {
		return new CloudFoundryAppScheduler(client, operations, properties,  (CloudFoundryTaskLauncher) taskLauncher, schedulerProperties);
	}

}
