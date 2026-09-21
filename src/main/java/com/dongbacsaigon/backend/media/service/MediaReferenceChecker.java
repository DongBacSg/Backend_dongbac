package com.dongbacsaigon.backend.media.service;

import java.util.UUID;

public interface MediaReferenceChecker {

    boolean isReferenced(UUID mediaId);
}
