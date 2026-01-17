package io.jonghyun.boilerplate.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = ["io.jonghyun.boilerplate"])
@EnableJpaRepositories(basePackages = ["io.jonghyun.boilerplate"])
internal class CoreJpaConfig
