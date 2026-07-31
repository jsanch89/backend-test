package com.gft.similarproducts.infrastructure;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.gft.similarproducts.infrastructure.adapter.out.http.ProductHttpClientAdapter;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SimilarProductsE2ETest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort())
            .configureStaticDsl(true)
            .build();

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("existing-apis.base-url", wireMock::baseUrl);
        registry.add("resilience4j.timelimiter.instances.productDetail.timeout-duration", () -> "500ms");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private ProductHttpClientAdapter productHttpClientAdapter;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUpWebTestClient() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterEach
    void resetInfrastructure() {
        wireMock.resetAll();
        circuitBreakerRegistry.circuitBreaker("productDetail").reset();
        productHttpClientAdapter.evictAllCaches();
    }

    @Test
    void shouldReturnDetailsInOrderWhenSimilarProductsExist() {
        stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(okJson("[\"2\",\"3\"]")));
        stubFor(get(urlEqualTo("/product/2"))
                .willReturn(okJson("{\"id\":\"2\",\"name\":\"Product 2\",\"price\":10.0,\"availability\":true}")));
        stubFor(get(urlEqualTo("/product/3"))
                .willReturn(okJson("{\"id\":\"3\",\"name\":\"Product 3\",\"price\":20.0,\"availability\":false}")));

        webTestClient.get().uri("/product/1/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo("2")
                .jsonPath("$[0].name").isEqualTo("Product 2")
                .jsonPath("$[1].id").isEqualTo("3")
                .jsonPath("$[1].name").isEqualTo("Product 3");
    }

    @Test
    void shouldReturn404WhenBaseProductDoesNotExist() {
        stubFor(get(urlEqualTo("/product/unknown/similarids"))
                .willReturn(notFound()));

        webTestClient.get().uri("/product/unknown/similar")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturnEmptyListWhenProductHasNoSimilarProducts() {
        stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(okJson("[]")));

        webTestClient.get().uri("/product/1/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void shouldOmitSimilarProductWhenItsDetailIsNotFound() {
        stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(okJson("[\"2\",\"3\"]")));
        stubFor(get(urlEqualTo("/product/2"))
                .willReturn(notFound()));
        stubFor(get(urlEqualTo("/product/3"))
                .willReturn(okJson("{\"id\":\"3\",\"name\":\"Product 3\",\"price\":20.0,\"availability\":false}")));

        webTestClient.get().uri("/product/1/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo("3");
    }

    @Test
    void shouldDegradeGracefullyWhenAnUpstreamDetailTimesOut() {
        stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(okJson("[\"2\",\"3\"]")));
        stubFor(get(urlEqualTo("/product/2"))
                .willReturn(aResponse()
                        .withFixedDelay(1500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"2\",\"name\":\"Product 2\",\"price\":10.0,\"availability\":true}")));
        stubFor(get(urlEqualTo("/product/3"))
                .willReturn(okJson("{\"id\":\"3\",\"name\":\"Product 3\",\"price\":20.0,\"availability\":false}")));

        webTestClient.get().uri("/product/1/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo("3");
    }

    @Test
    void shouldReturn400WhenProductIdIsBlank() {
        webTestClient.get().uri("/product/{productId}/similar", " ")
                .exchange()
                .expectStatus().isBadRequest();
    }
}
