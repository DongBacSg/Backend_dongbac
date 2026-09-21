package com.dongbacsaigon.backend.media.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class MediaUsageService {

    private final List<MediaReferenceChecker> mediaReferenceCheckers;

    public MediaUsageService(List<MediaReferenceChecker> mediaReferenceCheckers) {
        this.mediaReferenceCheckers = mediaReferenceCheckers;
    }

    public boolean isReferenced(UUID mediaId) {
        return mediaReferenceCheckers.stream().anyMatch(checker -> checker.isReferenced(mediaId));
    }
}
