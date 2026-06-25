package eventsystem.servlet;

import eventsystem.model.Event;
import eventsystem.model.User;
import eventsystem.service.OrganizerEventService;
import eventsystem.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.*;

@WebServlet("/organizer/edit-event")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 5 * 1024 * 1024,
        maxRequestSize = 6 * 1024 * 1024
)
public class EditEventServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

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

            request.setAttribute("event", event);
            request.setAttribute("departments", loadDepartments());
            request.setAttribute("categories", loadCategories());

            request.getRequestDispatcher("/edit-event.jsp").forward(request, response);

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("departments", loadDepartments());
            request.setAttribute("categories", loadCategories());
            request.getRequestDispatcher("/edit-event.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        User user = getLoggedInUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            int eventId = parseInteger(request.getParameter("eventId"), "Event id is required.");

            String title = request.getParameter("title");
            String description = request.getParameter("description");
            Integer departmentId = parseInteger(request.getParameter("departmentId"), "Department is required.");
            LocalDateTime eventDateTime = LocalDateTime.parse(request.getParameter("eventDateTime"));
            String location = request.getParameter("location");
            Integer capacity = parseInteger(request.getParameter("capacity"), "Capacity is required.");
            Integer categoryId = parseInteger(request.getParameter("categoryId"), "Category is required.");
            String eventType = request.getParameter("eventType");

            String currentImagePath = request.getParameter("currentImagePath");
            String uploadedImagePath = saveUploadedImage(request, "imageFile");

            String imagePath;
            if (uploadedImagePath != null && !uploadedImagePath.trim().isEmpty()) {
                imagePath = uploadedImagePath;
            } else {
                imagePath = currentImagePath;
            }

            organizerEventService.updateEvent(
                    user.getId(),
                    user.getRole(),
                    eventId,
                    title,
                    description,
                    departmentId,
                    eventDateTime,
                    location,
                    capacity,
                    categoryId,
                    eventType,
                    imagePath
            );

            response.sendRedirect(
                    request.getContextPath()
                            + "/organizer/manage-events?success=updated"
            );

        } catch (IllegalStateException e) {
            request.setAttribute("error", "Event image must be 5MB or less.");
            reloadEditPageAfterError(request, user);
            request.getRequestDispatcher("/edit-event.jsp").forward(request, response);

        } catch (Exception e) {
            request.setAttribute("error", e.getMessage());
            reloadEditPageAfterError(request, user);
            request.getRequestDispatcher("/edit-event.jsp").forward(request, response);
        }
    }

    private void reloadEditPageAfterError(HttpServletRequest request, User user) {
        try {
            int eventId = parseInteger(request.getParameter("eventId"), "Event id is required.");
            Event event = organizerEventService.getEventForManagement(user.getId(), user.getRole(), eventId);
            request.setAttribute("event", event);
        } catch (Exception ignored) {
        }

        request.setAttribute("departments", loadDepartments());
        request.setAttribute("categories", loadCategories());
    }

    private String saveUploadedImage(HttpServletRequest request, String partName)
            throws IOException, ServletException {

        Part imagePart = request.getPart(partName);

        if (imagePart == null || imagePart.getSize() == 0) {
            return null;
        }

        if (imagePart.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Event image must be 5MB or less.");
        }

        String contentType = imagePart.getContentType();

        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only JPG, PNG, GIF, and WEBP images are allowed.");
        }

        String originalFileName = Paths.get(imagePart.getSubmittedFileName()).getFileName().toString();

        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return null;
        }

        String extension = getFileExtension(originalFileName);

        if (!isAllowedExtension(extension)) {
            throw new IllegalArgumentException("Invalid image extension. Allowed: jpg, jpeg, png, gif, webp.");
        }

        String newFileName = UUID.randomUUID() + extension;

        String uploadRoot = getServletContext().getRealPath("/uploads/events");

        if (uploadRoot == null) {
            throw new IOException("Upload directory is not available.");
        }

        Path uploadDirectory = Paths.get(uploadRoot);
        Files.createDirectories(uploadDirectory);

        Path savedFilePath = uploadDirectory.resolve(newFileName);
        imagePart.write(savedFilePath.toString());

        return "uploads/events/" + newFileName;
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex == -1) {
            return "";
        }

        return fileName.substring(dotIndex).toLowerCase();
    }

    private boolean isAllowedExtension(String extension) {
        return ".jpg".equals(extension)
                || ".jpeg".equals(extension)
                || ".png".equals(extension)
                || ".gif".equals(extension)
                || ".webp".equals(extension);
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

    private List<Map<String, Object>> loadCategories() {
        List<Map<String, Object>> categories = new ArrayList<>();
        String sql = "SELECT id, name FROM event_categories ORDER BY name";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()
        ) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getInt("id"));
                row.put("name", rs.getString("name"));
                categories.add(row);
            }
        } catch (Exception ignored) {
        }

        return categories;
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