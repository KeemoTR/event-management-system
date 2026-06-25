package eventsystem.dao;

import eventsystem.model.Rating;
import eventsystem.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RatingDAO {

    public List<Rating> getAllRatings() {
        List<Rating> ratings = new ArrayList<>();

        String sql = "SELECT * FROM ratings";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                Rating rating = new Rating();

                rating.setId(resultSet.getInt("id"));
                rating.setStudentId(resultSet.getInt("student_id"));
                rating.setEventId(resultSet.getInt("event_id"));
                rating.setRating(resultSet.getInt("rating"));
                rating.setComment(resultSet.getString("comment"));

                Timestamp createdAt = resultSet.getTimestamp("created_at");
                if (createdAt != null) {
                    rating.setCreatedAt(createdAt.toLocalDateTime());
                }

                ratings.add(rating);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ratings;
    }

    public List<Rating> getAllRatings(Connection connection) {
        List<Rating> ratings = new ArrayList<>();
        String sql = "SELECT * FROM ratings";

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                Rating rating = new Rating();

                rating.setId(resultSet.getInt("id"));
                rating.setStudentId(resultSet.getInt("student_id"));
                rating.setEventId(resultSet.getInt("event_id"));
                rating.setRating(resultSet.getInt("rating"));
                rating.setComment(resultSet.getString("comment"));

                Timestamp createdAt = resultSet.getTimestamp("created_at");
                if (createdAt != null) {
                    rating.setCreatedAt(createdAt.toLocalDateTime());
                }

                ratings.add(rating);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ratings;
    }

    public Rating getRatingById(int id) {
        String sql = "SELECT * FROM ratings WHERE id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Rating rating = new Rating();

                    rating.setId(resultSet.getInt("id"));
                    rating.setStudentId(resultSet.getInt("student_id"));
                    rating.setEventId(resultSet.getInt("event_id"));
                    rating.setRating(resultSet.getInt("rating"));
                    rating.setComment(resultSet.getString("comment"));

                    Timestamp createdAt = resultSet.getTimestamp("created_at");
                    if (createdAt != null) {
                        rating.setCreatedAt(createdAt.toLocalDateTime());
                    }

                    return rating;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Rating getRatingById(Connection connection, int id) {
        String sql = "SELECT * FROM ratings WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                Rating rating = new Rating();

                rating.setId(resultSet.getInt("id"));
                rating.setStudentId(resultSet.getInt("student_id"));
                rating.setEventId(resultSet.getInt("event_id"));
                rating.setRating(resultSet.getInt("rating"));
                rating.setComment(resultSet.getString("comment"));

                Timestamp createdAt = resultSet.getTimestamp("created_at");
                if (createdAt != null) {
                    rating.setCreatedAt(createdAt.toLocalDateTime());
                }

                return rating;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean addRating(Rating rating) {
        String sql = "INSERT INTO ratings (student_id, event_id, rating, comment) VALUES (?, ?, ?, ?)";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, rating.getStudentId());
            statement.setInt(2, rating.getEventId());
            statement.setInt(3, rating.getRating());
            statement.setString(4, rating.getComment());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean addRating(Rating rating, Connection connection) {
        String sql = "INSERT INTO ratings (student_id, event_id, rating, comment) VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, rating.getStudentId());
            statement.setInt(2, rating.getEventId());
            statement.setInt(3, rating.getRating());
            statement.setString(4, rating.getComment());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateRating(Rating rating) {
        String sql = "UPDATE ratings SET student_id = ?, event_id = ?, rating = ?, comment = ? WHERE id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, rating.getStudentId());
            statement.setInt(2, rating.getEventId());
            statement.setInt(3, rating.getRating());
            statement.setString(4, rating.getComment());
            statement.setInt(5, rating.getId());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateRating(Rating rating, Connection connection) {
        String sql = "UPDATE ratings SET student_id = ?, event_id = ?, rating = ?, comment = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, rating.getStudentId());
            statement.setInt(2, rating.getEventId());
            statement.setInt(3, rating.getRating());
            statement.setString(4, rating.getComment());
            statement.setInt(5, rating.getId());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteRating(int id) {
        String sql = "DELETE FROM ratings WHERE id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteRating(int id, Connection connection) {
        String sql = "DELETE FROM ratings WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}