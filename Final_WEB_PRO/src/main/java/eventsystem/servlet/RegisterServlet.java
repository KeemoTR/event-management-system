package eventsystem.servlet;

import eventsystem.model.User;
import eventsystem.service.WebAuthService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final WebAuthService authService = new WebAuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setAttribute("departments", loadDepartments());
        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String faculty = request.getParameter("faculty");
        String departmentIdText = request.getParameter("departmentId");
        String admissionYearText = request.getParameter("admissionYear");

        try {
            if (password == null || !password.equals(confirmPassword)) {
                throw new WebAuthService.ServiceException("Passwords do not match.");
            }

            Integer departmentId = parseInteger(departmentIdText, "Department is required.");
            Integer admissionYear = parseInteger(admissionYearText, "Admission year is required.");

            User user = authService.registerStudent(
                    name,
                    email,
                    password,
                    faculty,
                    departmentId,
                    admissionYear
            );

            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("userId", user.getId());
            session.setAttribute("role", user.getRole());
            session.setMaxInactiveInterval(30 * 60);

            response.sendRedirect(request.getContextPath() + "/events?registered=success");

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());

            request.setAttribute("name", name);
            request.setAttribute("email", email);
            request.setAttribute("faculty", faculty);
            request.setAttribute("departmentId", departmentIdText);
            request.setAttribute("admissionYear", admissionYearText);

            request.setAttribute("departments", loadDepartments());
            request.getRequestDispatcher("/register.jsp").forward(request, response);
        }
    }

    private List<Map<String, Object>> loadDepartments() {
        List<Map<String, Object>> departments = new ArrayList<>();

        String sql = "SELECT id, name FROM departments ORDER BY name";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                Map<String, Object> department = new HashMap<>();
                department.put("id", resultSet.getInt("id"));
                department.put("name", resultSet.getString("name"));
                departments.add(department);
            }

        } catch (Exception ignored) {
            // If departments cannot be loaded, the JSP will still show a manual department ID input.
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
            throw new WebAuthService.ServiceException(errorMessage);
        }
    }
}