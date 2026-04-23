package com.sterling.user_service.repository;

import com.sterling.user_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

// @Repository marks this as a Spring-managed component
// that handles database operations.
@Repository

// JpaRepository<User, Long>:
//   User = which entity/table this repository manages
//   Long = the data type of the primary key (our id field is Long)
// By extending JpaRepository, you get these methods FOR FREE (no code needed):
//   save(user)         → INSERT or UPDATE
//   findById(id)       → SELECT WHERE id = ?
//   findAll()          → SELECT * FROM users
//   delete(user)       → DELETE
//   count()            → SELECT COUNT(*)
// You only need to write methods for CUSTOM queries.
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA reads the method name and auto-generates the SQL.
    // findByUsername → SELECT * FROM users WHERE username = ?
    // Optional<User> means: the result might exist or might not (prevents NullPointerException)
    Optional<User> findByUsername(String username);

    // SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // Returns true if any row with this username exists
    // SELECT COUNT(*) > 0 FROM users WHERE username = ?
    boolean existsByUsername(String username);

    // SELECT COUNT(*) > 0 FROM users WHERE email = ?
    boolean existsByEmail(String email);
}