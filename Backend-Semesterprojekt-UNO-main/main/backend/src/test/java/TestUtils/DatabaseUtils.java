package TestUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.UUID;

public class DatabaseUtils {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/unogame";

    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void deleteDeck(UUID deckId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM deck WHERE deck_id = ?")) {

            stmt.setObject(1, deckId);
            stmt.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteCardsByDeckId(UUID deckId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM card WHERE deck_id = ?")) {

            stmt.setObject(1, deckId);
            stmt.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteGameCardsByGameId(UUID gameId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM game_card WHERE game_id = ?")) {

            stmt.setObject(1, gameId);
            stmt.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean gameExists(UUID gameId) throws Exception {
        return exists(
                "SELECT COUNT(*) FROM game WHERE game_id = ?",
                gameId
        );
    }

    public static boolean playerExists(UUID playerId) throws Exception {
        return exists(
                "SELECT COUNT(*) FROM player WHERE player_id = ?",
                playerId
        );
    }

    public static boolean deckExists(UUID deckId) throws Exception {
        return exists(
                "SELECT COUNT(*) FROM deck WHERE deck_id = ?",
                deckId
        );
    }

    private static boolean exists(String sql, UUID id) throws Exception {

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 1;
            }
        }
    }

    public static int countCardsByDeck(UUID deckId) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM card WHERE deck_id = ?")) {

            stmt.setObject(1, deckId);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public static int countGameCardsByGameAndLocation(UUID gameId, String cardLocation) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM game_card WHERE game_id = ? AND card_location = ?")) {

            stmt.setObject(1, gameId);
            stmt.setString(2, cardLocation);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public static int countGameCardsByGameAndLocationAndPlayer(UUID gameId, String cardLocation, UUID playerId) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM game_card WHERE game_id = ? AND card_location = ? AND player_id = ?")) {

            stmt.setObject(1, gameId);
            stmt.setString(2, cardLocation);
            stmt.setObject(3, playerId);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public static boolean gameCardExistsWithLocation(
            UUID gameId,
            UUID cardId,
            String cardLocation
    ) throws Exception {

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     """
                             SELECT COUNT(*)
                             FROM game_card
                             WHERE game_id = ?
                               AND card_id = ?
                               AND card_location = ?
                             """
             )) {

            stmt.setObject(1, gameId);
            stmt.setObject(2, cardId);
            stmt.setString(3, cardLocation);

            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 1;
            }
        }
    }

    public static boolean cardExists(UUID cardId) throws Exception {
        return exists(
                "SELECT COUNT(*) FROM card WHERE card_id = ?",
                cardId
        );
    }

    public static String getCardImageName(UUID cardId) throws Exception {

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT image FROM card WHERE card_id = ?"
             )) {

            stmt.setObject(1, cardId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    throw new RuntimeException(
                            "No card found with id " + cardId
                    );
                }

                return rs.getString("image");
            }
        }
    }

    public static String getCardValueByImageName(String imageName) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT card_value FROM card WHERE image = ?"
             )) {

            stmt.setString(1, imageName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return rs.getString("card_value");
            }
        }
    }

    public static String getCardColorByImageName(String imageName) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT card_color FROM card WHERE image = ?"
             )) {

            stmt.setString(1, imageName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return rs.getString("card_color");
            }
        }
    }

    public static int getCardVariantByImageName(String imageName) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT variant FROM card WHERE image = ?"
             )) {

            stmt.setString(1, imageName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException(
                            "No card found with image " + imageName
                    );
                }

                return rs.getInt("variant");
            }
        }
    }

    public static String getLatestMoveTypeByPlayer(UUID playerId) throws Exception {

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     """
                     SELECT move_type
                     FROM move
                     WHERE player_id = ?
                     ORDER BY started_at DESC
                     LIMIT 1
                     """
             )) {

            stmt.setObject(1, playerId);

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                return rs.getString("move_type");
            }
        }
    }

    public static boolean databaseIsReachable() {

        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(1);
        } catch (Exception e) {
            return false;
        }
    }

    public static void runSqlScript(String resourcePath) throws Exception {

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             InputStream is = DatabaseUtils.class.getClassLoader().getResourceAsStream(resourcePath)) {

            if (is == null) {
                throw new RuntimeException("SQL file not found: " + resourcePath);
            }

            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            String[] queries = sql.split(";");

            for (String query : queries) {
                if (!query.trim().isEmpty()) {
                    stmt.execute(query.trim());
                }
            }
        }
    }
}