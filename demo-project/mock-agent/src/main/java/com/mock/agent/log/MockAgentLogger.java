package com.mock.agent.log;

import java.io.PrintStream;

public class MockAgentLogger {

    public enum Level {
        OFF(0), ERROR(1), WARN(2), INFO(3), DEBUG(4);

        final int value;

        Level(int value) {
            this.value = value;
        }
    }

    private static final String PREFIX = "[MockAgent] ";
    private static volatile Level currentLevel;

    static {
        initLevel();
    }

    private static void initLevel() {
        String prop = System.getProperty("mock.agent.log.level");
        if (prop == null) {
            boolean debug = "true".equals(System.getProperty("mock.agent.debug"));
            currentLevel = debug ? Level.DEBUG : Level.INFO;
        } else {
            currentLevel = Level.valueOf(prop.toUpperCase());
        }
    }

    public static void setLevel(Level level) {
        currentLevel = level;
    }

    public static Level getLevel() {
        return currentLevel;
    }

    public static boolean isDebugEnabled() {
        return currentLevel.value >= Level.DEBUG.value;
    }

    public static void error(String msg) {
        log(Level.ERROR, System.err, msg);
    }

    public static void error(String msg, Throwable t) {
        log(Level.ERROR, System.err, msg + ": " + t);
    }

    public static void warn(String msg) {
        log(Level.WARN, System.out, msg);
    }

    public static void info(String msg) {
        log(Level.INFO, System.out, msg);
    }

    public static void debug(String msg) {
        log(Level.DEBUG, System.out, msg);
    }

    private static void log(Level level, PrintStream out, String msg) {
        if (currentLevel.value >= level.value) {
            out.println(PREFIX + msg);
        }
    }
}
