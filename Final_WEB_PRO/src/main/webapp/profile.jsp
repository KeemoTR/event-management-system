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

    private boolean selected(Object currentValue, Object optionValue) {
        if (currentValue == null || optionValue == null) return false;
        return currentValue.toString().equals(optionValue.toString());
    }
%>

<%
    User profileUser = (User) request.getAttribute("profileUser");

    List<Map<String, Object>> departments =
            (List<Map<String, Object>>) request.getAttribute("departments");

    String error = (String) request.getAttribute("error");
    String success = request.getParameter("success");
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Profile | Campus Events</title>

    <style>
        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: #f4f6f8;
            color: #1f2937;
        }

        .page {
            max-width: 850px;
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
        }

        .error {
            background: #fee2e2;
            color: #991b1b;
        }

        .success {
            background: #dcfce7;
            color: #166534;
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
        }

        input,
        select {
            width: 100%;
            box-sizing: border-box;
            padding: 11px 12px;
            border: 1px solid #d1d5db;
            border-radius: 10px;
            font-size: 15px;
            background: white;
        }

        .readonly {
            background: #f9fafb;
            color: #6b7280;
        }

        .role-box {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
            margin-bottom: 18px;
        }

        .badge {
            display: inline-block;
            padding: 7px 11px;
            border-radius: 999px;
            font-size: 13px;
            font-weight: bold;
        }

        .role {
            background: #dbeafe;
            color: #1e40af;
        }

        .active {
            background: #dcfce7;
            color: #166534;
        }

        .blocked {
            background: #fee2e2;
            color: #991b1b;
        }

        .note {
            margin-top: 6px;
            color: #6b7280;
            font-size: 13px;
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

        .empty {
            text-align: center;
            color: #6b7280;
            padding: 32px;
        }

        @media (max-width: 750px) {
            .topbar {
                flex-direction: column;
                align-items: flex-start;
            }

            .grid {
                grid-template-columns: 1fr;
            }
        }
    </style>
</head>

<body>
<div class="page">

    <div class="topbar">
        <div>
            <h1>My Profile</h1>
            <div class="subtitle">
                View your account information and update your personal details.
            </div>
        </div>

        <div class="nav">
            <a href="<%= request.getContextPath() %>/events">Browse Events</a>

            <% if (profileUser != null && ("organizer".equalsIgnoreCase(profileUser.getRole())
                    || "admin".equalsIgnoreCase(profileUser.getRole()))) { %>
                <a href="<%= request.getContextPath() %>/organizer/manage-events">Manage Events</a>
            <% } %>

            <% if (profileUser != null && "admin".equalsIgnoreCase(profileUser.getRole())) { %>
                <a href="<%= request.getContextPath() %>/admin-users">Admin Users</a>
            <% } %>

            <a href="<%= request.getContextPath() %>/logout">Logout</a>
        </div>
    </div>

    <div class="card">

        <% if (error != null) { %>
            <div class="alert error"><%= h(error) %></div>
        <% } %>

        <% if ("updated".equals(success)) { %>
            <div class="alert success">Profile updated successfully.</div>
        <% } %>

        <% if (profileUser == null) { %>

            <div class="empty">
                Profile could not be loaded.
            </div>

        <% } else { %>

            <div class="role-box">
                <span class="badge role">Role: <%= h(profileUser.getRole()) %></span>

                <% if ("active".equalsIgnoreCase(profileUser.getStatus())) { %>
                    <span class="badge active">Status: active</span>
                <% } else { %>
                    <span class="badge blocked">Status: <%= h(profileUser.getStatus()) %></span>
                <% } %>
            </div>

            <form method="post" action="<%= request.getContextPath() %>/profile">

                <div class="grid">

                    <div class="full">
                        <label for="email">Email</label>
                        <input id="email"
                               class="readonly"
                               type="email"
                               value="<%= h(profileUser.getEmail()) %>"
                               readonly>
                        <div class="note">Email and role cannot be changed from profile.</div>
                    </div>

                    <div class="full">
                        <label for="name">Full Name</label>
                        <input id="name"
                               type="text"
                               name="name"
                               value="<%= h(profileUser.getName()) %>"
                               required>
                    </div>

                    <div>
                        <label for="faculty">Faculty</label>
                        <input id="faculty"
                               type="text"
                               name="faculty"
                               value="<%= h(profileUser.getFaculty()) %>"
                               required>
                    </div>

                    <div>
                        <label for="admissionYear">Admission Year</label>
                        <input id="admissionYear"
                               type="number"
                               name="admissionYear"
                               min="2000"
                               max="2100"
                               value="<%= h(profileUser.getAdmissionYear()) %>"
                               required>
                    </div>

                    <div class="full">
                        <label for="departmentId">Department</label>

                        <% if (departments != null && !departments.isEmpty()) { %>
                            <select id="departmentId" name="departmentId" required>
                                <% for (Map<String, Object> department : departments) { %>
                                    <option value="<%= h(department.get("id")) %>"
                                            <%= selected(profileUser.getDepartmentId(), department.get("id")) ? "selected" : "" %>>
                                        <%= h(department.get("name")) %>
                                    </option>
                                <% } %>
                            </select>
                        <% } else { %>
                            <input id="departmentId"
                                   type="number"
                                   name="departmentId"
                                   value="<%= h(profileUser.getDepartmentId()) %>"
                                   required>
                        <% } %>
                    </div>

                </div>

                <div class="actions">
                    <button type="submit">Save Profile</button>
                    <a class="cancel" href="<%= request.getContextPath() %>/events">Cancel</a>
                </div>

            </form>

        <% } %>

    </div>
</div>
</body>
</html>