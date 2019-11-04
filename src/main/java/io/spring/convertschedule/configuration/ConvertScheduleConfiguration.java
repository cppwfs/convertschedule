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

import io.pivotal.reactor.scheduler.ReactorSchedulerClient;
import io.pivotal.scheduler.SchedulerClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import reactor.core.publisher.Mono;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryTaskLauncher;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.deployer.spi.scheduler.cloudfoundry.CloudFoundryAppScheduler;
import org.springframework.cloud.deployer.spi.scheduler.cloudfoundry.CloudFoundrySchedulerProperties;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@EnableBatchProcessing
@EnableTask
public class ConvertScheduleConfiguration {

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;


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
	public Scheduler scheduler(ReactorSchedulerClient client,
			CloudFoundryOperations operations,
			CloudFoundryConnectionProperties properties,
			TaskLauncher taskLauncher,
			CloudFoundrySchedulerProperties schedulerProperties) {
		return new CloudFoundryAppScheduler(client, operations, properties,
				(CloudFoundryTaskLauncher) taskLauncher,
				schedulerProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public CloudFoundrySchedulerProperties cloudFoundrySchedulerProperties() {
		return new CloudFoundrySchedulerProperties();
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
	public Job importUserJob(Step step1) {
		return jobBuilderFactory.get("importUserJob")
				.incrementer(new RunIdIncrementer())
				.flow(step1)
				.end()
				.build();
	}

	@Bean
	public Step step1(SchedulerReader<ScheduleInfo> itemReader,
			SchedulerProcessor<ScheduleInfo> schedulerProcessor, SchedulerWriter writer) {
		return stepBuilderFactory.get("step1")
				.<ScheduleInfo, ScheduleInfo> chunk(10)
				.reader(itemReader)
				.processor(schedulerProcessor)
				.writer(writer)
				.build();
	}

	@Bean
	public SchedulerReader<ScheduleInfo> itemReader(Scheduler scheduler) {
		return new SchedulerReader<>(scheduler);
	}

	@Bean
	public SchedulerWriter<ScheduleInfo> itemWriter() {
		return new SchedulerWriter<>();
	}

	@Bean
	public SchedulerProcessor<ScheduleInfo> itemProcessor(CloudFoundryOperations cloudFoundryOperations, SchedulerClient client, CloudFoundryConnectionProperties properties ) {
		return new SchedulerProcessor<>(cloudFoundryOperations, client, properties);
	}


}
