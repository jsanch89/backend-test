package com.gft.similarproducts.domain.port.out;

import com.gft.similarproducts.domain.model.ProductDetail;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductClientPort {

    Mono<ProductDetail> getProductDetail(String productId);

    Flux<String> getSimilarIds(String productId);
}
