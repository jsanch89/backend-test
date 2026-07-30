package com.gft.similarproducts.infrastructure.adapter.in.rest;

import com.gft.similarproducts.domain.port.in.GetSimilarProductsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SimilarProductsControllerTest {

    private final GetSimilarProductsUseCase useCase = mock(GetSimilarProductsUseCase.class);
    private final SimilarProductsController controller = new SimilarProductsController(useCase);

    @Test
    void rejectsBlankProductIdWithoutCallingUseCase() {
        assertThatBlankProductIdIsRejected("   ");
        verifyNoInteractions(useCase);
    }

    @Test
    void rejectsNullProductIdWithoutCallingUseCase() {
        assertThatBlankProductIdIsRejected(null);
        verifyNoInteractions(useCase);
    }

    private void assertThatBlankProductIdIsRejected(String productId) {
        try {
            controller.getProductSimilar(productId, null);
        } catch (ResponseStatusException ex) {
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            return;
        }
        throw new AssertionError("Expected ResponseStatusException for productId=" + productId);
    }

    @Test
    void delegatesToUseCaseAndMapsDomainToDto() {
        when(useCase.getSimilarProducts("1"))
                .thenReturn(Flux.just(new com.gft.similarproducts.domain.model.ProductDetail("2", "Product 2", 10.0, true)));

        StepVerifier.create(controller.getProductSimilar("1", null))
                .assertNext(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK))
                .verifyComplete();
    }
}
