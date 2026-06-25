package eventsystem.service;

import eventsystem.dao.DepartmentDAO;
import eventsystem.dao.EventCategoryDAO;
import eventsystem.dao.EventDAO;
import eventsystem.dao.UserDAO;
import eventsystem.model.Department;
import eventsystem.model.Event;
import eventsystem.model.EventCategory;
import eventsystem.model.User;
import eventsystem.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class AdminService {

    private static final String ROLE_ADMIN = "admin";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_BLOCKED = "blocked";

    private final UserDAO userDAO;
    private final EventDAO eventDAO;
    private final DepartmentDAO departmentDAO;
    private final EventCategoryDAO eventCategoryDAO;

    public AdminService() {
        this.userDAO = new UserDAO();
        this.eventDAO = new EventDAO();
        this.departmentDAO = new DepartmentDAO();
        this.eventCategoryDAO = new EventCategoryDAO();
    }

    public List<User> getAllUsers(int adminId) {
        validateAdmin(adminId);
        return userDAO.getAllUsers();
    }

    public List<Event> getAllEvents(int adminId) {
        validateAdmin(adminId);
        return eventDAO.getAllEvents();
    }

    public List<Department> getAllDepartments(int adminId) {
        validateAdmin(adminId);
        return departmentDAO.getAllDepartments();
    }

    public List<EventCategory> getAllEventCategories(int adminId) {
        validateAdmin(adminId);
        return eventCategoryDAO.getAllEventCategories();
    }

    public User blockUser(int adminId, int targetUserId) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                validateAdmin(connection, adminId);

                User targetUser = getUserOrThrow(connection, targetUserId);

                if (targetUser.getId().equals(adminId)) {
                    throw new ServiceException("Admin cannot block himself.");
                }

                targetUser.setStatus(STATUS_BLOCKED);

                boolean updated = userDAO.updateUser(targetUser, connection);
                if (!updated) {
                    throw new ServiceException("Failed to block user.");
                }

                connection.commit();
                return targetUser;

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while blocking user.", e);
        }
    }

    public User unblockUser(int adminId, int targetUserId) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                validateAdmin(connection, adminId);

                User targetUser = getUserOrThrow(connection, targetUserId);
                targetUser.setStatus(STATUS_ACTIVE);

                boolean updated = userDAO.updateUser(targetUser, connection);
                if (!updated) {
                    throw new ServiceException("Failed to unblock user.");
                }

                connection.commit();
                return targetUser;

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while unblocking user.", e);
        }
    }

    public void deleteUser(int adminId, int targetUserId) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                validateAdmin(connection, adminId);

                User targetUser = getUserOrThrow(connection, targetUserId);

                if (targetUser.getId().equals(adminId)) {
                    throw new ServiceException("Admin cannot delete himself.");
                }

                boolean deleted = userDAO.deleteUser(targetUserId, connection);
                if (!deleted) {
                    throw new ServiceException("Failed to delete user.");
                }

                connection.commit();

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while deleting user.", e);
        }
    }

    public void deleteAnyEvent(int adminId, int eventId) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                validateAdmin(connection, adminId);

                Event event = eventDAO.getEventById(connection, eventId);
                if (event == null) {
                    throw new ServiceException("Event not found.");
                }

                boolean deleted = eventDAO.deleteEvent(eventId, connection);
                if (!deleted) {
                    throw new ServiceException("Failed to delete event.");
                }

                connection.commit();

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while deleting event.", e);
        }
    }

    public Event updateAnyEvent(int adminId, Event event) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                validateAdmin(connection, adminId);

                if (event == null || event.getId() == null) {
                    throw new ServiceException("Event data is required.");
                }

                Event oldEvent = eventDAO.getEventById(connection, event.getId());
                if (oldEvent == null) {
                    throw new ServiceException("Event not found.");
                }

                boolean updated = eventDAO.updateEvent(event, connection);
                if (!updated) {
                    throw new ServiceException("Failed to update event.");
                }

                connection.commit();
                return event;

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while updating event.", e);
        }
    }

    public Department addDepartment(int adminId, String name, String unitType) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                validateAdmin(connection, adminId);

                Department department = new Department();
                department.setName(cleanRequired(name, "Department name"));
                department.setUnitType(cleanRequired(unitType, "Unit type"));

                boolean added = departmentDAO.addDepartment(department, connection);
                if (!added) {
                    throw new ServiceException("Failed to add department.");
                }

                connection.commit();
                return department;

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while adding department.", e);
        }
    }

    public Department updateDepartment(int adminId, int departmentId, String name, String unitType) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                validateAdmin(connection, adminId);

                Department department = departmentDAO.getDepartmentById(connection, departmentId);
                if (department == null) {
                    throw new ServiceException("Department not found.");
                }

                department.setName(cleanRequired(name, "Department name"));
                department.setUnitType(cleanRequired(unitType, "Unit type"));

                boolean updated = departmentDAO.updateDepartment(department, connection);
                if (!updated) {
                    throw new ServiceException("Failed to update department.");
                }

                connection.commit();
                return department;

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while updating department.", e);
        }
    }

    public void deleteDepartment(int adminId, int departmentId) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                validateAdmin(connection, adminId);

                Department department = departmentDAO.getDepartmentById(connection, departmentId);
                if (department == null) {
                    throw new ServiceException("Department not found.");
                }

                boolean deleted = departmentDAO.deleteDepartment(departmentId, connection);
                if (!deleted) {
                    throw new ServiceException("Failed to delete department.");
                }

                connection.commit();

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while deleting department.", e);
        }
    }

    public EventCategory addEventCategory(int adminId, String name) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                validateAdmin(connection, adminId);

                EventCategory category = new EventCategory();
                category.setName(cleanRequired(name, "Category name"));

                boolean added = eventCategoryDAO.addEventCategory(category, connection);
                if (!added) {
                    throw new ServiceException("Failed to add event category.");
                }

                connection.commit();
                return category;

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while adding event category.", e);
        }
    }

    public EventCategory updateEventCategory(int adminId, int categoryId, String name) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                validateAdmin(connection, adminId);

                EventCategory category = eventCategoryDAO.getEventCategoryById(connection, categoryId);
                if (category == null) {
                    throw new ServiceException("Event category not found.");
                }

                category.setName(cleanRequired(name, "Category name"));

                boolean updated = eventCategoryDAO.updateEventCategory(category, connection);
                if (!updated) {
                    throw new ServiceException("Failed to update event category.");
                }

                connection.commit();
                return category;

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while updating event category.", e);
        }
    }

    public void deleteEventCategory(int adminId, int categoryId) {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                validateAdmin(connection, adminId);

                EventCategory category = eventCategoryDAO.getEventCategoryById(connection, categoryId);
                if (category == null) {
                    throw new ServiceException("Event category not found.");
                }

                boolean deleted = eventCategoryDAO.deleteEventCategory(categoryId, connection);
                if (!deleted) {
                    throw new ServiceException("Failed to delete event category.");
                }

                connection.commit();

            } catch (Exception e) {
                rollback(connection, e);
                throw wrap(e);
            }

        } catch (SQLException e) {
            throw new ServiceException("Database error while deleting event category.", e);
        }
    }

    private void validateAdmin(int adminId) {
        User admin = userDAO.getUserById(adminId);

        if (admin == null) {
            throw new ServiceException("Admin user not found.");
        }

        if (!ROLE_ADMIN.equalsIgnoreCase(admin.getRole())) {
            throw new ServiceException("Only admin can perform this action.");
        }

        if (!STATUS_ACTIVE.equalsIgnoreCase(admin.getStatus())) {
            throw new ServiceException("Blocked admin cannot perform this action.");
        }
    }

    private void validateAdmin(Connection connection, int adminId) {
        User admin = userDAO.getUserById(connection, adminId);

        if (admin == null) {
            throw new ServiceException("Admin user not found.");
        }

        if (!ROLE_ADMIN.equalsIgnoreCase(admin.getRole())) {
            throw new ServiceException("Only admin can perform this action.");
        }

        if (!STATUS_ACTIVE.equalsIgnoreCase(admin.getStatus())) {
            throw new ServiceException("Blocked admin cannot perform this action.");
        }
    }

    private User getUserOrThrow(Connection connection, int userId) {
        User user = userDAO.getUserById(connection, userId);

        if (user == null) {
            throw new ServiceException("User not found.");
        }

        return user;
    }

    private String cleanRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ServiceException(fieldName + " is required.");
        }

        return value.trim();
    }

    private void rollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    private ServiceException wrap(Exception e) {
        if (e instanceof ServiceException) {
            return (ServiceException) e;
        }

        return new ServiceException("Admin service operation failed.", e);
    }

    public static class ServiceException extends RuntimeException {
        public ServiceException(String message) {
            super(message);
        }

        public ServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
