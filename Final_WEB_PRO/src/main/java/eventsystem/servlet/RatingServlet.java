package eventsystem.servlet;

import eventsystem.model.User;
import eventsystem.service.RatingService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/rate-event")
public class RatingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final RatingService ratingService = new RatingService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        try {
            if (session == null || !(session.getAttribute("user") instanceof User)) {
                response.sendRedirect(request.getContextPath() + "/login?next=/my-reservations");
                return;
            }

            User loggedInUser = (User) session.getAttribute("user");

            if (!"student".equalsIgnoreCase(loggedInUser.getRole())) {
                session.setAttribute("error", "Only students can rate events.");
                response.sendRedirect(request.getContextPath() + "/my-reservations");
                return;
            }

            String eventIdText = request.getParameter("eventId");
            String ratingText = request.getParameter("rating");
            String comment = request.getParameter("comment");

            if (eventIdText == null || eventIdText.trim().isEmpty()) {
                session.setAttribute("error", "Event ID is required.");
                response.sendRedirect(request.getContextPath() + "/my-reservations");
                return;
            }

            if (ratingText == null || ratingText.trim().isEmpty()) {
                session.setAttribute("error", "Rating value is required.");
                response.sendRedirect(request.getContextPath() + "/my-reservations");
                return;
            }

            int studentId = loggedInUser.getId();
            int eventId = Integer.parseInt(eventIdText.trim());
            int ratingValue = Integer.parseInt(ratingText.trim());

            if (ratingValue < 1 || ratingValue > 5) {
                session.setAttribute("error", "Rating must be between 1 and 5.");
                response.sendRedirect(request.getContextPath() + "/my-reservations");
                return;
            }

            ratingService.addRating(studentId, eventId, ratingValue, comment);

            response.sendRedirect(request.getContextPath() + "/my-reservations?rated=success");

        } catch (Exception e) {
            if (session != null) {
                session.setAttribute("error", e.getMessage());
            }

            response.sendRedirect(request.getContextPath() + "/my-reservations");
        }
    }
}