package com.gft.similarproducts.infrastructure.adapter.in.rest;

import com.gft.similarproducts.domain.port.in.GetSimilarProductsUseCase;
import com.gft.similarproducts.infrastructure.generated.api.DefaultApi;
import com.gft.similarproducts.infrastructure.generated.model.ProductDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class SimilarProductsController implements DefaultApi {

    private final GetSimilarProductsUseCase getSimilarProductsUseCase;

    public SimilarProductsController(GetSimilarProductsUseCase getSimilarProductsUseCase) {
        this.getSimilarProductsUseCase = getSimilarProductsUseCase;
    }

    @Override
    public Mono<ResponseEntity<Flux<ProductDetail>>> getProductSimilar(String productId, ServerWebExchange exchange) {
        if (productId == null || productId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId must not be blank");
        }
        Flux<ProductDetail> body = getSimilarProductsUseCase.getSimilarProducts(productId)
                .map(ProductDetailMapper::toDto);
        return Mono.just(ResponseEntity.ok(body));
    }
}
