package com.y5neko.qrts.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.y5neko.qrts.model.Environment;
import com.y5neko.qrts.model.ToolCategory;
import com.y5neko.qrts.model.ToolItem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static final String DATA_DIR = "data";
    private static final String ENVIRONMENTS_FILE = "environments.json";
    private static final String CATEGORIES_FILE = "categories.json";
    private static final String TOOLS_FILE = "tools.json";

    private static DataManager instance;
    private ObjectMapper objectMapper;

    // 缓存数据以提高性能
    private List<Environment> cachedEnvironments;
    private List<ToolCategory> cachedCategories;
    private List<ToolItem> cachedTools;
    private boolean environmentsLoaded = false;
    private boolean categoriesLoaded = false;
    private boolean toolsLoaded = false;

    private DataManager() {
        objectMapper = new ObjectMapper();
        ensureDataDirectoryExists();
    }

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    private void ensureDataDirectoryExists() {
        try {
            Path dataDir = Paths.get(DATA_DIR);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
        } catch (IOException e) {
            System.err.println("Failed to create data directory: " + e.getMessage());
        }
    }

    // 清除缓存，强制重新加载
    public void clearCache() {
        environmentsLoaded = false;
        categoriesLoaded = false;
        toolsLoaded = false;
        cachedEnvironments = null;
        cachedCategories = null;
        cachedTools = null;
    }

    // 重新初始化所有配置文件
    public void reinitialize() {
        try {
            // 删除现有配置文件
            Files.deleteIfExists(Paths.get(DATA_DIR, ENVIRONMENTS_FILE));
            Files.deleteIfExists(Paths.get(DATA_DIR, CATEGORIES_FILE));
            Files.deleteIfExists(Paths.get(DATA_DIR, TOOLS_FILE));

            // 清除缓存
            clearCache();

            System.out.println("所有配置文件已重置为默认值");
        } catch (IOException e) {
            System.err.println("Failed to reinitialize config files: " + e.getMessage());
        }
    }

    // 环境管理
    public List<Environment> loadEnvironments() {
        File file = new File(DATA_DIR, ENVIRONMENTS_FILE);
        if (!file.exists()) {
            return createAndSaveDefaultEnvironments();
        }

        try {
            return objectMapper.readValue(file, new TypeReference<List<Environment>>() {});
        } catch (IOException e) {
            System.err.println("Failed to load environments: " + e.getMessage());
            return createAndSaveDefaultEnvironments();
        }
    }

    public void saveEnvironments(List<Environment> environments) {
        try {
            objectMapper.writeValue(new File(DATA_DIR, ENVIRONMENTS_FILE), environments);
        } catch (IOException e) {
            System.err.println("Failed to save environments: " + e.getMessage());
        }
    }

    private List<Environment> createAndSaveDefaultEnvironments() {
        List<Environment> environments = new ArrayList<>();

        // 默认Java环境
        environments.add(new Environment(
            "env-" + System.currentTimeMillis() + "-1",
            "Java 8 Default",
            "java",
            System.getProperty("java.home") + "/bin/java",
            "-Xms512m -Xmx1024m",
            "系统默认Java环境，用于运行Java应用程序"
        ));

        // 默认Python环境
        environments.add(new Environment(
            "env-" + System.currentTimeMillis() + "-2",
            "Python 3 Default",
            "python",
            "python3",
            "",
            "系统默认Python环境，用于运行Python脚本"
        ));

        // Node.js环境
        environments.add(new Environment(
            "env-" + System.currentTimeMillis() + "-3",
            "Node.js Default",
            "node",
            "node",
            "",
            "Node.js运行环境，用于运行JavaScript代码"
        ));

        saveEnvironments(environments);
        return environments;
    }

    // 工具分类管理
    public List<ToolCategory> loadCategories() {
        File file = new File(DATA_DIR, CATEGORIES_FILE);
        if (!file.exists()) {
            return createAndSaveDefaultCategories();
        }

        try {
            return objectMapper.readValue(file, new TypeReference<List<ToolCategory>>() {});
        } catch (IOException e) {
            System.err.println("Failed to load categories: " + e.getMessage());
            return createAndSaveDefaultCategories();
        }
    }

    public void saveCategories(List<ToolCategory> categories) {
        try {
            objectMapper.writeValue(new File(DATA_DIR, CATEGORIES_FILE), categories);
        } catch (IOException e) {
            System.err.println("Failed to save categories: " + e.getMessage());
        }
    }

    private List<ToolCategory> createAndSaveDefaultCategories() {
        List<ToolCategory> categories = new ArrayList<>();

        categories.add(new ToolCategory(
            "cat-" + System.currentTimeMillis() + "-1",
            "开发工具",
            "Java、Python、Node.js等开发相关工具",
            null
        ));

        categories.add(new ToolCategory(
            "cat-" + System.currentTimeMillis() + "-2",
            "系统工具",
            "系统管理和维护工具",
            null
        ));

        categories.add(new ToolCategory(
            "cat-" + System.currentTimeMillis() + "-3",
            "网络工具",
            "网络调试和测试工具",
            null
        ));

        categories.add(new ToolCategory(
            "cat-" + System.currentTimeMillis() + "-4",
            "数据库工具",
            "数据库管理和操作工具",
            null
        ));

        saveCategories(categories);
        return categories;
    }

    // 工具管理
    public List<ToolItem> loadTools() {
        File file = new File(DATA_DIR, TOOLS_FILE);
        if (!file.exists()) {
            return createAndSaveDefaultTools();
        }

        try {
            return objectMapper.readValue(file, new TypeReference<List<ToolItem>>() {});
        } catch (IOException e) {
            System.err.println("Failed to load tools: " + e.getMessage());
            return createAndSaveDefaultTools();
        }
    }

    public void saveTools(List<ToolItem> tools) {
        try {
            objectMapper.writeValue(new File(DATA_DIR, TOOLS_FILE), tools);
        } catch (IOException e) {
            System.err.println("Failed to save tools: " + e.getMessage());
        }
    }

    private List<ToolItem> createAndSaveDefaultTools() {
        List<ToolItem> tools = new ArrayList<>();
        List<ToolCategory> categories = loadCategories();
        List<Environment> environments = loadEnvironments();

        // 示例Java工具 - CLI工具
        ToolItem mavenTool = new ToolItem();
        mavenTool.setId("tool-" + System.currentTimeMillis() + "-1");
        mavenTool.setName("Maven构建");
        mavenTool.setDescription("Apache Maven项目构建工具");
        mavenTool.setCategoryId(categories.get(0).getId()); // 开发工具分类
        if (environments.size() > 0) {
            mavenTool.setEnvironmentId(environments.get(0).getId()); // Java环境
        }
        mavenTool.setCommand("mvn");
        mavenTool.setArguments("clean install");
        mavenTool.setWorkingDirectory("");
        mavenTool.setHasGUI(false); // 命令行工具
        tools.add(mavenTool);

        // 示例Python工具 - CLI工具
        ToolItem pipTool = new ToolItem();
        pipTool.setId("tool-" + System.currentTimeMillis() + "-2");
        pipTool.setName("Pip包管理");
        pipTool.setDescription("Python包管理工具");
        pipTool.setCategoryId(categories.get(0).getId()); // 开发工具分类
        if (environments.size() > 1) {
            pipTool.setEnvironmentId(environments.get(1).getId()); // Python环境
        }
        pipTool.setCommand("pip3");
        pipTool.setArguments("list");
        pipTool.setWorkingDirectory("");
        pipTool.setHasGUI(false); // 命令行工具
        tools.add(pipTool);

        // 示例系统工具 - CLI工具
        ToolItem topTool = new ToolItem();
        topTool.setId("tool-" + System.currentTimeMillis() + "-3");
        topTool.setName("进程监控");
        topTool.setDescription("系统进程监控工具");
        topTool.setCategoryId(categories.get(1).getId()); // 系统工具分类
        topTool.setCommand("top");
        topTool.setArguments("");
        topTool.setWorkingDirectory("");
        topTool.setHasGUI(false); // 命令行工具
        tools.add(topTool);

        // 示例网络工具 - CLI工具
        ToolItem pingTool = new ToolItem();
        pingTool.setId("tool-" + System.currentTimeMillis() + "-4");
        pingTool.setName("网络连通性测试");
        pingTool.setDescription("测试网络连接状态");
        pingTool.setCategoryId(categories.get(2).getId()); // 网络工具分类
        pingTool.setCommand("ping");
        pingTool.setArguments("www.baidu.com");
        pingTool.setWorkingDirectory("");
        pingTool.setHasGUI(false); // 命令行工具
        tools.add(pingTool);

        // 示例数据库工具 - CLI工具
        ToolItem mysqlTool = new ToolItem();
        mysqlTool.setId("tool-" + System.currentTimeMillis() + "-5");
        mysqlTool.setName("MySQL客户端");
        mysqlTool.setDescription("MySQL数据库客户端工具");
        mysqlTool.setCategoryId(categories.get(3).getId()); // 数据库工具分类
        mysqlTool.setCommand("mysql");
        mysqlTool.setArguments("-u root -p");
        mysqlTool.setWorkingDirectory("");
        mysqlTool.setHasGUI(false); // 命令行工具
        tools.add(mysqlTool);

        // 示例GUI工具 - GUI工具
        ToolItem notepadTool = new ToolItem();
        notepadTool.setId("tool-" + System.currentTimeMillis() + "-6");
        notepadTool.setName("记事本");
        notepadTool.setDescription("系统记事本工具");
        notepadTool.setCategoryId(categories.get(1).getId()); // 系统工具分类
        notepadTool.setCommand("notepad");
        notepadTool.setArguments("");
        notepadTool.setWorkingDirectory("");
        notepadTool.setHasGUI(true); // GUI工具
        tools.add(notepadTool);

        saveTools(tools);
        return tools;
    }
}