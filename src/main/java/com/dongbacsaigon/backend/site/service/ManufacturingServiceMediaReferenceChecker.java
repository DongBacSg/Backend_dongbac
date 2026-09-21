package com.dongbacsaigon.backend.site.service;

import java.util.UUID;

import com.dongbacsaigon.backend.media.service.MediaReferenceChecker;
import com.dongbacsaigon.backend.site.repository.ManufacturingServiceRepository;
import org.springframework.stereotype.Component;

@Component
class ManufacturingServiceMediaReferenceChecker implements MediaReferenceChecker {

    private final ManufacturingServiceRepository repository;

    ManufacturingServiceMediaReferenceChecker(ManufacturingServiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isReferenced(UUID mediaId) {
        return repository.existsByFeaturedMediaId(mediaId);
    }
}
