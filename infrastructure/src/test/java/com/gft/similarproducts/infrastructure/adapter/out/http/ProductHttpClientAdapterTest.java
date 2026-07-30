package com.gft.similarproducts.infrastructure.adapter.out.http;

import com.gft.similarproducts.domain.exception.ProductNotFoundException;
import com.gft.similarproducts.domain.model.ProductDetail;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ProductHttpClientAdapterTest {

    private MockWebServer server;
    private WebClient webClient;
    private ProductHttpClientAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofMillis(300));
        webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(
                CircuitBreakerConfig.custom().slidingWindowSize(20).build());
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.of(
                TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(500)).build());

        adapter = new ProductHttpClientAdapter(webClient, circuitBreakerRegistry, timeLimiterRegistry);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldReturnProductDetailOn200() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"1\",\"name\":\"Product 1\",\"price\":10.5,\"availability\":true}"));

        StepVerifier.create(adapter.getProductDetail("1"))
                .expectNext(new ProductDetail("1", "Product 1", 10.5, true))
                .verifyComplete();
    }

    @Test
    void shouldRaiseProductNotFoundOn404() {
        server.enqueue(new MockResponse().setResponseCode(404));

        StepVerifier.create(adapter.getProductDetail("unknown"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void shouldRaiseErrorOnResponseTimeout() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"1\",\"name\":\"Product 1\",\"price\":10.5,\"availability\":true}")
                .setBodyDelay(1, java.util.concurrent.TimeUnit.SECONDS));

        StepVerifier.create(adapter.getProductDetail("1"))
                .expectError()
                .verify();
    }

    @Test
    void shouldOpenCircuitBreakerAfterRepeatedFailures() {
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(4)
                        .minimumNumberOfCalls(4)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(10))
                        .build());
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.of(
                TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(500)).build());
        ProductHttpClientAdapter localAdapter =
                new ProductHttpClientAdapter(webClient, circuitBreakerRegistry, timeLimiterRegistry);

        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse().setResponseCode(500));
            StepVerifier.create(localAdapter.getProductDetail("1")).expectError().verify();
        }

        assertThat(circuitBreakerRegistry.circuitBreaker("productDetail").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void shouldReturnSimilarIdsOn200() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[\"2\",\"3\"]"));

        StepVerifier.create(adapter.getSimilarIds("1"))
                .expectNext("2")
                .expectNext("3")
                .verifyComplete();
    }

    @Test
    void shouldRaiseProductNotFoundWhenSimilarIdsReturns404() {
        server.enqueue(new MockResponse().setResponseCode(404));

        StepVerifier.create(adapter.getSimilarIds("unknown"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }
}
