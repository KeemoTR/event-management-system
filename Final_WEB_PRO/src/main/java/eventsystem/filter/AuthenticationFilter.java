package eventsystem.filter;

import eventsystem.model.User;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebFilter(urlPatterns = {
        "/profile",

        "/create-event",

        "/organizer/manage-events",
        "/organizer/event-attendees",
        "/organizer/event-ratings",
        "/organizer/edit-event",

        "/reserve-ticket",
        "/cancel-reservation",
        "/my-reservations",
        "/rate-event",

        "/admin-users",
        "/update-user-status",
        "/delete-user",
        "/admin-system-data"
})
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest servletRequest,
            ServletResponse servletResponse,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        HttpSession session = request.getSession(false);
        User user = null;

        if (session != null && session.getAttribute("user") instanceof User) {
            user = (User) session.getAttribute("user");
        }

        if (user == null) {
            redirectToLogin(request, response);
            return;
        }

        if (user.getStatus() != null && !"active".equalsIgnoreCase(user.getStatus())) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/login?blocked=true");
            return;
        }

        if (!isAllowed(user, request.getServletPath())) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "You are not allowed to access this page."
            );
            return;
        }

        chain.doFilter(servletRequest, servletResponse);
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String next = request.getServletPath();

        if (request.getQueryString() != null && !request.getQueryString().trim().isEmpty()) {
            next += "?" + request.getQueryString();
        }

        String encodedNext = URLEncoder.encode(next, StandardCharsets.UTF_8.name());

        response.sendRedirect(
                request.getContextPath() + "/login?next=" + encodedNext
        );
    }

    private boolean isAllowed(User user, String path) {
        String role = user.getRole();

        if (path == null || role == null) {
            return false;
        }

        if (path.equals("/profile")) {
            return true;
        }

        /*
         * Admin-only pages
         */
        if (path.equals("/admin-users")
                || path.equals("/update-user-status")
                || path.equals("/delete-user")
                || path.equals("/admin-system-data")) {
            return "admin".equalsIgnoreCase(role);
        }

        /*
         * Create Event is for organizers only.
         * Admin should not create events from the organizer create page.
         */
        if (path.equals("/create-event")) {
            return "organizer".equalsIgnoreCase(role);
        }

        /*
         * Organizer event management pages.
         * Admin can still manage/view events, but cannot create new events.
         */
        if (path.equals("/organizer/manage-events")
                || path.equals("/organizer/event-attendees")
                || path.equals("/organizer/event-ratings")
                || path.equals("/organizer/edit-event")) {
            return "organizer".equalsIgnoreCase(role)
                    || "admin".equalsIgnoreCase(role);
        }

        /*
         * Student-only pages
         */
        if (path.equals("/reserve-ticket")
                || path.equals("/cancel-reservation")
                || path.equals("/my-reservations")
                || path.equals("/rate-event")) {
            return "student".equalsIgnoreCase(role);
        }

        return false;
    }
}