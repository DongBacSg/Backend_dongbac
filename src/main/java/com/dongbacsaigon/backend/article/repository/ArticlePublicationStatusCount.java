package com.dongbacsaigon.backend.article.repository;

import com.dongbacsaigon.backend.article.entity.ArticlePublicationStatus;

public interface ArticlePublicationStatusCount {
    ArticlePublicationStatus getStatus();
    long getCount();
}
