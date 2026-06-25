package eventsystem.dao;

import eventsystem.model.Reservation;
import eventsystem.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    public List<Reservation> getAllReservations() {
        List<Reservation> reservations = new ArrayList<>();

        String sql = "SELECT * FROM reservations";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                Reservation reservation = new Reservation();

                reservation.setId(resultSet.getInt("id"));
                reservation.setStudentId(resultSet.getInt("student_id"));
                reservation.setEventId(resultSet.getInt("event_id"));
                reservation.setReservationStatus(resultSet.getString("reservation_status"));
                reservation.setAttendanceStatus(resultSet.getString("attendance_status"));

                Timestamp reservedAt = resultSet.getTimestamp("reserved_at");
                if (reservedAt != null) {
                    reservation.setReservedAt(reservedAt.toLocalDateTime());
                }

                reservations.add(reservation);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return reservations;
    }

    public List<Reservation> getAllReservations(Connection connection) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservations";

        try (
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                Reservation reservation = new Reservation();

                reservation.setId(resultSet.getInt("id"));
                reservation.setStudentId(resultSet.getInt("student_id"));
                reservation.setEventId(resultSet.getInt("event_id"));
                reservation.setReservationStatus(resultSet.getString("reservation_status"));
                reservation.setAttendanceStatus(resultSet.getString("attendance_status"));

                Timestamp reservedAt = resultSet.getTimestamp("reserved_at");
                if (reservedAt != null) {
                    reservation.setReservedAt(reservedAt.toLocalDateTime());
                }

                reservations.add(reservation);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return reservations;
    }

    public Reservation getReservationById(int id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Reservation reservation = new Reservation();

                    reservation.setId(resultSet.getInt("id"));
                    reservation.setStudentId(resultSet.getInt("student_id"));
                    reservation.setEventId(resultSet.getInt("event_id"));
                    reservation.setReservationStatus(resultSet.getString("reservation_status"));
                    reservation.setAttendanceStatus(resultSet.getString("attendance_status"));

                    Timestamp reservedAt = resultSet.getTimestamp("reserved_at");
                    if (reservedAt != null) {
                        reservation.setReservedAt(reservedAt.toLocalDateTime());
                    }

                    return reservation;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Reservation getReservationById(Connection connection, int id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                Reservation reservation = new Reservation();

                reservation.setId(resultSet.getInt("id"));
                reservation.setStudentId(resultSet.getInt("student_id"));
                reservation.setEventId(resultSet.getInt("event_id"));
                reservation.setReservationStatus(resultSet.getString("reservation_status"));
                reservation.setAttendanceStatus(resultSet.getString("attendance_status"));

                Timestamp reservedAt = resultSet.getTimestamp("reserved_at");
                if (reservedAt != null) {
                    reservation.setReservedAt(reservedAt.toLocalDateTime());
                }

                return reservation;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean addReservation(Reservation reservation) {
        String sql = "INSERT INTO reservations (student_id, event_id, reservation_status, attendance_status) VALUES (?, ?, ?, ?)";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, reservation.getStudentId());
            statement.setInt(2, reservation.getEventId());
            statement.setString(3, reservation.getReservationStatus());
            statement.setString(4, reservation.getAttendanceStatus());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean addReservation(Reservation reservation, Connection connection) {
        String sql = "INSERT INTO reservations (student_id, event_id, reservation_status, attendance_status) VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reservation.getStudentId());
            statement.setInt(2, reservation.getEventId());
            statement.setString(3, reservation.getReservationStatus());
            statement.setString(4, reservation.getAttendanceStatus());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateReservation(Reservation reservation) {
        String sql = "UPDATE reservations SET student_id = ?, event_id = ?, reservation_status = ?, attendance_status = ? WHERE id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, reservation.getStudentId());
            statement.setInt(2, reservation.getEventId());
            statement.setString(3, reservation.getReservationStatus());
            statement.setString(4, reservation.getAttendanceStatus());
            statement.setInt(5, reservation.getId());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateReservation(Reservation reservation, Connection connection) {
        String sql = "UPDATE reservations SET student_id = ?, event_id = ?, reservation_status = ?, attendance_status = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reservation.getStudentId());
            statement.setInt(2, reservation.getEventId());
            statement.setString(3, reservation.getReservationStatus());
            statement.setString(4, reservation.getAttendanceStatus());
            statement.setInt(5, reservation.getId());

            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteReservation(int id) {
        String sql = "DELETE FROM reservations WHERE id = ?";

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

    public boolean deleteReservation(int id, Connection connection) {
        String sql = "DELETE FROM reservations WHERE id = ?";

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