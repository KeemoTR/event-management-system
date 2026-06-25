package eventsystem.servlet;

import eventsystem.model.User;
import eventsystem.service.AdminService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/admin-users")
public class ManageUsersServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final AdminService adminService = new AdminService();

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

            List<User> users = adminService.getAllUsers(loggedInUser.getId());

            request.setAttribute("users", users);
            request.getRequestDispatcher("/admin-users.jsp").forward(request, response);

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/admin-users.jsp").forward(request, response);
        }
    }
}
