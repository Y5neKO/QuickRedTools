package com.y5neko.qrts.ui.terminal;

import com.y5neko.qrts.model.ToolItem;
import com.y5neko.qrts.model.Environment;
import com.y5neko.qrts.service.DataManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
        root.setStyle("-fx-background-color: #1e1e1e;");
        root.getChildren().add(terminal);
        VBox.setVgrow(terminal, Priority.ALWAYS);

        Scene scene = new Scene(root);
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

        // 设置工作目录
        if (tool.getWorkingDirectory() != null && !tool.getWorkingDirectory().trim().isEmpty()) {
            terminal.setWorkingDirectory(tool.getWorkingDirectory());
        }

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #1e1e1e;");

        // 工具信息区域
        TitledPane toolInfoPane = createToolInfoPane(tool);

        // 按钮区域
        HBox buttonBox = createButtonBox(tool);

        root.getChildren().addAll(toolInfoPane, buttonBox, terminal);
        VBox.setVgrow(terminal, Priority.ALWAYS);

        Scene scene = new Scene(root);
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
        toolInfoPane.setStyle("-fx-text-fill: #e6e6e6;");

        VBox infoBox = new VBox(5);
        infoBox.setPadding(new Insets(10));
        infoBox.setStyle("-fx-background-color: #2d2d2d;");

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

        return toolInfoPane;
    }

    /**
     * 创建信息标签
     */
    private Label createInfoLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #e6e6e6; -fx-font-family: 'Consolas', monospace;");
        return label;
    }

    /**
     * 创建按钮区域
     */
    private HBox createButtonBox(final ToolItem tool) {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(5, 0, 5, 0));

        Button executeBtn = createStyledButton("执行工具", "#4a90e2");
        executeBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                Environment env = findEnvironment(tool);
                if (env != null) {
                    executeTool(tool, env);
                }
            }
        });

        Button clearBtn = createStyledButton("清空终端", "#444");
        clearBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                if (terminal != null) {
                    terminal.clearOutput();
                }
            }
        });

        Button shellBtn = createStyledButton("启动 Shell", "#5cb85c");
        shellBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                if (terminal != null) {
                    // 通过命令启动 shell
                    terminal.executeToolCommand("shell", null, null);
                }
            }
        });

        buttonBox.getChildren().addAll(executeBtn, clearBtn, shellBtn);

        return buttonBox;
    }

    /**
     * 创建样式化按钮
     */
    private Button createStyledButton(String text, final String color) {
        final Button button = new Button(text);
        button.setStyle(String.format(
                "-fx-background-color: %s; -fx-text-fill: white; " +
                        "-fx-border-radius: 3; -fx-background-radius: 3; " +
                        "-fx-cursor: hand; -fx-padding: 8 16px; -fx-font-weight: bold;",
                color
        ));

        // 添加悬停效果
        button.setOnMouseEntered(new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            public void handle(javafx.scene.input.MouseEvent event) {
                button.setStyle(String.format(
                        "-fx-background-color: derive(%s, -10%%); -fx-text-fill: white; " +
                                "-fx-border-radius: 3; -fx-background-radius: 3; " +
                                "-fx-cursor: hand; -fx-padding: 8 16px; -fx-font-weight: bold;",
                        color
                ));
            }
        });

        button.setOnMouseExited(new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            public void handle(javafx.scene.input.MouseEvent event) {
                button.setStyle(String.format(
                        "-fx-background-color: %s; -fx-text-fill: white; " +
                                "-fx-border-radius: 3; -fx-background-radius: 3; " +
                                "-fx-cursor: hand; -fx-padding: 8 16px; -fx-font-weight: bold;",
                        color
                ));
            }
        });

        return button;
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

        // 设置警告框样式
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #2d2d2d;");
        javafx.scene.Node contentNode = dialogPane.lookup(".content.label");
        if (contentNode != null) {
            contentNode.setStyle("-fx-text-fill: #e6e6e6;");
        }

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
}