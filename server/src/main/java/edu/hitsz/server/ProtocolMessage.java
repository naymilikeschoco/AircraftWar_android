package edu.hitsz.server;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProtocolMessage {

    private final String type;
    private final Map<String, String> data = new LinkedHashMap<>();

    private ProtocolMessage(String type) {
        this.type = type;
    }

    public static ProtocolMessage of(String type) {
        return new ProtocolMessage(type);
    }

    public static ProtocolMessage parse(String raw) {
        ProtocolMessage message = new ProtocolMessage("");
        if (raw == null || raw.trim().isEmpty()) {
            return message;
        }

        String parsedType = "";
        String[] parts = raw.split(";");
        for (String part : parts) {
            int separatorIndex = part.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String key = part.substring(0, separatorIndex);
            String value = URLDecoder.decode(part.substring(separatorIndex + 1), StandardCharsets.UTF_8);
            if ("type".equals(key)) {
                parsedType = value;
            } else {
                message.data.put(key, value);
            }
        }
        return new ProtocolMessage(parsedType).putAll(message.data);
    }

    public ProtocolMessage put(String key, String value) {
        data.put(key, value == null ? "" : value);
        return this;
    }

    public ProtocolMessage put(String key, int value) {
        return put(key, String.valueOf(value));
    }

    public ProtocolMessage put(String key, boolean value) {
        return put(key, String.valueOf(value));
    }

    private ProtocolMessage putAll(Map<String, String> source) {
        data.putAll(source);
        return this;
    }

    public String getType() {
        return type;
    }

    public String getString(String key) {
        return data.getOrDefault(key, "");
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(getString(key));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key);
        if (value.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    public String serialize() {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, "type", type);
        for (Map.Entry<String, String> entry : data.entrySet()) {
            builder.append(';');
            appendPart(builder, entry.getKey(), entry.getValue());
        }
        return builder.toString();
    }

    private void appendPart(StringBuilder builder, String key, String value) {
        builder.append(key)
                .append('=')
                .append(URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8));
    }
}
