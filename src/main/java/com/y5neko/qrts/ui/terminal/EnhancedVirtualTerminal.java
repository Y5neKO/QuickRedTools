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
    private ExecutorService executor;
    private AtomicBoolean isRunning;
    private AtomicBoolean isExecuting; // 标记是否正在执行命令
    private StringBuilder currentOutput;
    private String workingDirectory;
    private String prompt;
    private Runnable onCloseCallback;

    // Shell 进程相关
    private Process shellProcess;
    private PrintWriter shellWriter;
    private volatile boolean isShellMode = false;
    private Thread outputReaderThread;
    private Thread errorReaderThread;

    // 命令历史
    private List<String> commandHistory;
    private int historyIndex = -1;

    // 输入缓冲
    private StringBuilder currentInput;
    private int cursorPosition;
    private int inputStartPosition; // 输入开始位置（提示符之后）

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
        currentInput = new StringBuilder();
        workingDirectory = System.getProperty("user.home");
        prompt = buildPrompt();

        initializeUI();
        displayWelcomeMessage();
        displayPrompt();
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

        // 终端显示区域（整合输入输出）
        terminalArea = new TextArea();
        terminalArea.setEditable(false); // 通过事件控制编辑
        terminalArea.setWrapText(true);

        // 应用终端区域样式
        updateTerminalAreaStyle();

        terminalArea.setFont(Font.font("Consolas", 14));

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

        Button shellBtn = createStyledButton("启动 Shell");
        updateButtonStyle(shellBtn);
        shellBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent e) {
                executeCommand("shell");
            }
        });

        Button stopBtn = createStyledButton("停止");
        updateButtonStyle(stopBtn);
        stopBtn.setOnAction(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent e) {
                stopShell();
            }
        });

        buttonBox.getChildren().addAll(clearBtn, copyBtn, pasteBtn, shellBtn, stopBtn);

        // 应用按钮容器样式
        updateButtonBoxStyle(buttonBox);

        getChildren().addAll(terminalArea, buttonBox);
        VBox.setVgrow(terminalArea, Priority.ALWAYS);

        setupEventHandlers();

        // 聚焦到终端
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                terminalArea.requestFocus();
                terminalArea.positionCaret(terminalArea.getLength());
            }
        });
    }

    private Button createStyledButton(String text) {
        final Button button = new Button(text);
        return button;
    }

    private void setupEventHandlers() {
        // 键盘事件处理
        terminalArea.setOnKeyPressed(new javafx.event.EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                handleKeyPress(e);
            }
        });

        // 防止直接编辑
        terminalArea.textProperty().addListener(new javafx.beans.value.ChangeListener<String>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends String> observable,
                                String oldValue, String newValue) {
                // 如果文本被直接修改（非通过我们的方法），恢复
                if (!isUpdatingText && newValue != null && !newValue.equals(currentOutput.toString())) {
                    isUpdatingText = true;
                    terminalArea.setText(currentOutput.toString());
                    terminalArea.positionCaret(terminalArea.getLength());
                    isUpdatingText = false;
                }
            }
        });

        // 点击事件 - 保持光标在末尾
        terminalArea.setOnMouseClicked(new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            public void handle(javafx.scene.input.MouseEvent event) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        terminalArea.positionCaret(terminalArea.getLength());
                    }
                });
            }
        });
    }

    private boolean isUpdatingText = false;

    private void handleKeyPress(KeyEvent e) {
        // 如果正在执行命令，只允许 Ctrl+C
        if (isExecuting.get() && !isShellMode) {
            if (e.isControlDown() && e.getCode() == KeyCode.C) {
                appendOutput("\n^C\n", "");
                displayPrompt();
            }
            e.consume();
            return;
        }

        if (isShellMode && shellWriter != null) {
            handleShellKeyPress(e);
            return;
        }

        // 处理特殊键
        if (e.getCode() == KeyCode.ENTER) {
            e.consume();
            String command = currentInput.toString().trim();
            currentInput.setLength(0);
            appendOutput("\n", "");

            if (!command.isEmpty()) {
                addToHistory(command);
                executeCommand(command);
            } else {
                displayPrompt();
            }
            historyIndex = -1;

        } else if (e.getCode() == KeyCode.BACK_SPACE) {
            e.consume();
            if (currentInput.length() > 0) {
                currentInput.deleteCharAt(currentInput.length() - 1);
                updateInputDisplay();
            }

        } else if (e.getCode() == KeyCode.UP) {
            e.consume();
            navigateHistory(-1);

        } else if (e.getCode() == KeyCode.DOWN) {
            e.consume();
            navigateHistory(1);

        } else if (e.getCode() == KeyCode.TAB) {
            e.consume();
            handleTabCompletion();

        } else if (e.getCode() == KeyCode.C && e.isControlDown()) {
            e.consume();
            if (!isExecuting.get()) {
                copyContent();
            }

        } else if (e.getCode() == KeyCode.V && e.isControlDown()) {
            e.consume();
            pasteToTerminal();

        } else if (e.getCode() == KeyCode.L && e.isControlDown()) {
            e.consume();
            clearTerminal();

        } else if (e.getCode() == KeyCode.D && e.isControlDown()) {
            e.consume();
            if (isShellMode) {
                sendToShell("\u0004");
            }

        } else if (!e.isControlDown() && !e.isAltDown() && e.getText() != null &&
                !e.getText().isEmpty() && !e.getText().equals("\r") && !e.getText().equals("\n")) {
            // 普通字符输入
            e.consume();
            currentInput.append(e.getText());
            updateInputDisplay();
        } else {
            e.consume();
        }
    }

    private void updateInputDisplay() {
        // 删除当前行的输入部分
        int currentLength = terminalArea.getLength();
        int inputLength = currentInput.length();
        int promptLength = prompt.length();

        // 重新构建当前行
        String currentLine = prompt + currentInput.toString();

        // 找到最后一个换行符
        String allText = currentOutput.toString();
        int lastNewLine = allText.lastIndexOf('\n');

        if (lastNewLine >= 0) {
            currentOutput.setLength(lastNewLine + 1);
        } else {
            currentOutput.setLength(0);
        }

        currentOutput.append(currentLine);
        updateTerminalDisplay();
    }

    private void handleShellKeyPress(KeyEvent e) {
        if (shellWriter == null) return;

        String toSend = null;

        if (e.getCode() == KeyCode.ENTER) {
            toSend = currentInput.toString() + "\n";
            appendOutput(currentInput.toString() + "\n", "");
            currentInput.setLength(0);
        } else if (e.getCode() == KeyCode.BACK_SPACE) {
            if (currentInput.length() > 0) {
                currentInput.deleteCharAt(currentInput.length() - 1);
                toSend = "\b \b"; // 退格，空格，退格
            }
        } else if (e.getCode() == KeyCode.UP) {
            toSend = "\u001b[A";
        } else if (e.getCode() == KeyCode.DOWN) {
            toSend = "\u001b[B";
        } else if (e.getCode() == KeyCode.RIGHT) {
            toSend = "\u001b[C";
        } else if (e.getCode() == KeyCode.LEFT) {
            toSend = "\u001b[D";
        } else if (e.getCode() == KeyCode.TAB) {
            toSend = "\t";
        } else if (!e.isControlDown() && !e.isAltDown() && e.getText() != null &&
                !e.getText().isEmpty()) {
            toSend = e.getText();
            currentInput.append(e.getText());
        }

        if (toSend != null) {
            sendToShell(toSend);
        }

        e.consume();
    }

    private void sendToShell(String text) {
        if (shellWriter != null && isShellMode) {
            try {
                shellWriter.write(text);
                shellWriter.flush();
            } catch (Exception e) {
                appendOutput("发送数据失败: " + e.getMessage() + "\n", "error");
            }
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
            currentInput.setLength(0);
            updateInputDisplay();
            return;
        }

        currentInput.setLength(0);
        currentInput.append(commandHistory.get(historyIndex));
        updateInputDisplay();
    }

    private void handleTabCompletion() {
        String current = currentInput.toString();
        if (current.isEmpty()) return;

        String[] commonCommands = {"help", "clear", "cd", "ls", "pwd", "echo", "cat", "exit", "shell", "history"};
        for (int i = 0; i < commonCommands.length; i++) {
            if (commonCommands[i].startsWith(current)) {
                currentInput.setLength(0);
                currentInput.append(commonCommands[i]);
                updateInputDisplay();
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
            if (isShellMode) {
                stopShell();
            } else {
                closeTerminal();
            }
            return;
        }

        if (command.startsWith("cd ")) {
            changeDirectory(command.substring(3).trim());
            return;
        }

        if (command.equalsIgnoreCase("shell") || command.equalsIgnoreCase("bash") ||
                command.equalsIgnoreCase("sh")) {
            startInteractiveShell();
            return;
        }

        if (command.equalsIgnoreCase("history")) {
            showHistory();
            return;
        }

        // 执行外部命令
        executeExternalCommand(command);
    }

    private void startInteractiveShell() {
        if (isShellMode) {
            appendOutput("Shell 已在运行中\n", "warning");
            displayPrompt();
            return;
        }

        if (isExecuting.get()) {
            appendOutput("正在执行其他命令，请稍候...\n", "warning");
            displayPrompt();
            return;
        }

        isExecuting.set(true);

        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String os = System.getProperty("os.name").toLowerCase();
                    List<String> command = new ArrayList<String>();

                    if (os.contains("win")) {
                        command.add("cmd.exe");
                    } else {
                        String shell = System.getenv("SHELL");
                        if (shell == null || shell.isEmpty()) {
                            shell = "/bin/bash";
                        }
                        command.add(shell);
                        command.add("-i");
                    }

                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.directory(new File(workingDirectory));

                    Map<String, String> env = pb.environment();
                    env.put("TERM", "xterm-256color");
                    env.put("PS1", ""); // 禁用 shell 自己的提示符

                    pb.redirectErrorStream(false);

                    shellProcess = pb.start();
                    shellWriter = new PrintWriter(new OutputStreamWriter(
                            shellProcess.getOutputStream(), StandardCharsets.UTF_8), true);

                    isShellMode = true;

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            appendOutput("=== 交互式 Shell 已启动 ===\n", "info");
                            appendOutput("输入命令后按 Enter 执行\n", "info");
                            appendOutput("输入 'exit' 退出 Shell\n\n", "info");
                        }
                    });

                    startOutputReader(shellProcess.getInputStream(), false);
                    startOutputReader(shellProcess.getErrorStream(), true);

                    int exitCode = shellProcess.waitFor();

                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if (isShellMode) {
                                appendOutput(String.format("\nShell 已退出，退出码: %d\n", exitCode),
                                        exitCode == 0 ? "success" : "error");
                                stopShell();
                                isExecuting.set(false);
                                displayPrompt();
                            }
                        }
                    });

                } catch (IOException e) {
                    final String errorMsg = e.getMessage();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            appendOutput("启动 Shell 失败: " + errorMsg + "\n", "error");
                            isExecuting.set(false);
                            displayPrompt();
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            appendOutput("Shell 被中断\n", "warning");
                            stopShell();
                            isExecuting.set(false);
                            displayPrompt();
                        }
                    });
                }
            }
        });
    }

    private void startOutputReader(final InputStream inputStream, final boolean isError) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8));

                    char[] buffer = new char[1024];
                    int charsRead;

                    while ((charsRead = reader.read(buffer)) != -1 && isRunning.get() && isShellMode) {
                        final String output = new String(buffer, 0, charsRead);
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                appendOutput(output, isError ? "error" : "");
                            }
                        });
                    }

                    reader.close();
                } catch (IOException e) {
                    if (isRunning.get() && isShellMode) {
                        final String errorMsg = e.getMessage();
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                appendOutput("\n读取输出错误: " + errorMsg + "\n", "error");
                            }
                        });
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        if (isError) {
            errorReaderThread = thread;
        } else {
            outputReaderThread = thread;
        }
    }

    private void stopShell() {
        isShellMode = false;

        if (shellProcess != null && shellProcess.isAlive()) {
            shellProcess.destroy();
            try {
                shellProcess.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (shellWriter != null) {
            shellWriter.close();
            shellWriter = null;
        }

        shellProcess = null;
        currentInput.setLength(0);

        if (!isExecuting.get()) {
            appendOutput("Shell 已停止\n", "warning");
            displayPrompt();
        }
    }

    private void executeExternalCommand(final String command) {
        if (isExecuting.get()) {
            appendOutput("正在执行命令，请稍候...\n", "warning");
            displayPrompt();
            return;
        }

        isExecuting.set(true);

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
        appendOutput("  shell    - 启动交互式 Shell\n", "");
        appendOutput("  history  - 显示命令历史\n", "");
        appendOutput("  exit     - 退出终端或 Shell\n\n", "");
        appendOutput("快捷键:\n", "");
        appendOutput("  ↑↓       - 浏览命令历史\n", "");
        appendOutput("  Tab      - 命令补全\n", "");
        appendOutput("  Ctrl+C   - 复制内容\n", "");
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

    private void displayPrompt() {
        currentInput.setLength(0);
        appendOutput(prompt, "prompt");
        inputStartPosition = currentOutput.length();
    }

    private void appendOutput(String text, String colorType) {
        currentOutput.append(text);

        // 限制输出缓冲区大小
        if (currentOutput.length() > 100000) {
            int deleteLength = currentOutput.length() - 80000;
            currentOutput.delete(0, deleteLength);
            inputStartPosition = Math.max(0, inputStartPosition - deleteLength);
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
        currentInput.setLength(0);
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
        if (isExecuting.get() && !isShellMode) {
            return; // 正在执行命令时不允许粘贴
        }

        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        String clipboardText = clipboard.getString();

        if (clipboardText != null && !clipboardText.trim().isEmpty()) {
            if (isShellMode) {
                sendToShell(clipboardText);
            } else {
                // 只粘贴第一行，避免一次执行多个命令
                String firstLine = clipboardText.split("\n")[0];
                currentInput.append(firstLine);
                updateInputDisplay();
            }
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
            if (!isShellMode && !isExecuting.get()) {
                displayPrompt();
            }
        }
    }

    public void closeTerminal() {
        isRunning.set(false);

        stopShell();

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
}