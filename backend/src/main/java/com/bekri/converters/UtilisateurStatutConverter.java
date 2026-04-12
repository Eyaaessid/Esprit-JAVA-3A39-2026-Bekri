package com.bekri.converters;

import com.bekri.enums.UtilisateurStatut;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class UtilisateurStatutConverter implements AttributeConverter<UtilisateurStatut, String> {

    @Override
    public String convertToDatabaseColumn(UtilisateurStatut attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public UtilisateurStatut convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return UtilisateurStatut.fromDbValue(dbData);
    }
}

