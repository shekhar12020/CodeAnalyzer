package com.codeanalyzer;

import java.io.*;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CodeQualitySample2 {
    // Unsafe SSL/TLS configuration
    public void unsafeSSLConfig() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Unsafe socket connection
    public void unsafeSocketConnection() {
        try {
            Socket socket = new Socket("example.com", 80);
            // Missing socket.close()
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Unsafe key store loading
    public void unsafeKeyStoreLoading() {
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream("keystore.jks"), "password".toCharArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Race condition
    private AtomicInteger counter = new AtomicInteger(0);
    public void raceCondition() {
        int value = counter.get();
        // Simulating some processing
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        counter.set(value + 1); // Race condition
    }
    
    // Unsafe exception handling
    public void unsafeExceptionHandling() {
        try {
            // Some risky operation
            throw new RuntimeException("Test exception");
        } catch (Exception e) {
            e.printStackTrace(); // Unsafe exception handling
        }
    }
    
    // Unsafe file operations
    public void unsafeFileOperations(String fileName) {
        try {
            File file = new File(fileName);
            if (file.exists()) {
                file.delete(); // Unsafe file deletion
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Unsafe string comparison
    public boolean unsafeStringComparison(String input) {
        return input == "test"; // Unsafe string comparison
    }
    
    // Unsafe array access
    public void unsafeArrayAccess(int[] array, int index) {
        int value = array[index]; // Potential array index out of bounds
    }
    
    // Unsafe type casting
    public void unsafeTypeCasting(Object obj) {
        String str = (String) obj; // Unsafe type casting
    }
    
    // Unsafe thread operations
    public void unsafeThreadOperations() {
        Thread thread = new Thread(() -> {
            // Some operation
        });
        thread.start();
        // Missing thread.join() or proper thread management
    }
} 