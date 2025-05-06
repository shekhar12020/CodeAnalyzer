package com.codeanalyzer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CodeQualitySample {
    // Hardcoded credentials
    private static final String DB_PASSWORD = "mysecretpassword123";
    private static final String API_KEY = "sk_live_123456789";
    
    // Unsafe deserialization
    public void unsafeDeserialization(byte[] data) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            Object obj = ois.readObject(); // Unsafe deserialization
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // SQL Injection vulnerability
    public void sqlInjection(String userInput) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/db");
            Statement stmt = conn.createStatement();
            stmt.executeQuery("SELECT * FROM users WHERE id = '" + userInput + "'"); // SQL Injection
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Insecure HTTP connection
    public void insecureHttpConnection() {
        try {
            URL url = new URL("http://example.com/api"); // Insecure HTTP
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Weak password hashing
    public void weakPasswordHashing(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(password.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
    
    // Resource leak
    public void resourceLeak() {
        try {
            FileInputStream fis = new FileInputStream("file.txt");
            // Missing fis.close()
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Unsafe thread pool
    public void unsafeThreadPool() {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        // Missing executor.shutdown()
    }
    
    // Unbounded collection growth
    public void unboundedCollectionGrowth() {
        List<String> list = new ArrayList<>();
        while (true) {
            list.add("item"); // Potential memory leak
        }
    }
    
    // Unsafe reflection
    public void unsafeReflection(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Object obj = clazz.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Command injection
    public void commandInjection(String userInput) {
        try {
            Runtime.getRuntime().exec("ping " + userInput); // Command injection
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Unsafe file path
    public void unsafeFilePath(String fileName) {
        try {
            File file = new File("/tmp/" + fileName); // Path traversal
            FileInputStream fis = new FileInputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 