package com.security;

import java.rmi.*;

public interface PrintServer extends Remote {
    void print(String filename, String printer, String token) throws RemoteException;
    String queue(String printer, String token) throws RemoteException;
    void topQueue(String printer, int job, String token) throws RemoteException;
    void start(String token) throws RemoteException;
    void stop(String token) throws RemoteException;
    void restart(String token) throws RemoteException;
    String status(String printer, String token) throws RemoteException;
    String readConfig(String parameter, String token) throws RemoteException;
    void setConfig(String parameter, String value, String token) throws RemoteException;
    String login(String username, String password) throws RemoteException;
    void reloadConfigurations() throws RemoteException;
}
