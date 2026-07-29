package com.gft.similarproducts.domain.port.in;

import com.gft.similarproducts.domain.model.ProductDetail;
import reactor.core.publisher.Flux;

public interface GetSimilarProductsUseCase {

    Flux<ProductDetail> getSimilarProducts(String productId);
}
