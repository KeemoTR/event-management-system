package eventsystem.servlet;

import eventsystem.model.User;
import eventsystem.service.ProfileService;
import eventsystem.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final ProfileService profileService = new ProfileService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User sessionUser = getLoggedInUser(request);

        if (sessionUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            User profileUser = profileService.getProfile(sessionUser.getId());

            request.setAttribute("profileUser", profileUser);
            request.setAttribute("departments", loadDepartments());
            request.getRequestDispatcher("/profile.jsp").forward(request, response);

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("departments", loadDepartments());
            request.getRequestDispatcher("/profile.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        User sessionUser = getLoggedInUser(request);

        if (sessionUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            String name = request.getParameter("name");
            String faculty = request.getParameter("faculty");
            Integer departmentId = parseInteger(request.getParameter("departmentId"), "Department is required.");
            Integer admissionYear = parseInteger(request.getParameter("admissionYear"), "Admission year is required.");

            User updatedUser = profileService.updateProfile(
                    sessionUser.getId(),
                    name,
                    faculty,
                    departmentId,
                    admissionYear
            );

            HttpSession session = request.getSession(false);
            if (session != null) {
                session.setAttribute("user", updatedUser);
                session.setAttribute("userId", updatedUser.getId());
                session.setAttribute("role", updatedUser.getRole());
            }

            response.sendRedirect(request.getContextPath() + "/profile?success=updated");

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());

            try {
                request.setAttribute("profileUser", profileService.getProfile(sessionUser.getId()));
            } catch (Exception ignored) {
            }

            request.setAttribute("departments", loadDepartments());
            request.getRequestDispatcher("/profile.jsp").forward(request, response);
        }
    }

    private User getLoggedInUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("user") instanceof User) {
            return (User) session.getAttribute("user");
        }

        return null;
    }

    private List<Map<String, Object>> loadDepartments() {
        List<Map<String, Object>> departments = new ArrayList<>();
        String sql = "SELECT id, name FROM departments ORDER BY name";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()
        ) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("name", rs.getString("name"));
                departments.add(row);
            }
        } catch (Exception ignored) {
        }

        return departments;
    }

    private Integer parseInteger(String value, String errorMessage) {
        try {
            if (value == null || value.trim().isEmpty()) {
                throw new NumberFormatException();
            }

            return Integer.parseInt(value.trim());

        } catch (NumberFormatException e) {
            throw new ProfileService.ServiceException(errorMessage);
        }
    }
}