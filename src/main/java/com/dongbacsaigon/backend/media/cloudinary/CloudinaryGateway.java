package com.dongbacsaigon.backend.media.cloudinary;

import java.util.Map;

import com.dongbacsaigon.backend.media.entity.MediaType;

public interface CloudinaryGateway {

    String signUploadParameters(Map<String, Object> parameters);

    CloudinaryAsset fetchResource(String publicId, MediaType mediaType);

    CloudinaryDeleteResult destroy(String publicId, MediaType mediaType);
}
