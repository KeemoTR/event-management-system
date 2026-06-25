package eventsystem.servlet;

import eventsystem.model.Event;
import eventsystem.service.EventSearchService;
import eventsystem.service.EventService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/events")
public class BrowseEventsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final EventService eventService = new EventService();
    private final EventSearchService eventSearchService = new EventSearchService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            eventService.expirePastEvents();

            String title = request.getParameter("title");
            String eventType = request.getParameter("eventType");
            

            if (eventType != null && eventType.trim().isEmpty()) {
                eventType = null;
            }

            if (eventType != null && eventType.equalsIgnoreCase("All")) {
                eventType = null;
            }
            String departmentIdText = request.getParameter("departmentId");
            String categoryIdText = request.getParameter("categoryId");
            String dateText = request.getParameter("date");
            String onlyAvailableText = request.getParameter("onlyAvailable");

            Integer departmentId = parseInteger(departmentIdText);
            Integer categoryId = parseInteger(categoryIdText);
            LocalDate date = parseDate(dateText);
            boolean onlyAvailable = "true".equalsIgnoreCase(onlyAvailableText);

            List<Event> events = eventSearchService.advancedSearch(
                    title,
                    departmentId,
                    categoryId,
                    eventType,
                    date,
                    onlyAvailable
            );

            request.setAttribute("events", events);
            request.getRequestDispatcher("/events.jsp").forward(request, response);

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/events.jsp").forward(request, response);
        }
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return Integer.parseInt(value.trim());
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return LocalDate.parse(value.trim());
    }
}
