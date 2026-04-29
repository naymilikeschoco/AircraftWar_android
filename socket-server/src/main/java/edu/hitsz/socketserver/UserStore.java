package edu.hitsz.socketserver;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public class UserStore {

    public static class UserConfig {
        public int unlockedDifficulty = 1;
        public int coins = 0;
        public boolean audioEnabled = true;
    }

    private final String filePath;
    private final Properties properties = new Properties();

    public UserStore(String filePath) {
        this.filePath = filePath;
        load();
    }

    public synchronized boolean register(String username, String password) {
        if (exists(username)) {
            return false;
        }
        properties.setProperty(key(username, "password"), sha256(password));
        properties.setProperty(key(username, "unlockedDifficulty"), "1");
        properties.setProperty(key(username, "coins"), "0");
        properties.setProperty(key(username, "audioEnabled"), "true");
        save();
        return true;
    }

    public synchronized boolean verify(String username, String password) {
        String stored = properties.getProperty(key(username, "password"));
        return stored != null && stored.equals(sha256(password));
    }

    public synchronized UserConfig getConfig(String username) {
        UserConfig config = new UserConfig();
        config.unlockedDifficulty = parseInt(properties.getProperty(key(username, "unlockedDifficulty")), 1);
        config.coins = parseInt(properties.getProperty(key(username, "coins")), 0);
        config.audioEnabled = Boolean.parseBoolean(properties.getProperty(key(username, "audioEnabled"), "true"));
        return config;
    }

    public synchronized void updateConfig(String username, int unlockedDifficulty, int coins, boolean audioEnabled) {
        properties.setProperty(key(username, "unlockedDifficulty"), String.valueOf(Math.max(1, unlockedDifficulty)));
        properties.setProperty(key(username, "coins"), String.valueOf(Math.max(0, coins)));
        properties.setProperty(key(username, "audioEnabled"), String.valueOf(audioEnabled));
        save();
    }

    private boolean exists(String username) {
        return properties.containsKey(key(username, "password"));
    }

    private String key(String username, String field) {
        return "user." + username + "." + field;
    }

    private int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void load() {
        try (FileInputStream in = new FileInputStream(filePath)) {
            properties.load(in);
        } catch (IOException ignored) {
        }
    }

    private void save() {
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            properties.store(out, "AircraftWar user data");
        } catch (IOException e) {
            System.err.println("Failed to save user store: " + e.getMessage());
        }
    }
}
