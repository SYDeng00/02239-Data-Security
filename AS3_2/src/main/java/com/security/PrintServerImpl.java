package com.security;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class PrintServerImpl extends UnicastRemoteObject implements PrintServer {

    // Simulating the data structure of a print queue
    private Map<String, Queue<String>> printQueues = new HashMap<>();
    private Map<String, String> userSalts = new HashMap<>();
    private Map<String, String> userHashedPasswords = new HashMap<>();
    private Map<String, String> authenticatedUsers = new HashMap<>();
    private static final String CREDENTIALS_FILE = "userCredentials.properties";
    private Map<String, String> tokenToUserRoleMap = new HashMap<>();
    private Map<String, Set<String>> roleHierarchy = new HashMap<>();
    // private Map<String, Set<String>> rolesAndPermissions = new HashMap<>();

    // Declare and initialize the userRoles map
    private Map<String, String> userRoles = new HashMap<>();

    private static final Logger LOGGER = Logger.getLogger(PrintServerImpl.class.getName());
    static {
        try {
            FileHandler fileHandler = new FileHandler("PrintServer.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Logger FileHandler not working.", e);
        }
    }

    protected PrintServerImpl() throws RemoteException {
        super();
        loadUserCredentials();
        loadAccessControlPolicy();  // Load the access control policy
        loadRoleHierarchy();
        initializeUserRoles();
    }
    

    private Map<String, Set<String>> accessControlPolicy = new HashMap<>();

    private void loadAccessControlPolicy() {
        Properties policyProps = new Properties();
        try (FileInputStream input = new FileInputStream("accessControlPolicy.properties")) {
            policyProps.load(input);
            for (String role : policyProps.stringPropertyNames()) {
                Set<String> allowedMethods = new HashSet<>(Arrays.asList(policyProps.getProperty(role).split(",")));
                accessControlPolicy.put(role, allowedMethods);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading roles and permissions.", e);
        }
        LOGGER.info("Access control policy loaded: " + accessControlPolicy);
    }
    
    private void addInheritedPermissions(String role, Set<String> effectivePermissions) {
        if (roleHierarchy.containsKey(role)) {
            for (String inheritedRole : roleHierarchy.get(role)) {
                effectivePermissions.addAll(accessControlPolicy.getOrDefault(inheritedRole, Collections.emptySet()));
                addInheritedPermissions(inheritedRole, effectivePermissions); // 递归地添加更深层次继承的权限
            }
        }
    }
    
    private void loadUserCredentials() {
            Properties userCredentials = new Properties();
            try (FileInputStream input = new FileInputStream(CREDENTIALS_FILE)) {
                userCredentials.load(input);
                for (String key : userCredentials.stringPropertyNames()) {
                    if (key.endsWith(".salt")) {
                        String username = key.substring(0, key.length() - ".salt".length());
                        String salt = userCredentials.getProperty(key);
                        String passwordHash = userCredentials.getProperty(username + ".passwordHash");
                        if (salt != null && passwordHash != null) {
                            userSalts.put(username, salt);
                            userHashedPasswords.put(username, passwordHash);
                        }
                    }
                }
                LOGGER.info("User credentials have been loaded from " + CREDENTIALS_FILE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    

        private void initializeUserRoles() {
            Properties roleProps = new Properties();
            try (FileInputStream input = new FileInputStream("userRoles.properties")) {
                roleProps.load(input);
                for (String username : roleProps.stringPropertyNames()) {
                    String role = roleProps.getProperty(username);
                    userRoles.put(username, role);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error loading user roles: " + e.getMessage(), e);
            }
        }
        
        private void loadRoleHierarchy() {
            Properties hierarchyProps = new Properties();
            try (FileInputStream input = new FileInputStream("roleHierarchy.properties")) {
                hierarchyProps.load(input);
                for (String role : hierarchyProps.stringPropertyNames()) {
                    Set<String> inheritedRoles = new HashSet<>(Arrays.asList(hierarchyProps.getProperty(role).split(",")));
                    roleHierarchy.put(role, inheritedRoles);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error loading role hierarchy: " + e.getMessage(), e);
            }
        }        


    @Override
    public String login(String username, String password) throws RemoteException {
        String storedSalt = userSalts.get(username);
        if (storedSalt != null) {
            String storedHash = userHashedPasswords.get(username);
            String computedHash = PasswordUtil.hashPassword(password, storedSalt);
            if (storedHash.equals(computedHash)) {
                String authToken = UUID.randomUUID().toString();
                authenticatedUsers.put(username, authToken);
    
                String userRole = userRoles.get(username); // Get user role
                tokenToUserRoleMap.put(authToken, userRole); // Save token and role mapping
    
                LOGGER.info("User " + username + " logged in. Role: " + userRole + ", Token: " + authToken);
                return authToken; // 返回生成的令牌
            }
        }
        LOGGER.warning("Failed login attempt for user " + username);
        return null; // 登录失败时返回 null
    }
    
    
   private boolean isUserAllowed(String methodName, String token) {
        String userRole = getUserRole(token);
        Set<String> effectivePermissions = new HashSet<>();
    
        // 添加直接权限
        effectivePermissions.addAll(accessControlPolicy.getOrDefault(userRole, Collections.emptySet()));
    
        // 添加继承的权限
        addInheritedPermissions(userRole, effectivePermissions);
    
        LOGGER.info("Checking access for method: " + methodName + ", Token: " + token + ", UserRole: " + userRole + ", Effective Permissions: " + effectivePermissions);
        return effectivePermissions.contains(methodName);
    }


    
     

    private String getUserRole(String token) {
        String role = tokenToUserRoleMap.get(token);
        if (role == null) {
            LOGGER.warning("Role not found for token: " + token);
        } else {
            LOGGER.info("Retrieved role for token " + token + ": " + role);
        }
        return role;
    }


    

    @Override
    public void print(String filename, String printer, String token) throws RemoteException {
        if (!isUserAllowed("print", token)) {
            LOGGER.severe("Unauthorized access attempt to print by token: " + token);
            throw new SecurityException("Unauthorized access");
        }   
        try {
            System.out.println("Print method called");
            System.out.println("Requested to print file: " + filename + " on printer: " + printer);
            printQueues.computeIfAbsent(printer, k -> new LinkedList<>()).offer(filename);
        } catch (Exception e) {
            System.err.println("Error in print method: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    @Override
    public String queue(String printer, String token) throws RemoteException {
        System.out.println("Requested to list the print queue for printer: " + printer);
        Queue<String> queue = printQueues.get(printer);
        if (queue == null || queue.isEmpty()) {
            return "The print queue is empty.";
        }
        StringBuilder sb = new StringBuilder();
        int jobNumber = 1;
        for (String job : queue) {
            sb.append(jobNumber++).append("   ").append(job).append("\n");
        }
        return sb.toString();
    }

    @Override
    public void topQueue(String printer, int job, String token) throws RemoteException {
        System.out.println("Requested to move job " + job + " to the top of the queue for printer: " + printer);
        Queue<String> queue = printQueues.get(printer);
        if (queue != null && job > 0 && job <= queue.size()) {
            List<String> jobs = new ArrayList<>(queue);
            String movedJob = jobs.remove(job - 1);
            queue.clear();
            queue.add(movedJob);
            queue.addAll(jobs);
        }
    }

    @Override
    public void start(String token) throws RemoteException {
        if (!isUserAllowed("start", token)) {
            LOGGER.severe("Unauthorized access attempt to start by token: " + token);
            throw new SecurityException("Unauthorized access");
        }
        System.out.println("Print server started.");
    }

    @Override
    public void stop(String token) throws RemoteException {
        if (!isUserAllowed("stop", token)) {
            LOGGER.severe("Unauthorized access attempt to start by token: " + token);
            throw new SecurityException("Unauthorized access");
        }
        System.out.println("Print server stopped.");
    }

    @Override
    public void restart(String token) throws RemoteException {
        if (!isUserAllowed("restart", token)) {
            LOGGER.severe("Unauthorized access attempt to start by token: " + token);
            throw new SecurityException("Unauthorized access");
        }
        System.out.println("Print server restarting...");
        stop(token);
        start(token);
    }

    @Override
    public String status(String printer, String token) throws RemoteException {
        if (!isUserAllowed("status", token)) {
            LOGGER.severe("Unauthorized access attempt to start by token: " + token);
            throw new SecurityException("Unauthorized access");
        }
        System.out.println("Requested to check the status of printer: " + printer);
        return token;
    }

    @Override
    public String readConfig(String parameter, String token) throws RemoteException {
        if (!isUserAllowed("readConfig", token)) {
            LOGGER.severe("Unauthorized access attempt to start by token: " + token);
            throw new SecurityException("Unauthorized access");
        }
        System.out.println("Requested to read configuration parameter: " + parameter);
        return "Value";
    }

    @Override
    public void setConfig(String parameter, String value, String token) throws RemoteException {
        if (!isUserAllowed("start", token)) {
            LOGGER.severe("Unauthorized access attempt to start by token: " + token);
            throw new SecurityException("Unauthorized access");
        }
        System.out.println("Setting configuration parameter: " + parameter + " to value: " + value);
    }
}

