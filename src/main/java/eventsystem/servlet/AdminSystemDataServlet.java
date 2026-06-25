package eventsystem.servlet;

import eventsystem.model.Department;
import eventsystem.model.EventCategory;
import eventsystem.model.User;
import eventsystem.service.AdminService;
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
import java.util.List;

@WebServlet("/admin-system-data")
public class AdminSystemDataServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final AdminService adminService = new AdminService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        try {
            if (session == null || !(session.getAttribute("user") instanceof User)) {
                response.sendRedirect(request.getContextPath() + "/login?next=/admin-system-data");
                return;
            }

            User loggedInUser = (User) session.getAttribute("user");

            if (!"admin".equalsIgnoreCase(loggedInUser.getRole())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only admins can manage system data.");
                return;
            }

            loadPageData(request, loggedInUser.getId());
            request.getRequestDispatcher("/admin-system-data.jsp").forward(request, response);

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/admin-system-data.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        try {
            if (session == null || !(session.getAttribute("user") instanceof User)) {
                response.sendRedirect(request.getContextPath() + "/login?next=/admin-system-data");
                return;
            }

            User loggedInUser = (User) session.getAttribute("user");

            if (!"admin".equalsIgnoreCase(loggedInUser.getRole())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only admins can manage system data.");
                return;
            }

            String entity = request.getParameter("entity");
            String action = request.getParameter("action");

            if (entity == null || action == null) {
                throw new IllegalArgumentException("Invalid system data request.");
            }

            int adminId = loggedInUser.getId();

            if ("department".equalsIgnoreCase(entity)) {
                handleDepartmentAction(adminId, action, request);
            } else if ("category".equalsIgnoreCase(entity)) {
                handleCategoryAction(adminId, action, request);
            } else {
                throw new IllegalArgumentException("Invalid entity type.");
            }

            response.sendRedirect(request.getContextPath() + "/admin-system-data?success=true");

        } catch (Exception e) {
            if (session != null) {
                session.setAttribute("error", e.getMessage());
            }

            response.sendRedirect(request.getContextPath() + "/admin-system-data");
        }
    }

    private void handleDepartmentAction(int adminId, String action, HttpServletRequest request) {
        if ("add".equalsIgnoreCase(action)) {
            String name = request.getParameter("departmentName");
            String unitType = request.getParameter("unitType");

            validateUnitType(unitType);

            adminService.addDepartment(adminId, name, unitType);
            return;
        }

        if ("update".equalsIgnoreCase(action)) {
            int departmentId = parseInt(request.getParameter("departmentId"), "Department ID is required.");
            String name = request.getParameter("departmentName");
            String unitType = request.getParameter("unitType");

            validateUnitType(unitType);

            adminService.updateDepartment(adminId, departmentId, name, unitType);
            return;
        }

        if ("delete".equalsIgnoreCase(action)) {
            int departmentId = parseInt(request.getParameter("departmentId"), "Department ID is required.");

            int userCount = countRowsByColumn("users", "department_id", departmentId);
            if (userCount > 0) {
                throw new IllegalArgumentException(
                        "Cannot delete this department because it is assigned to users."
                );
            }

            int eventCount = countRowsByColumn("events", "department_id", departmentId);
            if (eventCount > 0) {
                throw new IllegalArgumentException(
                        "Cannot delete this department because it is used by events."
                );
            }

            adminService.deleteDepartment(adminId, departmentId);
            return;
        }

        throw new IllegalArgumentException("Invalid department action.");
    }

    private void handleCategoryAction(int adminId, String action, HttpServletRequest request) {
        if ("add".equalsIgnoreCase(action)) {
            String name = request.getParameter("categoryName");
            adminService.addEventCategory(adminId, name);
            return;
        }

        if ("update".equalsIgnoreCase(action)) {
            int categoryId = parseInt(request.getParameter("categoryId"), "Category ID is required.");
            String name = request.getParameter("categoryName");

            adminService.updateEventCategory(adminId, categoryId, name);
            return;
        }

        if ("delete".equalsIgnoreCase(action)) {
            int categoryId = parseInt(request.getParameter("categoryId"), "Category ID is required.");

            int eventCount = countRowsByColumn("events", "category_id", categoryId);
            if (eventCount > 0) {
                throw new IllegalArgumentException(
                        "Cannot delete this category because it is used by events."
                );
            }

            adminService.deleteEventCategory(adminId, categoryId);
            return;
        }

        throw new IllegalArgumentException("Invalid category action.");
    }

    private void loadPageData(HttpServletRequest request, int adminId) {
        List<Department> departments = adminService.getAllDepartments(adminId);
        List<EventCategory> categories = adminService.getAllEventCategories(adminId);

        request.setAttribute("departments", departments);
        request.setAttribute("categories", categories);

        request.setAttribute("departmentCount", departments == null ? 0 : departments.size());
        request.setAttribute("categoryCount", categories == null ? 0 : categories.size());
        request.setAttribute("eventCount", countAllRows("events"));
        request.setAttribute("userCount", countAllRows("users"));

        HttpSession session = request.getSession(false);

        if (session != null) {
            String error = (String) session.getAttribute("error");

            if (error != null) {
                request.setAttribute("error", error);
                session.removeAttribute("error");
            }
        }
    }

    private int countAllRows(String tableName) {
        String sql = "SELECT COUNT(*) AS total_count FROM " + tableName;

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            if (resultSet.next()) {
                return resultSet.getInt("total_count");
            }

        } catch (Exception ignored) {
        }

        return 0;
    }

    private int countRowsByColumn(String tableName, String columnName, int value) {
        String sql = "SELECT COUNT(*) AS total_count FROM " + tableName + " WHERE " + columnName + " = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setInt(1, value);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total_count");
                }
            }

        } catch (Exception ignored) {
        }

        return 0;
    }

    private int parseInt(String value, String errorMessage) {
        try {
            if (value == null || value.trim().isEmpty()) {
                throw new NumberFormatException();
            }

            return Integer.parseInt(value.trim());

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void validateUnitType(String unitType) {
        if (unitType == null || unitType.trim().isEmpty()) {
            throw new IllegalArgumentException("Unit type is required.");
        }

        if (!"academic_department".equalsIgnoreCase(unitType)
                && !"club".equalsIgnoreCase(unitType)) {
            throw new IllegalArgumentException("Invalid unit type.");
        }
    }
}