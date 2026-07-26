package com.alexastudillo.geographicreference.api.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record CountryNameLookupApiResponse(
        String codeType,
        String code,
        String languageTag,
        String nameType,
        String name,
        boolean preferred
) {
}
