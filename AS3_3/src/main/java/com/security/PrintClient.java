package com.security;

import java.rmi.Naming;

public class PrintClient {
    public static void main(String[] args) {
        try {
            PrintServer server = (PrintServer) Naming.lookup("rmi://localhost:1099/PrintServer");
            String token = server.login("george", "george");
            if (token != null) {
                // Use the token returned from the server for subsequent operations
                server.print("document.txt", "printer1", token);
                System.out.println(server.queue("printer1", token));
                

                //verify operations below

                //server.topQueue(token, 0, token);   
                // String status = server.status("printer1", token);
                // System.out.println("Printer status: " + status);

            } else {
                System.out.println("Login failed");
            }
        } catch (Exception e) {
            System.err.println("PrintClient exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
