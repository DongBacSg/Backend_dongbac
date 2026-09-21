package com.dongbacsaigon.backend.catalog.repository;

import com.dongbacsaigon.backend.catalog.entity.ProductPublicationStatus;

public interface ProductPublicationStatusCount {
    ProductPublicationStatus getStatus();
    long getCount();
}
