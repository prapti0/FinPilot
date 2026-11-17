package com.finpilot.common.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Parses Render / Heroku style {@code DATABASE_URL} (postgres://…).
 */
@Configuration
@ConditionalOnExpression("!'${DATABASE_URL:}'.isEmpty()")
public class RenderDataSourceConfiguration {

    @Bean
    @Primary
    public DataSource renderDataSource(org.springframework.core.env.Environment env) {
        String databaseUrl = env.getRequiredProperty("DATABASE_URL");
        URI uri = URI.create(databaseUrl.replace("postgres://", "postgresql://"));
        String userInfo = uri.getUserInfo();
        String username = "";
        String password = "";
        if (userInfo != null && userInfo.contains(":")) {
            String[] parts = userInfo.split(":", 2);
            username = urlDecode(parts[0]);
            password = urlDecode(parts[1]);
        }
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                + uri.getPath()
                + "?sslmode=require";

        HikariDataSource ds = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .build();
        ds.setMaximumPoolSize(4);
        ds.setMinimumIdle(1);
        ds.setConnectionTimeout(30_000);
        return ds;
    }

    private static String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}