<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.sql.Timestamp" %>
<%@ page import="java.text.DecimalFormat" %>

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

    private String stars(Object value) {
        if (value == null) return "";

        int rating = Integer.parseInt(value.toString());
        StringBuilder result = new StringBuilder();

        for (int i = 1; i <= 5; i++) {
            if (i <= rating) {
                result.append("★");
            } else {
                result.append("☆");
            }
        }

        return result.toString();
    }
%>

<%
    Map<String, Object> event =
            (Map<String, Object>) request.getAttribute("event");

    List<Map<String, Object>> ratings =
            (List<Map<String, Object>>) request.getAttribute("ratings");

    String error = (String) request.getAttribute("error");

    double averageRating = 0.0;
    Object avgObj = request.getAttribute("averageRating");
    if (avgObj instanceof Double) {
        averageRating = (Double) avgObj;
    }

    int totalRatings = 0;
    Object totalObj = request.getAttribute("totalRatings");
    if (totalObj instanceof Integer) {
        totalRatings = (Integer) totalObj;
    }

    DecimalFormat df = new DecimalFormat("0.0");
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Event Ratings | Campus Events</title>

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
        }

        .error {
            background: #fee2e2;
            color: #991b1b;
        }

        .event-title {
            font-size: 22px;
            font-weight: bold;
            color: #111827;
            margin-bottom: 8px;
        }

        .muted {
            color: #6b7280;
            font-size: 14px;
        }

        .summary {
            display: flex;
            gap: 14px;
            flex-wrap: wrap;
            margin-top: 18px;
        }

        .summary-box {
            background: #f9fafb;
            border: 1px solid #e5e7eb;
            border-radius: 14px;
            padding: 14px 16px;
            min-width: 160px;
        }

        .summary-label {
            color: #6b7280;
            font-size: 13px;
            margin-bottom: 6px;
        }

        .summary-value {
            font-size: 22px;
            font-weight: bold;
            color: #111827;
        }

        .stars {
            color: #f59e0b;
            font-size: 20px;
            letter-spacing: 2px;
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

        .student {
            font-weight: bold;
            color: #111827;
        }

        .comment {
            max-width: 420px;
            line-height: 1.5;
        }

        .empty {
            text-align: center;
            padding: 40px;
            color: #6b7280;
        }

        @media (max-width: 800px) {
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
            <h1>Event Ratings</h1>
            <div class="subtitle">View student ratings and comments for your event.</div>
        </div>

        <div class="nav">
            <a href="<%= request.getContextPath() %>/organizer/manage-events">Manage Events</a>
            <a href="<%= request.getContextPath() %>/events">Browse Events</a>
            <a href="<%= request.getContextPath() %>/profile">Profile</a>
            <a href="<%= request.getContextPath() %>/logout">Logout</a>
        </div>
    </div>

    <% if (error != null) { %>
        <div class="alert error"><%= h(error) %></div>
    <% } %>

    <% if (event != null) { %>
        <div class="card">
            <div class="event-title"><%= h(event.get("title")) %></div>
            <div class="muted">
                Type: <%= h(event.get("eventType")) %>
                |
                Status: <%= h(event.get("status")) %>
                |
                Location: <%= h(event.get("location")) %>
            </div>

            <div class="summary">
                <div class="summary-box">
                    <div class="summary-label">Average Rating</div>
                    <div class="summary-value"><%= df.format(averageRating) %> / 5</div>
                </div>

                <div class="summary-box">
                    <div class="summary-label">Total Ratings</div>
                    <div class="summary-value"><%= totalRatings %></div>
                </div>
            </div>
        </div>
    <% } %>

    <div class="card">
        <% if (ratings == null || ratings.isEmpty()) { %>

            <div class="empty">
                No ratings yet for this event.
            </div>

        <% } else { %>

            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>Student</th>
                        <th>Email</th>
                        <th>Rating</th>
                        <th>Comment</th>
                        <th>Created At</th>
                    </tr>
                    </thead>

                    <tbody>
                    <% for (Map<String, Object> rating : ratings) { %>
                        <tr>
                            <td>
                                <div class="student"><%= h(rating.get("studentName")) %></div>
                            </td>

                            <td><%= h(rating.get("studentEmail")) %></td>

                            <td>
                                <div class="stars"><%= stars(rating.get("rating")) %></div>
                                <div class="muted"><%= h(rating.get("rating")) %> / 5</div>
                            </td>

                            <td>
                                <div class="comment">
                                    <%= h(rating.get("comment")) %>
                                </div>
                            </td>

                            <td><%= h(rating.get("createdAt")) %></td>
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