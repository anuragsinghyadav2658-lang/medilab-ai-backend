package com.medilab.backendlabreportassistant.controller;

import com.medilab.backendlabreportassistant.entity.User;
import com.medilab.backendlabreportassistant.repository.UserRepository;
import com.medilab.backendlabreportassistant.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
// @CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PutMapping("/preferences/email-alerts")
    public ResponseEntity<?> updateEmailAlertPreference(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Boolean> payload) {

        try {
            // 1. Authorization header check aur token extract karna
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Error: Missing or invalid Authorization header.");
            }

            String token = authHeader.substring(7);

            // 2. Token verify karke email nikalna
            String email = jwtUtil.verifyTokenAndGetEmail(token);

            // 3. Database me user dhoondhna
            Optional<User> userOptional = userRepository.findByEmail(email);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // 4. Preference update karke save karna
                Boolean emailAlertsEnabled = payload.get("emailAlertsEnabled");
                if (emailAlertsEnabled != null) {
                    user.setEmailAlertsEnabled(emailAlertsEnabled);
                    userRepository.save(user);
                    return ResponseEntity.ok(Map.of("message", "Email alert preferences updated successfully."));
                } else {
                    return ResponseEntity.badRequest()
                            .body("Error: 'emailAlertsEnabled' value is missing in the request body.");
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: User not found in database.");
            }
        } catch (Exception e) {
            // Token expire hone ya invalid hone par
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid or Expired JWT Token.");
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            // 1. Authorization header check karna
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Error: Missing or invalid Authorization header.");
            }

            String token = authHeader.substring(7);

            // 2. Token se email nikalna
            String email = jwtUtil.verifyTokenAndGetEmail(token);

            // 3. Database se user lana
            Optional<User> userOptional = userRepository.findByEmail(email);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // 4. React frontend ko JSON format me data bhejna
                return ResponseEntity.ok(Map.of(
                        "name", user.getFullName() != null ? user.getFullName() : "Not Provided",
                        "email", user.getEmail() != null ? user.getEmail() : "Not Provided",
                        "phone", user.getPhone() != null ? user.getPhone() : "Not Provided"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: User not found.");
            }
        } catch (Exception e) {
            e.printStackTrace(); // <--- Sirf ye nayi line add karni hai
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid or Expired JWT Token.");
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> payload) {

        try {
            // 1. Token check karna
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Missing or invalid token.");
            }

            String token = authHeader.substring(7);
            String currentEmail = jwtUtil.verifyTokenAndGetEmail(token);

            // 2. Database se user dhoondhna
            Optional<User> userOptional = userRepository.findByEmail(currentEmail);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // 3. Frontend se aaya hua naya data set karna
                if (payload.get("name") != null) {
                    user.setFullName(payload.get("name"));
                }
                if (payload.get("email") != null) {
                    user.setEmail(payload.get("email"));
                }
                if (payload.get("phone") != null) {
                    user.setPhone(payload.get("phone"));
                }

                // 4. Database me save karna
                userRepository.save(user);
                return ResponseEntity.ok(Map.of("message", "Profile updated successfully!"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: User not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid Token or Update failed.");
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> payload) {

        try {
            // 1. Token check karna
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Missing or invalid token.");
            }

            String token = authHeader.substring(7);
            String currentEmail = jwtUtil.verifyTokenAndGetEmail(token);

            // 2. Database se user lana
            Optional<User> userOptional = userRepository.findByEmail(currentEmail);

            if (userOptional.isPresent()) {
                User user = userOptional.get();

                String currentPassword = payload.get("currentPassword");
                String newPassword = payload.get("newPassword");

                                // 3. Current password match karna (Plain text aur Encrypted dono handle karega)
                boolean isMatch = passwordEncoder.matches(currentPassword, user.getPassword()) 
                                  || currentPassword.equals(user.getPassword());
                                  
                if (!isMatch) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Current password incorrect.");
                }


                // 4. Naya password encrypt karke save karna
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);

                return ResponseEntity.ok(Map.of("message", "Password changed successfully!"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: User not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid Token.");
        }
    }
}
