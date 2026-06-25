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

@WebServlet("/update-user-status")
public class UpdateUserStatusServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final AdminService adminService = new AdminService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        try {
            if (session == null || session.getAttribute("user") == null) {
            	response.sendRedirect(request.getContextPath() + "/login");                return;
            }

            User loggedInUser = (User) session.getAttribute("user");

            int adminId = loggedInUser.getId();
            int targetUserId = Integer.parseInt(request.getParameter("userId"));
            String action = request.getParameter("action");

            if ("block".equalsIgnoreCase(action)) {
                adminService.blockUser(adminId, targetUserId);
                response.sendRedirect("admin-users?blocked=success");
                return;
            }

            if ("unblock".equalsIgnoreCase(action)) {
                adminService.unblockUser(adminId, targetUserId);
                response.sendRedirect("admin-users?unblocked=success");
                return;
            }

            session.setAttribute("error", "Invalid action.");
            response.sendRedirect("admin-users");

        } catch (Exception e) {
            if (session != null) {
                session.setAttribute("error", e.getMessage());
            }

            response.sendRedirect("admin-users");
        }
    }
}
