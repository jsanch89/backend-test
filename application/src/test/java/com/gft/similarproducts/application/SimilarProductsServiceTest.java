package com.gft.similarproducts.application;

import com.gft.similarproducts.domain.exception.ProductNotFoundException;
import com.gft.similarproducts.domain.model.ProductDetail;
import com.gft.similarproducts.domain.port.out.ProductClientPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimilarProductsServiceTest {

    private ProductClientPort productClientPort;
    private SimilarProductsService service;

    @BeforeEach
    void setUp() {
        productClientPort = mock(ProductClientPort.class);
        service = new SimilarProductsService(productClientPort);
    }

    @Test
    void shouldReturnSimilarProductsInOrder() {
        when(productClientPort.getSimilarIds("1")).thenReturn(Flux.just("2", "3"));
        when(productClientPort.getProductDetail(eq("2")))
                .thenReturn(Mono.just(new ProductDetail("2", "Product 2", 10.0, true)));
        when(productClientPort.getProductDetail(eq("3")))
                .thenReturn(Mono.just(new ProductDetail("3", "Product 3", 20.0, true)));

        StepVerifier.create(service.getSimilarProducts("1"))
                .expectNext(new ProductDetail("2", "Product 2", 10.0, true))
                .expectNext(new ProductDetail("3", "Product 3", 20.0, true))
                .verifyComplete();
    }

    @Test
    void shouldOmitSimilarProductWhenDetailFails() {
        when(productClientPort.getSimilarIds("1")).thenReturn(Flux.just("2", "3"));
        when(productClientPort.getProductDetail(eq("2")))
                .thenReturn(Mono.error(new RuntimeException("not found")));
        when(productClientPort.getProductDetail(eq("3")))
                .thenReturn(Mono.just(new ProductDetail("3", "Product 3", 20.0, true)));

        StepVerifier.create(service.getSimilarProducts("1"))
                .expectNext(new ProductDetail("3", "Product 3", 20.0, true))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenNoSimilarIds() {
        when(productClientPort.getSimilarIds("1")).thenReturn(Flux.empty());

        StepVerifier.create(service.getSimilarProducts("1"))
                .verifyComplete();
    }

    @Test
    void shouldPropagateExceptionWhenBaseProductNotFound() {
        when(productClientPort.getSimilarIds("unknown"))
                .thenReturn(Flux.error(new ProductNotFoundException("unknown")));

        StepVerifier.create(service.getSimilarProducts("unknown"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }
}
