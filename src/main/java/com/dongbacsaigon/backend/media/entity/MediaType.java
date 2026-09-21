package com.dongbacsaigon.backend.media.entity;

public enum MediaType {
    IMAGE("image"),
    VIDEO("video");

    private final String cloudinaryResourceType;

    MediaType(String cloudinaryResourceType) {
        this.cloudinaryResourceType = cloudinaryResourceType;
    }

    public String cloudinaryResourceType() {
        return cloudinaryResourceType;
    }
}
