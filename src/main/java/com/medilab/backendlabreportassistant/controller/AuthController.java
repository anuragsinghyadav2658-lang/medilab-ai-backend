package com.medilab.backendlabreportassistant.controller;

import com.medilab.backendlabreportassistant.entity.User;
import com.medilab.backendlabreportassistant.repository.UserRepository;
import com.medilab.backendlabreportassistant.security.JwtUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import java.util.Collections;
import java.util.UUID;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
// @CrossOrigin(origins = "*") // Frontend ko connect karne ke liye
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // 1. Signup Endpoint
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: Email is already in use!");
        }

        // Create new user and hash password
        User newUser = new User();
        newUser.setFullName(request.getFullName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword())); // Password encrypt kar diya

        userRepository.save(newUser);

        // Generate JWT Token
        String token = jwtUtil.generateToken(newUser.getEmail());

        // Return token and user info
        return ResponseEntity.ok(new AuthResponse(token, newUser.getFullName(), newUser.getEmail()));
    }

    // 2. Login Endpoint
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest request) {
        // Step 1: Check if email exists in the database
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            // Email nahi mila toh 400 Bad Request return karo
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Account does not exist. Please create your account first."));
        }

        User user = userOptional.get();

        // Step 2: Check if the password matches
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // Password galat hone par error return karo
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid password! Please try again."));
        }

        // Step 3: Success! Generate JWT Token
        String token = jwtUtil.generateToken(user.getEmail());

        // Return token and user info normally
        return ResponseEntity.ok(new AuthResponse(token, user.getFullName(), user.getEmail()));
    }

    // 3. Google Social Login Endpoint (Access Token Verified)
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> payload) {
        String accessToken = payload.get("token");

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>("", headers);

            // Google se user ki detail fetch karna
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    entity,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> userInfo = response.getBody();
                String email = (String) userInfo.get("email");
                String name = (String) userInfo.get("name");

                Optional<User> userOptional = userRepository.findByEmail(email);
                User user;

                if (userOptional.isPresent()) {
                    user = userOptional.get();
                } else {
                    user = new User();
                    user.setFullName(name);
                    user.setEmail(email);
                    user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    user.setAuthProvider("google");
                    userRepository.save(user);
                }

                String jwtToken = jwtUtil.generateToken(user.getEmail());
                return ResponseEntity.ok(new AuthResponse(jwtToken, user.getFullName(), user.getEmail()));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid Google token."));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Google login failed on server."));
        }
    }

    // --- DTO Classes (Inner Classes taaki extra files na banani pade) ---

    @Data
    public static class SignupRequest {
        private String fullName;
        private String email;
        private String password;
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String fullName;
        private String email;

        public AuthResponse(String token, String fullName, String email) {
            this.token = token;
            this.fullName = fullName;
            this.email = email;
        }
    }
}
