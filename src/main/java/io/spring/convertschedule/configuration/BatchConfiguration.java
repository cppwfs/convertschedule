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

import io.spring.convertschedule.service.ConvertScheduleService;
import io.spring.convertschedule.batch.ConverterProperties;
import io.spring.convertschedule.batch.SchedulerProcessor;
import io.spring.convertschedule.batch.SchedulerReader;
import io.spring.convertschedule.batch.SchedulerWriter;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.deployer.spi.scheduler.ScheduleInfo;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
@EnableTask
public class BatchConfiguration {


	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

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
	public SchedulerReader<ScheduleInfo> itemReader(ConvertScheduleService scheduler) {
		return new SchedulerReader<>(scheduler);
	}

	@Bean
	public SchedulerWriter<ScheduleInfo> itemWriter(Scheduler scheduler) {
		SchedulerWriter<ScheduleInfo> result = new SchedulerWriter<>();
		result.setScheduler(scheduler);
		return result;
	}

	@Bean
	public SchedulerProcessor<ScheduleInfo> itemProcessor(ConvertScheduleService convertScheduleService ) {
		return new SchedulerProcessor<>(convertScheduleService);
	}

	@Bean
	@ConfigurationProperties
	public ConverterProperties converterProperties() {
		return new ConverterProperties();
	}
}
