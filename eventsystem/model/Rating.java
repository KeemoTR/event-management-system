package eventsystem.model;

import java.time.LocalDateTime;

public class Rating {

    private Integer id;
    private Integer studentId;
    private Integer eventId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public Rating() {
    }

    public Rating(Integer id, Integer studentId, Integer eventId, Integer rating,
                  String comment, LocalDateTime createdAt) {
        this.id = id;
        this.studentId = studentId;
        this.eventId = eventId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
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

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Rating{" +
                "id=" + id +
                ", studentId=" + studentId +
                ", eventId=" + eventId +
                ", rating=" + rating +
                ", comment='" + comment + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}