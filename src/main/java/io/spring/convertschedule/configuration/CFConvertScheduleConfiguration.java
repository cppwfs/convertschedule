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
import io.spring.convertschedule.CFConvertSchedulerService;
import io.spring.convertschedule.ConvertScheduleService;
import io.spring.convertschedule.ConverterCloudFoundryConnectionProperties;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

@Configuration
@Profile("cf")
public class CFConvertScheduleConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ReactorSchedulerClient reactorSchedulerClient(ConnectionContext context,
			TokenProvider passwordGrantTokenProvider,
			ConverterCloudFoundryConnectionProperties properties) {
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
	ConvertScheduleService scheduleService(CloudFoundryOperations cloudFoundryOperations,
			SchedulerClient schedulerClient,
			ConverterCloudFoundryConnectionProperties properties) {
		return new CFConvertSchedulerService(cloudFoundryOperations,
				schedulerClient, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	public CloudFoundryOperations cloudFoundryOperations(CloudFoundryClient cloudFoundryClient,
			ConverterCloudFoundryConnectionProperties properties) {
		return DefaultCloudFoundryOperations.builder()
				.cloudFoundryClient(cloudFoundryClient)
				.organization(properties.getOrg())
				.space(properties.getSpace())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConfigurationProperties(prefix = ConverterCloudFoundryConnectionProperties.CLOUDFOUNDRY_PROPERTIES)
	public ConverterCloudFoundryConnectionProperties cloudFoundryConnectionProperties() {
		return new ConverterCloudFoundryConnectionProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public CloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorCloudFoundryClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public ConnectionContext connectionContext(ConverterCloudFoundryConnectionProperties properties) {
		return DefaultConnectionContext.builder()
				.apiHost(properties.getUrl().getHost())
				.skipSslValidation(properties.isSkipSslValidation())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public TokenProvider tokenProvider(ConverterCloudFoundryConnectionProperties properties) {
		return PasswordGrantTokenProvider.builder()
				.username(properties.getUsername())
				.password(properties.getPassword())
				.loginHint(properties.getLoginHint())
				.build();
	}
}
