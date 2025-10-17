package com.y5neko.qrts.ui.terminal;

import com.y5neko.qrts.model.ToolItem;
import com.y5neko.qrts.model.Environment;
import com.y5neko.qrts.service.DataManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class TerminalDialog {
    private Stage stage;
    private VirtualTerminal terminal;
    private DataManager dataManager;
    private Runnable externalOnCloseCallback;

    public TerminalDialog() {
        dataManager = DataManager.getInstance();
    }

    public TerminalDialog(Runnable onCloseCallback) {
        this();
        this.externalOnCloseCallback = onCloseCallback;
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("虚拟终端");
        stage.setWidth(800);
        stage.setHeight(600);
        stage.setMinWidth(600);
        stage.setMinHeight(400);

        // 创建终端，传入关闭回调
        terminal = new VirtualTerminal(() -> {
            // 当用户输入exit命令时关闭窗口
            javafx.application.Platform.runLater(() -> {
                terminal.closeTerminal();
                stage.close();
                // 调用外部回调（如果存在）
                if (externalOnCloseCallback != null) {
                    externalOnCloseCallback.run();
                }
            });
        });

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.getChildren().add(terminal);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        // 设置窗口关闭事件
        stage.setOnCloseRequest(e -> {
            terminal.closeTerminal();
            stage.close();
            // 调用外部回调（如果存在）
            if (externalOnCloseCallback != null) {
                externalOnCloseCallback.run();
            }
        });

        stage.show();
    }

    public void show(ToolItem tool) {
        stage = new Stage();
        stage.setTitle("虚拟终端 - " + tool.getName());
        stage.setWidth(800);
        stage.setHeight(600);
        stage.setMinWidth(600);
        stage.setMinHeight(400);

        // 创建终端，传入关闭回调
        terminal = new VirtualTerminal(() -> {
            // 当用户输入exit命令时关闭窗口
            javafx.application.Platform.runLater(() -> {
                terminal.closeTerminal();
                stage.close();
                // 调用外部回调（如果存在）
                if (externalOnCloseCallback != null) {
                    externalOnCloseCallback.run();
                }
            });
        });

        // 设置工作目录
        if (tool.getWorkingDirectory() != null && !tool.getWorkingDirectory().trim().isEmpty()) {
            terminal.setWorkingDirectory(tool.getWorkingDirectory());
        }

        // 添加工具信息显示
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // 工具信息区域
        TitledPane toolInfoPane = new TitledPane();
        toolInfoPane.setText("工具信息");
        toolInfoPane.setExpanded(false);
        toolInfoPane.setCollapsible(true);

        VBox infoBox = new VBox(5);
        infoBox.setPadding(new Insets(10));

        Label nameLabel = new Label("工具名称: " + tool.getName());
        Label descLabel = new Label("描述: " + (tool.getDescription() != null ? tool.getDescription() : "无"));

        Environment env = dataManager.loadEnvironments().stream()
                .filter(e -> e.getId().equals(tool.getEnvironmentId()))
                .findFirst()
                .orElse(null);

        Label envLabel = new Label("运行环境: " + (env != null ? env.getName() : "未找到"));
        Label cmdLabel = new Label("执行命令: " + tool.getCommand());
        Label argsLabel = new Label("参数: " + (tool.getArguments() != null && !tool.getArguments().trim().isEmpty() ? tool.getArguments() : "无"));

        infoBox.getChildren().addAll(nameLabel, descLabel, envLabel, cmdLabel, argsLabel);
        toolInfoPane.setContent(infoBox);

        // 按钮区域
        HBox buttonBox = new HBox(10);
        Button executeBtn = new Button("执行工具");
        executeBtn.setStyle("-fx-background-color: #4a90e2; -fx-text-fill: white; -fx-border-radius: 3; -fx-background-radius: 3; -fx-padding: 8 16px;");
        executeBtn.setOnAction(e -> executeTool(tool, env));

        Button clearBtn = new Button("清空终端");
        clearBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-border-radius: 3; -fx-background-radius: 3; -fx-padding: 8 16px;");
        clearBtn.setOnAction(e -> terminal.clearOutput());

        buttonBox.getChildren().addAll(executeBtn, clearBtn);

        root.getChildren().addAll(toolInfoPane, buttonBox, terminal);
        VBox.setVgrow(terminal, javafx.scene.layout.Priority.ALWAYS);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        // 设置窗口关闭事件
        stage.setOnCloseRequest(e -> {
            terminal.closeTerminal();
            stage.close();
            // 调用外部回调（如果存在）
            if (externalOnCloseCallback != null) {
                externalOnCloseCallback.run();
            }
        });

        // 如果有工具信息，自动执行
        if (tool.getCommand() != null && !tool.getCommand().trim().isEmpty()) {
            // 延迟一点执行，确保界面已经显示
            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                executeTool(tool, env);
            });
        }

        stage.show();
    }

    private void executeTool(ToolItem tool, Environment environment) {
        if (environment == null) {
            showAlert("错误", "找不到运行环境，请检查工具配置");
            return;
        }

        // 验证环境可执行文件是否存在
        java.io.File executableFile = new java.io.File(environment.getExecutablePath());
        if (!executableFile.exists()) {
            showAlert("环境配置错误",
                "环境可执行文件不存在:\n" + environment.getExecutablePath() +
                "\n\n请检查环境配置中的可执行文件路径是否正确。");
            return;
        }

        // 构建完整的命令
        String fullCommand = tool.getCommand();
        if (tool.getArguments() != null && !tool.getArguments().trim().isEmpty()) {
            fullCommand += " " + tool.getArguments();
        }

        // 在虚拟终端中执行命令
        terminal.executeCommand(fullCommand, environment.getExecutablePath(), environment.getParameters());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public VirtualTerminal getTerminal() {
        return terminal;
    }

    public void close() {
        if (terminal != null) {
            terminal.closeTerminal();
        }
        if (stage != null) {
            stage.close();
        }
    }
}