package com.gft.similarproducts.infrastructure.adapter.out.http;

import com.gft.similarproducts.domain.exception.ProductNotFoundException;
import com.gft.similarproducts.domain.model.ProductDetail;
import com.gft.similarproducts.domain.port.out.ProductClientPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class ProductHttpClientAdapter implements ProductClientPort {

    private static final String PRODUCT_DETAIL_INSTANCE = "productDetail";

    private final WebClient webClient;
    private final CircuitBreaker productDetailCircuitBreaker;
    private final TimeLimiter productDetailTimeLimiter;

    public ProductHttpClientAdapter(
            WebClient existingApisWebClient,
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        this.webClient = existingApisWebClient;
        this.productDetailCircuitBreaker = circuitBreakerRegistry.circuitBreaker(PRODUCT_DETAIL_INSTANCE);
        this.productDetailTimeLimiter = timeLimiterRegistry.timeLimiter(PRODUCT_DETAIL_INSTANCE);
    }

    @Override
    public Mono<ProductDetail> getProductDetail(String productId) {
        return webClient.get()
                .uri("/product/{productId}", productId)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND,
                        response -> Mono.error(new ProductNotFoundException(productId)))
                .bodyToMono(ProductDetail.class)
                .transformDeferred(TimeLimiterOperator.of(productDetailTimeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(productDetailCircuitBreaker));
    }

    @Override
    public Flux<String> getSimilarIds(String productId) {
        return webClient.get()
                .uri("/product/{productId}/similarids", productId)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND,
                        response -> Mono.error(new ProductNotFoundException(productId)))
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .flatMapMany(Flux::fromIterable);
    }
}
