<%@ page import="java.util.List" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="eventsystem.model.Event" %>
<%@ page import="eventsystem.model.User" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

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

    private String statusClass(String status) {
        if (status == null) return "badge";
        String s = status.toLowerCase();

        if ("open".equals(s)) return "badge open";
        if ("closed".equals(s)) return "badge closed";
        if ("completed".equals(s)) return "badge completed";
        if ("expired".equals(s)) return "badge expired";

        return "badge";
    }
%>

<%
    User currentUser = null;
    String currentRole = "";

    if (session != null && session.getAttribute("user") instanceof User) {
        currentUser = (User) session.getAttribute("user");
        currentRole = currentUser.getRole() == null ? "" : currentUser.getRole();
    }

    String reserved = request.getParameter("reserved");

    String error = (String) request.getAttribute("error");
    if (error == null && session != null) {
        error = (String) session.getAttribute("error");
        session.removeAttribute("error");
    }

    List<Event> events = (List<Event>) request.getAttribute("events");

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    String titleValue = request.getParameter("title") == null ? "" : request.getParameter("title");
    String eventTypeValue = request.getParameter("eventType") == null ? "" : request.getParameter("eventType");
    String departmentIdValue = request.getParameter("departmentId") == null ? "" : request.getParameter("departmentId");
    String categoryIdValue = request.getParameter("categoryId") == null ? "" : request.getParameter("categoryId");
    String dateValue = request.getParameter("date") == null ? "" : request.getParameter("date");
    boolean onlyAvailableChecked = "true".equalsIgnoreCase(request.getParameter("onlyAvailable"));
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Browse Events | Campus Events</title>

    <style>
        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: #f4f6f8;
            color: #1f2937;
        }

        .page {
            max-width: 1200px;
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
            padding: 20px;
            box-shadow: 0 12px 35px rgba(0, 0, 0, 0.07);
            margin-bottom: 18px;
        }

        .alert {
            padding: 12px 14px;
            border-radius: 10px;
            margin-bottom: 16px;
            font-size: 14px;
        }

        .error {
            background: #fee2e2;
            color: #991b1b;
        }

        .success {
            background: #dcfce7;
            color: #166534;
        }

        .filters {
            display: grid;
            grid-template-columns: 1.5fr 1fr 1fr 1fr 1fr auto;
            gap: 12px;
            align-items: end;
        }

        label {
            display: block;
            font-weight: bold;
            margin-bottom: 6px;
            color: #374151;
            font-size: 14px;
        }

        input,
        select {
            width: 100%;
            box-sizing: border-box;
            padding: 10px 12px;
            border: 1px solid #d1d5db;
            border-radius: 10px;
            font-size: 14px;
            background: white;
        }

        .checkbox-row {
            display: flex;
            align-items: center;
            gap: 8px;
            padding-top: 26px;
            font-weight: bold;
            color: #374151;
            white-space: nowrap;
        }

        .checkbox-row input {
            width: auto;
        }

        button,
        .button-link {
            border: 0;
            border-radius: 9px;
            padding: 10px 12px;
            font-weight: bold;
            cursor: pointer;
            background: #2563eb;
            color: white;
            font-size: 14px;
            text-decoration: none;
            display: inline-block;
            font-family: Arial, sans-serif;
        }

        .button-link.secondary {
            background: #6b7280;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            overflow: hidden;
        }

        th {
            text-align: left;
            background: #f9fafb;
            color: #374151;
            font-size: 14px;
            padding: 12px;
            border-bottom: 1px solid #e5e7eb;
        }

        td {
            padding: 12px;
            border-bottom: 1px solid #e5e7eb;
            vertical-align: top;
            font-size: 14px;
        }

        .event-title {
            font-weight: bold;
            color: #111827;
        }

        .organizer-name {
            font-weight: bold;
            color: #111827;
        }

        .muted {
            color: #6b7280;
            font-size: 13px;
            margin-top: 4px;
        }

        .badge {
            display: inline-block;
            padding: 5px 9px;
            border-radius: 999px;
            font-size: 12px;
            font-weight: bold;
            background: #e5e7eb;
            color: #374151;
        }

        .open {
            background: #dcfce7;
            color: #166534;
        }

        .closed {
            background: #fef3c7;
            color: #92400e;
        }

        .completed {
            background: #dbeafe;
            color: #1e40af;
        }

        .expired {
            background: #fee2e2;
            color: #991b1b;
        }

        .empty {
            text-align: center;
            padding: 40px;
            color: #6b7280;
        }

        .action-note {
            color: #6b7280;
            font-size: 13px;
            font-weight: bold;
        }

        form {
            margin: 0;
        }

        @media (max-width: 1000px) {
            .topbar {
                align-items: flex-start;
                flex-direction: column;
            }

            .filters {
                grid-template-columns: 1fr 1fr;
            }

            table {
                min-width: 950px;
            }

            .table-wrap {
                overflow-x: auto;
            }
        }

        @media (max-width: 640px) {
            .filters {
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
            <h1>Browse Events</h1>
            <div class="subtitle">
                Search campus events, check seat availability, and reserve tickets.
            </div>
        </div>

        <div class="nav">
            <a href="<%= request.getContextPath() %>/events">Browse Events</a>

            <% if (currentUser == null) { %>
                <a href="<%= request.getContextPath() %>/login">Login</a>
                <a href="<%= request.getContextPath() %>/register">Register</a>
            <% } else { %>
                <a href="<%= request.getContextPath() %>/profile">Profile</a>

                <% if ("student".equalsIgnoreCase(currentRole)) { %>
                    <a href="<%= request.getContextPath() %>/my-reservations">My Reservations</a>
                <% } %>

                <% if ("organizer".equalsIgnoreCase(currentRole) || "admin".equalsIgnoreCase(currentRole)) { %>
                    <a href="<%= request.getContextPath() %>/create-event">Create Event</a>
                    <a href="<%= request.getContextPath() %>/organizer/manage-events">Manage Events</a>
                <% } %>

                <% if ("admin".equalsIgnoreCase(currentRole)) { %>
                    <a href="<%= request.getContextPath() %>/admin-users">Admin Users</a>
                    <a href="<%= request.getContextPath() %>/admin-system-data">System Data</a>
                <% } %>

                <a href="<%= request.getContextPath() %>/logout">Logout</a>
            <% } %>
        </div>
    </div>

    <div class="card">

        <% if ("success".equals(reserved)) { %>
            <div class="alert success">Reservation created successfully.</div>
        <% } %>

        <% if (error != null) { %>
            <div class="alert error"><%= h(error) %></div>
        <% } %>

        <form action="<%= request.getContextPath() %>/events" method="get">
            <div class="filters">

                <div>
                    <label>Title</label>
                    <input type="text" name="title" value="<%= h(titleValue) %>" placeholder="Search by title">
                </div>

                <div>
                    <label>Event Type</label>
                    <select name="eventType">
                        <option value="" <%= "".equals(eventTypeValue) ? "selected" : "" %>>All</option>
                        <option value="workshop" <%= "workshop".equals(eventTypeValue) ? "selected" : "" %>>Workshop</option>
                        <option value="seminar" <%= "seminar".equals(eventTypeValue) ? "selected" : "" %>>Seminar</option>
                        <option value="club_social_event" <%= "club_social_event".equals(eventTypeValue) ? "selected" : "" %>>Club Social Event</option>
                        <option value="sports_activity" <%= "sports_activity".equals(eventTypeValue) ? "selected" : "" %>>Sports Activity</option>
                    </select>
                </div>

                <div>
                    <label>Department ID</label>
                    <input type="number" name="departmentId" value="<%= h(departmentIdValue) %>" min="1">
                </div>

                <div>
                    <label>Category ID</label>
                    <input type="number" name="categoryId" value="<%= h(categoryIdValue) %>" min="1">
                </div>

                <div>
                    <label>Date</label>
                    <input type="date" name="date" value="<%= h(dateValue) %>">
                </div>

                <div class="checkbox-row">
                    <input type="checkbox" name="onlyAvailable" value="true" <%= onlyAvailableChecked ? "checked" : "" %>>
                    Available Only
                </div>

            </div>

            <div style="margin-top: 16px; display: flex; gap: 10px; flex-wrap: wrap;">
                <button type="submit">Search</button>
                <a class="button-link secondary" href="<%= request.getContextPath() %>/events">Clear</a>
            </div>
        </form>
    </div>

    <div class="card">
        <% if (events == null || events.isEmpty()) { %>

            <div class="empty">
                No events found.
            </div>

        <% } else { %>

            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Event</th>
                        <th>Type</th>
                        <th>Status</th>
                        <th>Date</th>
                        <th>Capacity</th>
                        <th>Remaining</th>
                        <th>Organizer</th>
                        <th>Action</th>
                    </tr>
                    </thead>

                    <tbody>
                    <% for (Event event : events) {
                        String displayDate = "-";

                        if (event.getEventDateTime() != null) {
                            displayDate = event.getEventDateTime().format(formatter);
                        }

                        boolean canReserve =
                                event.getStatus() != null
                                && "open".equalsIgnoreCase(event.getStatus())
                                && event.getRemainingSeats() != null
                                && event.getRemainingSeats() > 0;

                        boolean isStudent =
                                currentUser != null
                                && "student".equalsIgnoreCase(currentRole);
                    %>
                        <tr>
                            <td><%= h(event.getId()) %></td>

                            <td>
                                <div class="event-title"><%= h(event.getTitle()) %></div>
                                <div class="muted"><%= h(event.getLocation()) %></div>
                            </td>

                            <td><%= h(event.getEventType()) %></td>

                            <td>
                                <span class="<%= statusClass(event.getStatus()) %>">
                                    <%= h(event.getStatus()) %>
                                </span>
                            </td>

                            <td><%= h(displayDate) %></td>

                            <td><%= h(event.getCapacity()) %></td>

                            <td><%= h(event.getRemainingSeats()) %></td>

                            <td>
                                <% if (event.getOrganizerName() != null && !event.getOrganizerName().trim().isEmpty()) { %>
                                    <div class="organizer-name"><%= h(event.getOrganizerName()) %></div>
                                    <div class="muted">ID: <%= h(event.getOrganizerId()) %></div>
                                <% } else { %>
                                    <span class="action-note">Organizer ID: <%= h(event.getOrganizerId()) %></span>
                                <% } %>
                            </td>

                            <td>
                                <% if (canReserve && isStudent) { %>
                                    <form action="<%= request.getContextPath() %>/reserve-ticket" method="post">
                                        <input type="hidden" name="eventId" value="<%= h(event.getId()) %>">
                                        <button type="submit">Reserve</button>
                                    </form>
                                <% } else if (canReserve && currentUser == null) { %>
                                    <a class="button-link" href="<%= request.getContextPath() %>/login?next=/events">
                                        Login to reserve
                                    </a>
                                <% } else if (canReserve) { %>
                                    <span class="action-note">Only students can reserve</span>
                                <% } else { %>
                                    <span class="action-note">Not available</span>
                                <% } %>
                            </td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>

        <% } %>
    </div>

</div>
</body>
</html>