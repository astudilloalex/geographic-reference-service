package com.alexastudillo.geographicreference.domain.model.projection;

import com.alexastudillo.geographicreference.domain.model.enums.CountryCodeType;
import com.alexastudillo.geographicreference.domain.model.enums.GeographicNameType;
import com.alexastudillo.geographicreference.domain.model.valobj.LanguageTag;

/**
 * Read projection for a localized country name identified by a selected ISO code.
 */
public record CountryNameLookup(
        CountryCodeType codeType,
        String code,
        LanguageTag languageTag,
        GeographicNameType nameType,
        String name,
        boolean preferred
) {
}
