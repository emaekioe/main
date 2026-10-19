package de.seriousgames.control;

import de.seriousgames.config.DeckLoader;
import de.seriousgames.model.Card;
import de.seriousgames.model.CardEntity;
import de.seriousgames.model.DeckDefinition;
import de.seriousgames.model.DeckEntity;
import de.seriousgames.model.enumvalues.CardColor;
import de.seriousgames.model.enumvalues.CardValue;
import de.seriousgames.model.exception.DeckValidationException;
import de.seriousgames.persistence.CardRepository;
import de.seriousgames.persistence.DeckRepository;
import de.seriousgames.uno.model.DeckCreatedResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DeckUploadBA {

    private final DeckLoader deckLoader;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;

    @Value("${app.deck-image-upload-dir}")
    private String uploadDir;

    public DeckUploadBA(DeckLoader deckLoader, DeckRepository deckRepository, CardRepository cardRepository) {
        this.deckLoader = deckLoader;
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
    }

    public DeckCreatedResponse uploadDeck(MultipartFile deckJsonFile, List<MultipartFile> imageFiles) throws Exception {
        DeckDefinition deck = deckLoader.load(deckJsonFile.getInputStream());

        UUID deckId = UUID.randomUUID();

        validateUploadedImages(deck, imageFiles);
        saveImages(imageFiles, deckId);
        DeckEntity deckEntity = saveDeck(deck, deckId);
        saveCards(deck, deckEntity);

        DeckCreatedResponse response = new DeckCreatedResponse();
        response.setDeckId(deckId);
        response.setDeckName(deck.getName());

        return response;
    }

    private void validateUploadedImages(DeckDefinition deck, List<MultipartFile> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            throw new DeckValidationException("Deck images are missing");
        }

        Set<String> uploadedImageNames = imageFiles.stream()
                .map(MultipartFile::getOriginalFilename)
                .collect(Collectors.toSet());

        List<String> missingImageNames = deck.getCards().stream()
                .map(de.seriousgames.model.Card::getImageName)
                .filter(imageName -> !uploadedImageNames.contains(imageName))
                .distinct()
                .toList();

        if (!missingImageNames.isEmpty()) {
            throw new DeckValidationException(
                    "Missing image files: " + String.join(", ", missingImageNames)
            );
        }
    }

    private void saveImages(List<MultipartFile> imageFiles, UUID deckId) throws Exception {
        Path uploadPath = Path.of(uploadDir, deckId.toString());
        Files.createDirectories(uploadPath);

        for (MultipartFile imageFile : imageFiles) {
            if (imageFile.isEmpty()) {
                continue;
            }

            String originalFilename = imageFile.getOriginalFilename();

            if (originalFilename == null || originalFilename.isBlank()) {
                continue;
            }

            Path targetPath = uploadPath.resolve(originalFilename).normalize();

            Files.copy(
                    imageFile.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }

    private DeckEntity saveDeck(DeckDefinition deck, UUID deckId) {
        DeckEntity deckEntity = DeckEntity.builder()
                .deckId(deckId)
                .deckName(deck.getName())
                .build();

        deckRepository.save(deckEntity);

        return deckEntity;
    }

    private void saveCards(DeckDefinition deck, DeckEntity deckEntity) {
        for (Card card : deck.getCards()) {
            UUID cardId = UUID.randomUUID();

            CardEntity cardEntity = CardEntity.builder()
                    .cardId(cardId)
                    .cardValue(CardValue.fromDbValue(card.getValue()))
                    .cardColor(CardColor.fromColor(card.getColor()))
                    .image(card.getImageName())
                    .variant(card.getVariant())
                    .deck(deckEntity)
                    .build();

            cardRepository.save(cardEntity);
        }
    }
}