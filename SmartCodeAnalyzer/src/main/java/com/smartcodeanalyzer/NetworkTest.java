package com.smartcodeanalyzer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Network connectivity test for Railway database
 */
public class NetworkTest {

    public static void main(String[] args) {
        String host = "metro.proxy.rlwy.net";
        int port = 28742;
        int timeoutMillis = 5000;  // 5 seconds timeout
        
        System.out.println("Testing network connectivity to Railway MySQL server");
        System.out.println("Host: " + host);
        System.out.println("Port: " + port);
        System.out.println("Timeout: " + timeoutMillis + "ms");
        
        // Test 1: Try to ping the host
        try {
            System.out.println("\nTest 1: Pinging host...");
            InetAddress address = InetAddress.getByName(host);
            boolean reachable = address.isReachable(timeoutMillis);
            
            if (reachable) {
                System.out.println("SUCCESS: Host is reachable via ICMP ping!");
            } else {
                System.out.println("FAILED: Host is not responding to ICMP ping.");
                System.out.println("Note: Some hosts block ICMP ping requests, so this test might not be reliable.");
            }
        } catch (IOException e) {
            System.out.println("FAILED: Error pinging host: " + e.getMessage());
        }
        
        // Test 2: Try to establish a TCP connection
        try {
            System.out.println("\nTest 2: Attempting TCP connection to port " + port + "...");
            Socket socket = new Socket();
            java.net.InetSocketAddress socketAddress = new java.net.InetSocketAddress(host, port);
            
            long startTime = System.currentTimeMillis();
            socket.connect(socketAddress, timeoutMillis);
            long endTime = System.currentTimeMillis();
            
            if (socket.isConnected()) {
                System.out.println("SUCCESS: TCP connection established successfully!");
                System.out.println("Connection time: " + (endTime - startTime) + "ms");
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("FAILED: Could not establish TCP connection: " + e.getMessage());
            System.out.println("\nPossible reasons for connection failure:");
            System.out.println("1. The Railway MySQL service might not be running");
            System.out.println("2. The host/port information might be incorrect");
            System.out.println("3. A firewall might be blocking the connection");
            System.out.println("4. Your network might not allow outbound connections to this port");
            System.out.println("5. The Railway MySQL instance might have expired or been deleted");
        }
        
        System.out.println("\nNetwork test completed.");
    }
}