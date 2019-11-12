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

package io.spring.convertschedule;


import java.util.HashMap;

import io.pivotal.scheduler.SchedulerClient;
import io.spring.convertschedule.batch.ConvertScheduleInfo;
import io.spring.convertschedule.batch.ConverterProperties;
import io.spring.convertschedule.service.CFConvertSchedulerService;
import io.spring.convertschedule.service.TaskDefinitionRepository;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.GetApplicationEnvironmentsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleRequest;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CFConvertScheduleConfigurationTests {

	public static final String DEFAULT_SCHEDULE_NAME = "defaultScheduleName";
	public static final String DEFAULT_TASK_DEFINITION_NAME = "defaultTaskDefinitionName";
	public static final String DEFAULT_APP_NAME = "defaultAppName";
	public static final String DEFAULT_CMD_ARG = "defaultCmd=WOW";

	private CFConvertSchedulerService cfConvertSchedulerService;
	private CloudFoundryOperations cloudFoundryOperations;
	private CloudFoundryConnectionProperties cloudFoundryConnectionProperties;
	private ConverterProperties converterProperties;
	private TaskDefinitionRepository taskDefinitionRepository;
	private SchedulerClient schedulerClient;
	private Scheduler scheduler;

	@BeforeEach
		public void setup() {
		this.cloudFoundryOperations = Mockito.mock(CloudFoundryOperations.class);
		this.schedulerClient = Mockito.mock(SchedulerClient.class);
		this.cloudFoundryConnectionProperties = new CloudFoundryConnectionProperties();
		this.converterProperties = new ConverterProperties();
		this.taskDefinitionRepository = Mockito.mock(TaskDefinitionRepository.class);
		this.scheduler = Mockito.mock(Scheduler.class);
		this.cfConvertSchedulerService = new CFConvertSchedulerService(this.cloudFoundryOperations,
				this.schedulerClient,
				this.cloudFoundryConnectionProperties, this.converterProperties,
				this.taskDefinitionRepository) ;

	}

	@Test
	public void testEnrichment() {
		ConvertScheduleInfo convertScheduleInfo = createFoundationConvertScheduleInfo();
		assertThat(convertScheduleInfo.getAppProperties().keySet().size()).isEqualTo(2);
		assertThat(convertScheduleInfo.getAppProperties().get("tasklauncher.app.defaultAppName.foo")).isEqualTo("bar");
		assertThat(convertScheduleInfo.getAppProperties().get("spring.cloud.dataflow.client.serverUri")).isEqualTo("http://localhost:9393");
		assertThat(convertScheduleInfo.getCommandLineArgs().size()).isEqualTo(2);
		assertThat(convertScheduleInfo.getCommandLineArgs().get(0)).isEqualTo("cmdarg.tasklauncher.defaultCmd=WOW");
		assertThat(convertScheduleInfo.getCommandLineArgs().get(1)).isEqualTo("--spring.cloud.scheduler.task.launcher.taskName=defaultTaskDefinitionName");
	}

	@Test
	public void testEnrichmentNoProps() {
		ConvertScheduleInfo convertScheduleInfo = new ConvertScheduleInfo();
		convertScheduleInfo.setScheduleName(DEFAULT_SCHEDULE_NAME);
		convertScheduleInfo.setTaskDefinitionName(DEFAULT_TASK_DEFINITION_NAME);
		convertScheduleInfo.setScheduleProperties(new HashMap<>());
		convertScheduleInfo.setRegisteredAppName(DEFAULT_APP_NAME);
		Mockito.when(cloudFoundryOperations.applications()).thenReturn(new NoPropertyApplication());
		TaskDefinition taskDefinition = TaskDefinition.TaskDefinitionBuilder
				.from(new TaskDefinition("fooTask", "foo"))
				.setTaskName(DEFAULT_TASK_DEFINITION_NAME)
				.setRegisteredAppName(DEFAULT_APP_NAME)
				.build();
		Mockito.when(this.taskDefinitionRepository.findByTaskName(Mockito.any())).thenReturn(taskDefinition);
		convertScheduleInfo = this.cfConvertSchedulerService.enrichScheduleMetadata(convertScheduleInfo);
		assertThat(convertScheduleInfo.getAppProperties().keySet().size()).isEqualTo(1);
		assertThat(convertScheduleInfo.getAppProperties().get("spring.cloud.dataflow.client.serverUri")).isEqualTo("http://localhost:9393");
		assertThat(convertScheduleInfo.getCommandLineArgs().size()).isEqualTo(1);
		assertThat(convertScheduleInfo.getCommandLineArgs().get(0)).isEqualTo("--spring.cloud.scheduler.task.launcher.taskName=defaultTaskDefinitionName");
	}

	@Test
	public void testNoTaskDefinition() {
		ConvertScheduleInfo convertScheduleInfo = new ConvertScheduleInfo();
		convertScheduleInfo.setScheduleName(DEFAULT_SCHEDULE_NAME);
		convertScheduleInfo.setTaskDefinitionName(DEFAULT_TASK_DEFINITION_NAME);
		convertScheduleInfo.setScheduleProperties(new HashMap<>());
		convertScheduleInfo.setRegisteredAppName(DEFAULT_APP_NAME);
		convertScheduleInfo.getCommandLineArgs().add(DEFAULT_CMD_ARG);
		Mockito.when(cloudFoundryOperations.applications()).thenReturn(new SinglePropertyApplication());
		assertThrows(IllegalStateException.class, () -> this.cfConvertSchedulerService.enrichScheduleMetadata(convertScheduleInfo));
	}


	@Test
	public void testMigrate() {
		ConvertScheduleInfo convertScheduleInfo = createFoundationConvertScheduleInfo();
		this.cfConvertSchedulerService.migrateSchedule(this.scheduler, convertScheduleInfo);
		final ArgumentCaptor<ScheduleRequest> scheduleRequestArgument = ArgumentCaptor.forClass(ScheduleRequest.class);
		final ArgumentCaptor<String> scheduleNameArg = ArgumentCaptor.forClass(String.class);
		verify(this.scheduler, times(1)).schedule(scheduleRequestArgument.capture());
		verify(this.scheduler, times(1)).unschedule(scheduleNameArg.capture());
		assertThat(scheduleRequestArgument.getValue().getScheduleName()).isEqualTo("defaultScheduleName-scdf-defaultTaskDefinitionName");
		assertThat(scheduleNameArg.getValue()).isEqualTo(DEFAULT_SCHEDULE_NAME);
	}

	private ConvertScheduleInfo createFoundationConvertScheduleInfo() {
		ConvertScheduleInfo convertScheduleInfo = new ConvertScheduleInfo();
		convertScheduleInfo.setScheduleName(DEFAULT_SCHEDULE_NAME);
		convertScheduleInfo.setTaskDefinitionName(DEFAULT_TASK_DEFINITION_NAME);
		convertScheduleInfo.setScheduleProperties(new HashMap<>());
		convertScheduleInfo.setRegisteredAppName(DEFAULT_APP_NAME);
		convertScheduleInfo.getCommandLineArgs().add(DEFAULT_CMD_ARG);
		Mockito.when(cloudFoundryOperations.applications()).thenReturn(new SinglePropertyApplication());
		TaskDefinition taskDefinition = TaskDefinition.TaskDefinitionBuilder
				.from(new TaskDefinition("fooTask", "foo"))
				.setTaskName(DEFAULT_TASK_DEFINITION_NAME)
				.setRegisteredAppName(DEFAULT_APP_NAME)
				.build();
		Mockito.when(this.taskDefinitionRepository.findByTaskName(Mockito.any())).thenReturn(taskDefinition);
		return this.cfConvertSchedulerService.enrichScheduleMetadata(convertScheduleInfo);
	}

	public static class SinglePropertyApplication extends AbstractApplications {

		@Override
		public Mono<ApplicationEnvironments> getEnvironments(GetApplicationEnvironmentsRequest getApplicationEnvironmentsRequest) {
			return Mono.just(ApplicationEnvironments.builder().userProvided("SPRING_APPLICATION_JSON", "{\"foo\":\"bar\"}").build());
		}
	}
	public static class NoPropertyApplication extends AbstractApplications {
		@Override
		public Mono<ApplicationEnvironments> getEnvironments(GetApplicationEnvironmentsRequest getApplicationEnvironmentsRequest) {
			return Mono.empty();
		}
	}
}
