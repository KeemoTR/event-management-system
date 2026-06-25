<%@ page import="java.util.List" %>
<%@ page import="eventsystem.model.Department" %>
<%@ page import="eventsystem.model.EventCategory" %>
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

    private boolean selected(String currentValue, String optionValue) {
        if (currentValue == null || optionValue == null) return false;
        return currentValue.equalsIgnoreCase(optionValue);
    }
%>

<%
    List<Department> departments =
            (List<Department>) request.getAttribute("departments");

    List<EventCategory> categories =
            (List<EventCategory>) request.getAttribute("categories");

    String error = (String) request.getAttribute("error");
    String success = request.getParameter("success");

    Object departmentCount = request.getAttribute("departmentCount");
    Object categoryCount = request.getAttribute("categoryCount");
    Object eventCount = request.getAttribute("eventCount");
    Object userCount = request.getAttribute("userCount");
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Admin System Data | Campus Events</title>

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
            margin-bottom: 18px;
        }

        .summary-box {
            background: #f9fafb;
            border: 1px solid #e5e7eb;
            border-radius: 12px;
            padding: 12px 14px;
            min-width: 140px;
        }

        .summary-box strong {
            display: block;
            font-size: 22px;
            color: #111827;
        }

        .summary-box span {
            color: #6b7280;
            font-size: 13px;
        }

        .section-title {
            margin: 0 0 14px;
            font-size: 20px;
            color: #111827;
        }

        .form-grid {
            display: grid;
            grid-template-columns: 1.5fr 1fr auto;
            gap: 10px;
            align-items: end;
            margin-bottom: 18px;
        }

        label {
            display: block;
            font-weight: bold;
            color: #374151;
            margin-bottom: 6px;
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
            font-family: Arial, sans-serif;
        }

        button {
            border: 0;
            border-radius: 9px;
            padding: 10px 12px;
            font-weight: bold;
            cursor: pointer;
            color: white;
            font-size: 14px;
            font-family: Arial, sans-serif;
        }

        .add-btn {
            background: #2563eb;
        }

        .update-btn {
            background: #7c3aed;
        }

        .delete-btn {
            background: #dc2626;
        }

        .add-btn:hover {
            background: #1d4ed8;
        }

        .update-btn:hover {
            background: #6d28d9;
        }

        .delete-btn:hover {
            background: #b91c1c;
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

        .name {
            font-weight: bold;
            color: #111827;
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

        .academic {
            background: #dbeafe;
            color: #1e40af;
        }

        .club {
            background: #ecfdf5;
            color: #047857;
        }

        .row-actions {
            display: flex;
            gap: 8px;
            flex-wrap: wrap;
        }

        .inline-form {
            display: grid;
            grid-template-columns: 1.5fr 1fr auto auto;
            gap: 8px;
            align-items: center;
        }

        .inline-category-form {
            display: grid;
            grid-template-columns: 1.5fr auto auto;
            gap: 8px;
            align-items: center;
        }

        .empty {
            color: #6b7280;
            padding: 20px 0;
        }

        form {
            margin: 0;
        }

        @media (max-width: 900px) {
            .topbar {
                align-items: flex-start;
                flex-direction: column;
            }

            .form-grid,
            .inline-form,
            .inline-category-form {
                grid-template-columns: 1fr;
            }

            table {
                min-width: 900px;
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
            <h1>Admin - System Data</h1>
            <div class="subtitle">
                Manage departments, clubs, and event categories used across the system.
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

        <% if ("true".equals(success)) { %>
            <div class="alert success">System data updated successfully.</div>
        <% } %>

        <% if (error != null) { %>
            <div class="alert error"><%= h(error) %></div>
        <% } %>

        <div class="summary">
            <div class="summary-box">
                <strong><%= h(departmentCount) %></strong>
                <span>Departments / Clubs</span>
            </div>

            <div class="summary-box">
                <strong><%= h(categoryCount) %></strong>
                <span>Categories</span>
            </div>

            <div class="summary-box">
                <strong><%= h(eventCount) %></strong>
                <span>Events</span>
            </div>

            <div class="summary-box">
                <strong><%= h(userCount) %></strong>
                <span>Users</span>
            </div>
        </div>

    </div>

    <div class="card">
        <h2 class="section-title">Add Department / Club</h2>

        <form action="<%= request.getContextPath() %>/admin-system-data" method="post">
            <input type="hidden" name="entity" value="department">
            <input type="hidden" name="action" value="add">

            <div class="form-grid">
                <div>
                    <label for="departmentName">Name</label>
                    <input id="departmentName"
                           type="text"
                           name="departmentName"
                           placeholder="Example: Computer Science"
                           required>
                </div>

                <div>
                    <label for="unitType">Unit Type</label>
                    <select id="unitType" name="unitType" required>
                        <option value="">Select type</option>
                        <option value="academic_department">Academic Department</option>
                        <option value="club">Club</option>
                    </select>
                </div>

                <div>
                    <button class="add-btn" type="submit">Add</button>
                </div>
            </div>
        </form>

        <h2 class="section-title">Departments / Clubs</h2>

        <% if (departments == null || departments.isEmpty()) { %>

            <div class="empty">No departments or clubs found.</div>

        <% } else { %>

            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Edit Department / Club</th>
                    </tr>
                    </thead>

                    <tbody>
                    <% for (Department department : departments) { %>
                        <tr>
                            <td><%= h(department.getId()) %></td>

                            <td>
                                <form class="inline-form"
                                      action="<%= request.getContextPath() %>/admin-system-data"
                                      method="post">

                                    <input type="hidden" name="entity" value="department">
                                    <input type="hidden" name="departmentId" value="<%= h(department.getId()) %>">

                                    <input type="text"
                                           name="departmentName"
                                           value="<%= h(department.getName()) %>"
                                           required>

                                    <select name="unitType" required>
                                        <option value="academic_department"
                                                <%= selected(department.getUnitType(), "academic_department") ? "selected" : "" %>>
                                            Academic Department
                                        </option>
                                        <option value="club"
                                                <%= selected(department.getUnitType(), "club") ? "selected" : "" %>>
                                            Club
                                        </option>
                                    </select>

                                    <button class="update-btn"
                                            type="submit"
                                            name="action"
                                            value="update">
                                        Update
                                    </button>

                                    <button class="delete-btn"
                                            type="submit"
                                            name="action"
                                            value="delete"
                                            onclick="return confirm('Delete this department or club? This is allowed only if it is not used by users or events.');">
                                        Delete
                                    </button>
                                </form>
                            </td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>

        <% } %>
    </div>

    <div class="card">
        <h2 class="section-title">Add Event Category</h2>

        <form action="<%= request.getContextPath() %>/admin-system-data" method="post">
            <input type="hidden" name="entity" value="category">
            <input type="hidden" name="action" value="add">

            <div class="form-grid">
                <div>
                    <label for="categoryName">Category Name</label>
                    <input id="categoryName"
                           type="text"
                           name="categoryName"
                           placeholder="Example: Technical"
                           required>
                </div>

                <div></div>

                <div>
                    <button class="add-btn" type="submit">Add</button>
                </div>
            </div>
        </form>

        <h2 class="section-title">Event Categories</h2>

        <% if (categories == null || categories.isEmpty()) { %>

            <div class="empty">No event categories found.</div>

        <% } else { %>

            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>ID</th>
                        <th>Edit Category</th>
                    </tr>
                    </thead>

                    <tbody>
                    <% for (EventCategory category : categories) { %>
                        <tr>
                            <td><%= h(category.getId()) %></td>

                            <td>
                                <form class="inline-category-form"
                                      action="<%= request.getContextPath() %>/admin-system-data"
                                      method="post">

                                    <input type="hidden" name="entity" value="category">
                                    <input type="hidden" name="categoryId" value="<%= h(category.getId()) %>">

                                    <input type="text"
                                           name="categoryName"
                                           value="<%= h(category.getName()) %>"
                                           required>

                                    <button class="update-btn"
                                            type="submit"
                                            name="action"
                                            value="update">
                                        Update
                                    </button>

                                    <button class="delete-btn"
                                            type="submit"
                                            name="action"
                                            value="delete"
                                            onclick="return confirm('Delete this category? This is allowed only if it is not used by events.');">
                                        Delete
                                    </button>
                                </form>
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