package com.codeanalyzer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SampleCode {
    public static void main(String[] args) {
        // Weak password hashing
        String password = "password123";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(password.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // Potential null pointer
        String str = null;
        if (str.equals("test")) {
            System.out.println("This will cause NPE");
        }

        // Unused variable
        int unused = 42;

        // Hardcoded credentials
        String apiKey = "sk_test_123456789";
    }
} 