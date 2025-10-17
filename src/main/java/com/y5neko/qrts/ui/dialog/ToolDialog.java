package com.y5neko.qrts.ui.dialog;

import com.y5neko.qrts.model.Environment;
import com.y5neko.qrts.model.ToolCategory;
import com.y5neko.qrts.model.ToolItem;
import com.y5neko.qrts.service.DataManager;
import com.y5neko.qrts.service.ToolLauncher;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class ToolDialog {
    private Stage stage;
    private TabPane tabPane;
    private DataManager dataManager;
    private ToolLauncher toolLauncher;
    private Runnable refreshCallback; // 用于刷新首页的回调

    public ToolDialog() {
        dataManager = DataManager.getInstance();
        toolLauncher = ToolLauncher.getInstance();
    }

    public ToolDialog(Runnable refreshCallback) {
        this();
        this.refreshCallback = refreshCallback;
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("工具管理");
        stage.setWidth(1000);
        stage.setHeight(700);

        BorderPane root = new BorderPane();

        // 创建标签页
        createTabPane();

        // 创建底部按钮
        HBox bottomBox = createBottomBox();

        root.setCenter(tabPane);
        root.setBottom(bottomBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void createTabPane() {
        tabPane = new TabPane();

        // 工具分类管理标签页
        Tab categoryTab = new Tab("工具分类管理");
        categoryTab.setClosable(false);
        categoryTab.setContent(createCategoryPane());

        // 工具管理标签页
        Tab toolTab = new Tab("工具管理");
        toolTab.setClosable(false);
        toolTab.setContent(createToolPane());

        tabPane.getTabs().addAll(categoryTab, toolTab);
    }

    private VBox createCategoryPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));

        // 分类表格
        TableView<ToolCategory> categoryTable = new TableView<>();

        TableColumn<ToolCategory, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getId()));
        idCol.setPrefWidth(100);

        TableColumn<ToolCategory, String> nameCol = new TableColumn<>("分类名称");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(150);

        TableColumn<ToolCategory, String> descCol = new TableColumn<>("描述");
        descCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
        descCol.setPrefWidth(300);

        categoryTable.getColumns().addAll(idCol, nameCol, descCol);
        loadCategories(categoryTable);

        // 分类操作按钮
        HBox categoryButtonBox = new HBox(10);
        Button addCategoryBtn = new Button("添加分类");
        Button editCategoryBtn = new Button("编辑分类");
        Button deleteCategoryBtn = new Button("删除分类");

        addCategoryBtn.setOnAction(e -> showCategoryEditDialog(null, categoryTable));
        editCategoryBtn.setOnAction(e -> {
            ToolCategory selected = categoryTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showCategoryEditDialog(selected, categoryTable);
            } else {
                showAlert("请先选择要编辑的分类");
            }
        });

        deleteCategoryBtn.setOnAction(e -> {
            ToolCategory selected = categoryTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (showConfirmDialog("确定要删除分类 '" + selected.getName() + "' 吗？")) {
                    deleteCategory(selected, categoryTable);
                }
            } else {
                showAlert("请先选择要删除的分类");
            }
        });

        categoryButtonBox.getChildren().addAll(addCategoryBtn, editCategoryBtn, deleteCategoryBtn);
        categoryButtonBox.setAlignment(Pos.CENTER_LEFT);

        pane.getChildren().addAll(categoryTable, categoryButtonBox);
        return pane;
    }

    private VBox createToolPane() {
        VBox pane = new VBox(10);
        pane.setPadding(new Insets(10));

        // 工具表格
        TableView<ToolItem> toolTable = new TableView<>();

        TableColumn<ToolItem, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getId()));
        idCol.setPrefWidth(80);

        TableColumn<ToolItem, String> nameCol = new TableColumn<>("工具名称");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(120);

        TableColumn<ToolItem, String> descCol = new TableColumn<>("描述");
        descCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
        descCol.setPrefWidth(150);

        TableColumn<ToolItem, String> categoryCol = new TableColumn<>("分类");
        categoryCol.setCellValueFactory(data -> {
            String categoryId = data.getValue().getCategoryId();
            ToolCategory category = findCategoryById(categoryId);
            return new javafx.beans.property.SimpleStringProperty(category != null ? category.getName() : "未分类");
        });
        categoryCol.setPrefWidth(120);

        TableColumn<ToolItem, String> envCol = new TableColumn<>("环境");
        envCol.setCellValueFactory(data -> {
            String envId = data.getValue().getEnvironmentId();
            Environment env = findEnvironmentById(envId);
            return new javafx.beans.property.SimpleStringProperty(env != null ? env.getName() : "未知环境");
        });
        envCol.setPrefWidth(120);

        TableColumn<ToolItem, String> cmdCol = new TableColumn<>("工具路径");
        cmdCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCommand()));
        cmdCol.setPrefWidth(150);

        TableColumn<ToolItem, String> argsCol = new TableColumn<>("参数");
        argsCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getArguments()));
        argsCol.setPrefWidth(150);

        toolTable.getColumns().addAll(idCol, nameCol, descCol, categoryCol, envCol, cmdCol, argsCol);
        loadTools(toolTable);

        // 工具操作按钮
        HBox toolButtonBox = new HBox(10);
        Button addToolBtn = new Button("添加工具");
        Button editToolBtn = new Button("编辑工具");
        Button deleteToolBtn = new Button("删除工具");
        Button testToolBtn = new Button("测试启动");

        addToolBtn.setOnAction(e -> showToolEditDialog(null, toolTable));
        editToolBtn.setOnAction(e -> {
            ToolItem selected = toolTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showToolEditDialog(selected, toolTable);
            } else {
                showAlert("请先选择要编辑的工具");
            }
        });

        deleteToolBtn.setOnAction(e -> {
            ToolItem selected = toolTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (showConfirmDialog("确定要删除工具 '" + selected.getName() + "' 吗？")) {
                    deleteTool(selected, toolTable);
                }
            } else {
                showAlert("请先选择要删除的工具");
            }
        });

        testToolBtn.setOnAction(e -> {
            ToolItem selected = toolTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                testTool(selected);
            } else {
                showAlert("请先选择要测试的工具");
            }
        });

        toolButtonBox.getChildren().addAll(addToolBtn, editToolBtn, deleteToolBtn, testToolBtn);
        toolButtonBox.setAlignment(Pos.CENTER_LEFT);

        pane.getChildren().addAll(toolTable, toolButtonBox);
        return pane;
    }

    private HBox createBottomBox() {
        HBox bottomBox = new HBox(10);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setAlignment(Pos.CENTER_RIGHT);

        Button refreshBtn = new Button("刷新");
        refreshBtn.setOnAction(e -> {
            // 刷新所有标签页内容
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab.getText().equals("工具分类管理")) {
                // 刷新分类表格
                VBox categoryPane = (VBox) selectedTab.getContent();
                TableView<ToolCategory> categoryTable = (TableView<ToolCategory>) categoryPane.getChildren().get(0);
                loadCategories(categoryTable);
            } else {
                // 刷新工具表格
                VBox toolPane = (VBox) selectedTab.getContent();
                TableView<ToolItem> toolTable = (TableView<ToolItem>) toolPane.getChildren().get(0);
                loadTools(toolTable);
            }
        });

        bottomBox.getChildren().addAll(refreshBtn);
        return bottomBox;
    }

    private void showCategoryEditDialog(ToolCategory category, TableView<ToolCategory> categoryTable) {
        Dialog<ToolCategory> dialog = new Dialog<>();
        dialog.setTitle(category == null ? "添加分类" : "编辑分类");
        dialog.setHeaderText(null);

        // 设置为模态对话框
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField idField = new TextField();
        TextField nameField = new TextField();
        TextField descField = new TextField();

        // 创建必填字段标记
        Label nameRequiredLabel = new Label("*");
        nameRequiredLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        // 添加输入监听器来清除错误高亮
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                nameField.setStyle("");
            }
        });

        if (category != null) {
            idField.setText(category.getId());
            idField.setEditable(false);
            nameField.setText(category.getName());
            descField.setText(category.getDescription());
        } else {
            // 新建分类时不显示ID字段，ID将在保存时自动生成
            idField.setVisible(false);
        }

        // 只有编辑现有分类时才显示ID字段
        if (category != null) {
            grid.add(new Label("ID:"), 0, 0);
            grid.add(idField, 1, 0);
        }

        HBox nameBox = new HBox(5);
        nameBox.getChildren().addAll(new Label("分类名称:"), nameRequiredLabel);
        grid.add(nameBox, category != null ? 1 : 0, category != null ? 1 : 0);
        grid.add(nameField, 1, category != null ? 1 : 0);

        grid.add(new Label("描述:"), 0, 2);
        grid.add(descField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                ToolCategory cat = new ToolCategory();
                if (category != null) {
                    // 编辑现有分类，使用原ID
                    cat.setId(idField.getText());
                } else {
                    // 新建分类，自动生成ID
                    cat.setId("cat-" + System.currentTimeMillis());
                }
                cat.setName(nameField.getText());
                cat.setDescription(descField.getText());
                return cat;
            }
            return null;
        });

        // 保存按钮的验证逻辑
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // 清除之前的错误高亮
            nameField.setStyle("");

            // 验证必填字段
            boolean hasError = false;
            StringBuilder errorMessage = new StringBuilder("请填写以下必填字段：\n");

            // 验证分类名称
            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                nameField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                errorMessage.append("• 分类名称\n");
                hasError = true;
            }

            // 如果有错误，显示错误信息并阻止保存
            if (hasError) {
                showAlert(errorMessage.toString());
                event.consume(); // 阻止对话框关闭
            }
        });

        Optional<ToolCategory> result = dialog.showAndWait();
        result.ifPresent(cat -> {
            if (category == null) {
                List<ToolCategory> categories = dataManager.loadCategories();
                categories.add(cat);
                dataManager.saveCategories(categories);
            } else {
                updateCategory(cat);
            }
            loadCategories(categoryTable);
            triggerRefresh(); // 刷新首页
        });
    }

    private void showToolEditDialog(ToolItem tool, TableView<ToolItem> toolTable) {
        Dialog<ToolItem> dialog = new Dialog<>();
        dialog.setTitle(tool == null ? "添加工具" : "编辑工具");
        dialog.setHeaderText(null);

        // 设置为模态对话框
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField idField = new TextField();
        TextField nameField = new TextField();
        TextField descField = new TextField();
        ComboBox<ToolCategory> categoryComboBox = new ComboBox<>();
        categoryComboBox.getItems().addAll(dataManager.loadCategories());
        ComboBox<Environment> envComboBox = new ComboBox<>();
        envComboBox.getItems().addAll(dataManager.loadEnvironments());
        TextField cmdField = new TextField();
        TextField argsField = new TextField();
        TextField workDirField = new TextField();
        CheckBox guiCheckBox = new CheckBox("GUI工具");
        guiCheckBox.setTooltip(new Tooltip("勾选此项表示工具带有图形界面，直接运行；否则会在终端中执行工具路径"));

        // 创建必填字段标记
        Label nameRequiredLabel = new Label("*");
        nameRequiredLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        Label categoryRequiredLabel = new Label("*");
        categoryRequiredLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        Label cmdRequiredLabel = new Label("*");
        cmdRequiredLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        // 添加输入监听器来清除错误高亮
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                nameField.setStyle("");
            }
        });

        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                categoryComboBox.setStyle("");
            }
        });

        cmdField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                cmdField.setStyle("");
            }
        });

        // 浏览按钮
        Button browseCmdBtn = new Button("浏览");
        browseCmdBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择工具路径或脚本文件");
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                cmdField.setText(selectedFile.getAbsolutePath());
                // 清除错误高亮
                cmdField.setStyle("");
            }
        });

        Button browseDirBtn = new Button("浏览");
        browseDirBtn.setOnAction(e -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle("选择工作目录");
            File selectedDir = dirChooser.showDialog(stage);
            if (selectedDir != null) {
                workDirField.setText(selectedDir.getAbsolutePath());
            }
        });

        if (tool != null) {
            idField.setText(tool.getId());
            idField.setEditable(false);
            nameField.setText(tool.getName());
            descField.setText(tool.getDescription());

            ToolCategory selectedCategory = findCategoryById(tool.getCategoryId());
            if (selectedCategory != null) {
                categoryComboBox.setValue(selectedCategory);
            }

            Environment selectedEnv = findEnvironmentById(tool.getEnvironmentId());
            if (selectedEnv != null) {
                envComboBox.setValue(selectedEnv);
            }

            cmdField.setText(tool.getCommand());
            argsField.setText(tool.getArguments());
            workDirField.setText(tool.getWorkingDirectory());
            guiCheckBox.setSelected(tool.isHasGUI());
        } else {
            // 新建工具时不显示ID字段，ID将在保存时自动生成
            idField.setVisible(false);
        }

        // 只有编辑现有工具时才显示ID字段
        if (tool != null) {
            grid.add(new Label("ID:"), 0, 0);
            grid.add(idField, 1, 0);
        }

        HBox nameBox = new HBox(5);
        nameBox.getChildren().addAll(new Label("工具名称:"), nameRequiredLabel);
        grid.add(nameBox, tool != null ? 1 : 0, tool != null ? 1 : 0);
        grid.add(nameField, 1, tool != null ? 1 : 0);

        grid.add(new Label("描述:"), 0, 2);
        grid.add(descField, 1, 2);

        HBox categoryBox = new HBox(5);
        categoryBox.getChildren().addAll(new Label("分类:"), categoryRequiredLabel);
        grid.add(categoryBox, 0, 3);
        grid.add(categoryComboBox, 1, 3);

        grid.add(new Label("环境:"), 0, 4);
        grid.add(envComboBox, 1, 4);

        HBox cmdBox = new HBox(5);
        cmdBox.getChildren().addAll(new Label("工具路径:"), cmdRequiredLabel);
        grid.add(cmdBox, 0, 5);
        grid.add(cmdField, 1, 5);
        grid.add(browseCmdBtn, 2, 5);

        grid.add(new Label("参数:"), 0, 6);
        grid.add(argsField, 1, 6);
        grid.add(new Label("工作目录:"), 0, 7);
        grid.add(workDirField, 1, 7);
        grid.add(browseDirBtn, 2, 7);

        grid.add(new Label("界面类型:"), 0, 8);
        grid.add(guiCheckBox, 1, 8);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                ToolItem item = new ToolItem();
                if (tool != null) {
                    // 编辑现有工具，使用原ID
                    item.setId(idField.getText());
                } else {
                    // 新建工具，自动生成ID
                    item.setId("tool-" + System.currentTimeMillis());
                }
                item.setName(nameField.getText());
                item.setDescription(descField.getText());

                ToolCategory selectedCategory = categoryComboBox.getValue();
                if (selectedCategory != null) {
                    item.setCategoryId(selectedCategory.getId());
                }

                Environment selectedEnv = envComboBox.getValue();
                if (selectedEnv != null) {
                    item.setEnvironmentId(selectedEnv.getId());
                }

                item.setCommand(cmdField.getText());
                item.setArguments(argsField.getText());
                item.setWorkingDirectory(workDirField.getText());
                item.setHasGUI(guiCheckBox.isSelected());
                return item;
            }
            return null;
        });

        // 保存按钮的验证逻辑
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // 清除之前的错误高亮
            nameField.setStyle("");
            categoryComboBox.setStyle("");
            cmdField.setStyle("");

            // 验证必填字段
            boolean hasError = false;
            StringBuilder errorMessage = new StringBuilder("请填写以下必填字段：\n");

            // 验证工具名称
            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                nameField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                errorMessage.append("• 工具名称\n");
                hasError = true;
            }

            // 验证分类
            if (categoryComboBox.getValue() == null) {
                categoryComboBox.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                errorMessage.append("• 分类\n");
                hasError = true;
            }

            // 验证工具路径
            if (cmdField.getText() == null || cmdField.getText().trim().isEmpty()) {
                cmdField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                errorMessage.append("• 工具路径\n");
                hasError = true;
            }

            // 如果有错误，显示错误信息并阻止保存
            if (hasError) {
                showAlert(errorMessage.toString());
                event.consume(); // 阻止对话框关闭
            }
        });

        Optional<ToolItem> result = dialog.showAndWait();
        result.ifPresent(item -> {
            if (tool == null) {
                List<ToolItem> tools = dataManager.loadTools();
                tools.add(item);
                dataManager.saveTools(tools);
            } else {
                updateTool(item);
            }
            loadTools(toolTable);
            triggerRefresh(); // 刷新首页
        });
    }

    private void deleteCategory(ToolCategory category, TableView<ToolCategory> categoryTable) {
        List<ToolCategory> categories = dataManager.loadCategories();
        categories.removeIf(cat -> cat.getId().equals(category.getId()));
        dataManager.saveCategories(categories);
        loadCategories(categoryTable);
        triggerRefresh(); // 刷新首页
    }

    private void deleteTool(ToolItem tool, TableView<ToolItem> toolTable) {
        List<ToolItem> tools = dataManager.loadTools();
        tools.removeIf(t -> t.getId().equals(tool.getId()));
        dataManager.saveTools(tools);
        loadTools(toolTable);
        triggerRefresh(); // 刷新首页
    }

    private void updateCategory(ToolCategory updatedCategory) {
        List<ToolCategory> categories = dataManager.loadCategories();
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getId().equals(updatedCategory.getId())) {
                categories.set(i, updatedCategory);
                break;
            }
        }
        dataManager.saveCategories(categories);
    }

    private void updateTool(ToolItem updatedTool) {
        List<ToolItem> tools = dataManager.loadTools();
        for (int i = 0; i < tools.size(); i++) {
            if (tools.get(i).getId().equals(updatedTool.getId())) {
                tools.set(i, updatedTool);
                break;
            }
        }
        dataManager.saveTools(tools);
    }

    private void loadCategories(TableView<ToolCategory> categoryTable) {
        categoryTable.getItems().clear();
        categoryTable.getItems().addAll(dataManager.loadCategories());
    }

    private void loadTools(TableView<ToolItem> toolTable) {
        toolTable.getItems().clear();
        toolTable.getItems().addAll(dataManager.loadTools());
    }

    private void testTool(ToolItem tool) {
        try {
            // 详细验证并给出具体错误信息
            String validationError = validateToolDetailed(tool);
            if (validationError != null) {
                showAlert("工具配置无效\n\n" + validationError);
                return;
            }

            // 静默启动工具进行测试
            Process process = toolLauncher.launchTool(tool);

            // 对于GUI工具，后台监控进程
            if (process != null) {
                new Thread(() -> {
                    try {
                        process.waitFor();
                    } catch (InterruptedException e) {
                        System.err.println("测试工具 '" + tool.getName() + "' 时被中断: " + e.getMessage());
                    }
                }).start();
            }
            // 对于CLI工具，不需要监控进程，因为会打开虚拟终端

        } catch (Exception e) {
            // 只有启动失败时才显示详细错误信息
            String errorMessage = "无法启动工具 '" + tool.getName() + "':\n";
            errorMessage += "错误详情: " + e.getMessage() + "\n";

            // 添加更多调试信息
            if (tool.getEnvironmentId() != null) {
                Environment env = findEnvironmentById(tool.getEnvironmentId());
                if (env != null) {
                    errorMessage += "环境路径: " + env.getExecutablePath() + "\n";
                }
            }
            errorMessage += "工具路径: " + tool.getCommand();

            showAlert("启动失败\n\n" + errorMessage);
        }
    }

    private String validateToolDetailed(ToolItem tool) {
        if (tool == null) {
            return "工具对象为空";
        }

        if (tool.getCommand() == null || tool.getCommand().trim().isEmpty()) {
            return "工具路径未配置";
        }

        if (tool.getEnvironmentId() == null || tool.getEnvironmentId().trim().isEmpty()) {
            return "工具未指定运行环境";
        }

        Environment environment = findEnvironmentById(tool.getEnvironmentId());
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

        // 检查工具路径（如果是绝对路径）
        File toolCommand = new File(tool.getCommand());
        if (toolCommand.isAbsolute() && !toolCommand.exists()) {
            return "工具路径文件不存在: " + tool.getCommand();
        }

        return null; // 验证通过
    }

    private ToolCategory findCategoryById(String categoryId) {
        return dataManager.loadCategories().stream()
                .filter(cat -> cat.getId().equals(categoryId))
                .findFirst()
                .orElse(null);
    }

    private Environment findEnvironmentById(String envId) {
        return dataManager.loadEnvironments().stream()
                .filter(env -> env.getId().equals(envId))
                .findFirst()
                .orElse(null);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean showConfirmDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认");
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().get() == ButtonType.OK;
    }

    private void triggerRefresh() {
        if (refreshCallback != null) {
            refreshCallback.run();
        }
    }
}