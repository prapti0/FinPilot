package com.finpilot.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EventTransportProperties.class)
public class FinPilotCommonAutoConfiguration {}