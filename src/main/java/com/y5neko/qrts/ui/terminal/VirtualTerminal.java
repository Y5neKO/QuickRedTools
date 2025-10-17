package com.y5neko.qrts.ui.terminal;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualTerminal extends VBox {
    private TextArea outputArea;
    private TextField inputField;
    private Process currentProcess;
    private BufferedWriter processWriter;
    private BufferedReader processReader;
    private BufferedReader processErrorReader;
    private ExecutorService executor;
    private List<String> commandHistory;
    private int historyIndex;
    private String workingDirectory;
    private String prompt;
    private boolean isRunning;
    private Runnable onCloseCallback;

    public VirtualTerminal() {
        executor = Executors.newCachedThreadPool();
        commandHistory = new ArrayList<>();
        historyIndex = -1;
        workingDirectory = System.getProperty("user.home");
        prompt = "> ";
        isRunning = false;

        initializeUI();
    }

    public VirtualTerminal(Runnable onCloseCallback) {
        this();
        this.onCloseCallback = onCloseCallback;
    }

    private void initializeUI() {
        setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #333; -fx-border-width: 1;");
        setPadding(new Insets(10));
        setSpacing(5);

        // 输出区域
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setPrefHeight(400);
        VBox.setVgrow(outputArea, Priority.ALWAYS);

        // 强制设置输出区域样式
        outputArea.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-font-family: 'Courier New', monospace; -fx-font-size: 14px; -fx-border-color: #333; -fx-border-width: 1;-fx-control-inner-background: black;");
        outputArea.setBackground(new Background(new BackgroundFill(javafx.scene.paint.Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));

        // 输入区域
        HBox inputBox = new HBox(5);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        Label promptLabel = new Label(prompt);
        promptLabel.setTextFill(Color.WHITE);
        promptLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 14));

        inputField = new TextField();
        inputField.setStyle("-fx-background-color: #000000; -fx-text-fill: #ffffff; -fx-font-family: 'Courier New', monospace; -fx-font-size: 14px; -fx-border-color: #333; -fx-border-width: 1;");
        inputField.setPrefHeight(30);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        inputBox.getChildren().addAll(promptLabel, inputField);

        // 按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button clearBtn = new Button("清空");
        clearBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> clearOutput());

        Button copyBtn = new Button("复制");
        copyBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;");
        copyBtn.setOnAction(e -> copyOutput());

        Button pasteBtn = new Button("粘贴");
        pasteBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;");
        pasteBtn.setOnAction(e -> pasteToInput());

        Button historyBtn = new Button("历史");
        historyBtn.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand;");
        historyBtn.setOnAction(e -> showHistory());

        buttonBox.getChildren().addAll(clearBtn, copyBtn, pasteBtn, historyBtn);

        getChildren().addAll(outputArea, inputBox, buttonBox);

        // 设置键盘事件
        setupKeyHandlers();

        // 显示欢迎信息
        appendOutput("=== 虚拟终端 ===\n", Color.WHITE);
        appendOutput("工作目录: " + workingDirectory + "\n", Color.WHITE);
        appendOutput("输入 'help' 查看可用命令\n", Color.WHITE);
        appendOutput(prompt, Color.WHITE);
    }

    private void setupKeyHandlers() {
        inputField.setOnKeyPressed(this::handleKeyPress);
        setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.C) {
                copyOutput();
                e.consume();
            }
        });
    }

    private void handleKeyPress(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            String command = inputField.getText().trim();
            if (!command.isEmpty()) {
                executeCommand(command);
                commandHistory.add(command);
                historyIndex = commandHistory.size();
            }
            inputField.clear();
            e.consume();
        } else if (e.getCode() == KeyCode.UP) {
            if (historyIndex > 0) {
                historyIndex--;
                inputField.setText(commandHistory.get(historyIndex));
                inputField.positionCaret(inputField.getText().length());
            }
            e.consume();
        } else if (e.getCode() == KeyCode.DOWN) {
            if (historyIndex < commandHistory.size() - 1) {
                historyIndex++;
                inputField.setText(commandHistory.get(historyIndex));
                inputField.positionCaret(inputField.getText().length());
            } else {
                historyIndex = commandHistory.size();
                inputField.clear();
            }
            e.consume();
        } else if (e.getCode() == KeyCode.TAB) {
            // 简单的tab补全功能
            handleTabCompletion();
            e.consume();
        }
    }

    private void handleTabCompletion() {
        String currentText = inputField.getText();
        // 这里可以实现简单的命令补全逻辑
        // 暂时只是插入一个tab
        inputField.setText(currentText + "    ");
        inputField.positionCaret(inputField.getText().length());
    }

    public void executeCommand(String command) {
        appendOutput(command + "\n", Color.WHITE);
        scrollToBottom();

        if (command.equalsIgnoreCase("help")) {
            showHelp();
            return;
        }

        if (command.equalsIgnoreCase("clear")) {
            clearOutput();
            return;
        }

        if (command.equalsIgnoreCase("exit")) {
            closeTerminal();
            // 触发关闭回调，关闭终端窗口
            if (onCloseCallback != null) {
                onCloseCallback.run();
            }
            return;
        }

        if (command.equalsIgnoreCase("history")) {
            showHistory();
            return;
        }

        if (command.startsWith("cd ")) {
            changeDirectory(command.substring(3).trim());
            return;
        }

        // 支持历史命令快速执行 (!<数字>)
        if (command.startsWith("!") && command.length() > 1) {
            try {
                int historyNum = Integer.parseInt(command.substring(1));
                if (historyNum > 0 && historyNum <= commandHistory.size()) {
                    String historyCommand = commandHistory.get(historyNum - 1);
                    appendOutput("执行历史命令: " + historyCommand + "\n", Color.WHITE);
                    scrollToBottom();
                    executeExternalCommand(historyCommand);
                    return;
                } else {
                    appendOutput("历史命令编号超出范围\n", Color.WHITE);
                    appendOutput(prompt, Color.WHITE);
                    scrollToBottom();
                    return;
                }
            } catch (NumberFormatException e) {
                appendOutput("无效的历史命令编号格式\n", Color.WHITE);
                appendOutput(prompt, Color.WHITE);
                scrollToBottom();
                return;
            }
        }

        // 执行外部命令
        executeExternalCommand(command);
    }

    private void showHelp() {
        appendOutput("可用命令:\n", Color.WHITE);
        appendOutput("  help     - 显示帮助信息\n", Color.WHITE);
        appendOutput("  clear    - 清空屏幕\n", Color.WHITE);
        appendOutput("  history  - 显示命令历史\n", Color.WHITE);
        appendOutput("  cd <dir> - 切换目录\n", Color.WHITE);
        appendOutput("  exit     - 退出终端\n", Color.WHITE);
        appendOutput("  !<数字>  - 执行历史命令\n", Color.WHITE);
        appendOutput("  ↑↓       - 浏览历史命令\n", Color.WHITE);
        appendOutput("  Tab      - 命令补全\n", Color.WHITE);
        appendOutput("  Ctrl+C   - 复制选中内容\n", Color.WHITE);
        appendOutput("  其他命令会被传递给系统执行\n", Color.WHITE);
        appendOutput(prompt, Color.WHITE);
        scrollToBottom();
    }

    private void changeDirectory(String dir) {
        if (dir.isEmpty()) {
            workingDirectory = System.getProperty("user.home");
        } else {
            File newDir = new File(dir);
            if (!newDir.isAbsolute()) {
                newDir = new File(workingDirectory, dir);
            }

            if (newDir.exists() && newDir.isDirectory()) {
                workingDirectory = newDir.getAbsolutePath();
                appendOutput("切换到目录: " + workingDirectory + "\n", Color.WHITE);
            } else {
                appendOutput("错误: 目录不存在 - " + newDir.getAbsolutePath() + "\n", Color.WHITE);
            }
        }
        appendOutput(prompt, Color.WHITE);
        scrollToBottom();
    }

    private void executeExternalCommand(String command) {
        try {
            String[] cmdArray;
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                cmdArray = new String[]{"cmd", "/c", command};
            } else {
                cmdArray = new String[]{"/bin/sh", "-c", command};
            }

            ProcessBuilder pb = new ProcessBuilder(cmdArray);
            pb.directory(new File(workingDirectory));
            pb.redirectErrorStream(true);

            currentProcess = pb.start();

            // 读取输出
            executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String output = line;
                        Platform.runLater(() -> {
                            appendOutput(output + "\n", Color.WHITE);
                            scrollToBottom();
                        });
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        appendOutput("读取输出时出错: " + e.getMessage() + "\n", Color.WHITE);
                        scrollToBottom();
                    });
                }
            });

            // 等待进程结束
            executor.submit(() -> {
                try {
                    int exitCode = currentProcess.waitFor();
                    final String exitMsg = "进程结束，退出码: " + exitCode + "\n";
                    Platform.runLater(() -> {
                        appendOutput(exitMsg, Color.WHITE);
                        appendOutput(prompt, Color.WHITE);
                        scrollToBottom();
                    });
                } catch (InterruptedException e) {
                    Platform.runLater(() -> {
                        appendOutput("进程被中断\n", Color.WHITE);
                        appendOutput(prompt, Color.WHITE);
                        scrollToBottom();
                    });
                } finally {
                    currentProcess = null;
                }
            });

        } catch (IOException e) {
            appendOutput("执行命令失败: " + e.getMessage() + "\n", Color.WHITE);
            appendOutput(prompt, Color.WHITE);
        }
    }

    public void executeCommand(String command, String environmentPath, String environmentParams) {
        appendOutput("执行: " + command + "\n", Color.WHITE);

        // 验证环境路径
        if (environmentPath != null && !environmentPath.trim().isEmpty()) {
            File envFile = new File(environmentPath);
            if (!envFile.exists()) {
                appendOutput("错误: 环境可执行文件不存在 - " + environmentPath + "\n", Color.WHITE);
                appendOutput("请检查环境配置\n", Color.WHITE);
                appendOutput(prompt, Color.WHITE);
                scrollToBottom();
                return;
            }
            if (!envFile.canExecute()) {
                appendOutput("错误: 环境可执行文件没有执行权限 - " + environmentPath + "\n", Color.WHITE);
                appendOutput("请检查文件权限\n", Color.WHITE);
                appendOutput(prompt, Color.WHITE);
                scrollToBottom();
                return;
            }
        }

        try {
            List<String> cmdList = new ArrayList<>();

            // 添加环境可执行文件
            if (environmentPath != null && !environmentPath.trim().isEmpty()) {
                cmdList.add(environmentPath);

                // 添加环境参数
                if (environmentParams != null && !environmentParams.trim().isEmpty()) {
                    String[] params = environmentParams.split("\\s+");
                    for (String param : params) {
                        if (!param.trim().isEmpty()) {
                            cmdList.add(param.trim());
                        }
                    }
                }
            }

            // 添加工具命令
            cmdList.add(command);

            ProcessBuilder pb = new ProcessBuilder(cmdList);
            pb.directory(new File(workingDirectory));
            pb.redirectErrorStream(true);

            currentProcess = pb.start();
            isRunning = true;

            // 读取输出
            executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null && isRunning) {
                        final String output = line;
                        Platform.runLater(() -> {
                            appendOutput(output + "\n", Color.WHITE);
                            scrollToBottom();
                        });
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        Platform.runLater(() -> {
                            appendOutput("读取输出时出错: " + e.getMessage() + "\n", Color.WHITE);
                            scrollToBottom();
                        });
                    }
                }
            });

            // 等待进程结束
            executor.submit(() -> {
                try {
                    int exitCode = currentProcess.waitFor();
                    if (isRunning) {
                        final String exitMsg = "进程结束，退出码: " + exitCode + "\n";
                        Platform.runLater(() -> {
                            appendOutput(exitMsg, Color.WHITE);
                            appendOutput(prompt, Color.WHITE);
                            scrollToBottom();
                        });
                    }
                } catch (InterruptedException e) {
                    if (isRunning) {
                        Platform.runLater(() -> {
                            appendOutput("进程被中断\n", Color.WHITE);
                            appendOutput(prompt, Color.WHITE);
                            scrollToBottom();
                        });
                    }
                } finally {
                    currentProcess = null;
                    isRunning = false;
                }
            });

        } catch (IOException e) {
            appendOutput("执行命令失败: " + e.getMessage() + "\n", Color.WHITE);
            appendOutput(prompt, Color.WHITE);
        }
    }

    private void appendOutput(String text, Color color) {
        Text textNode = new Text(text);
        textNode.setFill(color);
        textNode.setFont(Font.font("Courier New", 14));

        outputArea.appendText(text);
    }

    private void scrollToBottom() {
        // 自动滚动到底部
        Platform.runLater(() -> {
            outputArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public void clearOutput() {
        outputArea.clear();
        appendOutput("=== 终端已清空 ===\n", Color.WHITE);
        appendOutput(prompt, Color.WHITE);
        scrollToBottom();
    }

    private void copyOutput() {
        String selectedText = outputArea.getSelectedText();
        if (selectedText.isEmpty()) {
            // 如果没有选择文本，复制全部内容
            selectedText = outputArea.getText();
        }

        if (!selectedText.isEmpty()) {
            java.util.Map<javafx.scene.input.DataFormat, Object> content = new java.util.HashMap<>();
            content.put(javafx.scene.input.DataFormat.PLAIN_TEXT, selectedText);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
            appendOutput("内容已复制到剪贴板\n", Color.WHITE);
        }
    }

    private void pasteToInput() {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        String clipboardText = (String) clipboard.getContent(javafx.scene.input.DataFormat.PLAIN_TEXT);

        if (clipboardText != null && !clipboardText.trim().isEmpty()) {
            inputField.setText(inputField.getText() + clipboardText);
            inputField.positionCaret(inputField.getText().length());
            appendOutput("已粘贴内容到输入框\n", Color.WHITE);
        } else {
            appendOutput("剪贴板为空或不包含文本\n", Color.WHITE);
        }
    }

    private void showHistory() {
        if (commandHistory.isEmpty()) {
            appendOutput("暂无命令历史记录\n", Color.WHITE);
            appendOutput(prompt, Color.WHITE);
            scrollToBottom();
            return;
        }

        appendOutput("=== 命令历史记录 ===\n", Color.WHITE);
        for (int i = 0; i < commandHistory.size(); i++) {
            appendOutput(String.format("%3d: %s\n", i + 1, commandHistory.get(i)), Color.WHITE);
        }
        appendOutput("使用 ↑↓ 键浏览历史命令，或输入 !<数字> 执行历史命令\n", Color.WHITE);
        appendOutput(prompt, Color.WHITE);
        scrollToBottom();
    }

    public void setWorkingDirectory(String directory) {
        if (directory != null && new File(directory).exists()) {
            this.workingDirectory = directory;
            appendOutput("工作目录设置为: " + workingDirectory + "\n", Color.WHITE);
        }
    }

    public void closeTerminal() {
        isRunning = false;

        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
        }

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        appendOutput("终端已关闭\n", Color.WHITE);
    }

    public boolean isRunning() {
        return isRunning || (currentProcess != null && currentProcess.isAlive());
    }
}