package com.gft.similarproducts.infrastructure.adapter.out.http;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.gft.similarproducts.domain.exception.ProductNotFoundException;
import com.gft.similarproducts.domain.model.ProductDetail;
import com.gft.similarproducts.domain.port.out.ProductClientPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
public class ProductHttpClientAdapter implements ProductClientPort {

    private static final String PRODUCT_DETAIL_INSTANCE = "productDetail";

    private final WebClient webClient;
    private final CircuitBreaker productDetailCircuitBreaker;
    private final TimeLimiter productDetailTimeLimiter;
    private final Duration cacheTtl;
    private final Cache<String, Mono<ProductDetail>> productDetailCache;
    private final Cache<String, Mono<List<String>>> similarIdsCache;

    public ProductHttpClientAdapter(
            WebClient existingApisWebClient,
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry,
            @Value("${cache.ttl-seconds:60}") long cacheTtlSeconds,
            @Value("${cache.max-size:10000}") long cacheMaxSize) {
        this.webClient = existingApisWebClient;
        this.productDetailCircuitBreaker = circuitBreakerRegistry.circuitBreaker(PRODUCT_DETAIL_INSTANCE);
        this.productDetailTimeLimiter = timeLimiterRegistry.timeLimiter(PRODUCT_DETAIL_INSTANCE);
        this.cacheTtl = Duration.ofSeconds(cacheTtlSeconds);
        this.productDetailCache = Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(this.cacheTtl)
                .build();
        this.similarIdsCache = Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(this.cacheTtl)
                .build();
    }

    @Override
    public Mono<ProductDetail> getProductDetail(String productId) {
        return productDetailCache.get(productId, this::fetchProductDetail);
    }

    @Override
    public Flux<String> getSimilarIds(String productId) {
        return similarIdsCache.get(productId, this::fetchSimilarIds)
                .flatMapMany(Flux::fromIterable);
    }

    private Mono<ProductDetail> fetchProductDetail(String productId) {
        return webClient.get()
                .uri("/product/{productId}", productId)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND,
                        response -> Mono.error(new ProductNotFoundException(productId)))
                .bodyToMono(ProductDetail.class)
                .transformDeferred(TimeLimiterOperator.of(productDetailTimeLimiter))
                .transformDeferred(CircuitBreakerOperator.of(productDetailCircuitBreaker))
                .cache(value -> cacheTtl, error -> Duration.ZERO, () -> Duration.ZERO);
    }

    private Mono<List<String>> fetchSimilarIds(String productId) {
        return webClient.get()
                .uri("/product/{productId}/similarids", productId)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND,
                        response -> Mono.error(new ProductNotFoundException(productId)))
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .cache(value -> cacheTtl, error -> Duration.ZERO, () -> Duration.ZERO);
    }

    public void evictAllCaches() {
        productDetailCache.invalidateAll();
        similarIdsCache.invalidateAll();
    }
}
