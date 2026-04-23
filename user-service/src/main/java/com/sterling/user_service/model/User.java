package com.sterling.user_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// @Entity tells JPA/Hibernate: "This class represents a database table."
// Hibernate will automatically create a table called "users" for this class.
@Entity

// @Table(name = "users") explicitly names the table "users".
// Without this, Hibernate uses the class name ("user") which can conflict
// with SQL reserved keywords in some databases.
@Table(name = "users")

// Lombok annotations — these generate code automatically:
// @Data = generates getters, setters, toString, equals, hashCode
// @NoArgsConstructor = generates empty constructor: new User()
// @AllArgsConstructor = generates constructor with all fields: new User(id, username, ...)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    // @Id = this field is the PRIMARY KEY of the table (unique identifier for each row)
    // @GeneratedValue = database auto-generates this value (auto-increment)
    // IDENTITY strategy means: let the database handle ID generation (1, 2, 3, 4...)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column(unique = true) means no two users can have the same username.
    // nullable = false means this field cannot be empty/null in the database.
    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    // IMPORTANT: We never store plain text passwords.
    // This field stores the BCrypt-hashed version of the password.
    // BCrypt is a one-way hashing algorithm — you can never reverse it back to plain text.
    @Column(nullable = false)
    private String password;

    // Role controls what this user is allowed to do.
    // "ROLE_USER" = regular user, "ROLE_ADMIN" = administrator
    // Spring Security looks for the "ROLE_" prefix when checking permissions.
    @Column(nullable = false)
    private String role;
}