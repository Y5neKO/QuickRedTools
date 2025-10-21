package com.y5neko.qrts.ui.terminal;

import com.y5neko.qrts.model.ToolItem;
import com.y5neko.qrts.model.Environment;
import com.y5neko.qrts.service.DataManager;
import com.y5neko.qrts.config.GlobalVariable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * 终端对话框类 - JDK 8 兼容版
 * 提供终端窗口的创建和管理功能
 */
public class TerminalDialog {
    private Stage stage;
    private EnhancedVirtualTerminal terminal;
    private final DataManager dataManager;
    private Runnable externalOnCloseCallback;

    public TerminalDialog() {
        dataManager = DataManager.getInstance();
    }

    public TerminalDialog(Runnable onCloseCallback) {
        this();
        this.externalOnCloseCallback = onCloseCallback;
    }

    /**
     * 显示普通终端窗口
     */
    public void show() {
        stage = new Stage();
        stage.setTitle("虚拟终端 - 交互式增强版");
        stage.setWidth(900);
        stage.setHeight(650);
        stage.setMinWidth(700);
        stage.setMinHeight(500);

        // 创建增强终端，传入关闭回调
        terminal = new EnhancedVirtualTerminal(new Runnable() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (terminal != null) {
                            terminal.closeTerminal();
                        }
                        if (stage != null) {
                            stage.close();
                        }
                        if (externalOnCloseCallback != null) {
                            externalOnCloseCallback.run();
                        }
                    }
                });
            }
        });

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // 应用黑暗模式
        applyDarkMode(root);

        root.getChildren().add(terminal);
        VBox.setVgrow(terminal, Priority.ALWAYS);

        Scene scene = new Scene(root);

        // 应用场景黑暗模式
        applySceneDarkMode(scene);

        stage.setScene(scene);

        // 设置窗口关闭事件
        stage.setOnCloseRequest(new javafx.event.EventHandler<javafx.stage.WindowEvent>() {
            @Override
            public void handle(javafx.stage.WindowEvent event) {
                if (terminal != null) {
                    terminal.closeTerminal();
                }
                if (externalOnCloseCallback != null) {
                    externalOnCloseCallback.run();
                }
            }
        });

        stage.show();
    }

    /**
     * 显示带工具信息的终端窗口
     */
    public void show(ToolItem tool) {
        stage = new Stage();
        stage.setTitle("虚拟终端 - " + tool.getName());
        stage.setWidth(900);
        stage.setHeight(650);
        stage.setMinWidth(700);
        stage.setMinHeight(500);

        // 创建增强终端，传入关闭回调
        terminal = new EnhancedVirtualTerminal(new Runnable() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if (terminal != null) {
                            terminal.closeTerminal();
                        }
                        if (stage != null) {
                            stage.close();
                        }
                        if (externalOnCloseCallback != null) {
                            externalOnCloseCallback.run();
                        }
                    }
                });
            }
        });

        // CLI工具窗口一打开就是运行状态，不需要复杂的状态监控

        // 设置工作目录
        if (tool.getWorkingDirectory() != null && !tool.getWorkingDirectory().trim().isEmpty()) {
            terminal.setWorkingDirectory(tool.getWorkingDirectory());
        }

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // 应用黑暗模式
        applyDarkMode(root);

        // 工具信息区域
        TitledPane toolInfoPane = createToolInfoPane(tool);

        // 按钮区域
        HBox buttonBox = createButtonBox(tool);

        root.getChildren().addAll(toolInfoPane, buttonBox, terminal);
        VBox.setVgrow(terminal, Priority.ALWAYS);

        Scene scene = new Scene(root);

        // 应用场景黑暗模式
        applySceneDarkMode(scene);

        stage.setScene(scene);

        // 设置窗口关闭事件
        stage.setOnCloseRequest(new javafx.event.EventHandler<javafx.stage.WindowEvent>() {
            @Override
            public void handle(javafx.stage.WindowEvent event) {
                if (terminal != null) {
                    terminal.closeTerminal();
                }
                if (externalOnCloseCallback != null) {
                    externalOnCloseCallback.run();
                }
            }
        });

        // 如果有工具信息，延迟自动执行
        if (tool.getCommand() != null && !tool.getCommand().trim().isEmpty()) {
            javafx.application.Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    Environment env = findEnvironment(tool);
                    if (env != null) {
                        executeTool(tool, env);
                    }
                }
            });
        }

        stage.show();
    }

    /**
     * 创建工具信息面板
     */
    private TitledPane createToolInfoPane(ToolItem tool) {
        TitledPane toolInfoPane = new TitledPane();
        toolInfoPane.setText("工具信息");
        toolInfoPane.setExpanded(false);
        toolInfoPane.setCollapsible(true);

        VBox infoBox = new VBox(5);
        infoBox.setPadding(new Insets(10));

        Label nameLabel = createInfoLabel("工具名称: " + tool.getName());
        Label descLabel = createInfoLabel("描述: " +
                (tool.getDescription() != null ? tool.getDescription() : "无"));

        Environment env = findEnvironment(tool);
        Label envLabel = createInfoLabel("运行环境: " +
                (env != null ? env.getName() : "未找到"));
        Label cmdLabel = createInfoLabel("执行命令: " + tool.getCommand());
        Label argsLabel = createInfoLabel("参数: " +
                (tool.getArguments() != null && !tool.getArguments().trim().isEmpty() ?
                        tool.getArguments() : "无"));
        Label workDirLabel = createInfoLabel("工作目录: " +
                (tool.getWorkingDirectory() != null && !tool.getWorkingDirectory().trim().isEmpty() ?
                        tool.getWorkingDirectory() : "默认"));

        infoBox.getChildren().addAll(nameLabel, descLabel, envLabel, cmdLabel, argsLabel, workDirLabel);
        toolInfoPane.setContent(infoBox);

        // 应用黑暗模式样式
        updateTitledPaneStyle(toolInfoPane);

        return toolInfoPane;
    }

    /**
     * 创建信息标签
     */
    private Label createInfoLabel(String text) {
        Label label = new Label(text);
        updateInfoLabelStyle(label);
        return label;
    }

    /**
     * 创建按钮区域
     */
    private HBox createButtonBox(final ToolItem tool) {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(5, 0, 5, 0));

        Button executeBtn = createStyledButton("执行工具");
        executeBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                Environment env = findEnvironment(tool);
                if (env != null) {
                    executeTool(tool, env);
                }
            }
        });
        updateButtonStyle(executeBtn);

        Button clearBtn = createStyledButton("清空终端");
        clearBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                if (terminal != null) {
                    terminal.clearOutput();
                }
            }
        });
        updateButtonStyle(clearBtn);

        Button shellBtn = createStyledButton("启动 Shell");
        shellBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                if (terminal != null) {
                    // 通过命令启动 shell
                    terminal.executeToolCommand("shell", null, null);
                }
            }
        });
        updateButtonStyle(shellBtn);

        buttonBox.getChildren().addAll(executeBtn, clearBtn, shellBtn);

        // 应用黑暗模式到按钮容器
        updateButtonBoxStyle(buttonBox);

        return buttonBox;
    }

    /**
     * 创建样式化按钮
     */
    private Button createStyledButton(String text) {
        return new Button(text);
    }

    /**
     * 查找工具对应的环境
     */
    private Environment findEnvironment(ToolItem tool) {
        for (Environment env : dataManager.loadEnvironments()) {
            if (env.getId().equals(tool.getEnvironmentId())) {
                return env;
            }
        }
        return null;
    }

    /**
     * 执行工具
     */
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

        // 在增强虚拟终端中执行命令
        if (terminal != null) {
            terminal.executeToolCommand(
                    fullCommand,
                    environment.getExecutablePath(),
                    environment.getParameters()
            );
        }
    }

    /**
     * 显示警告对话框
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // 应用黑暗模式样式
        updateDialogStyle(alert);

        alert.showAndWait();
    }

    /**
     * 获取终端实例
     */
    public EnhancedVirtualTerminal getTerminal() {
        return terminal;
    }

    /**
     * 关闭终端对话框
     */
    public void close() {
        if (terminal != null) {
            terminal.closeTerminal();
        }
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * 获取窗口 Stage
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * 应用黑暗模式
     */
    private void applyDarkMode(VBox root) {
        if (GlobalVariable.isDarkMode()) {
            root.setStyle("-fx-background-color: #1a202c;");
        } else {
            root.setStyle("-fx-background-color: #f8f9fa;");
        }
    }

    /**
     * 应用场景黑暗模式
     */
    private void applySceneDarkMode(Scene scene) {
        if (GlobalVariable.isDarkMode()) {
            scene.getRoot().getStyleClass().add("dark");
            // 加载黑暗模式CSS
            if (!scene.getStylesheets().contains("css/DarkMode.css")) {
                scene.getStylesheets().add("css/DarkMode.css");
            }
        } else {
            scene.getRoot().getStyleClass().remove("dark");
            // 移除黑暗模式CSS
            scene.getStylesheets().remove("css/DarkMode.css");
        }
    }

    /**
     * 更新TitledPane样式
     */
    private void updateTitledPaneStyle(TitledPane titledPane) {
        if (GlobalVariable.isDarkMode()) {
            titledPane.setStyle("-fx-text-fill: #e2e8f0; -fx-background-color: #2d3748;");
            // 更新内容区域样式
            VBox content = (VBox) titledPane.getContent();
            if (content != null) {
                content.setStyle("-fx-background-color: #4a5568;");
            }
        } else {
            titledPane.setStyle("-fx-text-fill: #333; -fx-background-color: #f8f9fa;");
            // 更新内容区域样式
            VBox content = (VBox) titledPane.getContent();
            if (content != null) {
                content.setStyle("-fx-background-color: white;");
            }
        }
    }

    /**
     * 更新信息标签样式
     */
    private void updateInfoLabelStyle(Label label) {
        if (GlobalVariable.isDarkMode()) {
            label.setStyle("-fx-text-fill: #e2e8f0; -fx-font-family: 'Consolas', monospace;");
        } else {
            label.setStyle("-fx-text-fill: #333; -fx-font-family: 'Consolas', monospace;");
        }
    }

    /**
     * 更新按钮样式
     */
    private void updateButtonStyle(Button button) {
        if (GlobalVariable.isDarkMode()) {
            button.setStyle("-fx-background-color: #4a5568; -fx-text-fill: #e2e8f0; -fx-border-color: #718096; -fx-border-radius: 4; -fx-background-radius: 4;");
        } else {
            button.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #333; -fx-border-color: #ddd; -fx-border-radius: 4; -fx-background-radius: 4;");
        }
    }

    /**
     * 更新按钮容器样式
     */
    private void updateButtonBoxStyle(HBox buttonBox) {
        if (GlobalVariable.isDarkMode()) {
            buttonBox.setStyle("-fx-background-color: #2d3748;");
        } else {
            buttonBox.setStyle("-fx-background-color: #f8f9fa;");
        }
    }

    /**
     * 更新对话框样式
     */
    private void updateDialogStyle(Alert alert) {
        if (GlobalVariable.isDarkMode()) {
            alert.getDialogPane().setStyle("-fx-background-color: #2d3748;");
            // 更新对话框内容区域
            if (alert.getDialogPane().getContent() instanceof VBox) {
                VBox content = (VBox) alert.getDialogPane().getContent();
                content.setStyle("-fx-background-color: #2d3748; -fx-text-fill: #e2e8f0;");
                // 递归更新所有标签
                updateDialogLabels(content);
            }
        }
    }

    /**
     * 递归更新对话框中的所有标签
     */
    private void updateDialogLabels(javafx.scene.layout.Pane parent) {
        for (javafx.scene.Node node : parent.getChildren()) {
            if (node instanceof Label) {
                Label label = (Label) node;
                label.setTextFill(Color.web("#e2e8f0"));
            } else if (node instanceof javafx.scene.layout.Pane) {
                updateDialogLabels((javafx.scene.layout.Pane) node);
            }
        }
    }
}