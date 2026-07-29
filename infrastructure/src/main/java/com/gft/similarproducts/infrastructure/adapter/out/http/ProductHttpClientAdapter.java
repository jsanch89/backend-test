package com.gft.similarproducts.infrastructure.adapter.out.http;

import com.gft.similarproducts.domain.exception.ProductNotFoundException;
import com.gft.similarproducts.domain.model.ProductDetail;
import com.gft.similarproducts.domain.port.out.ProductClientPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class ProductHttpClientAdapter implements ProductClientPort {

    private final WebClient webClient;

    public ProductHttpClientAdapter(WebClient existingApisWebClient) {
        this.webClient = existingApisWebClient;
    }

    @Override
    public Mono<ProductDetail> getProductDetail(String productId) {
        return webClient.get()
                .uri("/product/{productId}", productId)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND,
                        response -> Mono.error(new ProductNotFoundException(productId)))
                .bodyToMono(ProductDetail.class);
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
