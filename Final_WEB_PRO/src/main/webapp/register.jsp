<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

<%!
    private String h(Object value) {
        if (value == null) return "";
        return value.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
%>

<%
    String error = (String) request.getAttribute("error");

    String name = h(request.getAttribute("name"));
    String email = h(request.getAttribute("email"));
    String faculty = h(request.getAttribute("faculty"));
    String selectedDepartmentId = h(request.getAttribute("departmentId"));
    String admissionYear = h(request.getAttribute("admissionYear"));

    List<Map<String, Object>> departments =
            (List<Map<String, Object>>) request.getAttribute("departments");
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Register | Campus Events</title>

    <style>
        body {
            margin: 0;
            font-family: Arial, sans-serif;
            background: #f4f6f8;
            color: #1f2937;
        }

        .page {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 24px;
        }

        .card {
            width: 100%;
            max-width: 560px;
            background: #ffffff;
            border-radius: 16px;
            padding: 32px;
            box-shadow: 0 12px 35px rgba(0, 0, 0, 0.08);
        }

        h1 {
            margin: 0 0 8px;
            font-size: 28px;
        }

        .subtitle {
            margin: 0 0 24px;
            color: #6b7280;
        }

        .grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 14px;
        }

        .full {
            grid-column: 1 / -1;
        }

        label {
            display: block;
            margin: 14px 0 6px;
            font-weight: bold;
        }

        input,
        select {
            width: 100%;
            box-sizing: border-box;
            padding: 12px 14px;
            border: 1px solid #d1d5db;
            border-radius: 10px;
            font-size: 15px;
            background: white;
        }

        button {
            width: 100%;
            margin-top: 22px;
            padding: 12px;
            border: 0;
            border-radius: 10px;
            background: #2563eb;
            color: white;
            font-weight: bold;
            font-size: 16px;
            cursor: pointer;
        }

        button:hover {
            background: #1d4ed8;
        }

        .alert {
            padding: 12px;
            border-radius: 10px;
            margin-bottom: 16px;
            font-size: 14px;
            background: #fee2e2;
            color: #991b1b;
        }

        .hint {
            font-size: 13px;
            color: #6b7280;
            margin-top: 6px;
        }

        .link {
            margin-top: 18px;
            text-align: center;
        }

        a {
            color: #2563eb;
            text-decoration: none;
            font-weight: bold;
        }

        @media (max-width: 640px) {
            .grid {
                grid-template-columns: 1fr;
            }

            .card {
                padding: 24px;
            }
        }
    </style>
</head>

<body>
<div class="page">
    <div class="card">
        <h1>Create student account</h1>
        <p class="subtitle">
            Registration creates a student account.
            Organizer and admin accounts are managed by the system administrator.
        </p>

        <% if (error != null) { %>
            <div class="alert"><%= h(error) %></div>
        <% } %>

        <form action="<%= request.getContextPath() %>/register" method="post">
            <div class="grid">

                <div class="full">
                    <label for="name">Full name</label>
                    <input id="name"
                           type="text"
                           name="name"
                           value="<%= name %>"
                           required>
                </div>

                <div class="full">
                    <label for="email">Email</label>
                    <input id="email"
                           type="email"
                           name="email"
                           value="<%= email %>"
                           required>
                </div>

                <div>
                    <label for="password">Password</label>
                    <input id="password"
                           type="password"
                           name="password"
                           minlength="6"
                           required>
                </div>

                <div>
                    <label for="confirmPassword">Confirm password</label>
                    <input id="confirmPassword"
                           type="password"
                           name="confirmPassword"
                           minlength="6"
                           required>
                </div>

                <div>
                    <label for="faculty">Faculty</label>
                    <input id="faculty"
                           type="text"
                           name="faculty"
                           value="<%= faculty %>"
                           required>
                </div>

                <div>
                    <label for="admissionYear">Admission year</label>
                    <input id="admissionYear"
                           type="number"
                           name="admissionYear"
                           min="2000"
                           max="2100"
                           value="<%= admissionYear %>"
                           required>
                </div>

                <div class="full">
                    <label for="departmentId">Department / Club</label>

                    <% if (departments != null && !departments.isEmpty()) { %>
                        <select id="departmentId" name="departmentId" required>
                            <option value="">Select department</option>

                            <% for (Map<String, Object> department : departments) {
                                String id = String.valueOf(department.get("id"));
                                String departmentName = String.valueOf(department.get("name"));
                                String selected = id.equals(selectedDepartmentId) ? "selected" : "";
                            %>
                                <option value="<%= h(id) %>" <%= selected %>>
                                    <%= h(departmentName) %>
                                </option>
                            <% } %>
                        </select>
                    <% } else { %>
                        <input id="departmentId"
                               type="number"
                               name="departmentId"
                               value="<%= selectedDepartmentId %>"
                               required>

                        <div class="hint">
                            Departments could not be loaded, so enter department ID manually.
                        </div>
                    <% } %>
                </div>
            </div>

            <button type="submit">Create account</button>
        </form>

        <div class="link">
            Already have an account?
            <a href="<%= request.getContextPath() %>/login">Login</a>
        </div>
    </div>
</div>
</body>
</html>