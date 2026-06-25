<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="eventsystem.model.User" %>
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
%>

<%
    User currentUser = null;
    String currentRole = "";

    if (session != null && session.getAttribute("user") instanceof User) {
        currentUser = (User) session.getAttribute("user");
        currentRole = currentUser.getRole() == null ? "" : currentUser.getRole();
    }

    List<Map<String, Object>> departments =
            (List<Map<String, Object>>) request.getAttribute("departments");

    List<Map<String, Object>> categories =
            (List<Map<String, Object>>) request.getAttribute("categories");

    String error = (String) request.getAttribute("error");

    if (error == null && session != null) {
        error = (String) session.getAttribute("error");
        session.removeAttribute("error");
    }

    String created = request.getParameter("created");
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Create Event | Campus Events</title>

    <style>
        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: #f4f6f8;
            color: #1f2937;
        }

        .page {
            max-width: 980px;
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
            margin-bottom: 18px;
        }

        .alert {
            padding: 12px 14px;
            border-radius: 10px;
            margin-bottom: 16px;
            font-size: 14px;
            font-weight: bold;
        }

        .success {
            background: #dcfce7;
            color: #166534;
        }

        .error {
            background: #fee2e2;
            color: #991b1b;
        }

        .form-grid {
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
            font-size: 14px;
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

        .hint {
            color: #6b7280;
            font-size: 13px;
            margin-top: 6px;
            line-height: 1.4;
        }

        .actions {
            margin-top: 22px;
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
        }

        button,
        .button-link {
            border: 0;
            border-radius: 10px;
            padding: 11px 14px;
            font-weight: bold;
            cursor: pointer;
            background: #2563eb;
            color: white;
            font-size: 14px;
            text-decoration: none;
            display: inline-block;
            font-family: Arial, sans-serif;
        }

        button:hover {
            background: #1d4ed8;
        }

        .button-link.secondary {
            background: #6b7280;
        }

        .button-link.secondary:hover {
            background: #4b5563;
        }

        .section-title {
            font-size: 18px;
            margin: 0 0 16px;
            color: #111827;
        }

        @media (max-width: 800px) {
            .topbar {
                align-items: flex-start;
                flex-direction: column;
            }

            .form-grid {
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
            <h1>Create Event</h1>
            <div class="subtitle">
                Add a new campus event and open it for student reservations.
            </div>
        </div>

        <div class="nav">
            <a href="<%= request.getContextPath() %>/events">Browse Events</a>
            <a href="<%= request.getContextPath() %>/profile">Profile</a>

            <% if ("organizer".equalsIgnoreCase(currentRole) || "admin".equalsIgnoreCase(currentRole)) { %>
                <a href="<%= request.getContextPath() %>/create-event">Create Event</a>
                <a href="<%= request.getContextPath() %>/organizer/manage-events">Manage Events</a>
            <% } %>

            <% if ("admin".equalsIgnoreCase(currentRole)) { %>
                <a href="<%= request.getContextPath() %>/admin-users">Admin Users</a>
            <% } %>

            <a href="<%= request.getContextPath() %>/logout">Logout</a>
        </div>
    </div>

    <div class="card">

        <% if ("success".equals(created)) { %>
            <div class="alert success">Event created successfully.</div>
        <% } %>

        <% if (error != null) { %>
            <div class="alert error"><%= h(error) %></div>
        <% } %>

        <h2 class="section-title">Event Details</h2>

        <form action="<%= request.getContextPath() %>/create-event"
              method="post"
              enctype="multipart/form-data">

            <div class="form-grid">

                <div class="full">
                    <label for="title">Title</label>
                    <input id="title"
                           type="text"
                           name="title"
                           placeholder="Example: Java Basics Workshop"
                           required>
                </div>

                <div class="full">
                    <label for="description">Description</label>
                    <textarea id="description"
                              name="description"
                              placeholder="Write a clear description for students."
                              required></textarea>
                </div>

                <div>
                    <label for="eventType">Event Type</label>
                    <select id="eventType" name="eventType" required>
                        <option value="">Select event type</option>
                        <option value="workshop">Workshop</option>
                        <option value="seminar">Seminar</option>
                        <option value="club_social_event">Club Social Event</option>
                        <option value="sports_activity">Sports Activity</option>
                    </select>
                </div>

                <div>
                    <label for="eventDateTime">Date and Time</label>
                    <input id="eventDateTime"
                           type="datetime-local"
                           name="eventDateTime"
                           required>
                </div>

                <div>
                    <label for="departmentId">Department / Club</label>

                    <% if (departments != null && !departments.isEmpty()) { %>
                        <select id="departmentId" name="departmentId" required>
                            <option value="">Select department or club</option>

                            <% for (Map<String, Object> department : departments) { %>
                                <option value="<%= h(department.get("id")) %>">
                                    <%= h(department.get("name")) %>
                                </option>
                            <% } %>
                        </select>

                        <div class="hint">
                            Choose the department or club responsible for this event.
                        </div>
                    <% } else { %>
                        <input id="departmentId"
                               type="number"
                               name="departmentId"
                               min="1"
                               placeholder="Example: 1"
                               required>
                        <div class="hint">
                            Departments could not be loaded. Enter an existing department ID.
                        </div>
                    <% } %>
                </div>

                <div>
                    <label for="categoryId">Category</label>

                    <% if (categories != null && !categories.isEmpty()) { %>
                        <select id="categoryId" name="categoryId" required>
                            <option value="">Select category</option>

                            <% for (Map<String, Object> category : categories) { %>
                                <option value="<%= h(category.get("id")) %>">
                                    <%= h(category.get("name")) %>
                                </option>
                            <% } %>
                        </select>

                        <div class="hint">
                            Choose the event category, such as Educational, Social, Sports, or Technical.
                        </div>
                    <% } else { %>
                        <input id="categoryId"
                               type="number"
                               name="categoryId"
                               min="1"
                               placeholder="Example: 1"
                               required>
                        <div class="hint">
                            Categories could not be loaded. Enter an existing category ID.
                        </div>
                    <% } %>
                </div>

                <div>
                    <label for="location">Location</label>
                    <input id="location"
                           type="text"
                           name="location"
                           placeholder="Example: Lab 101"
                           required>
                </div>

                <div>
                    <label for="capacity">Capacity</label>
                    <input id="capacity"
                           type="number"
                           name="capacity"
                           min="1"
                           placeholder="Example: 30"
                           required>
                    <div class="hint">Capacity must be greater than zero.</div>
                </div>

                <div class="full">
                    <label for="imageFile">Event Image</label>
                    <input id="imageFile"
                           type="file"
                           name="imageFile"
                           accept="image/jpeg,image/png,image/gif,image/webp">
                    <div class="hint">
                        Optional. Allowed types: JPG, PNG, GIF, WEBP. Maximum size: 5MB.
                    </div>
                </div>

            </div>

            <div class="actions">
                <button type="submit">Create Event</button>

                <a class="button-link secondary"
                   href="<%= request.getContextPath() %>/organizer/manage-events">
                    Manage Events
                </a>

                <a class="button-link secondary"
                   href="<%= request.getContextPath() %>/events">
                    Browse Events
                </a>
            </div>

        </form>
    </div>

</div>
</body>
</html>