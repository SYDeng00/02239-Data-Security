// package com.security;

// import java.rmi.*;

// public class PrintClient {
//     public static void main(String[] args) {
//         try {
//             PrintServer server = (PrintServer) Naming.lookup("rmi://localhost:1099/PrintServer");
//             if (server.login("test", "test")) {
//                 String token = "authenticated"; 
//                 server.print("document.txt", "printer1", token);
//                 System.out.println(server.queue("printer1", token));
//             } else {
//                 System.out.println("Login failed");
//             }
//         } catch (Exception e) {
//             System.err.println("PrintClient exception: " + e.getMessage());
//             e.printStackTrace();
//         }
//     }
// }

package com.security;

import java.rmi.*;

public class PrintClient {
    public static void main(String[] args) {
        try {
            PrintServer server = (PrintServer) Naming.lookup("rmi://localhost:1099/PrintServer");
            String token = server.login("user1", "password1");
            if (token != null) {
                // Use the token returned from the server for subsequent operations
                server.print("document.txt", "printer1", token);
                System.out.println(server.queue("printer1", token));
            } else {
                System.out.println("Login failed");
            }
        } catch (Exception e) {
            System.err.println("PrintClient exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
