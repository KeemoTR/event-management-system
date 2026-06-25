<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="eventsystem.model.Event" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.sql.Timestamp" %>

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

    private String attendanceClass(Object value) {
        if (value == null) return "badge pending";

        String status = value.toString().toLowerCase();

        if ("present".equals(status)) return "badge present";
        if ("absent".equals(status)) return "badge absent";

        return "badge pending";
    }
%>

<%
    Event event = (Event) request.getAttribute("event");

    List<Map<String, Object>> attendees =
            (List<Map<String, Object>>) request.getAttribute("attendees");

    String error = (String) request.getAttribute("error");
    String success = request.getParameter("success");

    Integer eventId = null;

    if (event != null && event.getId() != null) {
        eventId = event.getId();
    } else if (request.getParameter("eventId") != null) {
        try {
            eventId = Integer.parseInt(request.getParameter("eventId"));
        } catch (Exception ignored) {
        }
    }
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Event Attendees | Campus Events</title>

    <style>
        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: #f4f6f8;
            color: #1f2937;
        }

        .page {
            max-width: 1100px;
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

        .event-box {
            background: #f9fafb;
            border: 1px solid #e5e7eb;
            border-radius: 14px;
            padding: 16px;
            margin-bottom: 18px;
        }

        .event-title {
            font-size: 20px;
            font-weight: bold;
            margin-bottom: 6px;
        }

        .muted {
            color: #6b7280;
            font-size: 14px;
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

        .student-name {
            font-weight: bold;
            color: #111827;
        }

        .badge {
            display: inline-block;
            padding: 5px 9px;
            border-radius: 999px;
            font-size: 12px;
            font-weight: bold;
        }

        .present {
            background: #dcfce7;
            color: #166534;
        }

        .absent {
            background: #fee2e2;
            color: #991b1b;
        }

        .pending {
            background: #fef3c7;
            color: #92400e;
        }

        .reserved {
            background: #dbeafe;
            color: #1e40af;
        }

        .cancelled {
            background: #e5e7eb;
            color: #374151;
        }

        .actions {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
        }

        form {
            margin: 0;
        }

        button {
            border: 0;
            border-radius: 9px;
            padding: 8px 10px;
            font-weight: bold;
            cursor: pointer;
            color: white;
            font-size: 13px;
        }

        .present-btn {
            background: #16a34a;
        }

        .absent-btn {
            background: #dc2626;
        }

        .empty {
            text-align: center;
            padding: 40px;
            color: #6b7280;
        }

        @media (max-width: 900px) {
            .topbar {
                align-items: flex-start;
                flex-direction: column;
            }

            table {
                min-width: 850px;
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
            <h1>Event Attendees</h1>
            <div class="subtitle">
                View reservations and mark student attendance.
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

        <% if ("attendance".equals(success)) { %>
            <div class="alert success">Attendance updated successfully.</div>
        <% } %>

        <% if (event != null) { %>
            <div class="event-box">
                <div class="event-title"><%= h(event.getTitle()) %></div>
                <div class="muted">
                    Type: <%= h(event.getEventType()) %>
                    |
                    Status: <%= h(event.getStatus()) %>
                    |
                    Location: <%= h(event.getLocation()) %>
                </div>
            </div>
        <% } %>

        <% if (attendees == null || attendees.isEmpty()) { %>

            <div class="empty">
                No attendees found for this event.
            </div>

        <% } else { %>

            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Reservation ID</th>
                        <th>Student</th>
                        <th>Email</th>
                        <th>Reservation Status</th>
                        <th>Attendance</th>
                        <th>Reserved At</th>
                        <th>Actions</th>
                    </tr>
                    </thead>

                    <tbody>
                    <% for (Map<String, Object> attendee : attendees) {
                        Object reservationId = attendee.get("reservationId");
                        Object studentName = attendee.get("studentName");
                        Object studentEmail = attendee.get("studentEmail");
                        Object reservationStatus = attendee.get("reservationStatus");
                        Object attendanceStatus = attendee.get("attendanceStatus");
                        Object reservedAt = attendee.get("reservedAt");

                        boolean isReserved = reservationStatus != null
                                && "reserved".equalsIgnoreCase(reservationStatus.toString());
                    %>

                        <tr>
                            <td><%= h(reservationId) %></td>

                            <td>
                                <div class="student-name"><%= h(studentName) %></div>
                            </td>

                            <td><%= h(studentEmail) %></td>

                            <td>
                                <% if ("reserved".equalsIgnoreCase(String.valueOf(reservationStatus))) { %>
                                    <span class="badge reserved">reserved</span>
                                <% } else { %>
                                    <span class="badge cancelled"><%= h(reservationStatus) %></span>
                                <% } %>
                            </td>

                            <td>
                                <span class="<%= attendanceClass(attendanceStatus) %>">
                                    <%= attendanceStatus == null ? "pending" : h(attendanceStatus) %>
                                </span>
                            </td>

                            <td><%= h(reservedAt) %></td>

                            <td>
                                <% if (isReserved && eventId != null) { %>
                                    <div class="actions">
                                        <form method="post" action="<%= request.getContextPath() %>/organizer/event-attendees">
                                            <input type="hidden" name="eventId" value="<%= h(eventId) %>">
                                            <input type="hidden" name="reservationId" value="<%= h(reservationId) %>">
                                            <input type="hidden" name="attendanceStatus" value="present">
                                            <button class="present-btn" type="submit">Mark Present</button>
                                        </form>

                                        <form method="post" action="<%= request.getContextPath() %>/organizer/event-attendees">
                                            <input type="hidden" name="eventId" value="<%= h(eventId) %>">
                                            <input type="hidden" name="reservationId" value="<%= h(reservationId) %>">
                                            <input type="hidden" name="attendanceStatus" value="absent">
                                            <button class="absent-btn" type="submit">Mark Absent</button>
                                        </form>
                                    </div>
                                <% } else { %>
                                    Not available
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