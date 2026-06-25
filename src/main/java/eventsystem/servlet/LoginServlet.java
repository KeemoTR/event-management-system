package eventsystem.servlet;

import eventsystem.model.User;
import eventsystem.service.WebAuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final WebAuthService authService = new WebAuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("user") instanceof User) {
            redirectByRole(request, response, (User) session.getAttribute("user"));
            return;
        }

        request.setAttribute("next", request.getParameter("next"));

        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            User user = authService.login(email, password);

            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
            session.setAttribute("role", user.getRole());
            session.setMaxInactiveInterval(30 * 60);

            String next = request.getParameter("next");

            if (isSafeNext(next)) {
                response.sendRedirect(request.getContextPath() + next);
                return;
            }

            redirectByRole(request, response, user);

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("email", email);
            request.setAttribute("next", request.getParameter("next"));
            request.getRequestDispatcher("/login.jsp").forward(request, response);
        }
    }

    private void redirectByRole(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {

        String context = request.getContextPath();
        String role = user.getRole();

        if ("admin".equalsIgnoreCase(role)) {
            response.sendRedirect(context + "/admin-users");
        } else if ("organizer".equalsIgnoreCase(role)) {
            response.sendRedirect(context + "/events");
        } else {
            response.sendRedirect(context + "/events");
        }
    }

    private boolean isSafeNext(String next) {
        if (next == null || next.trim().isEmpty()) {
            return false;
        }

        String value = next.trim();

        return value.startsWith("/")
                && !value.startsWith("//")
                && !value.contains("://")
                && !value.contains("\\");
    }
}