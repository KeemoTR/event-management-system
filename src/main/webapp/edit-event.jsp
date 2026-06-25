<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="eventsystem.model.Event" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

<%!
    private String h(Object value) {
        if (value == null) return "";
        return value.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private boolean selected(Object currentValue, Object optionValue) {
        if (currentValue == null || optionValue == null) return false;
        return currentValue.toString().equals(optionValue.toString());
    }
%>

<%
    Event event = (Event) request.getAttribute("event");

    List<Map<String, Object>> departments =
            (List<Map<String, Object>>) request.getAttribute("departments");

    List<Map<String, Object>> categories =
            (List<Map<String, Object>>) request.getAttribute("categories");

    String error = (String) request.getAttribute("error");

    String eventDateTimeValue = "";

    if (event != null && event.getEventDateTime() != null) {
        eventDateTimeValue = event.getEventDateTime().toString();

        if (eventDateTimeValue.length() > 16) {
            eventDateTimeValue = eventDateTimeValue.substring(0, 16);
        }
    }
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Edit Event | Campus Events</title>

    <style>
        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: #f4f6f8;
            color: #1f2937;
        }

        .page {
            max-width: 900px;
            margin: 0 auto;
            padding: 28px;
        }

        .topbar {
            display: flex;
            justify-content: space-between;
            gap: 16px;
            align-items: center;
            margin-bottom: 22px;
        }

        h1 {
            margin: 0;
            font-size: 30px;
        }

        .subtitle {
            margin-top: 6px;
            color: #6b7280;
        }

        .nav {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
        }

        .nav a {
            color: #2563eb;
            text-decoration: none;
            font-weight: bold;
            background: white;
            border: 1px solid #dbe3ef;
            padding: 9px 12px;
            border-radius: 10px;
        }

        .card {
            background: white;
            border-radius: 16px;
            padding: 24px;
            box-shadow: 0 12px 35px rgba(0, 0, 0, 0.07);
        }

        .alert {
            padding: 12px 14px;
            border-radius: 10px;
            margin-bottom: 16px;
            font-size: 14px;
            font-weight: bold;
        }

        .error {
            background: #fee2e2;
            color: #991b1b;
        }

        .grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 16px;
        }

        .full {
            grid-column: 1 / -1;
        }

        label {
            display: block;
            font-weight: bold;
            margin-bottom: 6px;
            color: #374151;
            font-size: 14px;
        }

        input,
        textarea,
        select {
            width: 100%;
            box-sizing: border-box;
            padding: 11px 12px;
            border: 1px solid #d1d5db;
            border-radius: 10px;
            font-size: 15px;
            background: white;
            font-family: Arial, sans-serif;
        }

        input[type="file"] {
            padding: 10px;
            cursor: pointer;
        }

        textarea {
            min-height: 110px;
            resize: vertical;
        }

        .readonly {
            background: #f9fafb;
            color: #6b7280;
        }

        .hint {
            color: #6b7280;
            font-size: 13px;
            margin-top: 6px;
            line-height: 1.4;
        }

        .hint a {
            color: #2563eb;
            font-weight: bold;
            text-decoration: none;
        }

        .image-preview {
            margin-top: 10px;
            padding: 10px;
            border: 1px solid #e5e7eb;
            border-radius: 12px;
            background: #f9fafb;
        }

        .image-preview img {
            max-width: 220px;
            max-height: 140px;
            border-radius: 10px;
            display: block;
            margin-top: 8px;
            object-fit: cover;
        }

        .actions {
            margin-top: 22px;
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
        }

        button {
            border: 0;
            border-radius: 10px;
            padding: 12px 16px;
            font-weight: bold;
            cursor: pointer;
            background: #2563eb;
            color: white;
            font-size: 15px;
            font-family: Arial, sans-serif;
        }

        button:hover {
            background: #1d4ed8;
        }

        .cancel {
            display: inline-block;
            border-radius: 10px;
            padding: 12px 16px;
            font-weight: bold;
            background: #e5e7eb;
            color: #374151;
            text-decoration: none;
        }

        .cancel:hover {
            background: #d1d5db;
        }

        .empty {
            text-align: center;
            color: #6b7280;
            padding: 32px;
        }

        .empty a {
            color: #2563eb;
            font-weight: bold;
            text-decoration: none;
        }

        @media (max-width: 750px) {
            .topbar {
                flex-direction: column;
                align-items: flex-start;
            }

            .grid {
                grid-template-columns: 1fr;
            }

            .page {
                padding: 18px;
            }
        }
    </style>
</head>

