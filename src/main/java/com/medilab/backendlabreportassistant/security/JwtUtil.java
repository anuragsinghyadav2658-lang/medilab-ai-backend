package com.medilab.backendlabreportassistant.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

    // Ab ye direct application.properties se value uthayega
    @Value("${jwt.secret}")
    private String secret;

    // 1. JWT Token Generate karne ka method (24 hours validity)
    public String generateToken(String email) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        
        long expiryTimeInMilliseconds = 1000L * 60 * 60 * 24; // 24 Ghante
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiryTimeInMilliseconds);

        return JWT.create()
                .withSubject(email)
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(algorithm);
    }

    // 2. Token ko verify karke usme se User ka Email nikalne ka method
    public String verifyTokenAndGetEmail(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            
            DecodedJWT decodedJWT = verifier.verify(token);
            return decodedJWT.getSubject(); // Email as subject save kiya tha
        } catch (Exception e) {
            // Token invalid ya expire hone par exception aayegi
            throw new RuntimeException("Invalid or Expired JWT Token");
        }
    }
}
