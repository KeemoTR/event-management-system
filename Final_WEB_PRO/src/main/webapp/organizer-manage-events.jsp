<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="eventsystem.model.Event" %>
<%@ page import="eventsystem.model.User" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.time.format.DateTimeFormatter" %>

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
    List<Event> events = (List<Event>) request.getAttribute("events");

    Map<Integer, Integer> reservationCounts =
            (Map<Integer, Integer>) request.getAttribute("reservationCounts");

    User loggedInUser = (User) request.getAttribute("loggedInUser");

    String error = (String) request.getAttribute("error");
    String success = request.getParameter("success");

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    boolean isAdmin = loggedInUser != null && "admin".equalsIgnoreCase(loggedInUser.getRole());
    boolean isOrganizer = loggedInUser != null && "organizer".equalsIgnoreCase(loggedInUser.getRole());
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Manage Events | Campus Events</title>

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

        .title {
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

        .actions {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            align-items: center;
        }

        form {
            margin: 0;
        }

        button,
        .action-link {
            border: 0;
            border-radius: 9px;
            padding: 8px 10px;
            font-weight: bold;
            cursor: pointer;
            background: #2563eb;
            color: white;
            font-size: 13px;
            text-decoration: none;
            display: inline-block;
            font-family: Arial, sans-serif;
        }

        .action-link.info {
            background: #0f766e;
        }

        .action-link.ratings {
            background: #9333ea;
        }

        .action-link.edit {
            background: #7c3aed;
        }

        button.secondary {
            background: #6b7280;
        }

        button.warning {
            background: #f59e0b;
        }

        button.danger {
            background: #dc2626;
        }

        .empty {
            text-align: center;
            padding: 40px;
            color: #6b7280;
        }

        .empty a {
            color: #2563eb;
            font-weight: bold;
        }

        @media (max-width: 900px) {
            .topbar {
                align-items: flex-start;
                flex-direction: column;
            }

            table {
                min-width: 1000px;
            }

            .table-wrap {
                overflow-x: auto;
            }
        }
    </style>
</head>

<body>
<div class="page">

    <div class="topbar">
        <div>
            <h1><%= isAdmin ? "Manage All Events" : "Manage My Events" %></h1>
            <div class="subtitle">
                Close registration, reopen events, edit event details, view attendees, ratings, and mark attendance.
            </div>
        </div>

        <div class="nav">
            <a href="<%= request.getContextPath() %>/events">Browse Events</a>

            <% if (isOrganizer) { %>
                <a href="<%= request.getContextPath() %>/create-event">Create Event</a>
            <% } %>

            <a href="<%= request.getContextPath() %>/organizer/manage-events">Manage Events</a>
            <a href="<%= request.getContextPath() %>/profile">Profile</a>
            <a href="<%= request.getContextPath() %>/logout">Logout</a>
        </div>
    </div>

    <div class="card">

        <% if (error != null) { %>
            <div class="alert error"><%= h(error) %></div>
        <% } %>

        <% if ("created".equals(success)) { %>
            <div class="alert success">Event created successfully.</div>
        <% } else if ("updated".equals(success)) { %>
            <div class="alert success">Event updated successfully.</div>
        <% } else if ("closed".equals(success)) { %>
            <div class="alert success">Registration closed successfully.</div>
        <% } else if ("reopened".equals(success)) { %>
            <div class="alert success">Registration reopened successfully.</div>
        <% } else if ("completed".equals(success)) { %>
            <div class="alert success">Event marked as completed successfully.</div>
        <% } else if ("deleted".equals(success)) { %>
            <div class="alert success">Event deleted successfully.</div>
        <% } %>

        <% if (events == null || events.isEmpty()) { %>

            <div class="empty">
                No events found.
                <br><br>

                <% if (isOrganizer) { %>
                    <a href="<%= request.getContextPath() %>/create-event">Create your first event</a>
                <% } else { %>
                    No events are available to manage.
                <% } %>
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
                        <th>Reservations</th>
                        <th>Actions</th>
                    </tr>
                    </thead>

                    <tbody>
                    <% for (Event event : events) {
                        String status = event.getStatus();
                        Integer activeReservations = 0;

                        boolean isOpen = "open".equalsIgnoreCase(status);
                        boolean isClosed = "closed".equalsIgnoreCase(status);
                        boolean isExpired = "expired".equalsIgnoreCase(status);
                        boolean isCompleted = "completed".equalsIgnoreCase(status);

                        if (event.getId() != null
                                && reservationCounts != null
                                && reservationCounts.get(event.getId()) != null) {
                            activeReservations = reservationCounts.get(event.getId());
                        }

                        String dateValue = "-";
                        if (event.getEventDateTime() != null) {
                            dateValue = event.getEventDateTime().format(formatter);
                        }
                    %>

                        <tr>
                            <td><%= h(event.getId()) %></td>

                            <td>
                                <div class="title"><%= h(event.getTitle()) %></div>
                                <div class="muted"><%= h(event.getLocation()) %></div>
                            </td>

                            <td><%= h(event.getEventType()) %></td>

                            <td>
                                <span class="<%= statusClass(status) %>">
                                    <%= h(status) %>
                                </span>
                            </td>

                            <td><%= h(dateValue) %></td>

                            <td><%= h(event.getCapacity()) %></td>

                            <td><%= h(event.getRemainingSeats()) %></td>

                            <td><%= h(activeReservations) %></td>

                            <td>
                                <div class="actions">

                                    <a class="action-link info"
                                       href="<%= request.getContextPath() %>/organizer/event-attendees?eventId=<%= h(event.getId()) %>">
                                        Attendees
                                    </a>

                                    <a class="action-link ratings"
                                       href="<%= request.getContextPath() %>/organizer/event-ratings?eventId=<%= h(event.getId()) %>">
                                        Ratings
                                    </a>

                                    <% if (!isCompleted) { %>
                                        <a class="action-link edit"
                                           href="<%= request.getContextPath() %>/organizer/edit-event?eventId=<%= h(event.getId()) %>">
                                            Edit
                                        </a>
                                    <% } %>

                                    <% if (isOpen) { %>
                                        <form method="post" action="<%= request.getContextPath() %>/organizer/manage-events">
                                            <input type="hidden" name="eventId" value="<%= h(event.getId()) %>">
                                            <input type="hidden" name="action" value="close">
                                            <button class="warning" type="submit">Close</button>
                                        </form>
                                    <% } %>

                                    <% if (isClosed) { %>
                                        <form method="post" action="<%= request.getContextPath() %>/organizer/manage-events">
                                            <input type="hidden" name="eventId" value="<%= h(event.getId()) %>">
                                            <input type="hidden" name="action" value="reopen">
                                            <button type="submit">Reopen</button>
                                        </form>
                                    <% } %>

                                    <% if (isOpen || isClosed || isExpired) { %>
                                        <form method="post" action="<%= request.getContextPath() %>/organizer/manage-events">
                                            <input type="hidden" name="eventId" value="<%= h(event.getId()) %>">
                                            <input type="hidden" name="action" value="complete">
                                            <button class="secondary" type="submit">Complete</button>
                                        </form>
                                    <% } %>

                                    <% if (!isCompleted && activeReservations == 0) { %>
                                        <form method="post"
                                              action="<%= request.getContextPath() %>/organizer/manage-events"
                                              onsubmit="return confirm('Are you sure you want to delete this event?');">
                                            <input type="hidden" name="eventId" value="<%= h(event.getId()) %>">
                                            <input type="hidden" name="action" value="delete">
                                            <button class="danger" type="submit">Delete</button>
                                        </form>
                                    <% } %>

                                </div>
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