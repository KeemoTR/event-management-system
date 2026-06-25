package eventsystem.servlet;

import eventsystem.model.Event;
import eventsystem.model.User;
import eventsystem.service.EventService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/create-event")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 5 * 1024 * 1024,
        maxRequestSize = 6 * 1024 * 1024
)
public class CreateEventServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024L * 1024L;

    private final EventService eventService = new EventService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        prepareCreateEventPage(request);
        request.getRequestDispatcher("/create-event.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        try {
            if (session == null || !(session.getAttribute("user") instanceof User)) {
                response.sendRedirect(request.getContextPath() + "/login");
                return;
            }

            User loggedInUser = (User) session.getAttribute("user");

            if (!"organizer".equalsIgnoreCase(loggedInUser.getRole())
                    && !"admin".equalsIgnoreCase(loggedInUser.getRole())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Only organizers and admins can create events.");
                return;
            }

            int actorId = loggedInUser.getId();

            String title = request.getParameter("title");
            String description = request.getParameter("description");
            String eventType = request.getParameter("eventType");
            String location = request.getParameter("location");

            Integer departmentId = parseInteger(
                    request.getParameter("departmentId"),
                    "Department is required."
            );

            Integer categoryId = parseInteger(
                    request.getParameter("categoryId"),
                    "Category is required."
            );

            Integer capacity = parseInteger(
                    request.getParameter("capacity"),
                    "Capacity is required."
            );

            String dateTimeValue = request.getParameter("eventDateTime");

            if (dateTimeValue == null || dateTimeValue.trim().isEmpty()) {
                throw new IllegalArgumentException("Event date and time is required.");
            }

            LocalDateTime eventDateTime = LocalDateTime.parse(dateTimeValue);

            String imagePath = saveUploadedImage(request, "imageFile");

            Event createdEvent = eventService.createEventUsingFactory(
                    actorId,
                    eventType,
                    title,
                    description,
                    departmentId,
                    eventDateTime,
                    location,
                    capacity,
                    categoryId,
                    imagePath
            );

            response.sendRedirect(
                    request.getContextPath()
                            + "/organizer/manage-events?success=created"
            );

        } catch (IllegalStateException e) {
            prepareCreateEventPage(request);
            request.setAttribute("error", "Event image must be 5MB or less.");
            request.getRequestDispatcher("/create-event.jsp").forward(request, response);

        } catch (Exception e) {
            prepareCreateEventPage(request);
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/create-event.jsp").forward(request, response);
        }
    }

    private void prepareCreateEventPage(HttpServletRequest request) {
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

        if (!isAllowedImageContentType(contentType)) {
            throw new IllegalArgumentException("Only JPG, PNG, GIF, and WEBP images are allowed.");
        }

        String submittedFileName = imagePart.getSubmittedFileName();

        if (submittedFileName == null || submittedFileName.trim().isEmpty()) {
            return null;
        }

        String originalFileName = Paths.get(submittedFileName)
                .getFileName()
                .toString();

        String extension = getFileExtension(originalFileName);

        if (!isAllowedImageExtension(extension)) {
            throw new IllegalArgumentException("Invalid image extension. Allowed: jpg, jpeg, png, gif, webp.");
        }

        String newFileName = System.currentTimeMillis()
                + "_"
                + java.util.UUID.randomUUID()
                + extension;

        String uploadRoot = getServletContext().getRealPath("/uploads/events");

        if (uploadRoot == null) {
            throw new IOException("Upload folder is not available.");
        }

        Path uploadDirectory = Paths.get(uploadRoot);
        Files.createDirectories(uploadDirectory);

        Path savedFilePath = uploadDirectory.resolve(newFileName);

        imagePart.write(savedFilePath.toString());

        return "uploads/events/" + newFileName;
    }

    private boolean isAllowedImageContentType(String contentType) {
        if (contentType == null) {
            return false;
        }

        String type = contentType.toLowerCase();

        return "image/jpeg".equals(type)
                || "image/png".equals(type)
                || "image/gif".equals(type)
                || "image/webp".equals(type);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex == -1) {
            return "";
        }

        return fileName.substring(dotIndex).toLowerCase();
    }

    private boolean isAllowedImageExtension(String extension) {
        return ".jpg".equals(extension)
                || ".jpeg".equals(extension)
                || ".png".equals(extension)
                || ".gif".equals(extension)
                || ".webp".equals(extension);
    }

    private Integer parseInteger(String value, String errorMessage) {
        try {
            if (value == null || value.trim().isEmpty()) {
                throw new NumberFormatException();
            }

            return Integer.parseInt(value.trim());

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private List<Map<String, Object>> loadDepartments() {
        List<Map<String, Object>> departments = new ArrayList<>();

        String sql =
                "SELECT id, name " +
                "FROM departments " +
                "ORDER BY name";

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

        } catch (Exception e) {
            // If departments fail to load, create-event.jsp can fall back or show empty options.
        }

        return departments;
    }

    private List<Map<String, Object>> loadCategories() {
        List<Map<String, Object>> categories = new ArrayList<>();

        String sql =
                "SELECT id, name " +
                "FROM event_categories " +
                "ORDER BY name";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                Map<String, Object> category = new HashMap<>();
                category.put("id", resultSet.getInt("id"));
                category.put("name", resultSet.getString("name"));
                categories.add(category);
            }

        } catch (Exception e) {
            // If categories fail to load, create-event.jsp can fall back or show empty options.
        }

        return categories;
    }
}