package com.dongbacsaigon.backend.catalog.service;

import java.util.UUID;

import com.dongbacsaigon.backend.catalog.repository.ProductRevisionImageRepository;
import com.dongbacsaigon.backend.media.service.MediaReferenceChecker;
import org.springframework.stereotype.Component;

@Component
public class CatalogMediaReferenceChecker implements MediaReferenceChecker {

    private final ProductRevisionImageRepository imageRepository;

    public CatalogMediaReferenceChecker(ProductRevisionImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @Override
    public boolean isReferenced(UUID mediaId) {
        return imageRepository.existsByMediaId(mediaId);
    }
}
