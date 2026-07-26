package com.alexastudillo.geographicreference.application.dto;

/**
 * Country-name lookup response using the requested ISO code type.
 */
public record CountryNameLookupResponse(
        String codeType,
        String code,
        String languageTag,
        String nameType,
        String name,
        boolean preferred
) {
}
