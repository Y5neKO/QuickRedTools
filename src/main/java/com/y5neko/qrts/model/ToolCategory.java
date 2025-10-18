package com.y5neko.qrts.model;

import java.util.ArrayList;
import java.util.List;

public class ToolCategory {
    private String id;
    private String name;
    private String description;
    private String iconPath;
    private int sortOrder;
    private List<ToolItem> tools;

    public ToolCategory() {
        this.sortOrder = 0;
        this.tools = new ArrayList<>();
    }

    public ToolCategory(String id, String name, String description, String iconPath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.iconPath = iconPath;
        this.sortOrder = 0;
        this.tools = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconPath() { return iconPath; }
    public void setIconPath(String iconPath) { this.iconPath = iconPath; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public List<ToolItem> getTools() { return tools; }
    public void setTools(List<ToolItem> tools) { this.tools = tools; }

    public void addTool(ToolItem tool) {
        this.tools.add(tool);
    }

    public void removeTool(ToolItem tool) {
        this.tools.remove(tool);
    }

    @Override
    public String toString() {
        return name;
    }
}