CREATE DATABASE event_system;
USE event_system;

CREATE TABLE departments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    unit_type ENUM('academic_department', 'club') NOT NULL
);

CREATE TABLE event_categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('student', 'organizer', 'admin') NOT NULL,
    status ENUM('active', 'blocked') NOT NULL DEFAULT 'active',
    faculty VARCHAR(100) NOT NULL,
    department_id INT NOT NULL,
    admission_year INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (department_id) REFERENCES departments(id)
);

CREATE TABLE events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    department_id INT NOT NULL,
    event_date_time DATETIME NOT NULL,
    location VARCHAR(150) NOT NULL,
    capacity INT NOT NULL,
    remaining_seats INT NOT NULL,
    category_id INT NOT NULL,
    event_type ENUM('workshop', 'seminar', 'club_social_event', 'sports_activity') NOT NULL,
    image_path VARCHAR(255),
    status ENUM('open', 'closed', 'completed') NOT NULL DEFAULT 'open',
    organizer_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (department_id) REFERENCES departments(id),
    FOREIGN KEY (category_id) REFERENCES event_categories(id),
    FOREIGN KEY (organizer_id) REFERENCES users(id)
);

CREATE TABLE reservations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    event_id INT NOT NULL,
    reservation_status ENUM('reserved', 'cancelled') NOT NULL DEFAULT 'reserved',
    attendance_status ENUM('present', 'absent') DEFAULT NULL,
    reserved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (student_id) REFERENCES users(id),
    FOREIGN KEY (event_id) REFERENCES events(id),
    UNIQUE (student_id, event_id)
);

CREATE TABLE ratings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    event_id INT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (student_id) REFERENCES users(id),
    FOREIGN KEY (event_id) REFERENCES events(id),
    UNIQUE (student_id, event_id)
);

