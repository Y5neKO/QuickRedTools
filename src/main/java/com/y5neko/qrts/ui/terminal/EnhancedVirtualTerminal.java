package com.y5neko.qrts.ui.terminal;

import com.y5neko.qrts.config.GlobalVariable;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 增强虚拟终端 - 真实终端输入体验
 * 模拟真正终端的输入输出方式，命令串行执行
 */
public class EnhancedVirtualTerminal extends VBox {
    private TextArea terminalArea;
    private TextField commandInput; // 新增：独立的命令输入框
    private Label promptLabel; // 提示符标签
    private ExecutorService executor;
    private AtomicBoolean isRunning;
    private AtomicBoolean isExecuting; // 标记是否正在执行命令
    private StringBuilder currentOutput;
    private String workingDirectory;
    private String prompt;
    private Runnable onCloseCallback;

    // 当前执行的进程
    private volatile Process currentProcess;

    // 命令历史
    private List<String> commandHistory;
    private int historyIndex = -1;

    // 颜色标记（使用特殊字符标记）
    private static final String COLOR_RESET = "\u0000";
    private static final String COLOR_ERROR = "\u0001";
    private static final String COLOR_SUCCESS = "\u0002";
    private static final String COLOR_WARNING = "\u0003";
    private static final String COLOR_INFO = "\u0004";
    private static final String COLOR_PROMPT = "\u0005";

    public EnhancedVirtualTerminal() {
        executor = Executors.newSingleThreadExecutor(); // 使用单线程确保串行执行
        isRunning = new AtomicBoolean(true);
        isExecuting = new AtomicBoolean(false);
        currentOutput = new StringBuilder();
        commandHistory = new ArrayList<String>();
        workingDirectory = System.getProperty("user.home");
        prompt = buildPrompt();

        initializeUI();
        displayWelcomeMessage();
    }

    public EnhancedVirtualTerminal(Runnable onCloseCallback) {
        this();
        this.onCloseCallback = onCloseCallback;
    }

    
    
