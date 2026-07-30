package com.gft.similarproducts.infrastructure.adapter.in.rest;

import com.gft.similarproducts.infrastructure.generated.model.ProductDetail;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDetailMapperTest {

    @Test
    void mapsDomainProductDetailToGeneratedDto() {
        com.gft.similarproducts.domain.model.ProductDetail domain =
                new com.gft.similarproducts.domain.model.ProductDetail("1", "Product 1", 10.5, true);

        ProductDetail dto = ProductDetailMapper.toDto(domain);

        assertThat(dto.getId()).isEqualTo("1");
        assertThat(dto.getName()).isEqualTo("Product 1");
        assertThat(dto.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(10.5));
        assertThat(dto.getAvailability()).isTrue();
    }
}
