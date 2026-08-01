package com.mock.agent;

import com.mock.agent.log.MockAgentLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppNameDetector {

    public static String detect() {
        // 1. Explicit mock.agent.server.appName
        String name = System.getProperty("mock.agent.server.appName");
        if (name != null && !name.isEmpty()) {
            MockAgentLogger.info("[AppNameDetector] from mock.agent.server.appName: " + name);
            return name;
        }

        // 2. spring.application.name system property (Spring Boot may set this)
        name = System.getProperty("spring.application.name");
        if (name != null && !name.isEmpty()) {
            MockAgentLogger.info("[AppNameDetector] from spring.application.name: " + name);
            return name;
        }

        // 3. Environment variable
        name = System.getenv("SPRING_APPLICATION_NAME");
        if (name != null && !name.isEmpty()) {
            MockAgentLogger.info("[AppNameDetector] from env SPRING_APPLICATION_NAME: " + name);
            return name;
        }

        // 4. Parse application.yml from classpath
        name = parseYml("application.yml");
        if (name != null) {
            MockAgentLogger.info("[AppNameDetector] from application.yml: " + name);
            return name;
        }

        // 5. Parse application.yaml (alternative extension)
        name = parseYml("application.yaml");
        if (name != null) {
            MockAgentLogger.info("[AppNameDetector] from application.yaml: " + name);
            return name;
        }

        // 6. Parse application.properties
        name = parseProperties("application.properties");
        if (name != null) {
            MockAgentLogger.info("[AppNameDetector] from application.properties: " + name);
            return name;
        }

        // 7. Try bootstrap.yml
        name = parseYml("bootstrap.yml");
        if (name != null) {
            MockAgentLogger.info("[AppNameDetector] from bootstrap.yml: " + name);
            return name;
        }

        MockAgentLogger.info("[AppNameDetector] not detected, using default");
        return "default";
    }

    private static String parseYml(String filename) {
        InputStream is = openResource(filename);
        if (is == null) {
            MockAgentLogger.debug("[AppNameDetector] " + filename + " not found on classpath");
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            int springIndent = -1;
            int appIndent = -1;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

                String trimmed = line.trim();
                int indent = line.length() - line.replaceFirst("^\\s*", "").length();

                if ("spring:".equals(trimmed)) {
                    springIndent = indent;
                    appIndent = -1;
                    continue;
                }

                if (springIndent >= 0) {
                    if (indent <= springIndent && !"application:".equals(trimmed)) {
                        springIndent = -1;
                        appIndent = -1;
                        continue;
                    }
                    if ("application:".equals(trimmed)) {
                        appIndent = indent;
                        continue;
                    }
                    if (appIndent >= 0 && indent > appIndent && trimmed.startsWith("name:")) {
                        String val = trimmed.substring(5).trim().replaceAll("^\"|\"$", "");
                        if (!val.isEmpty()) return val;
                    }
                }
            }
        } catch (Exception e) {
            MockAgentLogger.warn("[AppNameDetector] failed to parse " + filename + ": " + e.getMessage());
        }
        return null;
    }

    private static String parseProperties(String filename) {
        InputStream is = openResource(filename);
        if (is == null) return null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#") || trimmed.isEmpty()) continue;
                if (trimmed.startsWith("spring.application.name=") || trimmed.startsWith("spring.application.name =")) {
                    return trimmed.substring(trimmed.indexOf('=') + 1).trim();
                }
            }
        } catch (Exception e) {
            MockAgentLogger.warn("[AppNameDetector] failed to parse " + filename + ": " + e.getMessage());
        }
        return null;
    }

    private static InputStream openResource(String name) {
        // Try all available classloaders
        for (ClassLoader cl : collectClassLoaders()) {
            if (cl == null) continue;
            try {
                InputStream is = cl.getResourceAsStream(name);
                if (is != null) {
                    MockAgentLogger.debug("[AppNameDetector] found " + name + " via " + cl.getClass().getName());
                    return is;
                }
            } catch (Exception ignore) {
            }
        }
        MockAgentLogger.debug("[AppNameDetector] " + name + " not found in any classloader");
        return null;
    }

    /**
     * Collect classloader candidates in priority order. The application's real classloader
     * (the context classloader of the "main" thread) is tried first: the agent premain runs
     * before the application boots, so threads spawned from premain inherit the system
     * classloader and cannot see resources inside a Spring Boot fat jar (BOOT-INF/classes).
     * By the time detection runs, JarLauncher has set the main thread's context classloader
     * to the URL classloader that reads those nested resources.
     */
    private static List<ClassLoader> collectClassLoaders() {
        List<ClassLoader> result = new ArrayList<>();

        ClassLoader mainClassLoader = null;
        try {
            for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                if ("main".equals(entry.getKey().getName())) {
                    mainClassLoader = entry.getKey().getContextClassLoader();
                    break;
                }
            }
        } catch (Exception ignore) {
            // best effort
        }
        addIfAbsent(result, mainClassLoader);
        addIfAbsent(result, Thread.currentThread().getContextClassLoader());
        addIfAbsent(result, AppNameDetector.class.getClassLoader());
        addIfAbsent(result, ClassLoader.getSystemClassLoader());
        return result;
    }

    private static void addIfAbsent(List<ClassLoader> list, ClassLoader cl) {
        if (cl != null && !list.contains(cl)) {
            list.add(cl);
        }
    }
}
