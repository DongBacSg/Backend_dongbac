package com.dongbacsaigon.backend.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.dongbacsaigon.backend.catalog.repository.ProductRevisionImageRepository;
import org.junit.jupiter.api.Test;

class CatalogMediaReferenceCheckerTest {

    @Test
    void productRevisionImageCountsAsMediaUsage() {
        ProductRevisionImageRepository repository = mock(ProductRevisionImageRepository.class);
        UUID mediaId = UUID.randomUUID();
        when(repository.existsByMediaId(mediaId)).thenReturn(true);

        assertThat(new CatalogMediaReferenceChecker(repository).isReferenced(mediaId)).isTrue();
    }
}
