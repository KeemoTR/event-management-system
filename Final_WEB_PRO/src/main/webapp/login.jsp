<%@ page contentType="text/html;charset=UTF-8" language="java" %>

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

    String email = "";
    if (request.getAttribute("email") != null) {
        email = request.getAttribute("email").toString();
    }

    String next = "";
    if (request.getAttribute("next") != null) {
        next = request.getAttribute("next").toString();
    } else if (request.getParameter("next") != null) {
        next = request.getParameter("next");
    }
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Login | Campus Events</title>

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
            max-width: 420px;
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

        label {
            display: block;
            margin: 14px 0 6px;
            font-weight: bold;
        }

        input {
            width: 100%;
            box-sizing: border-box;
            padding: 12px 14px;
            border: 1px solid #d1d5db;
            border-radius: 10px;
            font-size: 15px;
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
        }

        .error {
            background: #fee2e2;
            color: #991b1b;
        }

        .success {
            background: #dcfce7;
            color: #166534;
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
    </style>
</head>

<body>
<div class="page">
    <div class="card">
        <h1>Welcome back</h1>
        <p class="subtitle">Login to manage campus events and reservations.</p>

        <% if (error != null) { %>
            <div class="alert error"><%= h(error) %></div>
        <% } %>

        <% if ("true".equals(request.getParameter("loggedOut"))) { %>
            <div class="alert success">You have been logged out successfully.</div>
        <% } %>

        <% if ("true".equals(request.getParameter("blocked"))) { %>
            <div class="alert error">Your account is blocked. Please contact the admin.</div>
        <% } %>

        <form action="<%= request.getContextPath() %>/login" method="post">
            <input type="hidden" name="next" value="<%= h(next) %>">

            <label for="email">Email</label>
            <input id="email"
                   type="email"
                   name="email"
                   value="<%= h(email) %>"
                   required>

            <label for="password">Password</label>
            <input id="password"
                   type="password"
                   name="password"
                   required>

            <button type="submit">Login</button>
        </form>

        <div class="link">
            New student?
            <a href="<%= request.getContextPath() %>/register">Create an account</a>
        </div>
    </div>
</div>
</body>
</html>