    private void initializeUI() {
        setPadding(new Insets(10));
        setSpacing(5);

        // 应用黑暗模式
        applyDarkMode();

        // 终端显示区域（仅用于显示输出）
        terminalArea = new TextArea();
        terminalArea.setEditable(false);
        terminalArea.setWrapText(true);

        // 应用终端区域样式
        updateTerminalAreaStyle();

        terminalArea.setFont(Font.font("Consolas", 14));

        // 命令输入区域
        HBox inputBox = new HBox(5);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        promptLabel = new Label();
        promptLabel.setText(prompt);
        promptLabel.setFont(Font.font("Consolas", 14));
        updatePromptLabelStyle(promptLabel);

        commandInput = new TextField();
        commandInput.setPromptText("输入命令...");
        commandInput.setFont(Font.font("Consolas", 14));
        updateInputFieldStyle(commandInput);

        inputBox.getChildren().addAll(promptLabel, commandInput);
        HBox.setHgrow(commandInput, Priority.ALWAYS);

        // 按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button clearBtn = createStyledButton("清空");
        updateButtonStyle(clearBtn);
        clearBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent e) {
                clearTerminal();
            }
        });

        Button copyBtn = createStyledButton("复制");
        updateButtonStyle(copyBtn);
        copyBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent e) {
                copyContent();
            }
        });

        Button pasteBtn = createStyledButton("粘贴");
        updateButtonStyle(pasteBtn);
        pasteBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent e) {
                pasteToTerminal();
            }
        });

        buttonBox.getChildren().addAll(clearBtn, copyBtn, pasteBtn);

        // 应用容器样式
        updateInputBoxStyle(inputBox);
        updateButtonBoxStyle(buttonBox);

        getChildren().addAll(terminalArea, inputBox, buttonBox);
        VBox.setVgrow(terminalArea, Priority.ALWAYS);

        setupEventHandlers();

        // 聚焦到输入框
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                commandInput.requestFocus();
            }
        });
    }

    private Button createStyledButton(String text) {
        final Button button = new Button(text);
        return button;
    }

    private void setupEventHandlers() {
        // 输入框键盘事件处理
        commandInput.setOnKeyPressed(new javafx.event.EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                // 优先处理Ctrl+C中断
                if (e.isControlDown() && e.getCode() == KeyCode.C) {
                    if (isExecuting.get()) {
                        interruptCurrentCommand();
                        e.consume();
                        return;
                    }
                }
                handleInputKeyPress(e);
            }
        });

        // 输入框回车事件处理
        commandInput.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent e) {
                executeCommandFromInput();
            }
        });

        // 添加全局键盘事件监听器
        commandInput.getParent().setOnKeyPressed(new javafx.event.EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                // 全局Ctrl+C处理，无论输入框是否被禁用
                if (e.isControlDown() && e.getCode() == KeyCode.C) {
                    if (isExecuting.get()) {
                        System.out.println("检测到Ctrl+C中断请求");
                        interruptCurrentCommand();
                        e.consume();
                    }
                }
            }
        });

        // 确保输入框始终可以获得焦点
        terminalArea.setOnMouseClicked(new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            public void handle(javafx.scene.input.MouseEvent event) {
                commandInput.requestFocus();
            }
        });

        // 添加延迟的Scene事件监听器
        Platform.runLater(() -> {
            javafx.scene.Scene scene = commandInput.getScene();
            if (scene != null) {
                scene.addEventHandler(KeyEvent.KEY_PRESSED, new javafx.event.EventHandler<KeyEvent>() {
                    @Override
                    public void handle(KeyEvent e) {
                        // 全局Ctrl+C处理
                        if (e.isControlDown() && e.getCode() == KeyCode.C) {
                            if (isExecuting.get()) {
                                System.out.println("Scene级别检测到Ctrl+C中断请求");
                                interruptCurrentCommand();
                                e.consume();
                            }
                        }
                    }
                });
            }
        });
    }

    private boolean isUpdatingText = false;

    /**
     * 处理输入框键盘事件
     */
    private void handleInputKeyPress(KeyEvent e) {
        // 如果正在执行命令，只允许 Ctrl+C 中断
        if (isExecuting.get()) {
            if (e.isControlDown() && e.getCode() == KeyCode.C) {
                interruptCurrentCommand();
            }
            e.consume();
            return;
        }

        // 处理历史记录导航
        if (e.getCode() == KeyCode.UP) {
            e.consume();
            navigateHistory(-1);
        } else if (e.getCode() == KeyCode.DOWN) {
            e.consume();
            navigateHistory(1);
        } else if (e.getCode() == KeyCode.TAB) {
            e.consume();
            handleTabCompletion();
        } else if (e.isControlDown() && e.getCode() == KeyCode.C) {
            e.consume();
            copyContent(); // Ctrl+C 复制功能（在没有执行命令时）
        } else if (e.isControlDown() && e.getCode() == KeyCode.V) {
            e.consume();
            pasteToTerminal();
        } else if (e.isControlDown() && e.getCode() == KeyCode.L) {
            e.consume();
            clearTerminal();
        }
    }

    
    /**
     * 中断当前执行的命令
     */
    private void interruptCurrentCommand() {
        System.out.println("interruptCurrentCommand 被调用");
        appendOutput("^C\n", "warning");

        // 如果有正在执行的进程，尝试终止它
        if (currentProcess != null && currentProcess.isAlive()) {
            try {
                System.out.println("正在终止进程");
                // 发送中断信号
                currentProcess.destroyForcibly();

                // 等待进程结束
                if (!currentProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    appendOutput("强制终止进程\n", "warning");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                appendOutput("中断命令时出错\n", "error");
            }
        } else {
            System.out.println("没有需要终止的进程");
        }

        // 重置执行状态
        isExecuting.set(false);
        currentProcess = null;
        System.out.println("重置了执行状态");

        // 清空当前输入
        commandInput.clear();

        // 强制启用输入框
        Platform.runLater(() -> {
            commandInput.setDisable(false);
            commandInput.requestFocus();
            System.out.println("重新启用输入框");
        });

        // 显示新的提示符
        displayPrompt();

        appendOutput("命令已中断\n", "info");
    }

    /**
     * 从输入框执行命令
     */
    private void executeCommandFromInput() {
        String command = commandInput.getText().trim();
        commandInput.clear();

        if (!command.isEmpty()) {
            // 显示执行的命令并执行
            String currentPrompt = promptLabel.getText();
            appendOutput(currentPrompt + command + "\n", "prompt");
            addToHistory(command);
            executeCommand(command);
        }
    }

    
    
    
    private void addToHistory(String command) {
        if (!command.trim().isEmpty()) {
            commandHistory.add(command);
            if (commandHistory.size() > 100) {
                commandHistory.remove(0);
            }
        }
    }

    private void navigateHistory(int direction) {
        if (commandHistory.isEmpty()) return;

        if (historyIndex == -1) {
            historyIndex = commandHistory.size();
        }

        historyIndex += direction;

        if (historyIndex < 0) {
            historyIndex = 0;
        } else if (historyIndex >= commandHistory.size()) {
            historyIndex = commandHistory.size();
            commandInput.clear();
            return;
        }

        commandInput.setText(commandHistory.get(historyIndex));
        commandInput.positionCaret(commandInput.getText().length());
    }

    private void handleTabCompletion() {
        String current = commandInput.getText();
        if (current.isEmpty()) return;

        String[] commonCommands = {"help", "clear", "cd", "ls", "pwd", "echo", "cat", "exit", "history"};
        for (int i = 0; i < commonCommands.length; i++) {
            if (commonCommands[i].startsWith(current)) {
                commandInput.setText(commonCommands[i]);
                commandInput.positionCaret(commandInput.getText().length());
                break;
            }
        }
    }

    private void executeCommand(final String command) {
        if (isExecuting.get()) {
            appendOutput("正在执行命令，请稍候...\n", "warning");
            displayPrompt();
            return;
        }

        if (command.equalsIgnoreCase("help")) {
            showHelp();
            return;
        }

        if (command.equalsIgnoreCase("clear")) {
            clearTerminal();
            return;
        }

        if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("quit")) {
            closeTerminal();
            return;
        }

        if (command.startsWith("cd ")) {
            changeDirectory(command.substring(3).trim());
            return;
        }

        
        if (command.equalsIgnoreCase("history")) {
            showHistory();
            return;
        }

        // 执行外部命令
        executeExternalCommand(command);
    }

    /**
     * 启用输入框
     */
    private void enableInput() {
        Platform.runLater(() -> {
            if (commandInput != null) {
                commandInput.setDisable(false);
                commandInput.requestFocus();
            }
        });
    }

    /**
     * 禁用输入框
     */
    private void disableInput() {
        Platform.runLater(() -> {
            if (commandInput != null) {
                commandInput.setDisable(true);
            }
        });
    }

    
    
    
    private void executeExternalCommand(final String command) {
        if (isExecuting.get()) {
            appendOutput("正在执行命令，请稍候...\n", "warning");
            displayPrompt();
            return;
        }

        isExecuting.set(true);

        // 禁用输入框
        disableInput();

        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessBuilder pb = new ProcessBuilder();
                    String os = System.getProperty("os.name").toLowerCase();

                    if (os.contains("win")) {
                        pb.command("cmd", "/c", command);
                    } else {
                        pb.command("/bin/sh", "-c", command);
                    }

                    pb.directory(new File(workingDirectory));
                    pb.redirectErrorStream(true);

                    Process process = pb.start();

                    // 保存当前进程引用（executeExternalCommand方法）
                    currentProcess = process;

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

                    String line;
                    while ((line = reader.readLine()) != null && isRunning.get()) {
                        final String output = line;
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                appendOutput(output + "\n", "");
                            }
                        });
                    }

                    reader.close();

                    final int exitCode = process.waitFor();

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if (exitCode != 0) {
                                appendOutput(String.format("命令执行失败，退出码: %d\n", exitCode), "error");
                            }
                            isExecuting.set(false);
                            // 清除当前进程引用
                            currentProcess = null;
                            // 重新启用输入框
                            enableInput();
                            displayPrompt();
                        }
                    });

                } catch (IOException e) {
                    final String errorMsg = e.getMessage();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            appendOutput("执行命令失败: " + errorMsg + "\n", "error");
                            isExecuting.set(false);
                            // 清除当前进程引用
                            currentProcess = null;
                            // 重新启用输入框
                            enableInput();
                            displayPrompt();
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            appendOutput("命令被中断\n", "warning");
                            isExecuting.set(false);
                            // 清除当前进程引用
                            currentProcess = null;
                            // 重新启用输入框
                            enableInput();
                            displayPrompt();
                        }
                    });
                }
            }
        });
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
                try {
                    workingDirectory = newDir.getCanonicalPath();
                    appendOutput("", "");
                } catch (IOException e) {
                    appendOutput("错误: " + e.getMessage() + "\n", "error");
                }
            } else {
                appendOutput("错误: 目录不存在 - " + newDir.getAbsolutePath() + "\n", "error");
            }
        }
        prompt = buildPrompt();
        displayPrompt();
    }

    private String buildPrompt() {
        String user = System.getProperty("user.name");
        String dir = workingDirectory;

        String home = System.getProperty("user.home");
        if (dir.startsWith(home)) {
            dir = "~" + dir.substring(home.length());
        }

        return String.format("[%s@QRT %s]$ ", user, dir);
    }

    private void showHelp() {
        appendOutput("=== QuickRedTools 虚拟终端 ===\n\n", "info");
        appendOutput("内置命令:\n", "");
        appendOutput("  help     - 显示此帮助信息\n", "");
        appendOutput("  clear    - 清空终端屏幕\n", "");
        appendOutput("  cd <dir> - 切换工作目录\n", "");
        appendOutput("  history  - 显示命令历史\n", "");
        appendOutput("  exit     - 退出终端\n\n", "");
        appendOutput("快捷键:\n", "");
        appendOutput("  ↑↓       - 浏览命令历史\n", "");
        appendOutput("  Tab      - 命令补全\n", "");
        appendOutput("  Ctrl+C   - 中断命令\n", "");
        appendOutput("  Ctrl+V   - 粘贴内容\n", "");
        appendOutput("  Ctrl+L   - 清空终端\n\n", "");
        displayPrompt();
    }

    private void showHistory() {
        appendOutput("=== 命令历史记录 ===\n", "info");

        int size = commandHistory.size();
        int start = Math.max(0, size - 20);

        for (int i = start; i < size; i++) {
            appendOutput(String.format("%3d: %s\n", i + 1, commandHistory.get(i)), "");
        }

        if (commandHistory.isEmpty()) {
            appendOutput("暂无历史记录\n", "warning");
        }

        appendOutput("\n", "");
        displayPrompt();
    }

    private void displayWelcomeMessage() {
        appendOutput("=================================\n", "info");
        appendOutput("  QuickRedTools 虚拟终端 v3.0\n", "info");
        appendOutput("  真实终端输入体验\n", "info");
        appendOutput("=================================\n", "info");
        appendOutput("输入 'help' 查看帮助信息\n\n", "");
    }

    private void updatePromptLabel() {
        if (promptLabel != null) {
            promptLabel.setText(prompt);
        }
    }

    private void displayPrompt() {
        // 更新输入框前的提示符标签
        updatePromptLabel();

        // 清空输入框并请求焦点
        commandInput.clear();

        // 如果没有正在执行命令，启用输入框并请求焦点
        if (!isExecuting.get()) {
            commandInput.setDisable(false);
            commandInput.requestFocus();
        }
    }

    private void appendOutput(String text, String colorType) {
        currentOutput.append(text);

        // 限制输出缓冲区大小
        if (currentOutput.length() > 100000) {
            int deleteLength = currentOutput.length() - 80000;
            currentOutput.delete(0, deleteLength);
        }

        updateTerminalDisplay();
    }

    private void updateTerminalDisplay() {
        isUpdatingText = true;
        String text = currentOutput.toString();
        terminalArea.setText(text);
        terminalArea.positionCaret(text.length());

        // 自动滚动到底部
        terminalArea.setScrollTop(Double.MAX_VALUE);
        isUpdatingText = false;
    }

    public void clearTerminal() {
        currentOutput.setLength(0);
        commandInput.clear();
        updateTerminalDisplay();
        displayPrompt();
    }

    public void clearOutput() {
        clearTerminal();
    }

    private void copyContent() {
        String selectedText = terminalArea.getSelectedText();
        String content = selectedText != null && !selectedText.isEmpty() ?
                selectedText : currentOutput.toString();

        if (!content.isEmpty()) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent clipboardContent =
                    new javafx.scene.input.ClipboardContent();
            clipboardContent.putString(content);
            clipboard.setContent(clipboardContent);
        }
    }

    private void pasteToTerminal() {
        if (isExecuting.get()) {
            return; // 正在执行命令时不允许粘贴
        }

        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        String clipboardText = clipboard.getString();

        if (clipboardText != null && !clipboardText.trim().isEmpty()) {
            // 只粘贴第一行，避免一次执行多个命令
            String firstLine = clipboardText.split("\n")[0];
            commandInput.setText(commandInput.getText() + firstLine);
            commandInput.positionCaret(commandInput.getText().length());
        }
    }

    /**
     * 执行工具命令
     */
    public void executeToolCommand(final String command, final String environmentPath,
                                   final String environmentParams) {
        if (isExecuting.get()) {
            appendOutput("正在执行命令，请稍候...\n", "warning");
            displayPrompt();
            return;
        }

        appendOutput("执行工具: " + command + "\n", "info");

        if (environmentPath != null && !environmentPath.trim().isEmpty()) {
            File envFile = new File(environmentPath);
            if (!envFile.exists()) {
                appendOutput("错误: 环境可执行文件不存在 - " + environmentPath + "\n", "error");
                displayPrompt();
                return;
            }
        }

        isExecuting.set(true);

        // 禁用输入框
        disableInput();

        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    List<String> cmdList = new ArrayList<String>();

                    if (environmentPath != null && !environmentPath.trim().isEmpty()) {
                        cmdList.add(environmentPath);
                        if (environmentParams != null && !environmentParams.trim().isEmpty()) {
                            String[] params = environmentParams.split("\\s+");
                            for (int i = 0; i < params.length; i++) {
                                cmdList.add(params[i]);
                            }
                        }
                    }

                    String[] commandParts = command.split("\\s+");
                    for (int i = 0; i < commandParts.length; i++) {
                        cmdList.add(commandParts[i]);
                    }

                    ProcessBuilder pb = new ProcessBuilder(cmdList);
                    pb.directory(new File(workingDirectory));
                    pb.redirectErrorStream(true);

                    Process process = pb.start();

                    // 保存当前进程引用（executeToolCommand方法）
                    currentProcess = process;

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

                    String line;
                    while ((line = reader.readLine()) != null && isRunning.get()) {
                        final String output = line;
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                appendOutput(output + "\n", "");
                            }
                        });
                    }

                    reader.close();

                    final int exitCode = process.waitFor();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            appendOutput(String.format("工具执行完成，退出码: %d\n", exitCode),
                                    exitCode == 0 ? "success" : "error");
                            isExecuting.set(false);
                            // 清除当前进程引用
                            currentProcess = null;
                            // 重新启用输入框
                            enableInput();
                            displayPrompt();
                        }
                    });

                } catch (IOException e) {
                    final String errorMsg = e.getMessage();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            appendOutput("执行工具失败: " + errorMsg + "\n", "error");
                            isExecuting.set(false);
                            // 清除当前进程引用
                            currentProcess = null;
                            // 重新启用输入框
                            enableInput();
                            displayPrompt();
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            appendOutput("工具执行被中断\n", "warning");
                            isExecuting.set(false);
                            // 清除当前进程引用
                            currentProcess = null;
                            // 重新启用输入框
                            enableInput();
                            displayPrompt();
                        }
                    });
                }
            }
        });
    }

    public void setWorkingDirectory(String directory) {
        if (directory != null && new File(directory).exists()) {
            this.workingDirectory = directory;
            this.prompt = buildPrompt();
            appendOutput("工作目录设置为: " + workingDirectory + "\n", "");
            if (!isExecuting.get()) {
                displayPrompt();
            }
        }
    }

    public void closeTerminal() {
        isRunning.set(false);

        // 中断当前执行的进程
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
            currentProcess = null;
        }

        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }

        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public boolean isExecuting() {
        return isExecuting.get();
    }

    /**
     * 应用黑暗模式
     */
    private void applyDarkMode() {
        if (GlobalVariable.isDarkMode()) {
            setStyle("-fx-background-color: #1a202c; -fx-border-color: #4a5568; -fx-border-width: 1;");
        } else {
            setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ddd; -fx-border-width: 1;");
        }
    }

    /**
     * 更新终端区域样式
     */
    private void updateTerminalAreaStyle() {
        if (GlobalVariable.isDarkMode()) {
            terminalArea.setStyle(
                    "-fx-control-inner-background: #1a202c; " +
                            "-fx-text-fill: #e2e8f0; " +
                            "-fx-font-family: 'Consolas', 'Courier New', monospace; " +
                            "-fx-font-size: 14px; " +
                            "-fx-highlight-fill: #4a5568; " +
                            "-fx-highlight-text-fill: #ffffff;"
            );
        } else {
            terminalArea.setStyle(
                    "-fx-control-inner-background: white; " +
                            "-fx-text-fill: #333; " +
                            "-fx-font-family: 'Consolas', 'Courier New', monospace; " +
                            "-fx-font-size: 14px; " +
                            "-fx-highlight-fill: #3a3a3a; " +
                            "-fx-highlight-text-fill: #ffffff;"
            );
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
     * 更新输入框容器样式
     */
    private void updateInputBoxStyle(HBox inputBox) {
        if (GlobalVariable.isDarkMode()) {
            inputBox.setStyle("-fx-background-color: #2d3748; -fx-border-color: #4a5568; -fx-border-width: 1px; -fx-border-radius: 4; -fx-background-radius: 4;");
        } else {
            inputBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ddd; -fx-border-width: 1px; -fx-border-radius: 4; -fx-background-radius: 4;");
        }
    }

    /**
     * 更新提示符标签样式
     */
    private void updatePromptLabelStyle(Label promptLabel) {
        if (GlobalVariable.isDarkMode()) {
            promptLabel.setTextFill(Color.web("#e2e8f0"));
        } else {
            promptLabel.setTextFill(Color.web("#333"));
        }
    }

    /**
     * 更新输入框样式
     */
    private void updateInputFieldStyle(TextField inputField) {
        if (GlobalVariable.isDarkMode()) {
            inputField.setStyle("-fx-background-color: #4a5568; -fx-text-fill: #e2e8f0; -fx-border-color: #718096; -fx-border-width: 1px; -fx-border-radius: 4; -fx-background-radius: 4; -fx-prompt-text-fill: #a0aec0;");
        } else {
            inputField.setStyle("-fx-background-color: white; -fx-text-fill: #333; -fx-border-color: #ddd; -fx-border-width: 1px; -fx-border-radius: 4; -fx-background-radius: 4; -fx-prompt-text-fill: #999;");
        }
    }
}