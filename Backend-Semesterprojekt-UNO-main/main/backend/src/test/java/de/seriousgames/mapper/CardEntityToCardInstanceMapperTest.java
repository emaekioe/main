package de.seriousgames.mapper;

import de.seriousgames.model.CardEntity;
import de.seriousgames.model.enumvalues.CardColor;
import de.seriousgames.model.enumvalues.CardValue;
import de.seriousgames.uno.model.CardInstance;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CardEntityToCardInstanceMapperTest {

    @Test
    void shouldMapFullCardCorrectly() {
        CardEntity entity = createEntity(CardColor.RED, CardValue.FIVE, 2);

        CardInstance result = CardEntityToCardInstanceMapper.mapGameCardToCardInstance(entity);

        assertEquals(CardInstance.ColorEnum.RED, result.getColor());
        assertEquals(CardInstance.ValueEnum._5, result.getValue());
        assertEquals(CardInstance.VariantEnum.NUMBER_2, result.getVariant());
    }

    @Test
    void shouldMapAllColors() {
        assertEquals(CardInstance.ColorEnum.RED,
                map(CardColor.RED).getColor());

        assertEquals(CardInstance.ColorEnum.BLUE,
                map(CardColor.BLUE).getColor());

        assertEquals(CardInstance.ColorEnum.GREEN,
                map(CardColor.GREEN).getColor());

        assertEquals(CardInstance.ColorEnum.YELLOW,
                map(CardColor.YELLOW).getColor());

        assertEquals(CardInstance.ColorEnum.BLACK,
                map(CardColor.BLACK).getColor());
    }

    @Test
    void shouldMapAllValues() {
        assertEquals(CardInstance.ValueEnum._0, map(CardValue.ZERO).getValue());
        assertEquals(CardInstance.ValueEnum._1, map(CardValue.ONE).getValue());
        assertEquals(CardInstance.ValueEnum._2, map(CardValue.TWO).getValue());
        assertEquals(CardInstance.ValueEnum._3, map(CardValue.THREE).getValue());
        assertEquals(CardInstance.ValueEnum._4, map(CardValue.FOUR).getValue());
        assertEquals(CardInstance.ValueEnum._5, map(CardValue.FIVE).getValue());
        assertEquals(CardInstance.ValueEnum._6, map(CardValue.SIX).getValue());
        assertEquals(CardInstance.ValueEnum._7, map(CardValue.SEVEN).getValue());
        assertEquals(CardInstance.ValueEnum._8, map(CardValue.EIGHT).getValue());
        assertEquals(CardInstance.ValueEnum._9, map(CardValue.NINE).getValue());

        assertEquals(CardInstance.ValueEnum.REVERSE, map(CardValue.REVERSE).getValue());
        assertEquals(CardInstance.ValueEnum._22, map(CardValue.PLUS_TWO).getValue());
        assertEquals(CardInstance.ValueEnum._32, map(CardValue.PLUS_THREE).getValue());
        assertEquals(CardInstance.ValueEnum._12, map(CardValue.MINUS_ONE).getValue());
        assertEquals(CardInstance.ValueEnum.HOURGLASS, map(CardValue.HOURGLASS).getValue());
        assertEquals(CardInstance.ValueEnum.CLOVERLEAF, map(CardValue.CLOVERLEAF).getValue());
    }

    @Test
    void shouldMapAllVariants() {
        assertEquals(CardInstance.VariantEnum.NUMBER_1, map(1).getVariant());
        assertEquals(CardInstance.VariantEnum.NUMBER_2, map(2).getVariant());
        assertEquals(CardInstance.VariantEnum.NUMBER_3, map(3).getVariant());
        assertEquals(CardInstance.VariantEnum.NUMBER_4, map(4).getVariant());
    }

    @Test
    void shouldHandleNullsGracefully() {
        CardEntity entity = createEntity(null, null, null);

        CardInstance result = CardEntityToCardInstanceMapper.mapGameCardToCardInstance(entity);

        assertNull(result.getColor());
        assertNull(result.getValue());
        assertNull(result.getVariant());
    }


    private CardInstance map(CardColor color) {
        return CardEntityToCardInstanceMapper.mapGameCardToCardInstance(
                createEntity(color, CardValue.FIVE, 1)
        );
    }

    private CardInstance map(CardValue value) {
        return CardEntityToCardInstanceMapper.mapGameCardToCardInstance(
                createEntity(CardColor.RED, value, 1)
        );
    }

    private CardInstance map(Integer variant) {
        return CardEntityToCardInstanceMapper.mapGameCardToCardInstance(
                createEntity(CardColor.RED, CardValue.FIVE, variant)
        );
    }

    private CardEntity createEntity(CardColor color, CardValue value, Integer variant) {
        CardEntity entity = new CardEntity();
        entity.setCardId(UUID.randomUUID());
        entity.setImage("img.png");
        entity.setCardColor(color);
        entity.setCardValue(value);
        entity.setVariant(variant);
        return entity;
    }
}