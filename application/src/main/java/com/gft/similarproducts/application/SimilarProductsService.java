package com.gft.similarproducts.application;

import com.gft.similarproducts.domain.model.ProductDetail;
import com.gft.similarproducts.domain.port.in.GetSimilarProductsUseCase;
import com.gft.similarproducts.domain.port.out.ProductClientPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SimilarProductsService implements GetSimilarProductsUseCase {

    private static final int DETAIL_CONCURRENCY = 8;

    private final ProductClientPort productClientPort;

    public SimilarProductsService(ProductClientPort productClientPort) {
        this.productClientPort = productClientPort;
    }

    @Override
    public Flux<ProductDetail> getSimilarProducts(String productId) {
        return productClientPort.getSimilarIds(productId)
                .flatMapSequential(
                        id -> productClientPort.getProductDetail(id).onErrorResume(e -> Mono.empty()),
                        DETAIL_CONCURRENCY
                );
    }
}
