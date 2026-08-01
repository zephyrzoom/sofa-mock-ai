package com.mock.agent.match;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.regex.Pattern;

public class ConditionEvaluator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static boolean evaluate(String condition, String requestBody, Map<String, String> requestHeaders) {
        if (condition == null || condition.isEmpty()) {
            return true;
        }

        try {
            return parseExpression(condition.trim(), requestBody, requestHeaders);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean parseExpression(String expr, String requestBody, Map<String, String> requestHeaders) {
        // Handle AND (&&)
        if (expr.contains("&&")) {
            String[] parts = expr.split("&&", 2);
            return parseExpression(parts[0].trim(), requestBody, requestHeaders)
                    && parseExpression(parts[1].trim(), requestBody, requestHeaders);
        }

        // Handle OR (||)
        if (expr.contains("||")) {
            String[] parts = expr.split("\\|\\|", 2);
            return parseExpression(parts[0].trim(), requestBody, requestHeaders)
                    || parseExpression(parts[1].trim(), requestBody, requestHeaders);
        }

        // Handle comparison operators
        if (expr.contains("==")) {
            String[] parts = expr.split("==", 2);
            Object left = resolveValue(parts[0].trim(), requestBody, requestHeaders);
            Object right = resolveValue(parts[1].trim(), requestBody, requestHeaders);
            return left != null && left.equals(right);
        }

        if (expr.contains("!=")) {
            String[] parts = expr.split("!=", 2);
            Object left = resolveValue(parts[0].trim(), requestBody, requestHeaders);
            Object right = resolveValue(parts[1].trim(), requestBody, requestHeaders);
            return left == null || !left.equals(right);
        }

        if (expr.contains("contains")) {
            String[] parts = expr.split("contains", 2);
            Object left = resolveValue(parts[0].trim(), requestBody, requestHeaders);
            Object right = resolveValue(parts[1].trim(), requestBody, requestHeaders);
            if (left instanceof String && right instanceof String) {
                return ((String) left).contains((String) right);
            }
            return false;
        }

        if (expr.contains("matches")) {
            String[] parts = expr.split("matches", 2);
            Object left = resolveValue(parts[0].trim(), requestBody, requestHeaders);
            Object right = resolveValue(parts[1].trim(), requestBody, requestHeaders);
            if (left instanceof String && right instanceof String) {
                return Pattern.matches((String) right, (String) left);
            }
            return false;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private static Object resolveValue(String token, String requestBody, Map<String, String> requestHeaders) {
        token = token.trim();

        // String literal
        if (token.startsWith("'") && token.endsWith("'")) {
            return token.substring(1, token.length() - 1);
        }
        if (token.startsWith("\"") && token.endsWith("\"")) {
            return token.substring(1, token.length() - 1);
        }

        // Header reference: headers['X-Env'] or headers["X-Env"]
        if (token.startsWith("headers[")) {
            String key = token.substring(8, token.length() - 1);
            key = key.replace("'", "").replace("\"", "");
            return requestHeaders.get(key);
        }

        // Body field reference: body.fieldName
        if (token.startsWith("body.")) {
            String field = token.substring(5);
            Map<String, Object> bodyMap = parseJson(requestBody);
            if (bodyMap != null) {
                Object val = bodyMap.get(field);
                return val != null ? val.toString() : null;
            }
            return null;
        }

        return token;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            Object obj = MAPPER.readValue(json, Object.class);
            if (obj instanceof Map) {
                return (Map<String, Object>) obj;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
