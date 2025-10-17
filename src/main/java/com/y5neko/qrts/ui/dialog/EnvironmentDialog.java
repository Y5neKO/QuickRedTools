package com.y5neko.qrts.ui.dialog;

import com.y5neko.qrts.model.Environment;
import com.y5neko.qrts.service.DataManager;
import com.y5neko.qrts.service.ToolLauncher;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class EnvironmentDialog {
    private Stage stage;
    private TableView<Environment> tableView;
    private DataManager dataManager;
    private ToolLauncher toolLauncher;
    private Runnable refreshCallback; // 用于刷新首页的回调

    public EnvironmentDialog() {
        dataManager = DataManager.getInstance();
        toolLauncher = ToolLauncher.getInstance();
    }

    public EnvironmentDialog(Runnable refreshCallback) {
        this();
        this.refreshCallback = refreshCallback;
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("环境配置管理");
        stage.setWidth(800);
        stage.setHeight(600);

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        // 创建表格
        createTableView();

        // 创建按钮
        HBox buttonBox = createButtonBox();

        root.getChildren().addAll(tableView, buttonBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void createTableView() {
        tableView = new TableView<>();

        // ID列
        TableColumn<Environment, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getId()));
        idCol.setPrefWidth(100);

        // 名称列
        TableColumn<Environment, String> nameCol = new TableColumn<>("名称");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(150);

        // 类型列
        TableColumn<Environment, String> typeCol = new TableColumn<>("类型");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType()));
        typeCol.setPrefWidth(80);

        // 路径列
        TableColumn<Environment, String> pathCol = new TableColumn<>("可执行文件路径");
        pathCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getExecutablePath()));
        pathCol.setPrefWidth(200);

        // 参数列
        TableColumn<Environment, String> paramsCol = new TableColumn<>("启动参数");
        paramsCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getParameters()));
        paramsCol.setPrefWidth(150);

        // 描述列
        TableColumn<Environment, String> descCol = new TableColumn<>("描述");
        descCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
        descCol.setPrefWidth(200);

        tableView.getColumns().addAll(idCol, nameCol, typeCol, pathCol, paramsCol, descCol);

        // 加载数据
        refreshTable();

        // 双击编辑
        tableView.setRowFactory(tv -> {
            TableRow<Environment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Environment env = row.getItem();
                    showEnvironmentEditDialog(env);
                }
            });
            return row;
        });
    }

    private HBox createButtonBox() {
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button addButton = new Button("添加环境");
        addButton.setOnAction(e -> showEnvironmentEditDialog(null));

        Button editButton = new Button("编辑");
        editButton.setOnAction(e -> {
            Environment selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEnvironmentEditDialog(selected);
            } else {
                showAlert("请先选择要编辑的环境");
            }
        });

        Button deleteButton = new Button("删除");
        deleteButton.setOnAction(e -> {
            Environment selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (showConfirmDialog("确定要删除环境 '" + selected.getName() + "' 吗？")) {
                    deleteEnvironment(selected);
                }
            } else {
                showAlert("请先选择要删除的环境");
            }
        });

        Button refreshButton = new Button("刷新");
        refreshButton.setOnAction(e -> refreshTable());

        buttonBox.getChildren().addAll(addButton, editButton, deleteButton, refreshButton);
        return buttonBox;
    }

    private void showEnvironmentEditDialog(Environment environment) {
        Dialog<Environment> dialog = new Dialog<>();
        dialog.setTitle(environment == null ? "添加环境" : "编辑环境");
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
        ComboBox<String> typeComboBox = new ComboBox<>();
        typeComboBox.getItems().addAll("java", "python", "go", "gem", "node", "npm", "yarn", "其他");
        TextField pathField = new TextField();
        TextField paramsField = new TextField();
        TextField descField = new TextField();

        // 创建必填字段标记
        Label nameRequiredLabel = new Label("*");
        nameRequiredLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        Label typeRequiredLabel = new Label("*");
        typeRequiredLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        Label pathRequiredLabel = new Label("*");
        pathRequiredLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        // 添加输入监听器来清除错误高亮
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                nameField.setStyle("");
            }
        });

        typeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                typeComboBox.setStyle("");
            }
        });

        pathField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                pathField.setStyle("");
            }
        });

        // 选择文件按钮
        Button browseButton = new Button("浏览");
        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            if (typeComboBox.getValue() != null) {
                switch (typeComboBox.getValue()) {
                    case "java":
                        fileChooser.setTitle("选择Java可执行文件");
                        break;
                    case "python":
                        fileChooser.setTitle("选择Python可执行文件");
                        break;
                    default:
                        fileChooser.setTitle("选择可执行文件");
                }
            }
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                pathField.setText(selectedFile.getAbsolutePath());
                // 清除错误高亮
                pathField.setStyle("");
            }
        });

        if (environment != null) {
            idField.setText(environment.getId());
            idField.setEditable(false); // 编辑时ID不可修改
            nameField.setText(environment.getName());
            typeComboBox.setValue(environment.getType());
            pathField.setText(environment.getExecutablePath());
            paramsField.setText(environment.getParameters());
            descField.setText(environment.getDescription());
        } else {
            // 新建环境时不显示ID字段，ID将在保存时自动生成
            idField.setVisible(false);
        }

        // 只有编辑现有环境时才显示ID字段
        if (environment != null) {
            grid.add(new Label("ID:"), 0, 0);
            grid.add(idField, 1, 0);
        }

        HBox nameBox = new HBox(5);
        nameBox.getChildren().addAll(new Label("名称:"), nameRequiredLabel);
        grid.add(nameBox, environment != null ? 1 : 0, environment != null ? 1 : 0);
        grid.add(nameField, 1, environment != null ? 1 : 0);

        HBox typeBox = new HBox(5);
        typeBox.getChildren().addAll(new Label("类型:"), typeRequiredLabel);
        grid.add(typeBox, 0, 2);
        grid.add(typeComboBox, 1, 2);

        HBox pathBox = new HBox(5);
        pathBox.getChildren().addAll(new Label("可执行文件路径:"), pathRequiredLabel);
        grid.add(pathBox, 0, 3);
        grid.add(pathField, 1, 3);
        grid.add(browseButton, 2, 3);

        grid.add(new Label("启动参数:"), 0, 4);
        grid.add(paramsField, 1, 4);
        grid.add(new Label("描述:"), 0, 5);
        grid.add(descField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Environment env = new Environment();
                if (environment != null) {
                    // 编辑现有环境，使用原ID
                    env.setId(idField.getText());
                } else {
                    // 新建环境，自动生成ID
                    env.setId("env-" + System.currentTimeMillis());
                }
                env.setName(nameField.getText());
                env.setType(typeComboBox.getValue());
                env.setExecutablePath(pathField.getText());
                env.setParameters(paramsField.getText());
                env.setDescription(descField.getText());
                return env;
            }
            return null;
        });

        // 保存按钮的验证逻辑
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // 清除之前的错误高亮
            nameField.setStyle("");
            typeComboBox.setStyle("");
            pathField.setStyle("");

            // 验证必填字段
            boolean hasError = false;
            StringBuilder errorMessage = new StringBuilder("请填写以下必填字段：\n");

            // 验证环境名称
            if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
                nameField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                errorMessage.append("• 环境名称\n");
                hasError = true;
            }

            // 验证环境类型
            if (typeComboBox.getValue() == null) {
                typeComboBox.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                errorMessage.append("• 环境类型\n");
                hasError = true;
            }

            // 验证可执行文件路径
            if (pathField.getText() == null || pathField.getText().trim().isEmpty()) {
                pathField.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                errorMessage.append("• 可执行文件路径\n");
                hasError = true;
            }

            // 如果有错误，显示错误信息并阻止保存
            if (hasError) {
                showAlert(errorMessage.toString());
                event.consume(); // 阻止对话框关闭
            }
        });

        Optional<Environment> result = dialog.showAndWait();
        result.ifPresent(env -> {
            if (environment == null) {
                // 添加新环境
                List<Environment> environments = dataManager.loadEnvironments();
                environments.add(env);
                dataManager.saveEnvironments(environments);
            } else {
                // 更新现有环境
                updateEnvironment(env);
            }
            refreshTable();
            triggerRefresh(); // 刷新首页
        });
    }

    private void deleteEnvironment(Environment environment) {
        List<Environment> environments = dataManager.loadEnvironments();
        environments.removeIf(env -> env.getId().equals(environment.getId()));
        dataManager.saveEnvironments(environments);
        refreshTable();
        triggerRefresh(); // 刷新首页
    }

    private void updateEnvironment(Environment updatedEnvironment) {
        List<Environment> environments = dataManager.loadEnvironments();
        for (int i = 0; i < environments.size(); i++) {
            if (environments.get(i).getId().equals(updatedEnvironment.getId())) {
                environments.set(i, updatedEnvironment);
                break;
            }
        }
        dataManager.saveEnvironments(environments);
    }

    private void refreshTable() {
        tableView.getItems().clear();
        tableView.getItems().addAll(dataManager.loadEnvironments());
    }

    private String generateId() {
        return "env-" + System.currentTimeMillis();
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