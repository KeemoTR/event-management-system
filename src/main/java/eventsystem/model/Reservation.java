package eventsystem.model;

import java.time.LocalDateTime;

public class Reservation {

    private Integer id;
    private Integer studentId;
    private Integer eventId;
    private String reservationStatus;
    private String attendanceStatus;
    private LocalDateTime reservedAt;

    public Reservation() {
    }

    public Reservation(Integer id, Integer studentId, Integer eventId,
                       String reservationStatus, String attendanceStatus,
                       LocalDateTime reservedAt) {
        this.id = id;
        this.studentId = studentId;
        this.eventId = eventId;
        this.reservationStatus = reservationStatus;
        this.attendanceStatus = attendanceStatus;
        this.reservedAt = reservedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public String getReservationStatus() {
        return reservationStatus;
    }

    public void setReservationStatus(String reservationStatus) {
        this.reservationStatus = reservationStatus;
    }

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(LocalDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", studentId=" + studentId +
                ", eventId=" + eventId +
                ", reservationStatus='" + reservationStatus + '\'' +
                ", attendanceStatus='" + attendanceStatus + '\'' +
                ", reservedAt=" + reservedAt +
                '}';
    }
}