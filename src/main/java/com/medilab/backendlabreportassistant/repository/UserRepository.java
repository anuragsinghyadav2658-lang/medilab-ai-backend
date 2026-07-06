package com.medilab.backendlabreportassistant.repository;

import com.medilab.backendlabreportassistant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Login ke time email se user fetch karne ke liye custom method
    Optional<User> findByEmail(String email);
}
