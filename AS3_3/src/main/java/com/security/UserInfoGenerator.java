package com.security;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class UserInfoGenerator {

    private static final String CREDENTIALS_FILE = "userCredentials.properties";

    public static void main(String[] args) {
        Properties userCredentials = new Properties();
        
        addUser(userCredentials, "test", "test");
        addUser(userCredentials, "george", "george");
        addUser(userCredentials, "henry", "henry");
        addUser(userCredentials, "ida", "ida");


        try (FileOutputStream output = new FileOutputStream(CREDENTIALS_FILE)) {
            userCredentials.store(output, null);
            System.out.println("User credentials have been saved to " + CREDENTIALS_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addUser(Properties userCredentials, String username, String password) {
        String salt = PasswordUtil.generateSalt();
        String hashedPassword = PasswordUtil.hashPassword(password, salt);
        userCredentials.setProperty(username + ".salt", salt);
        userCredentials.setProperty(username + ".passwordHash", hashedPassword);
    }
}
