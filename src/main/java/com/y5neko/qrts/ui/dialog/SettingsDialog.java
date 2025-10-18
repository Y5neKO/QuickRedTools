package com.y5neko.qrts.ui.dialog;

import com.y5neko.qrts.config.GlobalVariable;
import com.y5neko.qrts.service.DataManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

/**
 * 设置对话框
 */
public class SettingsDialog {
    private Stage dialogStage;
    private DataManager dataManager;

    // 字体设置
    private ComboBox<String> fontComboBox;
    private Slider titleFontSizeSlider;
    private Slider categoryFontSizeSlider;
    private Slider categoryDescFontSizeSlider;
    private Slider toolDescFontSizeSlider;
    private Slider buttonFontSizeSlider;

    // 滑块容器
    private VBox titleFontSliderBox;
    private VBox categoryFontSliderBox;
    private VBox categoryDescFontSliderBox;
    private VBox toolDescFontSliderBox;
    private VBox buttonFontSliderBox;

    // 预览组件
    private Label titlePreview;
    private Label categoryTitlePreview;
    private Label categoryDescPreview;
    private Label toolDescPreview;
    private Label titleLabel; // 对话框标题

    // 自定义字体管理
    private static final String CUSTOM_FONT_DIR = "fonts";
    private Button uploadFontButton;

    // 当前设置值
    private String selectedFont = "Microsoft YaHei";
    private double titleFontSize = 16;
    private double categoryFontSize = 14;
    private double categoryDescFontSize = 12;
    private double toolDescFontSize = 10;
    private double buttonFontSize = 12; // 独立的按钮字体大小

    public SettingsDialog() {
        this.dataManager = DataManager.getInstance();
        loadCurrentSettings();
    }

    /**
     * 显示设置对话框
     */
    public void show() {
        dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("设置");
        dialogStage.getIcons().add(GlobalVariable.icon);

        // 创建主容器
        VBox mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setPrefWidth(500);

        // 创建标题
        titleLabel = new Label("字体设置");
        titleLabel.setFont(new Font(selectedFont, 18));
        titleLabel.setStyle("-fx-font-weight: bold;");

        // 字体选择区域
        VBox fontSelectionBox = createFontSelectionBox();

        // 字体大小调整区域
        VBox fontSizeBox = createFontSizeBox();

        // 预览区域
        VBox previewBox = createPreviewBox();

        // 按钮区域
        HBox buttonBox = createButtonBox();

        mainContainer.getChildren().addAll(titleLabel, fontSelectionBox, fontSizeBox, previewBox, buttonBox);

        // 设置场景和样式
        Scene scene = new Scene(mainContainer);
        scene.getStylesheets().add("/css/Style.css");
        dialogStage.setScene(scene);
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
    }

    /**
     * 创建字体选择区域
     */
    private VBox createFontSelectionBox() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: white;");

        Label fontLabel = new Label("字体选择:");
        fontLabel.setFont(new Font(selectedFont, 14));

        // 使用JavaFX常用字体，确保跨平台兼容性
        List<String> systemFonts = Arrays.asList(
            "Microsoft YaHei", // 微软雅黑 (默认)
            "System", // 系统默认字体
            "Arial", // 无衬线字体
            "Calibri", // 现代无衬线字体
            "Consolas", // 等宽字体
            "Courier New", // 经典等宽字体
            "Helvetica", // 经典无衬线字体
            "Monospaced", // 等宽字体
            "SansSerif", // 无衬线字体通用
            "Serif", // 衬线字体通用
            "Tahoma", // 无衬线字体
            "Verdana", // 网页常用字体
            "Times New Roman", // 衬线字体
            "Georgia" // 衬线字体
        );

        // 加载自定义字体
        loadCustomFonts(systemFonts);

        fontComboBox = new ComboBox<>();
        fontComboBox.getItems().addAll(systemFonts);
        fontComboBox.setValue(selectedFont);
        fontComboBox.setPrefWidth(200);

        // 字体选择监听器
        fontComboBox.setOnAction(e -> {
            selectedFont = fontComboBox.getValue();
            updatePreviewFonts();
        });

        // 上传字体按钮
        uploadFontButton = new Button("上传字体");
        uploadFontButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-border-radius: 3; -fx-background-radius: 3; -fx-cursor: hand; -fx-padding: 8 12px;");
        uploadFontButton.setOnAction(e -> uploadCustomFont());

