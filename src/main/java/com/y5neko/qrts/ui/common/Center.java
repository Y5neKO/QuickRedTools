package com.y5neko.qrts.ui.common;

import com.y5neko.qrts.model.Environment;
import com.y5neko.qrts.model.ToolCategory;
import com.y5neko.qrts.model.ToolItem;
import com.y5neko.qrts.service.DataManager;
import com.y5neko.qrts.service.ToolLauncher;
import com.y5neko.qrts.ui.dialog.EnvironmentDialog;
import com.y5neko.qrts.ui.dialog.ToolDialog;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Center {
    private DataManager dataManager;
    private ToolLauncher toolLauncher;
    private Map<String, VBox> categoryPanes;
    private VBox centerBox;
    private ScrollPane mainScrollPane;
    private TextField searchField; // 搜索框
    private List<ToolItem> allTools; // 存储所有工具数据
    private List<ToolCategory> allCategories; // 存储所有分类数据
    private HBox statusBar; // 状态栏
    private Label statusContent; // 状态栏内容
    private boolean statusBarVisible = true; // 状态栏是否可见
    private List<RunningToolInfo> runningTools; // 正在运行的工具信息

    public Center() {
        dataManager = DataManager.getInstance();
        toolLauncher = ToolLauncher.getInstance();
        categoryPanes = new HashMap<>();
        runningTools = new ArrayList<>();
    }

    public VBox getCenterBox(){
        centerBox = new VBox();
        centerBox.getStylesheets().add("css/TextField.css");
        centerBox.setPadding(new Insets(10, 10, 10, 10));
        centerBox.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));

        // 创建工具栏
        HBox toolBar = createToolBar();
        centerBox.getChildren().add(toolBar);

        // 创建主要内容区域 - 延迟加载以提高启动性能
        mainScrollPane = createMainContent();
        VBox.setVgrow(mainScrollPane, Priority.ALWAYS);
        centerBox.getChildren().add(mainScrollPane);

        // 创建状态栏
        statusBar = createSimpleStatusBar();
        centerBox.getChildren().add(statusBar);

        // 启动定时器，每秒更新状态栏
        startStatusTimer();

        return centerBox;
    }

    private HBox createToolBar() {
        HBox toolBar = new HBox(10);
        toolBar.setPadding(new Insets(5));
        toolBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");
        toolBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("快速启动工具");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));

        // 搜索框
        searchField = new TextField();
        searchField.setPromptText("搜索工具...");
        searchField.setPrefWidth(180);
        searchField.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 4; -fx-padding: 8 12; -fx-font-size: 13px; -fx-border-width: 1; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-focus-traversable: false;");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterAndDisplayTools(newVal);
        });

        // 搜索清除按钮
        Button clearSearchBtn = new Button("✕");
        clearSearchBtn.setStyle("-fx-background-color: transparent; -fx-border: none; -fx-text-fill: #999; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 4;");
        clearSearchBtn.setOnAction(e -> {
            searchField.clear();
        });

        // 搜索图标 - 使用文本而不是emoji
        Label searchIcon = new Label("⚲");
        searchIcon.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");

        // 搜索框容器
        HBox searchBox = new HBox(2);
        searchBox.getChildren().addAll(searchIcon, searchField, clearSearchBtn);
        searchBox.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 4; -fx-padding: 6 10; -fx-border-width: 1; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        searchBox.setAlignment(Pos.CENTER_LEFT);

        // 搜索框焦点效果 - 移除蓝色焦点边框
        searchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            // 不改变边框颜色，只保持一致的样式
        });

        // 清除按钮悬停效果
        clearSearchBtn.setOnMouseEntered(e -> {
            clearSearchBtn.setStyle("-fx-background-color: transparent; -fx-border: none; -fx-text-fill: #666; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 4;");
        });
        clearSearchBtn.setOnMouseExited(e -> {
            clearSearchBtn.setStyle("-fx-background-color: transparent; -fx-border: none; -fx-text-fill: #999; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 4;");
        });

        Button refreshBtn = new Button("刷新");
        refreshBtn.setOnAction(e -> refreshToolDisplay());
        refreshBtn.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ddd; -fx-border-radius: 4; -fx-background-radius: 4; -fx-border-width: 1; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-padding: 8 16px; -fx-cursor: hand;");

        Button manageEnvBtn = new Button("环境配置");
        manageEnvBtn.setOnAction(e -> new EnvironmentDialog(this::refreshToolDisplay).show());
        manageEnvBtn.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ddd; -fx-border-radius: 4; -fx-background-radius: 4; -fx-border-width: 1; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-padding: 8 16px; -fx-cursor: hand;");

        Button manageToolsBtn = new Button("工具管理");
        manageToolsBtn.setOnAction(e -> new ToolDialog(this::refreshToolDisplay).show());
        manageToolsBtn.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ddd; -fx-border-radius: 4; -fx-background-radius: 4; -fx-border-width: 1; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-padding: 8 16px; -fx-cursor: hand;");

        // 添加按钮悬停效果
        String buttonHoverStyle = "-fx-background-color: #e9ecef; -fx-border-color: #adb5bd; -fx-border-radius: 4; -fx-background-radius: 4; -fx-border-width: 1; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-padding: 8 16px; -fx-cursor: hand;";
        String buttonNormalStyle = "-fx-background-color: #f8f9fa; -fx-border-color: #ddd; -fx-border-radius: 4; -fx-background-radius: 4; -fx-border-width: 1; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-padding: 8 16px; -fx-cursor: hand;";

        refreshBtn.setOnMouseEntered(e -> refreshBtn.setStyle(buttonHoverStyle));
        refreshBtn.setOnMouseExited(e -> refreshBtn.setStyle(buttonNormalStyle));

        manageEnvBtn.setOnMouseEntered(e -> manageEnvBtn.setStyle(buttonHoverStyle));
        manageEnvBtn.setOnMouseExited(e -> manageEnvBtn.setStyle(buttonNormalStyle));

        manageToolsBtn.setOnMouseEntered(e -> manageToolsBtn.setStyle(buttonHoverStyle));
        manageToolsBtn.setOnMouseExited(e -> manageToolsBtn.setStyle(buttonNormalStyle));

        toolBar.getChildren().addAll(title, new Separator(Orientation.VERTICAL), searchBox, new Separator(Orientation.VERTICAL), refreshBtn, manageEnvBtn, manageToolsBtn);

        return toolBar;
    }

    private HBox createSimpleStatusBar() {
        // 主状态栏
        statusBar = new HBox(10);
        statusBar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ddd; -fx-border-width: 1 0 0 0; -fx-padding: 10 15;");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        // 状态标签
        Label statusLabel = new Label("运行状态: ");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #333;");

        // 工具信息显示区域
        statusContent = new Label();
        statusContent.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        HBox.setHgrow(statusContent, Priority.ALWAYS);

        // 控制按钮区域
        HBox controlBox = new HBox(5);
        controlBox.setAlignment(Pos.CENTER_RIGHT);

        Button toggleBtn = new Button(statusBarVisible ? "隐藏" : "显示");
        toggleBtn.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; -fx-padding: 4 10px; -fx-font-size: 11px;");
        toggleBtn.setOnAction(e -> {
            toggleStatusBar();
            toggleBtn.setText(statusBarVisible ? "隐藏" : "显示");
        });

        Button clearBtn = new Button("清理");
        clearBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; -fx-padding: 4 10px; -fx-font-size: 11px;");
        clearBtn.setOnAction(e -> clearCompletedTools());

        controlBox.getChildren().addAll(toggleBtn, clearBtn);

        statusBar.getChildren().addAll(statusLabel, statusContent, controlBox);

        // 初始化状态显示
        updateSimpleStatus();

        return statusBar;
    }

    private void toggleStatusBar() {
        statusBarVisible = !statusBarVisible;

        if (statusBarVisible) {
            // 显示完整状态栏
            statusBar.setPrefHeight(50);
            statusBar.setMaxHeight(50);
            statusBar.setMinHeight(50);

            // 显示所有组件
            for (javafx.scene.Node node : statusBar.getChildren()) {
                node.setVisible(true);
                node.setManaged(true);
            }
        } else {
            // 隐藏状态栏，但保留控制按钮
            statusBar.setPrefHeight(35);
            statusBar.setMaxHeight(35);
            statusBar.setMinHeight(35);

            // 只显示控制按钮和状态标签
            for (int i = 0; i < statusBar.getChildren().size(); i++) {
                javafx.scene.Node node = statusBar.getChildren().get(i);
                if (i == 0) { // 状态标签
                    node.setVisible(true);
                    node.setManaged(true);
                } else if (i == 2) { // 控制按钮
                    node.setVisible(true);
                    node.setManaged(true);
                } else { // 工具信息内容
                    node.setVisible(false);
                    node.setManaged(false);
                }
            }
        }
    }

    private void updateSimpleStatus() {
        // 获取工具信息标签
        Label toolsLabel = statusContent;

        if (runningTools.isEmpty()) {
            toolsLabel.setText("当前没有运行的工具");
            return;
        }

        // 构建状态信息
        StringBuilder statusText = new StringBuilder();

        // 统计运行中和已停止的工具数量
        long runningCount = runningTools.stream().mapToInt(t -> t.isRunning() ? 1 : 0).sum();
        long stoppedCount = runningTools.stream().mapToInt(t -> t.isRunning() ? 0 : 1).sum();

        statusText.append("总计: ").append(runningTools.size())
                   .append(" | 运行中: ").append(runningCount)
                   .append(" | 已停止: ").append(stoppedCount);

        // 如果有运行中的工具，显示工具名称
        if (runningCount > 0) {
            statusText.append(" | 运行中工具: ");
            StringBuilder runningToolsText = new StringBuilder();

            for (RunningToolInfo toolInfo : runningTools) {
                if (toolInfo.isRunning()) {
                    if (runningToolsText.length() > 0) {
                        runningToolsText.append(", ");
                    }
                    String name = toolInfo.getName();
                    if (name.length() > 15) {
                        name = name.substring(0, 14) + "...";
                    }
                    runningToolsText.append(name);
                }
            }

            statusText.append(runningToolsText.toString());
        }

        toolsLabel.setText(statusText.toString());
    }

    private void clearCompletedTools() {
        int removedCount = 0;
        Iterator<RunningToolInfo> iterator = runningTools.iterator();

        while (iterator.hasNext()) {
            RunningToolInfo toolInfo = iterator.next();
            if (!toolInfo.isRunning()) {
                // 强制停止已停止的进程（防止僵尸进程）
                if (toolInfo.getProcess() != null && toolInfo.getProcess().isAlive()) {
                    toolInfo.getProcess().destroyForcibly();
                }
                iterator.remove();
                removedCount++;
            }
        }

        // 如果有清理操作，显示提示
        if (removedCount > 0) {
            System.out.println("清理了 " + removedCount + " 个已完成的工具");
        } else {
            System.out.println("没有需要清理的工具");
        }

        updateSimpleStatus();
    }

    private void stopTool(RunningToolInfo toolInfo) {
        if (toolInfo.getProcess() != null && toolInfo.getProcess().isAlive()) {
            toolInfo.getProcess().destroyForcibly();
            toolInfo.setRunning(false);
            updateSimpleStatus();
        }
    }

    private void startStatusTimer() {
        Timeline timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (!runningTools.isEmpty()) {
                updateSimpleStatus();
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private ScrollPane createMainContent() {
        mainScrollPane = new ScrollPane();
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setFitToHeight(true);
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // 创建占位内容，避免启动时加载慢
        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(15));

        // 添加加载提示
        Label loadingLabel = new Label("正在加载工具数据...");
        loadingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #666;");
        loadingLabel.setAlignment(Pos.CENTER);

        VBox.setVgrow(loadingLabel, Priority.ALWAYS);
        mainContent.getChildren().add(loadingLabel);

        mainScrollPane.setContent(mainContent);

        // 延迟加载实际内容，提高启动速度
        Platform.runLater(() -> {
            refreshToolDisplay();
        });

        return mainScrollPane;
    }

    private void refreshToolDisplay() {
        // 重新加载数据
        allCategories = dataManager.loadCategories();
        allTools = dataManager.loadTools();

        // 根据当前搜索内容过滤和显示
        String searchText = searchField != null ? searchField.getText().trim() : "";
        filterAndDisplayTools(searchText);
    }

    private void filterAndDisplayTools(String searchText) {
        if (mainScrollPane == null || mainScrollPane.getContent() == null) {
            return;
        }
        VBox mainContent = (VBox) mainScrollPane.getContent();

        // 清除所有现有内容，避免重复显示
        mainContent.getChildren().clear();

        // 如果没有数据，先加载数据
        if (allCategories == null) {
            refreshToolDisplay();
            return;
        }

        // 过滤工具
        List<ToolItem> filteredTools = filterTools(searchText);

        if (allCategories.isEmpty()) {
            Label noCategoryLabel = new Label("暂无工具分类，请先添加工具分类");
            noCategoryLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
            mainContent.getChildren().add(noCategoryLabel);
            return;
        }

        // 显示搜索结果提示
        if (!searchText.isEmpty()) {
            Label searchResultLabel = new Label("搜索结果: '" + searchText + "' (找到 " + filteredTools.size() + " 个工具)");
            searchResultLabel.setStyle("-fx-text-fill: #4a90e2; -fx-font-size: 12px; -fx-font-weight: bold;");
            mainContent.getChildren().add(searchResultLabel);
        }

        // 显示分类和过滤后的工具
        boolean hasVisibleTools = false;
        for (ToolCategory category : allCategories) {
            VBox categoryPane = createCategoryPane(category, filteredTools);
            mainContent.getChildren().add(categoryPane);

            // 检查该分类是否有工具
            List<ToolItem> categoryTools = filteredTools.stream()
                    .filter(tool -> belongsToCategory(tool, category))
                    .collect(Collectors.toList());
            if (!categoryTools.isEmpty()) {
                hasVisibleTools = true;
            }
        }

        // 如果搜索后没有找到工具，显示提示
        if (!searchText.isEmpty() && !hasVisibleTools) {
            Label noResultLabel = new Label("未找到匹配的工具，请尝试其他关键词");
            noResultLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 14px;");
            mainContent.getChildren().add(noResultLabel);
        }

        // 检查是否有未分类的工具
        boolean hasUncategorizedTools = allTools.stream().anyMatch(tool -> tool.getCategoryId() == null);
        if (hasUncategorizedTools) {
            Label uncategorizedLabel = new Label("存在未分类的工具，请在工具管理中为工具指定分类");
            uncategorizedLabel.setStyle("-fx-text-fill: #ff6600; -fx-font-size: 12px;");
            mainContent.getChildren().add(uncategorizedLabel);
        }

        if (allTools.isEmpty()) {
            Label noToolLabel = new Label("暂无工具，请通过工具管理添加工具");
            noToolLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
            mainContent.getChildren().add(noToolLabel);
        }
    }

    private List<ToolItem> filterTools(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return allTools != null ? new ArrayList<>(allTools) : new ArrayList<>();
        }

        String lowerSearchText = searchText.toLowerCase().trim();

        return allTools.stream()
                .filter(tool -> {
                    // 搜索工具名称
                    boolean nameMatch = tool.getName() != null &&
                            tool.getName().toLowerCase().contains(lowerSearchText);

                    // 搜索工具描述
                    boolean descMatch = tool.getDescription() != null &&
                            tool.getDescription().toLowerCase().contains(lowerSearchText);

                    return nameMatch || descMatch;
                })
                .collect(Collectors.toList());
    }

    private VBox createCategoryPane(ToolCategory category, List<ToolItem> allTools) {
        VBox categoryPane = new VBox(10);
        categoryPane.setPadding(new Insets(15));
        categoryPane.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5; -fx-border-width: 1; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-focus-traversable: false;");
        categoryPane.setFocusTraversable(false);

        // 分类标题
        Label categoryLabel = new Label(category.getName());
        categoryLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        categoryLabel.setStyle("-fx-text-fill: #333; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        categoryLabel.setFocusTraversable(false);

        if (category.getDescription() != null && !category.getDescription().trim().isEmpty()) {
            Label descLabel = new Label(category.getDescription());
            descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            descLabel.setFocusTraversable(false);
            categoryPane.getChildren().addAll(categoryLabel, descLabel);
        } else {
            categoryPane.getChildren().add(categoryLabel);
        }

        // 工具网格
        FlowPane toolFlowPane = new FlowPane();
        toolFlowPane.setHgap(15);
        toolFlowPane.setVgap(15);
        toolFlowPane.setPadding(new Insets(10, 0, 0, 0));
        toolFlowPane.setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-border-color: transparent;");
        toolFlowPane.setFocusTraversable(false);

        // 筛选属于当前分类的工具
        List<ToolItem> categoryTools = allTools.stream()
                .filter(tool -> belongsToCategory(tool, category))
                .collect(Collectors.toList());

        if (categoryTools.isEmpty()) {
            Label noToolLabel = new Label("该分类下暂无工具");
            noToolLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 12px;");
            toolFlowPane.getChildren().add(noToolLabel);
        } else {
            for (ToolItem tool : categoryTools) {
                VBox toolBox = createToolBox(tool);
                toolFlowPane.getChildren().add(toolBox);
            }
        }

        categoryPane.getChildren().add(toolFlowPane);
        return categoryPane;
    }

    private VBox createToolBox(ToolItem tool) {
        VBox toolBox = new VBox(5);
        toolBox.setPadding(new Insets(10));
        toolBox.setPrefSize(150, 80);
        toolBox.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand; -fx-border-width: 1; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-focus-traversable: false; -fx-border-insets: 0;");
        toolBox.setAlignment(Pos.CENTER);
        toolBox.setFocusTraversable(false);

        // 鼠标悬停效果
        toolBox.setOnMouseEntered(e -> {
            toolBox.setStyle("-fx-background-color: #f0f8ff; -fx-border-color: #4a90e2; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand; -fx-border-width: 1; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-focus-traversable: false; -fx-border-insets: 0;");
        });
        toolBox.setOnMouseExited(e -> {
            toolBox.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand; -fx-border-width: 1; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-focus-traversable: false; -fx-border-insets: 0;");
        });

        // 工具名称
        Label nameLabel = new Label(tool.getName());
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        nameLabel.setStyle("-fx-text-fill: #333; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
        nameLabel.setWrapText(true);
        nameLabel.setFocusTraversable(false);

        // 工具描述
        Label descLabel = new Label();
        if (tool.getDescription() != null && !tool.getDescription().trim().isEmpty()) {
            descLabel.setText(tool.getDescription());
            descLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 10px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            descLabel.setWrapText(true);
            descLabel.setFocusTraversable(false);
        }

        // 启动按钮
        Button launchBtn = new Button("启动");
        launchBtn.setStyle("-fx-background-color: #4a90e2; -fx-text-fill: white; -fx-border-radius: 3; -fx-background-radius: 3; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-focus-traversable: false;");
        launchBtn.setFocusTraversable(false);
        launchBtn.setOnAction(e -> launchTool(tool));

        toolBox.getChildren().addAll(nameLabel);
        if (descLabel.getText() != null && !descLabel.getText().isEmpty()) {
            toolBox.getChildren().add(descLabel);
        }
        toolBox.getChildren().add(launchBtn);

        return toolBox;
    }

    private boolean belongsToCategory(ToolItem tool, ToolCategory category) {
        // 根据工具的categoryId字段来判断是否属于该分类
        if (tool.getCategoryId() == null) {
            return false; // 没有分类的工具不显示
        }
        return tool.getCategoryId().equals(category.getId());
    }

    private void launchTool(ToolItem tool) {
        try {
            // 详细验证并给出具体错误信息
            String validationError = validateToolDetailed(tool);
            if (validationError != null) {
                showAlert("工具配置无效", validationError);
                return;
            }

            // 为CLI工具创建关闭回调
            Runnable closeCallback = null;
            if (!tool.isHasGUI()) {
                closeCallback = () -> {
                    Platform.runLater(() -> {
                        // 查找对应的工具信息并更新状态
                        for (RunningToolInfo runningTool : runningTools) {
                            if (runningTool.getName().equals(tool.getName()) && runningTool.isRunning()) {
                                runningTool.setRunning(false);
                                runningTool.setStatus("已停止");
                                updateSimpleStatus();

                                // 5秒后自动移除
                                setTimeout(() -> {
                                    runningTools.remove(runningTool);
                                    updateSimpleStatus();
                                }, 5000);
                                break;
                            }
                        }
                    });
                };
            }

            // 静默启动工具，不显示成功提示
            Process process = toolLauncher.launchTool(tool, closeCallback);

            // 对于所有工具，都需要注册到状态栏（包括CLI工具）
            RunningToolInfo toolInfo = new RunningToolInfo(tool.getName(), process, !tool.isHasGUI());
            runningTools.add(toolInfo);
            updateSimpleStatus();

            // 对于GUI工具，启动监控线程来更新状态栏
            if (process != null) {
                monitorProcessWithStatus(process, toolInfo);
            }
            // 对于CLI工具，不再需要虚拟监控线程，因为有了准确的关闭回调
            else {
                // CLI工具现在通过回调准确检测关闭，不需要猜测
            }

        } catch (Exception e) {
            // 只有启动失败时才显示详细错误信息
            String errorMessage = "无法启动工具 '" + tool.getName() + "':\n";
            errorMessage += "错误详情: " + e.getMessage() + "\n";

            // 添加更多调试信息
            if (tool.getEnvironmentId() != null) {
                Environment env = dataManager.loadEnvironments().stream()
                        .filter(e2 -> e2.getId().equals(tool.getEnvironmentId()))
                        .findFirst()
                        .orElse(null);
                if (env != null) {
                    errorMessage += "环境路径: " + env.getExecutablePath() + "\n";
                }
            }
            errorMessage += "工具命令: " + tool.getCommand();

            showAlert("启动失败", errorMessage);
        }
    }

    private String validateToolDetailed(ToolItem tool) {
        if (tool == null) {
            return "工具对象为空";
        }

        if (tool.getCommand() == null || tool.getCommand().trim().isEmpty()) {
            return "工具命令未配置";
        }

        if (tool.getEnvironmentId() == null || tool.getEnvironmentId().trim().isEmpty()) {
            return "工具未指定运行环境";
        }

        Environment environment = dataManager.loadEnvironments().stream()
                .filter(env -> env.getId().equals(tool.getEnvironmentId()))
                .findFirst()
                .orElse(null);

        if (environment == null) {
            return "找不到ID为 '" + tool.getEnvironmentId() + "' 的运行环境";
        }

        if (environment.getExecutablePath() == null || environment.getExecutablePath().trim().isEmpty()) {
            return "环境 '" + environment.getName() + "' 的可执行文件路径未配置";
        }

        File executable = new File(environment.getExecutablePath());
        if (!executable.exists()) {
            return "环境可执行文件不存在: " + environment.getExecutablePath();
        }

        if (!executable.isFile()) {
            return "环境可执行文件不是有效的文件: " + environment.getExecutablePath();
        }

        if (!executable.canExecute()) {
            return "环境可执行文件没有执行权限: " + environment.getExecutablePath();
        }

        // 检查工具命令（如果是绝对路径）
        File toolCommand = new File(tool.getCommand());
        if (toolCommand.isAbsolute() && !toolCommand.exists()) {
            return "工具命令文件不存在: " + tool.getCommand();
        }

        return null; // 验证通过
    }

    private void monitorProcessSilently(Process process, ToolItem tool) {
        new Thread(() -> {
            try {
                // 静默等待进程结束，不显示任何提示
                process.waitFor();
            } catch (InterruptedException e) {
                // 监控被中断时也不显示提示
                System.err.println("监控工具 '" + tool.getName() + "' 进程时被中断: " + e.getMessage());
            }
        }).start();
    }

    private void monitorProcessWithStatus(Process process, RunningToolInfo toolInfo) {
        new Thread(() -> {
            try {
                // 等待进程结束
                process.waitFor();

                // 进程结束后更新状态栏
                Platform.runLater(() -> {
                    toolInfo.setRunning(false);
                    updateSimpleStatus();
                });

                // 5秒后自动移除已完成的工具
                Thread.sleep(5000);
                Platform.runLater(() -> {
                    // 再次检查，如果用户还没有清理，则自动移除
                    if (runningTools.contains(toolInfo)) {
                        runningTools.remove(toolInfo);
                        updateSimpleStatus();
                    }
                });

            } catch (InterruptedException e) {
                // 监控被中断时更新状态
                Platform.runLater(() -> {
                    toolInfo.setRunning(false);
                    updateSimpleStatus();
                });
                System.err.println("监控工具 '" + toolInfo.getName() + "' 进程时被中断: " + e.getMessage());
            }
        }).start();
    }

    private void monitorVirtualTool(RunningToolInfo toolInfo) {
        // 对于CLI工具，我们无法实际监控进程，但可以提供一个合理的监控时间
        new Thread(() -> {
            try {
                // 假设命令行工具运行10分钟后认为它已经完成
                // 这是一个合理的估计，因为虚拟终端会自己处理exit命令
                Thread.sleep(10 * 60 * 1000); // 10分钟

                Platform.runLater(() -> {
                    if (runningTools.contains(toolInfo)) {
                        toolInfo.setRunning(false);
                        toolInfo.setStatus("已结束");
                        updateSimpleStatus();

                        // 5秒后自动移除
                        setTimeout(() -> {
                            if (runningTools.contains(toolInfo)) {
                                runningTools.remove(toolInfo);
                                updateSimpleStatus();
                            }
                        }, 5000);
                    }
                });
            } catch (InterruptedException e) {
                // 监控被中断时更新状态
                Platform.runLater(() -> {
                    toolInfo.setRunning(false);
                    updateSimpleStatus();
                });
                System.err.println("监控虚拟工具 '" + toolInfo.getName() + "' 时被中断: " + e.getMessage());
            }
        }).start();
    }

    private void setTimeout(Runnable runnable, long delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                Platform.runLater(runnable);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 运行工具信息类
     */
    private static class RunningToolInfo {
        private String name;
        private Process process;
        private long startTime;
        private boolean running;
        private String status;

        public RunningToolInfo(String name, Process process) {
            this.name = name;
            this.process = process;
            this.startTime = System.currentTimeMillis();
            this.running = true;
            this.status = "运行中";
        }

        public RunningToolInfo(String name, Process process, boolean isCLI) {
            this.name = name;
            this.process = process;
            this.startTime = System.currentTimeMillis();
            this.running = true;
            this.status = isCLI ? "命令行工具" : "运行中";
        }

        public String getName() {
            return name;
        }

        public Process getProcess() {
            return process;
        }

        public long getStartTime() {
            return startTime;
        }

        public boolean isRunning() {
            if (running && process != null) {
                running = process.isAlive();
                if (!running) {
                    status = "已停止";
                }
            }
            return running;
        }

        public void setRunning(boolean running) {
            this.running = running;
            this.status = running ? "运行中" : "已停止";
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatus() {
            isRunning(); // 更新状态
            return status;
        }

        public String getStatusColor() {
            if (isRunning()) {
                return "#28a745"; // 绿色
            } else {
                return "#dc3545"; // 红色
            }
        }

        public String getRunningTime() {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - startTime;

            long seconds = elapsed / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;

            if (hours > 0) {
                return String.format("%d小时%d分钟", hours, minutes % 60);
            } else if (minutes > 0) {
                return String.format("%d分钟%d秒", minutes, seconds % 60);
            } else {
                return String.format("%d秒", seconds);
            }
        }
    }
}
