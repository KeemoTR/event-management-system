package eventsystem.model;

import java.time.LocalDateTime;

public class Event {

    private Integer id;
    private String title;
    private String description;
    private Integer departmentId;
    private LocalDateTime eventDateTime;
    private String location;
    private Integer capacity;
    private Integer remainingSeats;
    private Integer categoryId;
    private String eventType;
    private String imagePath;
    private String status;
    private Integer organizerId;
    private String organizerName;
    private LocalDateTime createdAt;

    public Event() {
    }

    public Event(Integer id, String title, String description, Integer departmentId,
                 LocalDateTime eventDateTime, String location, Integer capacity,
                 Integer remainingSeats, Integer categoryId, String eventType,
                 String imagePath, String status, Integer organizerId,
                 LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.departmentId = departmentId;
        this.eventDateTime = eventDateTime;
        this.location = location;
        this.capacity = capacity;
        this.remainingSeats = remainingSeats;
        this.categoryId = categoryId;
        this.eventType = eventType;
        this.imagePath = imagePath;
        this.status = status;
        this.organizerId = organizerId;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public LocalDateTime getEventDateTime() {
        return eventDateTime;
    }

    public void setEventDateTime(LocalDateTime eventDateTime) {
        this.eventDateTime = eventDateTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getRemainingSeats() {
        return remainingSeats;
    }

    public void setRemainingSeats(Integer remainingSeats) {
        this.remainingSeats = remainingSeats;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(Integer organizerId) {
        this.organizerId = organizerId;
    }

    public String getOrganizerName() {
        return organizerName;
    }

    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", departmentId=" + departmentId +
                ", eventDateTime=" + eventDateTime +
                ", location='" + location + '\'' +
                ", capacity=" + capacity +
                ", remainingSeats=" + remainingSeats +
                ", categoryId=" + categoryId +
                ", eventType='" + eventType + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", status='" + status + '\'' +
                ", organizerId=" + organizerId +
                ", organizerName='" + organizerName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}