        HBox fontSelectBox = new HBox(10);
        fontSelectBox.getChildren().addAll(fontLabel, fontComboBox, uploadFontButton);
        fontSelectBox.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().add(fontSelectBox);
        return box;
    }

    /**
     * 创建预览区域
     */
    private VBox createPreviewBox() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: white;");

        Label previewLabel = new Label("字体预览:");
        previewLabel.setFont(new Font(selectedFont, 14));

        // 标题预览
        Label titlePreview = new Label("快速启动工具");
        titlePreview.setFont(new Font(selectedFont, titleFontSize));
        titlePreview.setStyle("-fx-font-weight: bold;");

        // 分类标题预览
        Label categoryTitlePreview = new Label("开发工具");
        categoryTitlePreview.setFont(new Font(selectedFont, categoryFontSize));
        categoryTitlePreview.setStyle("-fx-font-weight: bold;");

        // 分类描述预览
        Label categoryDescPreview = new Label("编程开发相关的工具集合");
        categoryDescPreview.setFont(new Font(selectedFont, categoryDescFontSize));

        // 工具描述预览
        Label toolDescPreview = new Label("Maven构建工具");
        toolDescPreview.setFont(new Font(selectedFont, toolDescFontSize));

        VBox previewContent = new VBox(5);
        previewContent.getChildren().addAll(titlePreview, categoryTitlePreview, categoryDescPreview, toolDescPreview);

        box.getChildren().addAll(previewLabel, previewContent);

        // 保存预览组件的引用，用于更新字体
        this.titlePreview = titlePreview;
        this.categoryTitlePreview = categoryTitlePreview;
        this.categoryDescPreview = categoryDescPreview;
        this.toolDescPreview = toolDescPreview;

        return box;
    }

    /**
     * 创建字体大小调整区域
     */
    private VBox createFontSizeBox() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(15));
        box.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-color: white;");

        Label sizeLabel = new Label("字体大小调整:");
        sizeLabel.setFont(new Font(selectedFont, 14));

        // 标题字体大小
        titleFontSliderBox = createFontSizeSlider("标题字体", titleFontSize, 20, 8);

        // 分类标题字体大小
        categoryFontSliderBox = createFontSizeSlider("分类标题", categoryFontSize, 18, 8);

        // 分类描述字体大小
        categoryDescFontSliderBox = createFontSizeSlider("分类描述", categoryDescFontSize, 16, 8);

        // 工具描述字体大小
        toolDescFontSliderBox = createFontSizeSlider("工具描述", toolDescFontSize, 14, 6);

        // 按钮字体大小
        buttonFontSliderBox = createFontSizeSlider("按钮字体", buttonFontSize, 16, 8);

        box.getChildren().addAll(sizeLabel, titleFontSliderBox, categoryFontSliderBox,
                                categoryDescFontSliderBox, toolDescFontSliderBox, buttonFontSliderBox);
        return box;
    }

    /**
     * 创建字体大小滑块
     */
    private VBox createFontSizeSlider(String labelName, double initialValue, double maxValue, double minValue) {
        VBox box = new VBox(5);

        Label label = new Label(labelName + ": " + String.format("%.0f", initialValue) + "px");
        label.setFont(new Font(selectedFont, 12));

        Slider slider = new Slider(minValue, maxValue, initialValue);
        slider.setPrefWidth(300);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(2);
        slider.setMinorTickCount(1);
        slider.setSnapToTicks(true);

        // 更新标签显示和预览
        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double size = newVal.doubleValue();
            label.setText(labelName + ": " + String.format("%.0f", size) + "px");

            // 更新对应的字体大小变量和预览
            if (labelName.contains("标题") && !labelName.contains("分类")) {
                titleFontSize = size;
                if (titlePreview != null) {
                    titlePreview.setFont(new Font(selectedFont, size));
                }
            } else if (labelName.contains("分类标题")) {
                categoryFontSize = size;
                if (categoryTitlePreview != null) {
                    categoryTitlePreview.setFont(new Font(selectedFont, size));
                }
            } else if (labelName.contains("分类描述")) {
                categoryDescFontSize = size;
                if (categoryDescPreview != null) {
                    categoryDescPreview.setFont(new Font(selectedFont, size));
                }
            } else if (labelName.contains("工具描述")) {
                toolDescFontSize = size;
                if (toolDescPreview != null) {
                    toolDescPreview.setFont(new Font(selectedFont, size));
                }
            } else if (labelName.contains("按钮字体")) {
                buttonFontSize = size;
            }
        });

        // 保存滑块引用
        if (labelName.contains("标题") && !labelName.contains("分类")) {
            titleFontSizeSlider = slider;
        } else if (labelName.contains("分类标题")) {
            categoryFontSizeSlider = slider;
        } else if (labelName.contains("分类描述")) {
            categoryDescFontSizeSlider = slider;
        } else if (labelName.contains("工具描述")) {
            toolDescFontSizeSlider = slider;
        } else if (labelName.contains("按钮字体")) {
            buttonFontSizeSlider = slider;
        }

        box.getChildren().addAll(label, slider);
        return box;
    }

    /**
     * 创建按钮区域
     */
    private HBox createButtonBox() {
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);

        Button saveButton = new Button("保存");
        saveButton.setPrefWidth(80);
        saveButton.setStyle("-fx-background-color: #4285f4; -fx-text-fill: white; -fx-background-radius: 3;");
        saveButton.setOnAction(e -> saveSettings());

        Button applyButton = new Button("应用");
        applyButton.setPrefWidth(80);
        applyButton.setOnAction(e -> applySettings());

        Button cancelButton = new Button("取消");
        cancelButton.setPrefWidth(80);
        cancelButton.setOnAction(e -> dialogStage.close());

        Button resetButton = new Button("重置");
        resetButton.setPrefWidth(80);
        resetButton.setOnAction(e -> resetToDefaults());

        buttonBox.getChildren().addAll(saveButton, applyButton, cancelButton, resetButton);
        return buttonBox;
    }

    /**
     * 加载当前设置
     */
    private void loadCurrentSettings() {
        try {
            // 从DataManager加载字体设置（如果有的话）
            String savedFont = dataManager.getAppConfig("selectedFont");
            if (savedFont != null) {
                selectedFont = savedFont;
            }

            String savedTitleSize = dataManager.getAppConfig("titleFontSize");
            if (savedTitleSize != null) {
                titleFontSize = Double.parseDouble(savedTitleSize);
            }

            String savedCategorySize = dataManager.getAppConfig("categoryFontSize");
            if (savedCategorySize != null) {
                categoryFontSize = Double.parseDouble(savedCategorySize);
            }

            String savedCategoryDescSize = dataManager.getAppConfig("categoryDescFontSize");
            if (savedCategoryDescSize != null) {
                categoryDescFontSize = Double.parseDouble(savedCategoryDescSize);
            }

            String savedToolDescSize = dataManager.getAppConfig("toolDescFontSize");
            if (savedToolDescSize != null) {
                toolDescFontSize = Double.parseDouble(savedToolDescSize);
            }

            String savedButtonSize = dataManager.getAppConfig("buttonFontSize");
            if (savedButtonSize != null) {
                buttonFontSize = Double.parseDouble(savedButtonSize);
            }
        } catch (Exception e) {
            System.err.println("加载设置失败，使用默认值: " + e.getMessage());
        }
    }

    /**
     * 保存设置
     */
    private void saveSettings() {
        applySettings();
        dialogStage.close();
    }

    /**
     * 应用设置
     */
    private void applySettings() {
        try {
            // 保存到DataManager
            dataManager.setAppConfig("selectedFont", selectedFont);
            dataManager.setAppConfig("titleFontSize", String.valueOf(titleFontSize));
            dataManager.setAppConfig("categoryFontSize", String.valueOf(categoryFontSize));
            dataManager.setAppConfig("categoryDescFontSize", String.valueOf(categoryDescFontSize));
            dataManager.setAppConfig("toolDescFontSize", String.valueOf(toolDescFontSize));
            dataManager.setAppConfig("buttonFontSize", String.valueOf(buttonFontSize));

            // 更新全局字体设置
            GlobalVariable.updateFontSettings(selectedFont, titleFontSize, categoryFontSize,
                                            categoryDescFontSize, toolDescFontSize, buttonFontSize);

            // 刷新Center界面
            com.y5neko.qrts.ui.common.Center.refreshAll();

            showAlert("设置已应用", "字体设置已成功应用并保存！", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            showAlert("保存失败", "保存设置时发生错误: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * 重置为默认值
     */
    private void resetToDefaults() {
        selectedFont = "Microsoft YaHei";
        titleFontSize = 16;
        categoryFontSize = 14;
        categoryDescFontSize = 12;
        toolDescFontSize = 10;
        buttonFontSize = 12;

        // 更新控件显示
        fontComboBox.setValue(selectedFont);
        titleFontSizeSlider.setValue(titleFontSize);
        categoryFontSizeSlider.setValue(categoryFontSize);
        categoryDescFontSizeSlider.setValue(categoryDescFontSize);
        toolDescFontSizeSlider.setValue(toolDescFontSize);
        buttonFontSizeSlider.setValue(buttonFontSize);

        updatePreviewFonts();
    }

    /**
     * 更新预览字体
     */
    private void updatePreviewFonts() {
        // 更新预览组件的字体
        if (titlePreview != null) {
            titlePreview.setFont(new Font(selectedFont, titleFontSize));
        }
        if (categoryTitlePreview != null) {
            categoryTitlePreview.setFont(new Font(selectedFont, categoryFontSize));
        }
        if (categoryDescPreview != null) {
            categoryDescPreview.setFont(new Font(selectedFont, categoryDescFontSize));
        }
        if (toolDescPreview != null) {
            toolDescPreview.setFont(new Font(selectedFont, toolDescFontSize));
        }

        // 更新对话框标题的字体
        titleLabel.setFont(new Font(selectedFont, 18));
    }

    /**
     * 递归更新节点字体
     */
    private void updateNodeFont(Node node) {
        if (node instanceof Label) {
            Label label = (Label) node;
            label.setFont(new Font(selectedFont, label.getFont().getSize()));
        } else if (node instanceof VBox) {
            VBox vbox = (VBox) node;
            for (Node child : vbox.getChildren()) {
                updateNodeFont(child);
            }
        } else if (node instanceof HBox) {
            HBox hbox = (HBox) node;
            for (Node child : hbox.getChildren()) {
                updateNodeFont(child);
            }
        }
    }

    /**
     * 显示提示对话框
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 加载自定义字体
     */
    private void loadCustomFonts(List<String> fontsList) {
        try {
            Path fontDir = Paths.get(CUSTOM_FONT_DIR);
            if (!Files.exists(fontDir)) {
                Files.createDirectories(fontDir);
                return;
            }

            File[] fontFiles = fontDir.toFile().listFiles((dir, name) ->
                name.toLowerCase().endsWith(".ttf") ||
                name.toLowerCase().endsWith(".otf") ||
                name.toLowerCase().endsWith(".woff") ||
                name.toLowerCase().endsWith(".woff2")
            );

            if (fontFiles != null) {
                for (File fontFile : fontFiles) {
                    String fontName = fontFile.getName();
                    // 移除扩展名作为字体名称
                    String displayName = fontName.substring(0, fontName.lastIndexOf('.'));
                    if (!fontsList.contains(displayName)) {
                        fontsList.add(displayName);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("加载自定义字体失败: " + e.getMessage());
        }
    }

    /**
     * 上传自定义字体
     */
    private void uploadCustomFont() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择字体文件");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("字体文件", "*.ttf", "*.otf", "*.woff", "*.woff2"),
            new FileChooser.ExtensionFilter("TrueType字体", "*.ttf"),
            new FileChooser.ExtensionFilter("OpenType字体", "*.otf"),
            new FileChooser.ExtensionFilter("Web字体", "*.woff", "*.woff2")
        );

        File selectedFile = fileChooser.showOpenDialog(dialogStage);
        if (selectedFile != null) {
            try {
                // 创建字体目录
                Path fontDir = Paths.get(CUSTOM_FONT_DIR);
                if (!Files.exists(fontDir)) {
                    Files.createDirectories(fontDir);
                }

                // 复制字体文件到应用目录
                Path targetPath = fontDir.resolve(selectedFile.getName());
                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                // 获取字体名称（不含扩展名）
                String fontName = selectedFile.getName();
                String displayName = fontName.substring(0, fontName.lastIndexOf('.'));

                // 添加到字体列表
                if (!fontComboBox.getItems().contains(displayName)) {
                    fontComboBox.getItems().add(displayName);
                    showAlert("上传成功", "字体文件已成功上传: " + displayName, Alert.AlertType.INFORMATION);
                } else {
                    showAlert("字体已存在", "该字体已经存在: " + displayName, Alert.AlertType.WARNING);
                }

            } catch (IOException e) {
                showAlert("上传失败", "上传字体文件时发生错误: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }
}