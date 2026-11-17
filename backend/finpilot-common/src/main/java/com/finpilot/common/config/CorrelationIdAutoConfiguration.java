package com.finpilot.common.config;

import com.finpilot.common.http.CorrelationIdServletFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CorrelationIdAutoConfiguration {

    @Bean
    FilterRegistrationBean<CorrelationIdServletFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdServletFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new CorrelationIdServletFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}