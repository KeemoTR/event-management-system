<%@ page import="java.util.List" %>
<%@ page import="eventsystem.model.Reservation" %>
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

        if ("reserved".equals(s)) return "badge reserved";
        if ("cancelled".equals(s)) return "badge cancelled";
        if ("present".equals(s)) return "badge present";
        if ("absent".equals(s)) return "badge absent";

        return "badge";
    }
%>

<%
    User currentUser = null;

    if (session != null && session.getAttribute("user") instanceof User) {
        currentUser = (User) session.getAttribute("user");
    }

    String cancelled = request.getParameter("cancelled");
    String rated = request.getParameter("rated");

    String error = (String) request.getAttribute("error");

    if (error == null && session != null) {
        error = (String) session.getAttribute("error");
        session.removeAttribute("error");
    }

    List<Reservation> reservations =
            (List<Reservation>) request.getAttribute("reservations");
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>My Reservations | Campus Events</title>

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

        .table-wrap {
            overflow-x: auto;
        }

        .reservation-id {
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

        .reserved {
            background: #dcfce7;
            color: #166534;
        }

        .cancelled {
            background: #fee2e2;
            color: #991b1b;
        }

        .present {
            background: #dbeafe;
            color: #1e40af;
        }

        .absent {
            background: #fef3c7;
            color: #92400e;
        }

        button {
            border: 0;
            border-radius: 9px;
            padding: 9px 12px;
            font-weight: bold;
            cursor: pointer;
            background: #2563eb;
            color: white;
            font-size: 14px;
            font-family: Arial, sans-serif;
        }

        .danger {
            background: #dc2626;
        }

        .danger:hover {
            background: #b91c1c;
        }

        button:hover {
            background: #1d4ed8;
        }

        select,
        input[type="text"] {
            box-sizing: border-box;
            padding: 9px 10px;
            border: 1px solid #d1d5db;
            border-radius: 9px;
            font-size: 14px;
            background: white;
        }

        .rating-form {
            display: flex;
            gap: 8px;
            flex-wrap: wrap;
            align-items: center;
        }

        .empty {
            text-align: center;
            padding: 44px 20px;
            color: #6b7280;
        }

        .empty h2 {
            margin: 0 0 8px;
            color: #111827;
        }

        .button-link {
            border-radius: 9px;
            padding: 10px 12px;
            font-weight: bold;
            background: #2563eb;
            color: white;
            text-decoration: none;
            display: inline-block;
            margin-top: 12px;
        }

        .action-note {
            color: #6b7280;
            font-size: 13px;
            font-weight: bold;
        }

        form {
            margin: 0;
        }

        @media (max-width: 800px) {
            .topbar {
                align-items: flex-start;
                flex-direction: column;
            }

            table {
                min-width: 850px;
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
            <h1>My Reservations</h1>
            <div class="subtitle">
                <% if (currentUser != null) { %>
                    Welcome, <%= h(currentUser.getName()) %>. View, cancel, and rate your reserved events.
                <% } else { %>
                    View, cancel, and rate your reserved events.
                <% } %>
            </div>
        </div>

        <div class="nav">
            <a href="<%= request.getContextPath() %>/events">Browse Events</a>
            <a href="<%= request.getContextPath() %>/profile">Profile</a>
            <a href="<%= request.getContextPath() %>/my-reservations">My Reservations</a>
            <a href="<%= request.getContextPath() %>/logout">Logout</a>
        </div>
    </div>

    <div class="card">

        <% if ("success".equals(cancelled)) { %>
            <div class="alert success">Reservation cancelled successfully.</div>
        <% } %>

        <% if ("success".equals(rated)) { %>
            <div class="alert success">Rating added successfully.</div>
        <% } %>

        <% if (error != null) { %>
            <div class="alert error"><%= h(error) %></div>
        <% } %>

        <% if (reservations == null || reservations.isEmpty()) { %>

            <div class="empty">
                <h2>No reservations found</h2>
                <div>You have not reserved any events yet.</div>
                <a class="button-link" href="<%= request.getContextPath() %>/events">
                    Browse Events
                </a>
            </div>

        <% } else { %>

            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Reservation</th>
                        <th>Event ID</th>
                        <th>Status</th>
                        <th>Attendance</th>
                        <th>Reserved At</th>
                        <th>Cancel</th>
                        <th>Rate</th>
                    </tr>
                    </thead>

                    <tbody>
                    <%
                        for (Reservation reservation : reservations) {
                            boolean canCancel =
                                    reservation.getReservationStatus() != null
                                    && "reserved".equalsIgnoreCase(reservation.getReservationStatus());

                            boolean canRate =
                                    reservation.getReservationStatus() != null
                                    && "reserved".equalsIgnoreCase(reservation.getReservationStatus());

                            String attendanceStatus =
                                    reservation.getAttendanceStatus() == null
                                    ? "-"
                                    : reservation.getAttendanceStatus();
                    %>
                        <tr>
                            <td>
                                <div class="reservation-id">#<%= h(reservation.getId()) %></div>
                                <div class="muted">Reservation record</div>
                            </td>

                            <td><%= h(reservation.getEventId()) %></td>

                            <td>
                                <span class="<%= statusClass(reservation.getReservationStatus()) %>">
                                    <%= h(reservation.getReservationStatus()) %>
                                </span>
                            </td>

                            <td>
                                <% if ("-".equals(attendanceStatus)) { %>
                                    <span class="action-note">Not marked</span>
                                <% } else { %>
                                    <span class="<%= statusClass(attendanceStatus) %>">
                                        <%= h(attendanceStatus) %>
                                    </span>
                                <% } %>
                            </td>

                            <td><%= h(reservation.getReservedAt()) %></td>

                            <td>
                                <% if (canCancel) { %>
                                    <form action="<%= request.getContextPath() %>/cancel-reservation" method="post">
                                        <input type="hidden" name="eventId" value="<%= h(reservation.getEventId()) %>">
                                        <button class="danger" type="submit">Cancel</button>
                                    </form>
                                <% } else { %>
                                    <span class="action-note">Not available</span>
                                <% } %>
                            </td>

                            <td>
                                <% if (canRate) { %>
                                    <form class="rating-form" action="<%= request.getContextPath() %>/rate-event" method="post">
                                        <input type="hidden" name="eventId" value="<%= h(reservation.getEventId()) %>">

                                        <select name="rating" required>
                                            <option value="">Rating</option>
                                            <option value="5">5</option>
                                            <option value="4">4</option>
                                            <option value="3">3</option>
                                            <option value="2">2</option>
                                            <option value="1">1</option>
                                        </select>

                                        <input type="text" name="comment" placeholder="Comment">

                                        <button type="submit">Rate</button>
                                    </form>
                                <% } else { %>
                                    <span class="action-note">Not available</span>
                                <% } %>
                            </td>
                        </tr>
                    <%
                        }
                    %>
                    </tbody>
                </table>
            </div>

        <% } %>

    </div>

</div>
</body>
</html>