package eventsystem.servlet;

import eventsystem.model.Reservation;
import eventsystem.model.User;
import eventsystem.service.ReservationService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/my-reservations")
public class MyReservationsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final ReservationService reservationService = new ReservationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            HttpSession session = request.getSession(false);

            if (session == null || session.getAttribute("user") == null) {
            	response.sendRedirect(request.getContextPath() + "/login");
            	return;
            }

            User loggedInUser = (User) session.getAttribute("user");

            List<Reservation> reservations =
                    reservationService.getReservationsByStudent(loggedInUser.getId());

            request.setAttribute("reservations", reservations);
            request.getRequestDispatcher("/my-reservations.jsp").forward(request, response);

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/my-reservations.jsp").forward(request, response);
        }
    }
}