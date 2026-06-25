<%@ page import="java.util.List" %>
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

        if ("active".equals(s)) return "badge active";
        if ("blocked".equals(s)) return "badge blocked";

        return "badge";
    }

    private String roleClass(String role) {
        if (role == null) return "role-badge";

        String r = role.toLowerCase();

        if ("admin".equals(r)) return "role-badge admin";
        if ("organizer".equals(r)) return "role-badge organizer";
        if ("student".equals(r)) return "role-badge student";

        return "role-badge";
    }
%>

<%
    User currentUser = null;

    if (session != null && session.getAttribute("user") instanceof User) {
        currentUser = (User) session.getAttribute("user");
    }

    String blocked = request.getParameter("blocked");
    String unblocked = request.getParameter("unblocked");
    String deleted = request.getParameter("deleted");

    String error = (String) request.getAttribute("error");

    if (error == null && session != null) {
        error = (String) session.getAttribute("error");
        session.removeAttribute("error");
    }

    List<User> users = (List<User>) request.getAttribute("users");
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Admin Users | Campus Events</title>

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

        .summary {
            display: flex;
            gap: 12px;
            flex-wrap: wrap;
            margin-bottom: 16px;
        }

        .summary-box {
            background: #f9fafb;
            border: 1px solid #e5e7eb;
            border-radius: 12px;
            padding: 12px 14px;
            min-width: 130px;
        }

        .summary-box strong {
            display: block;
            font-size: 20px;
            color: #111827;
        }

        .summary-box span {
            color: #6b7280;
            font-size: 13px;
        }

        .table-wrap {
            overflow-x: auto;
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
            white-space: nowrap;
        }

        td {
            padding: 12px;
            border-bottom: 1px solid #e5e7eb;
            vertical-align: top;
            font-size: 14px;
        }

        .user-name {
            font-weight: bold;
            color: #111827;
        }

        .muted {
            color: #6b7280;
            font-size: 13px;
            margin-top: 4px;
        }

        .badge,
        .role-badge {
            display: inline-block;
            padding: 5px 9px;
            border-radius: 999px;
            font-size: 12px;
            font-weight: bold;
            background: #e5e7eb;
            color: #374151;
        }

        .active {
            background: #dcfce7;
            color: #166534;
        }

        .blocked {
            background: #fee2e2;
            color: #991b1b;
        }

        .admin {
            background: #ede9fe;
            color: #5b21b6;
        }

        .organizer {
            background: #dbeafe;
            color: #1e40af;
        }

        .student {
            background: #ecfdf5;
            color: #047857;
        }

        .action-buttons {
            display: flex;
            gap: 8px;
            flex-wrap: wrap;
            align-items: center;
        }

        form {
            margin: 0;
        }

        button {
            border: 0;
            border-radius: 9px;
            padding: 9px 12px;
            font-weight: bold;
            cursor: pointer;
            color: white;
            font-size: 14px;
            font-family: Arial, sans-serif;
        }

        .block-btn {
            background: #f59e0b;
        }

        .block-btn:hover {
            background: #d97706;
        }

        .unblock-btn {
            background: #2563eb;
        }

        .unblock-btn:hover {
            background: #1d4ed8;
        }

        .delete-btn {
            background: #dc2626;
        }

        .delete-btn:hover {
            background: #b91c1c;
        }

        .protected {
            display: inline-block;
            color: #5b21b6;
            background: #ede9fe;
            border-radius: 999px;
            padding: 6px 10px;
            font-size: 12px;
            font-weight: bold;
        }

        .current {
            display: inline-block;
            color: #374151;
            background: #f3f4f6;
            border-radius: 999px;
            padding: 6px 10px;
            font-size: 12px;
            font-weight: bold;
        }

        .action-note {
            color: #6b7280;
            font-size: 13px;
            font-weight: bold;
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

        @media (max-width: 900px) {
            .topbar {
                align-items: flex-start;
                flex-direction: column;
            }

            table {
                min-width: 950px;
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
            <h1>Admin - Manage Users</h1>
            <div class="subtitle">
                View users, control account access, and safely delete accounts.
            </div>
        </div>

        <div class="nav">
            <a href="<%= request.getContextPath() %>/events">Browse Events</a>
            <a href="<%= request.getContextPath() %>/profile">Profile</a>
            <a href="<%= request.getContextPath() %>/create-event">Create Event</a>
            <a href="<%= request.getContextPath() %>/organizer/manage-events">Manage Events</a>
            <a href="<%= request.getContextPath() %>/admin-users">Admin Users</a>
            <a href="<%= request.getContextPath() %>/admin-system-data">System Data</a>
            <a href="<%= request.getContextPath() %>/logout">Logout</a>
        </div>
    </div>

    <div class="card">

        <% if ("success".equals(blocked)) { %>
            <div class="alert success">User blocked successfully.</div>
        <% } %>

        <% if ("success".equals(unblocked)) { %>
            <div class="alert success">User unblocked successfully.</div>
        <% } %>

        <% if ("success".equals(deleted)) { %>
            <div class="alert success">User deleted successfully.</div>
        <% } %>

        <% if (error != null) { %>
            <div class="alert error"><%= h(error) %></div>
        <% } %>

        <%
            int totalUsers = users == null ? 0 : users.size();
            int activeUsers = 0;
            int blockedUsers = 0;
            int adminUsers = 0;

            if (users != null) {
                for (User user : users) {
                    if (user.getStatus() != null && "active".equalsIgnoreCase(user.getStatus())) {
                        activeUsers++;
                    }

                    if (user.getStatus() != null && "blocked".equalsIgnoreCase(user.getStatus())) {
                        blockedUsers++;
                    }

                    if (user.getRole() != null && "admin".equalsIgnoreCase(user.getRole())) {
                        adminUsers++;
                    }
                }
            }
        %>

        <div class="summary">
            <div class="summary-box">
                <strong><%= totalUsers %></strong>
                <span>Total Users</span>
            </div>

            <div class="summary-box">
                <strong><%= activeUsers %></strong>
                <span>Active</span>
            </div>

            <div class="summary-box">
                <strong><%= blockedUsers %></strong>
                <span>Blocked</span>
            </div>

            <div class="summary-box">
                <strong><%= adminUsers %></strong>
                <span>Admins</span>
            </div>
        </div>

        <% if (users == null || users.isEmpty()) { %>

            <div class="empty">
                <h2>No users found</h2>
                <div>The system does not currently have any users to display.</div>
            </div>

        <% } else { %>

            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>User</th>
                        <th>Role</th>
                        <th>Status</th>
                        <th>Faculty</th>
                        <th>Department</th>
                        <th>Admission Year</th>
                        <th>Actions</th>
                    </tr>
                    </thead>

                    <tbody>
                    <%
                        for (User user : users) {
                            boolean isActive =
                                    user.getStatus() != null
                                    && "active".equalsIgnoreCase(user.getStatus());

                            boolean isBlocked =
                                    user.getStatus() != null
                                    && "blocked".equalsIgnoreCase(user.getStatus());

                            boolean isAdmin =
                                    user.getRole() != null
                                    && "admin".equalsIgnoreCase(user.getRole());

                            boolean isCurrentUser =
                                    currentUser != null
                                    && currentUser.getId() == user.getId();
                    %>
                        <tr>
                            <td><%= h(user.getId()) %></td>

                            <td>
                                <div class="user-name"><%= h(user.getName()) %></div>
                                <div class="muted"><%= h(user.getEmail()) %></div>
                            </td>

                            <td>
                                <span class="<%= roleClass(user.getRole()) %>">
                                    <%= h(user.getRole()) %>
                                </span>
                            </td>

                            <td>
                                <span class="<%= statusClass(user.getStatus()) %>">
                                    <%= h(user.getStatus()) %>
                                </span>
                            </td>

                            <td><%= h(user.getFaculty()) %></td>

                            <td><%= h(user.getDepartmentId()) %></td>

                            <td><%= h(user.getAdmissionYear()) %></td>

                            <td>
                                <% if (isAdmin) { %>

                                    <span class="protected">Protected admin</span>

                                <% } else if (isCurrentUser) { %>

                                    <span class="current">Current account</span>

                                <% } else { %>

                                    <div class="action-buttons">

                                        <% if (isActive) { %>
                                            <form action="<%= request.getContextPath() %>/update-user-status" method="post">
                                                <input type="hidden" name="userId" value="<%= h(user.getId()) %>">
                                                <input type="hidden" name="action" value="block">
                                                <button class="block-btn" type="submit">Block</button>
                                            </form>
                                        <% } else if (isBlocked) { %>
                                            <form action="<%= request.getContextPath() %>/update-user-status" method="post">
                                                <input type="hidden" name="userId" value="<%= h(user.getId()) %>">
                                                <input type="hidden" name="action" value="unblock">
                                                <button class="unblock-btn" type="submit">Unblock</button>
                                            </form>
                                        <% } else { %>
                                            <span class="action-note">No status action</span>
                                        <% } %>

                                        <form action="<%= request.getContextPath() %>/delete-user"
                                              method="post"
                                              onsubmit="return confirm('Are you sure you want to delete this user? This action cannot be undone.');">
                                            <input type="hidden" name="userId" value="<%= h(user.getId()) %>">
                                            <button class="delete-btn" type="submit">Delete</button>
                                        </form>

                                    </div>

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