package com.y5neko.template.ui.common;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class Center {
    public VBox getCenterBox(){
        VBox centerBox = new VBox();
        centerBox.getStylesheets().add("css/TextField.css");
        centerBox.setPadding(new Insets(10, 10, 10, 10));
        centerBox.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));


        // ==================================================创建标签页========================================================
        TabPane tabPane = new TabPane();

        // 标签页1
        Tab tab1 = new Tab("Tab 1");
        tab1.setContent(new StackPane(new Label("Content 1")));

        // 标签页2
        Tab tab2 = new Tab("Tab2");
        tab2.setContent(new StackPane(new Label("Content 2")));

        // 设置标签页
        Tab settingsTab = new Tab("设置");
        settingsTab.setContent(new StackPane(new Label("设置页面")));

        // 添加到 TabPane
        tabPane.getTabs().addAll(tab1, tab2, settingsTab);
        // 禁止关闭标签
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        // 设置默认选中第一个标签页
        tabPane.getSelectionModel().select(0);
        centerBox.getChildren().add(tabPane);

        // ===============================================================================
        return centerBox;
    }
}
