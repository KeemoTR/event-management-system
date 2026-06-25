package eventsystem.servlet;

import eventsystem.model.User;
import eventsystem.service.ReservationService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/reserve-ticket")
public class ReserveTicketServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final ReservationService reservationService = new ReservationService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        try {
            if (session == null || !(session.getAttribute("user") instanceof User)) {
                response.sendRedirect(request.getContextPath() + "/login?next=/events");
                return;
            }

            User loggedInUser = (User) session.getAttribute("user");

            if (!"student".equalsIgnoreCase(loggedInUser.getRole())) {
                session.setAttribute("error", "Only students can reserve tickets.");
                response.sendRedirect(request.getContextPath() + "/events");
                return;
            }

            String eventIdText = request.getParameter("eventId");

            if (eventIdText == null || eventIdText.trim().isEmpty()) {
                session.setAttribute("error", "Event ID is required.");
                response.sendRedirect(request.getContextPath() + "/events");
                return;
            }

            int studentId = loggedInUser.getId();
            int eventId = Integer.parseInt(eventIdText);

            reservationService.reserveSeat(studentId, eventId);

            response.sendRedirect(request.getContextPath() + "/events?reserved=success");

        } catch (Exception e) {
            if (session != null) {
                session.setAttribute("error", e.getMessage());
            }

            response.sendRedirect(request.getContextPath() + "/events");
        }
    }
}