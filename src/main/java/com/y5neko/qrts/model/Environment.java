package com.y5neko.qrts.model;

import java.util.List;

public class Environment {
    private String id;
    private String name;
    private String type; // java, python, go, gem等
    private String executablePath; // 可执行文件路径
    private String parameters; // 启动参数
    private String description;

    public Environment() {}

    public Environment(String id, String name, String type, String executablePath, String parameters, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.executablePath = executablePath;
        this.parameters = parameters;
        this.description = description;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getExecutablePath() { return executablePath; }
    public void setExecutablePath(String executablePath) { this.executablePath = executablePath; }

    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}