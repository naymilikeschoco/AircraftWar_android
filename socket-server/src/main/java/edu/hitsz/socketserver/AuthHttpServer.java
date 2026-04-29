package edu.hitsz.socketserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthHttpServer {

    private final UserStore userStore = new UserStore("users.db");
    private final Map<String, String> tokenToUser = new ConcurrentHashMap<>();

    public void start(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/register", new RegisterHandler());
            server.createContext("/login", new LoginHandler());
            server.createContext("/config", new ConfigHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("Auth HTTP server started on port: " + port);
        } catch (IOException e) {
            System.err.println("Auth HTTP server failed: " + e.getMessage());
        }
    }

    private class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"ok\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }
            Map<String, String> form = parseForm(exchange);
            String username = form.getOrDefault("username", "").trim();
            String password = form.getOrDefault("password", "").trim();
            if (username.isEmpty() || password.length() < 6) {
                sendJson(exchange, 400, "{\"ok\":false,\"message\":\"Invalid username or password\"}");
                return;
            }
            boolean created = userStore.register(username, password);
            if (!created) {
                sendJson(exchange, 409, "{\"ok\":false,\"message\":\"User already exists\"}");
                return;
            }
            sendJson(exchange, 200, "{\"ok\":true,\"message\":\"Register success\"}");
        }
    }

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJson(exchange, 405, "{\"ok\":false,\"message\":\"Method Not Allowed\"}");
                return;
            }
            Map<String, String> form = parseForm(exchange);
            String username = form.getOrDefault("username", "").trim();
            String password = form.getOrDefault("password", "").trim();
            if (!userStore.verify(username, password)) {
                sendJson(exchange, 401, "{\"ok\":false,\"message\":\"Invalid credentials\"}");
                return;
            }
            String token = UUID.randomUUID().toString();
            tokenToUser.put(token, username);
            sendJson(exchange, 200, "{\"ok\":true,\"token\":\"" + token + "\"}");
        }
    }

    private class ConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                handleGet(exchange);
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handlePost(exchange);
                return;
            }
            sendJson(exchange, 405, "{\"ok\":false,\"message\":\"Method Not Allowed\"}");
        }

        private void handleGet(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String token = query.getOrDefault("token", "");
            String username = tokenToUser.get(token);
            if (username == null) {
                sendJson(exchange, 401, "{\"ok\":false,\"message\":\"Invalid token\"}");
                return;
            }
            UserStore.UserConfig c = userStore.getConfig(username);
            String body = "{\"ok\":true,\"unlockedDifficulty\":" + c.unlockedDifficulty
                    + ",\"coins\":" + c.coins
                    + ",\"audioEnabled\":" + c.audioEnabled + "}";
            sendJson(exchange, 200, body);
        }

        private void handlePost(HttpExchange exchange) throws IOException {
            Map<String, String> form = parseForm(exchange);
            String token = form.getOrDefault("token", "");
            String username = tokenToUser.get(token);
            if (username == null) {
                sendJson(exchange, 401, "{\"ok\":false,\"message\":\"Invalid token\"}");
                return;
            }
            int unlocked = safeInt(form.get("unlockedDifficulty"), 1);
            int coins = safeInt(form.get("coins"), 0);
            boolean audioEnabled = Boolean.parseBoolean(form.getOrDefault("audioEnabled", "true"));
            userStore.updateConfig(username, unlocked, coins, audioEnabled);
            sendJson(exchange, 200, "{\"ok\":true,\"message\":\"Config saved\"}");
        }
    }

    private Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        byte[] bodyBytes = readAll(exchange.getRequestBody());
        String body = new String(bodyBytes, StandardCharsets.UTF_8);
        return parseQuery(body);
    }

    private Map<String, String> parseQuery(String query) throws IOException {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return map;
        }
        String[] parts = query.split("&");
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = pair.length > 1
                    ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
                    : "";
            map.put(key, value);
        }
        return map;
    }

    private byte[] readAll(InputStream inputStream) throws IOException {
        return inputStream.readAllBytes();
    }

    private int safeInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void sendJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
