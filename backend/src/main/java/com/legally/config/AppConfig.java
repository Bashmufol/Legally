package com.legally.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper(ObjectProvider<Jackson2ObjectMapperBuilder> builderProvider) {
        Jackson2ObjectMapperBuilder builder = builderProvider.getIfAvailable();
        if (builder != null) {
            return builder.build();
        }
        return new ObjectMapper();
    }

    @Bean
    public RestClient restClient() {
        var httpClient = HttpClients.custom().disableAutomaticRetries().build();
        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
