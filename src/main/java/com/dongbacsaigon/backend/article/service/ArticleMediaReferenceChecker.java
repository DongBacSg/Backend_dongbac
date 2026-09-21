package com.dongbacsaigon.backend.article.service;

import java.util.UUID;

import com.dongbacsaigon.backend.article.repository.ArticleRevisionMediaRepository;
import com.dongbacsaigon.backend.media.service.MediaReferenceChecker;
import org.springframework.stereotype.Component;

@Component
public class ArticleMediaReferenceChecker implements MediaReferenceChecker {

    private final ArticleRevisionMediaRepository repository;

    public ArticleMediaReferenceChecker(ArticleRevisionMediaRepository repository) { this.repository = repository; }

    @Override
    public boolean isReferenced(UUID mediaId) { return repository.existsByMediaId(mediaId); }
}
