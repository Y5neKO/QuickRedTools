package com.y5neko.qrts.ui.dialog;

import com.y5neko.qrts.config.CopyRight;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
        root.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-width: 1;");

        // 应用图标
        ImageView iconView = new ImageView(icon);
        iconView.setFitHeight(80);
        iconView.setPreserveRatio(true);

        // 应用名称
        Label appNameLabel = new Label("QuickRedTools");
        appNameLabel.setFont(new Font("Microsoft YaHei Bold", 24));
        appNameLabel.setStyle("-fx-text-fill: #333;");

        // 版本信息
        Label versionLabel = new Label("版本 " + CopyRight.VERSION);
        versionLabel.setFont(new Font("Microsoft YaHei", 14));
        versionLabel.setStyle("-fx-text-fill: #666;");

        // 描述信息
        Label descriptionLabel = new Label("一个基于JavaFX的快速启动工具");
        descriptionLabel.setFont(new Font("Microsoft YaHei", 12));
        descriptionLabel.setStyle("-fx-text-fill: #666;");
        descriptionLabel.setTextAlignment(TextAlignment.CENTER);
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(400);

        // 分割线
        HBox separator = new HBox();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color: #ddd;");
        separator.setMaxWidth(300);

        // 功能特性
        Label featuresTitle = new Label("主要功能");
        featuresTitle.setFont(new Font("Microsoft YaHei Bold", 14));
        featuresTitle.setStyle("-fx-text-fill: #333;");

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
            featureLabel.setStyle("-fx-text-fill: #555;");
            featureLabel.setWrapText(true);
            featureLabel.setMaxWidth(400);
            featuresBox.getChildren().add(featureLabel);
        }

        // 技术信息
        Label techLabel = new Label("基于: JDK 8 + JavaFX 8 + Maven");
        techLabel.setFont(new Font("Microsoft YaHei", 10));
        techLabel.setStyle("-fx-text-fill: #888;");

        // 版权信息
        Label copyrightLabel = new Label("© 2025 Y5neKO. All rights reserved.");
        copyrightLabel.setFont(new Font("Microsoft YaHei", 9));
        copyrightLabel.setStyle("-fx-text-fill: #999;");
        copyrightLabel.setWrapText(true);
        copyrightLabel.setMaxWidth(400);
        copyrightLabel.setTextAlignment(TextAlignment.CENTER);

        // 关闭按钮
        Button closeButton = new Button("确定");
        closeButton.setFont(new Font("Microsoft YaHei", 12));
        closeButton.setStyle("-fx-background-color: #007acc; -fx-text-fill: white; -fx-background-radius: 4; -fx-border-radius: 4; -fx-padding: 8 30px; -fx-cursor: hand;");
        closeButton.setPrefWidth(100);
        closeButton.setOnAction(e -> stage.close());

        // 按钮悬停效果
        closeButton.setOnMouseEntered(e -> {
            closeButton.setStyle("-fx-background-color: #005a9e; -fx-text-fill: white; -fx-background-radius: 4; -fx-border-radius: 4; -fx-padding: 8 30px; -fx-cursor: hand;");
        });
        closeButton.setOnMouseExited(e -> {
            closeButton.setStyle("-fx-background-color: #007acc; -fx-text-fill: white; -fx-background-radius: 4; -fx-border-radius: 4; -fx-padding: 8 30px; -fx-cursor: hand;");
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
}