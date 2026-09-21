package com.dongbacsaigon.backend.media.cloudinary;

import java.math.BigDecimal;
import java.util.Map;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dongbacsaigon.backend.media.config.CloudinaryProperties;
import com.dongbacsaigon.backend.media.entity.MediaType;
import org.springframework.stereotype.Component;

@Component
class DefaultCloudinaryGateway implements CloudinaryGateway {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    DefaultCloudinaryGateway(CloudinaryProperties cloudinaryProperties) {
        this.cloudinaryProperties = cloudinaryProperties;
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudinaryProperties.cloudName(),
                "api_key", cloudinaryProperties.apiKey(),
                "api_secret", cloudinaryProperties.apiSecret(),
                "timeout", cloudinaryProperties.httpTimeoutSeconds(),
                "secure", true
        ));
    }

    @Override
    public String signUploadParameters(Map<String, Object> parameters) {
        return cloudinary.apiSignRequest(parameters, cloudinaryProperties.apiSecret());
    }

    @Override
    public CloudinaryAsset fetchResource(String publicId, MediaType mediaType) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.api().resource(publicId, ObjectUtils.asMap(
                    "resource_type", mediaType.cloudinaryResourceType()
            ));
            return toAsset(result, mediaType);
        } catch (Exception exception) {
            throw new IllegalStateException("Cloudinary resource verification failed.", exception);
        }
    }

    @Override
    public CloudinaryDeleteResult destroy(String publicId, MediaType mediaType) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", mediaType.cloudinaryResourceType(),
                    "invalidate", true,
                    "timeout", cloudinaryProperties.httpTimeoutSeconds() * 1_000,
                    "connect_timeout", cloudinaryProperties.httpTimeoutSeconds() * 1_000
            ));
            String deleteResult = String.valueOf(result.get("result"));
            return new CloudinaryDeleteResult(
                    "ok".equalsIgnoreCase(deleteResult) || "not found".equalsIgnoreCase(deleteResult),
                    deleteResult
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Cloudinary deletion failed.", exception);
        }
    }

    private CloudinaryAsset toAsset(Map<String, Object> result, MediaType fallbackMediaType) {
        String resourceType = stringValue(result.get("resource_type"));
        MediaType mediaType = "video".equalsIgnoreCase(resourceType) ? MediaType.VIDEO : fallbackMediaType;
        return new CloudinaryAsset(
                stringValue(result.get("asset_id")),
                stringValue(result.get("public_id")),
                mediaType,
                stringValue(result.get("format")),
                stringValue(result.get("secure_url")),
                integerValue(result.get("width")),
                integerValue(result.get("height")),
                longValue(result.get("bytes")),
                decimalValue(result.get("duration")),
                stringValue(result.get("folder"))
        );
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer integerValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private BigDecimal decimalValue(Object value) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return null;
    }
}
