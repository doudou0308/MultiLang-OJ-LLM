package io.multilang.oj.runner;

import java.nio.file.Path;
import java.util.List;

public enum Language {
    JAVA("Main.java"), C("solution.c"), CPP("solution.cpp");

    private final String fileName;

    Language(String fileName) {
        this.fileName = fileName;
    }

    String fileName() {
        return fileName;
    }

    List<String> compile(Path build) {
        Path source = build.resolve(fileName);
        return switch (this) {
            case JAVA -> List.of(tool("OJ_JAVAC", "javac"), "-encoding", "UTF-8", "-d",
                    build.toString(), source.toString());
            case C, CPP -> List.of(
                    this == C ? tool("OJ_CC", "gcc") : tool("OJ_CXX", "g++"),
                    this == C ? "-std=c17" : "-std=c++17", "-O2", source.toString(), "-o",
                    binary(build).toString());
        };
    }

    List<String> run(Path build) {
        return switch (this) {
            case JAVA -> List.of(tool("OJ_JAVA", "java"), "-Xmx128m", "-XX:ActiveProcessorCount=2",
                    "-Dfile.encoding=UTF-8", "-classpath", build.toString(), "Main");
            case C, CPP -> List.of(binary(build).toString());
        };
    }

    private static Path binary(Path build) {
        return build.resolve(System.getProperty("os.name").startsWith("Windows") ? "solution.exe" : "solution");
    }

    private static String tool(String variable, String fallback) {
        String value = System.getenv(variable);
        return value == null || value.isBlank() ? fallback : value;
    }
}
