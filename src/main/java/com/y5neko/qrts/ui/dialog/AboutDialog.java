package com.y5neko.qrts.ui.dialog;

import com.y5neko.qrts.config.CopyRight;
import com.y5neko.qrts.config.GlobalVariable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;

import static com.y5neko.qrts.config.GlobalVariable.icon;

public class AboutDialog {
    private Stage stage;

    public AboutDialog() {
        initializeUI();
    }

    private void initializeUI() {
        stage = new Stage();
        stage.setTitle("关于 QuickRedTools");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.setWidth(500);
        stage.setHeight(550);

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);
        updateRootStyle(root);

        // 应用图标
        ImageView iconView = new ImageView(icon);
        iconView.setFitHeight(80);
        iconView.setPreserveRatio(true);

        // 应用名称
        Label appNameLabel = new Label("QuickRedTools");
        appNameLabel.setFont(new Font("Microsoft YaHei Bold", 24));
        updateLabelStyle(appNameLabel);

        // 版本信息
        Label versionLabel = new Label("版本 " + CopyRight.VERSION);
        versionLabel.setFont(new Font("Microsoft YaHei", 14));
        updateSecondaryLabelStyle(versionLabel);

        // 描述信息
        Label descriptionLabel = new Label("一个基于JavaFX的快速启动工具");
        descriptionLabel.setFont(new Font("Microsoft YaHei", 12));
        updateSecondaryLabelStyle(descriptionLabel);
        descriptionLabel.setTextAlignment(TextAlignment.CENTER);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(400);

        // 分割线
        HBox separator = new HBox();
        separator.setPrefHeight(1);
        updateSeparatorStyle(separator);
        separator.setMaxWidth(300);

        // 功能特性
        Label featuresTitle = new Label("主要功能");
        featuresTitle.setFont(new Font("Microsoft YaHei Bold", 14));
        updateLabelStyle(featuresTitle);

        VBox featuresBox = new VBox(6);
        featuresBox.setAlignment(Pos.CENTER_LEFT);
        featuresBox.setPadding(new Insets(0, 20, 0, 20));

        String[] features = {
            "• 多环境配置管理 (Java, Python, Go等)",
            "• 工具分类和快速启动",
            "• GUI/CLI工具区分支持",
            "• 内置虚拟终端",
            "• 实时工具状态监控"
        };

        for (String feature : features) {
            Label featureLabel = new Label(feature);
            featureLabel.setFont(new Font("Microsoft YaHei", 11));
            updateFeatureLabelStyle(featureLabel);
            featureLabel.setWrapText(true);
            featureLabel.setMaxWidth(400);
            featuresBox.getChildren().add(featureLabel);
        }

        // 技术信息
        Label techLabel = new Label("基于: JDK 8 + JavaFX 8 + Maven");
        techLabel.setFont(new Font("Microsoft YaHei", 10));
        updateTechLabelStyle(techLabel);

        // 版权信息
        Label copyrightLabel = new Label("© 2025 Y5neKO. All rights reserved.");
        copyrightLabel.setFont(new Font("Microsoft YaHei", 9));
        updateCopyrightLabelStyle(copyrightLabel);
        copyrightLabel.setWrapText(true);
        copyrightLabel.setMaxWidth(400);
        copyrightLabel.setTextAlignment(TextAlignment.CENTER);

        // 关闭按钮
        Button closeButton = new Button("确定");
        closeButton.setFont(new Font("Microsoft YaHei", 12));
        updateButtonStyle(closeButton);
        closeButton.setPrefWidth(100);
        closeButton.setOnAction(e -> stage.close());

        // 按钮悬停效果
        closeButton.setOnMouseEntered(e -> {
            updateButtonHoverStyle(closeButton);
        });
        closeButton.setOnMouseExited(e -> {
            updateButtonStyle(closeButton);
        });

        // 添加所有组件
        root.getChildren().addAll(
            iconView,
            appNameLabel,
            versionLabel,
            descriptionLabel,
            separator,
            featuresTitle,
            featuresBox,
            techLabel,
            copyrightLabel,
            closeButton
        );

        Scene scene = new Scene(root);
        stage.setScene(scene);

        // 居中显示
        stage.centerOnScreen();
    }

    public void show() {
        stage.show();
    }

    /**
     * 更新根容器样式
     */
    private void updateRootStyle(VBox root) {
        if (GlobalVariable.isDarkMode()) {
            root.setStyle("-fx-background-color: #2d3748; -fx-border-color: #4a5568; -fx-border-width: 1;");
        } else {
            root.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1;");
        }
    }

    /**
     * 更新主要标签样式
     */
    private void updateLabelStyle(Label label) {
        if (GlobalVariable.isDarkMode()) {
            label.setTextFill(Color.web("#e2e8f0"));
        } else {
            label.setTextFill(Color.web("#333"));
        }
    }

    /**
     * 更新次要标签样式
     */
    private void updateSecondaryLabelStyle(Label label) {
        if (GlobalVariable.isDarkMode()) {
            label.setTextFill(Color.web("#a0aec0"));
        } else {
            label.setTextFill(Color.web("#666"));
        }
    }

    /**
     * 更新特性标签样式
     */
    private void updateFeatureLabelStyle(Label label) {
        if (GlobalVariable.isDarkMode()) {
            label.setTextFill(Color.web("#cbd5e0"));
        } else {
            label.setTextFill(Color.web("#555"));
        }
    }

    /**
     * 更新技术标签样式
     */
    private void updateTechLabelStyle(Label label) {
        if (GlobalVariable.isDarkMode()) {
            label.setTextFill(Color.web("#718096"));
        } else {
            label.setTextFill(Color.web("#888"));
        }
    }

    /**
     * 更新版权标签样式
     */
    private void updateCopyrightLabelStyle(Label label) {
        if (GlobalVariable.isDarkMode()) {
            label.setTextFill(Color.web("#4a5568"));
        } else {
            label.setTextFill(Color.web("#999"));
        }
    }

    /**
     * 更新分割线样式
     */
    private void updateSeparatorStyle(HBox separator) {
        if (GlobalVariable.isDarkMode()) {
            separator.setStyle("-fx-background-color: #4a5568;");
        } else {
            separator.setStyle("-fx-background-color: #ddd;");
        }
    }

    /**
     * 更新按钮样式
     */
    private void updateButtonStyle(Button button) {
        if (GlobalVariable.isDarkMode()) {
            button.setStyle("-fx-background-color: #4285f4; -fx-text-fill: white; -fx-background-radius: 4; -fx-border-radius: 4; -fx-padding: 8 30px; -fx-cursor: hand;");
        } else {
            button.setStyle("-fx-background-color: #007acc; -fx-text-fill: white; -fx-background-radius: 4; -fx-border-radius: 4; -fx-padding: 8 30px; -fx-cursor: hand;");
        }
    }

    /**
     * 更新按钮悬停样式
     */
    private void updateButtonHoverStyle(Button button) {
        if (GlobalVariable.isDarkMode()) {
            button.setStyle("-fx-background-color: #3367d6; -fx-text-fill: white; -fx-background-radius: 4; -fx-border-radius: 4; -fx-padding: 8 30px; -fx-cursor: hand;");
        } else {
            button.setStyle("-fx-background-color: #005a9e; -fx-text-fill: white; -fx-background-radius: 4; -fx-border-radius: 4; -fx-padding: 8 30px; -fx-cursor: hand;");
        }
    }
}