<body>
<div class="page">

    <div class="topbar">
        <div>
            <h1>Edit Event</h1>
            <div class="subtitle">
                Update event details while keeping reservations consistent.
            </div>
        </div>

        <div class="nav">
            <a href="<%= request.getContextPath() %>/organizer/manage-events">Manage Events</a>
            <a href="<%= request.getContextPath() %>/events">Browse Events</a>
            <a href="<%= request.getContextPath() %>/profile">Profile</a>
            <a href="<%= request.getContextPath() %>/logout">Logout</a>
        </div>
    </div>

    <div class="card">

        <% if (error != null) { %>
            <div class="alert error"><%= h(error) %></div>
        <% } %>

        <% if (event == null) { %>

            <div class="empty">
                Event could not be loaded.
                <br><br>
                <a href="<%= request.getContextPath() %>/organizer/manage-events">Back to Manage Events</a>
            </div>

        <% } else { %>

            <form method="post"
                  action="<%= request.getContextPath() %>/organizer/edit-event"
                  enctype="multipart/form-data">

                <input type="hidden" name="eventId" value="<%= h(event.getId()) %>">
                <input type="hidden" name="currentImagePath" value="<%= h(event.getImagePath()) %>">

                <div class="grid">

                    <div class="full">
                        <label for="title">Title</label>
                        <input id="title"
                               type="text"
                               name="title"
                               value="<%= h(event.getTitle()) %>"
                               required>
                    </div>

                    <div class="full">
                        <label for="description">Description</label>
                        <textarea id="description"
                                  name="description"
                                  required><%= h(event.getDescription()) %></textarea>
                    </div>

                    <div>
                        <label for="eventType">Event Type</label>
                        <select id="eventType" name="eventType" required>
                            <option value="workshop" <%= selected(event.getEventType(), "workshop") ? "selected" : "" %>>Workshop</option>
                            <option value="seminar" <%= selected(event.getEventType(), "seminar") ? "selected" : "" %>>Seminar</option>
                            <option value="club_social_event" <%= selected(event.getEventType(), "club_social_event") ? "selected" : "" %>>Club Social Event</option>
                            <option value="sports_activity" <%= selected(event.getEventType(), "sports_activity") ? "selected" : "" %>>Sports Activity</option>
                        </select>
                    </div>

                    <div>
                        <label for="eventDateTime">Date and Time</label>
                        <input id="eventDateTime"
                               type="datetime-local"
                               name="eventDateTime"
                               value="<%= h(eventDateTimeValue) %>"
                               required>
                    </div>

                    <div>
                        <label for="departmentId">Department</label>

                        <% if (departments != null && !departments.isEmpty()) { %>
                            <select id="departmentId" name="departmentId" required>
                                <% for (Map<String, Object> department : departments) { %>
                                    <option value="<%= h(department.get("id")) %>"
                                            <%= selected(event.getDepartmentId(), department.get("id")) ? "selected" : "" %>>
                                        <%= h(department.get("name")) %>
                                    </option>
                                <% } %>
                            </select>
                        <% } else { %>
                            <input id="departmentId"
                                   type="number"
                                   name="departmentId"
                                   value="<%= h(event.getDepartmentId()) %>"
                                   required>
                        <% } %>
                    </div>

                    <div>
                        <label for="categoryId">Category</label>

                        <% if (categories != null && !categories.isEmpty()) { %>
                            <select id="categoryId" name="categoryId" required>
                                <% for (Map<String, Object> category : categories) { %>
                                    <option value="<%= h(category.get("id")) %>"
                                            <%= selected(event.getCategoryId(), category.get("id")) ? "selected" : "" %>>
                                        <%= h(category.get("name")) %>
                                    </option>
                                <% } %>
                            </select>
                        <% } else { %>
                            <input id="categoryId"
                                   type="number"
                                   name="categoryId"
                                   value="<%= h(event.getCategoryId()) %>"
                                   required>
                        <% } %>
                    </div>

                    <div>
                        <label for="location">Location</label>
                        <input id="location"
                               type="text"
                               name="location"
                               value="<%= h(event.getLocation()) %>"
                               required>
                    </div>

                    <div>
                        <label for="capacity">Capacity</label>
                        <input id="capacity"
                               type="number"
                               name="capacity"
                               min="1"
                               value="<%= h(event.getCapacity()) %>"
                               required>
                    </div>

                    <div class="full">
                        <label for="imageFile">Event Image</label>

                        <input id="imageFile"
                               type="file"
                               name="imageFile"
                               accept="image/jpeg,image/png,image/gif,image/webp">

                        <% if (event.getImagePath() != null && !event.getImagePath().trim().isEmpty()) { %>
                            <div class="image-preview">
                                <div class="hint">
                                    Current image:
                                    <a href="<%= request.getContextPath() %>/<%= h(event.getImagePath()) %>"
                                       target="_blank">
                                        View image
                                    </a>
                                </div>

                                <img src="<%= request.getContextPath() %>/<%= h(event.getImagePath()) %>"
                                     alt="Current event image">
                            </div>
                        <% } else { %>
                            <div class="hint">
                                No image uploaded yet. Optional. Allowed types: JPG, PNG, GIF, WEBP.
                                Maximum size: 5MB.
                            </div>
                        <% } %>
                    </div>

                    <div>
                        <label>Status</label>
                        <input class="readonly"
                               type="text"
                               value="<%= h(event.getStatus()) %>"
                               readonly>
                    </div>

                </div>

                <div class="actions">
                    <button type="submit">Save Changes</button>
                    <a class="cancel" href="<%= request.getContextPath() %>/organizer/manage-events">Cancel</a>
                </div>

            </form>

        <% } %>

    </div>
</div>
</body>
</html>