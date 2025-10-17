package com.y5neko.qrts.service;

import com.y5neko.qrts.model.Environment;
import com.y5neko.qrts.model.ToolItem;
import com.y5neko.qrts.ui.terminal.TerminalDialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ToolLauncher {
    private static ToolLauncher instance;
    private DataManager dataManager;

    private ToolLauncher() {
        dataManager = DataManager.getInstance();
    }

    public static synchronized ToolLauncher getInstance() {
        if (instance == null) {
            instance = new ToolLauncher();
        }
        return instance;
    }

    public Process launchTool(ToolItem tool) throws IOException, IllegalArgumentException {
        return launchTool(tool, null);
    }

    public Process launchTool(ToolItem tool, Runnable onCloseCallback) throws IOException, IllegalArgumentException {
        Environment environment = findEnvironmentById(tool.getEnvironmentId());
        if (environment == null) {
            throw new IllegalArgumentException("未找到ID为 " + tool.getEnvironmentId() + " 的环境");
        }

        List<String> command = new ArrayList<>();

        // 根据工具类型选择启动方式
        if (tool.isHasGUI()) {
            // GUI工具：直接运行
            return launchGUITool(tool, environment, command);
        } else {
            // CLI工具：使用虚拟终端
            launchCLIToolWithVirtualTerminal(tool, environment, onCloseCallback);
            return null; // 虚拟终端模式不返回Process对象
        }
    }

    private Process launchGUITool(ToolItem tool, Environment environment, List<String> command) throws IOException {
        // 添加环境可执行文件路径
        command.add(environment.getExecutablePath());

        // 添加环境参数
        if (environment.getParameters() != null && !environment.getParameters().trim().isEmpty()) {
            command.addAll(parseParameters(environment.getParameters()));
        }

        // 添加工具命令
        if (tool.getCommand() != null && !tool.getCommand().trim().isEmpty()) {
            command.add(tool.getCommand());
        }

        // 添加工具参数
        if (tool.getArguments() != null && !tool.getArguments().trim().isEmpty()) {
            command.addAll(parseParameters(tool.getArguments()));
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        // 设置工作目录
        if (tool.getWorkingDirectory() != null && !tool.getWorkingDirectory().trim().isEmpty()) {
            File workDir = new File(tool.getWorkingDirectory());
            if (workDir.exists() && workDir.isDirectory()) {
                processBuilder.directory(workDir);
            }
        }

        return processBuilder.start();
    }

    private void launchCLIToolWithVirtualTerminal(ToolItem tool, Environment environment, Runnable onCloseCallback) {
        // 在JavaFX应用线程中打开终端对话框
        javafx.application.Platform.runLater(() -> {
            TerminalDialog terminalDialog = new TerminalDialog(onCloseCallback);
            terminalDialog.show(tool);
        });
    }

    private Environment findEnvironmentById(String environmentId) {
        return dataManager.loadEnvironments().stream()
                .filter(env -> env.getId().equals(environmentId))
                .findFirst()
                .orElse(null);
    }

    private List<String> parseParameters(String parameters) {
        List<String> result = new ArrayList<>();
        if (parameters == null || parameters.trim().isEmpty()) {
            return result;
        }

        String[] parts = parameters.split("\\s+");
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                result.add(part.trim());
            }
        }
        return result;
    }

    public boolean validateEnvironment(Environment environment) {
        if (environment == null || environment.getExecutablePath() == null) {
            return false;
        }

        File executable = new File(environment.getExecutablePath());
        return executable.exists() && executable.isFile() && executable.canExecute();
    }

    public boolean validateTool(ToolItem tool) {
        if (tool == null) {
            return false;
        }

        // 检查必填字段
        if (tool.getCommand() == null || tool.getCommand().trim().isEmpty()) {
            return false;
        }

        if (tool.getEnvironmentId() == null || tool.getEnvironmentId().trim().isEmpty()) {
            return false;
        }

        Environment environment = findEnvironmentById(tool.getEnvironmentId());
        if (environment == null) {
            return false;
        }

        // 检查可执行文件是否存在
        if (!validateEnvironment(environment)) {
            return false;
        }

        // 检查工具命令是否存在
        if (tool.getCommand() != null && !tool.getCommand().trim().isEmpty()) {
            File toolCommand = new File(tool.getCommand());
            // 如果是绝对路径，检查文件是否存在
            if (toolCommand.isAbsolute() && !toolCommand.exists()) {
                return false;
            }
        }

        return true;
    }
}