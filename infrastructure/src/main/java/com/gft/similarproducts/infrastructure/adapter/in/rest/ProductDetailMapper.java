package com.gft.similarproducts.infrastructure.adapter.in.rest;

import com.gft.similarproducts.infrastructure.generated.model.ProductDetail;
import java.math.BigDecimal;

final class ProductDetailMapper {

    private ProductDetailMapper() {
    }

    static ProductDetail toDto(com.gft.similarproducts.domain.model.ProductDetail domain) {
        return new ProductDetail(
                domain.id(),
                domain.name(),
                BigDecimal.valueOf(domain.price()),
                domain.availability()
        );
    }
}
