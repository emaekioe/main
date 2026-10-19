package de.seriousgames.model.converter;

import de.seriousgames.model.enumvalues.CardValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class CardValueConverter implements AttributeConverter<CardValue, String> {

    @Override
    public String convertToDatabaseColumn(CardValue attribute) {
        if (attribute == null) {
            return null;
        }

        return attribute.getDbValue();
    }

    @Override
    public CardValue convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        return CardValue.fromDbValue(dbData);
    }
}
