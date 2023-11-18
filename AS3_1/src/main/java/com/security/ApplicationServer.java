package com.security;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class ApplicationServer {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);
            PrintServer server = new PrintServerImpl();
            Naming.rebind("rmi://localhost/PrintServer", server);
            System.out.println("Print Server is ready.");
        } catch (RemoteException e) {
            System.err.println("Remote Exception: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Print Server failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
