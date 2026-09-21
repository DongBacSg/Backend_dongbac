package com.dongbacsaigon.backend.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugServiceTest {

    private final SlugService slugService = new SlugService();

    @Test
    void generatesUrlSafeSlugFromVietnameseName() {
        assertThat(slugService.normalize("  Phân bón lá ĐÔNG BẮC!  "))
                .isEqualTo("phan-bon-la-dong-bac");
    }
}
