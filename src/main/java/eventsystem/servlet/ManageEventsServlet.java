package eventsystem.servlet;

import eventsystem.model.Event;
import eventsystem.model.User;
import eventsystem.service.EventService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/organizer/manage-events")
public class ManageEventsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final EventService eventService = new EventService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User loggedInUser = getLoggedInUser(request, response);
        if (loggedInUser == null) {
            return;
        }

        showManageEventsPage(request, response, loggedInUser);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        User loggedInUser = getLoggedInUser(request, response);
        if (loggedInUser == null) {
            return;
        }

        try {
            int eventId = parseInteger(request.getParameter("eventId"), "Event id is required.");
            String action = request.getParameter("action");

            if (action == null || action.trim().isEmpty()) {
                throw new EventService.ServiceException("Action is required.");
            }

            action = action.trim().toLowerCase();

            if ("close".equals(action)) {
                eventService.closeRegistration(loggedInUser.getId(), eventId);
                response.sendRedirect(request.getContextPath() + "/organizer/manage-events?success=closed");
                return;
            }

            if ("reopen".equals(action)) {
                eventService.reopenRegistration(loggedInUser.getId(), eventId);
                response.sendRedirect(request.getContextPath() + "/organizer/manage-events?success=reopened");
                return;
            }

            if ("complete".equals(action)) {
                eventService.markCompleted(loggedInUser.getId(), eventId);
                response.sendRedirect(request.getContextPath() + "/organizer/manage-events?success=completed");
                return;
            }

            if ("delete".equals(action)) {
                eventService.deleteEvent(loggedInUser.getId(), eventId);
                response.sendRedirect(request.getContextPath() + "/organizer/manage-events?success=deleted");
                return;
            }

            throw new EventService.ServiceException("Unknown action.");

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
            showManageEventsPage(request, response, loggedInUser);
        }
    }

    private void showManageEventsPage(HttpServletRequest request, HttpServletResponse response, User loggedInUser)
            throws ServletException, IOException {

        eventService.expirePastEvents();

        List<Event> events;

        if ("admin".equalsIgnoreCase(loggedInUser.getRole())) {
            events = eventService.getAllEvents();
        } else {
            events = eventService.getEventsByOrganizer(loggedInUser.getId());
        }

        Map<Integer, Integer> reservationCounts = new HashMap<>();

        for (Event event : events) {
            if (event.getId() != null) {
                reservationCounts.put(
                        event.getId(),
                        eventService.countActiveReservationsForEvent(event.getId())
                );
            }
        }

        request.setAttribute("events", events);
        request.setAttribute("reservationCounts", reservationCounts);
        request.setAttribute("loggedInUser", loggedInUser);

        request.getRequestDispatcher("/organizer-manage-events.jsp").forward(request, response);
    }

    private User getLoggedInUser(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("user") instanceof User) {
            return (User) session.getAttribute("user");
        }

        String next = request.getServletPath();

        response.sendRedirect(
                request.getContextPath()
                        + "/login?next="
                        + URLEncoder.encode(next, StandardCharsets.UTF_8.name())
        );

        return null;
    }

    private int parseInteger(String value, String errorMessage) {
        try {
            if (value == null || value.trim().isEmpty()) {
                throw new NumberFormatException();
            }

            return Integer.parseInt(value.trim());

        } catch (NumberFormatException e) {
            throw new EventService.ServiceException(errorMessage);
        }
    }
}