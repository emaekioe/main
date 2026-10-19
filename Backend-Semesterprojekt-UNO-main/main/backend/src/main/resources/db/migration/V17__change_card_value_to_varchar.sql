ALTER TABLE card
ALTER COLUMN card_value TYPE varchar(255)
USING card_value::text;