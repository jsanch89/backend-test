package com.gft.similarproducts.infrastructure.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient existingApisWebClient(
            @Value("${existing-apis.base-url}") String baseUrl,
            @Value("${existing-apis.connect-timeout-ms:1000}") int connectTimeoutMs,
            @Value("${existing-apis.response-timeout-ms:2000}") long responseTimeoutMs,
            @Value("${existing-apis.max-connections:50}") int maxConnections,
            @Value("${existing-apis.pending-acquire-max-count:200}") int pendingAcquireMaxCount,
            @Value("${existing-apis.pending-acquire-timeout-ms:2000}") long pendingAcquireTimeoutMs) {

        ConnectionProvider connectionProvider = ConnectionProvider.builder("existing-apis")
                .maxConnections(maxConnections)
                .pendingAcquireMaxCount(pendingAcquireMaxCount)
                .pendingAcquireTimeout(Duration.ofMillis(pendingAcquireTimeoutMs))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(responseTimeoutMs));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
