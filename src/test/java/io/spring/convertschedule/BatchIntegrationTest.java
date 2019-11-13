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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import io.spring.convertschedule.batch.ConvertScheduleInfo;
import io.spring.convertschedule.configuration.BatchConfiguration;
import io.spring.convertschedule.service.ConvertScheduleService;
import io.spring.convertschedule.service.TaskDefinitionRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBatchTest
@SpringBootTest
@EnableAutoConfiguration(exclude = {CloudFoundryDeployerAutoConfiguration.class})
@ContextConfiguration(classes = { BatchIntegrationTest.BatchTestConfiguration.class, BatchConfiguration.class})
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
		DirtiesContextTestExecutionListener.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class BatchIntegrationTest {

	public static final String DEFAULT_SCHEDULE_NAME = "defaultScheduleName";
	public static final String DEFAULT_TASK_DEFINITION_NAME = "defaultTaskDefinitionName";
	public static final String DEFAULT_APP_NAME = "defaultAppName";

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Autowired
	private ConvertScheduleService convertScheduleService;

	@MockBean
	private TaskDefinitionRepository taskDefinitionRepository;

	@MockBean
	private Scheduler scheduler;

	@Test
	public void baseTest() {

		JobExecution jobExecution = jobLauncherTestUtils.launchStep(
				"step1", defaultJobParameters());
		Collection actualStepExecutions = jobExecution.getStepExecutions();
		ExitStatus actualJobExitStatus = jobExecution.getExitStatus();
		assertThat(actualStepExecutions.size()).isEqualTo(1);
		assertThat(actualJobExitStatus.getExitCode()).isEqualTo("COMPLETED");
		final ArgumentCaptor<Scheduler> schedulerArgumentCaptor = ArgumentCaptor.forClass(Scheduler.class);
		final ArgumentCaptor<ConvertScheduleInfo> convertScheduleInfoArgumentCaptor = ArgumentCaptor.forClass(ConvertScheduleInfo.class);
		verify(this.convertScheduleService, times(1)).enrichScheduleMetadata(convertScheduleInfoArgumentCaptor.capture());
		verify(this.convertScheduleService, times(1)).migrateSchedule(schedulerArgumentCaptor.capture(), convertScheduleInfoArgumentCaptor.capture());
	}

	private JobParameters defaultJobParameters() {
		JobParametersBuilder paramsBuilder = new JobParametersBuilder();
		return paramsBuilder.toJobParameters();
	}

	@Configuration
	public static class BatchTestConfiguration {
		@Bean
		public ConvertScheduleService convertScheduleService() {
			ConvertScheduleService convertScheduleService = mock(ConvertScheduleService.class);
			ConvertScheduleInfo convertScheduleInfo = new ConvertScheduleInfo();
			convertScheduleInfo.setScheduleName(DEFAULT_SCHEDULE_NAME);
			convertScheduleInfo.setTaskDefinitionName(DEFAULT_TASK_DEFINITION_NAME);
			convertScheduleInfo.setScheduleProperties(new HashMap<>());
			convertScheduleInfo.setRegisteredAppName(DEFAULT_APP_NAME);
			when(convertScheduleService.scheduleInfoList()).thenReturn(Collections.singletonList(convertScheduleInfo));
			when(convertScheduleService.enrichScheduleMetadata(any())).thenReturn(convertScheduleInfo);
			return convertScheduleService;
		}
	}
}
