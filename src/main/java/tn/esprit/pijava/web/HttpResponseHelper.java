package tn.esprit.pijava.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpResponseHelper {
    private static final ObjectMapper MAPPER = JsonSupport.MAPPER;

    private HttpResponseHelper() {
    }

    public static void sendJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] payload = MAPPER.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, payload.length);
        exchange.getResponseBody().write(payload);
    }

    public static void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(204, -1);
    }

    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        sendJson(exchange, statusCode, Map.of("error", message));
    }

    public static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }
}
