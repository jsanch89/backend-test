package com.gft.similarproducts.infrastructure.adapter.out.http;

import com.gft.similarproducts.domain.exception.ProductNotFoundException;
import com.gft.similarproducts.domain.model.ProductDetail;
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

class ProductHttpClientAdapterTest {

    private MockWebServer server;
    private ProductHttpClientAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofMillis(300));
        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        adapter = new ProductHttpClientAdapter(webClient);
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
