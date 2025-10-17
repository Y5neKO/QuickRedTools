package com.y5neko.qrts.model;

public class ToolItem {
    private String id;
    private String name;
    private String description;
    private String categoryId; // 关联的分类ID
    private String environmentId; // 关联的环境ID
    private String command; // 执行命令或脚本路径
    private String arguments; // 额外参数
    private String workingDirectory; // 工作目录
    private String iconPath; // 图标路径
    private boolean hasGUI; // 是否为GUI工具，默认为false（命令行工具）

    public ToolItem() {}

    public ToolItem(String id, String name, String description, String categoryId, String environmentId, String command, String arguments, String workingDirectory, String iconPath, boolean hasGUI) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.environmentId = environmentId;
        this.command = command;
        this.arguments = arguments;
        this.workingDirectory = workingDirectory;
        this.iconPath = iconPath;
        this.hasGUI = hasGUI;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getEnvironmentId() { return environmentId; }
    public void setEnvironmentId(String environmentId) { this.environmentId = environmentId; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public String getArguments() { return arguments; }
    public void setArguments(String arguments) { this.arguments = arguments; }

    public String getWorkingDirectory() { return workingDirectory; }
    public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory; }

    public String getIconPath() { return iconPath; }
    public void setIconPath(String iconPath) { this.iconPath = iconPath; }

    public boolean isHasGUI() { return hasGUI; }
    public void setHasGUI(boolean hasGUI) { this.hasGUI = hasGUI; }

    @Override
    public String toString() {
        return name;
    }
}