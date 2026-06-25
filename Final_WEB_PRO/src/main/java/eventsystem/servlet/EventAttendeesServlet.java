package eventsystem.servlet;

import eventsystem.model.Event;
import eventsystem.model.User;
import eventsystem.service.OrganizerEventService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/organizer/event-attendees")
public class EventAttendeesServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final OrganizerEventService organizerEventService = new OrganizerEventService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = getLoggedInUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            int eventId = parseInteger(request.getParameter("eventId"), "Event id is required.");

            Event event = organizerEventService.getEventForManagement(user.getId(), user.getRole(), eventId);
            List<Map<String, Object>> attendees =
                    organizerEventService.getEventAttendees(user.getId(), user.getRole(), eventId);

            request.setAttribute("event", event);
            request.setAttribute("attendees", attendees);
            request.getRequestDispatcher("/event-attendees.jsp").forward(request, response);

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/event-attendees.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User user = getLoggedInUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            int eventId = parseInteger(request.getParameter("eventId"), "Event id is required.");
            int reservationId = parseInteger(request.getParameter("reservationId"), "Reservation id is required.");
            String attendanceStatus = request.getParameter("attendanceStatus");

            organizerEventService.markAttendance(
                    user.getId(),
                    user.getRole(),
                    eventId,
                    reservationId,
                    attendanceStatus
            );

            response.sendRedirect(
                    request.getContextPath()
                            + "/organizer/event-attendees?eventId="
                            + eventId
                            + "&success=attendance"
            );

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
            doGet(request, response);
        }
    }

    private User getLoggedInUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("user") instanceof User) {
            return (User) session.getAttribute("user");
        }

        return null;
    }

    private int parseInteger(String value, String errorMessage) {
        try {
            if (value == null || value.trim().isEmpty()) {
                throw new NumberFormatException();
            }

            return Integer.parseInt(value.trim());

        } catch (NumberFormatException e) {
            throw new OrganizerEventService.ServiceException(errorMessage);
        }
    }
}