package com.y5neko.qrts.ui.common;

import com.y5neko.qrts.config.GlobalVariable;
import com.y5neko.qrts.service.DataManager;
import com.y5neko.qrts.ui.dialog.AboutDialog;
import com.y5neko.qrts.ui.dialog.SettingsDialog;
import com.y5neko.qrts.ui.event.Components;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;

import static com.y5neko.qrts.config.GlobalVariable.icon;

public class Header {
    private MenuBar menuBar;
    private MenuItem darkModeMenuItem; // 存储黑暗模式菜单项引用

    private double xOffset = 0;
    private double yOffset = 0;

    public HBox getTitleBar(Stage primaryStage){
        /*
          设置一个网格视图作为菜单栏
         */
        // 创建一个GridPane并设置其列宽为百分比，以便它们平均分布
        GridPane gridPaneToolBar = new GridPane();gridPaneToolBar.setPadding(new Insets(0, 0, 0, 0));gridPaneToolBar.setHgap(10);gridPaneToolBar.setVgap(10); // 行之间的垂直间距
        // 为GridPane添加三列，并设置它们的百分比宽度
        ColumnConstraints columnToolBar1 = new ColumnConstraints(Region.USE_COMPUTED_SIZE, 33.3, Double.MAX_VALUE);columnToolBar1.setHgrow(Priority.ALWAYS);columnToolBar1.setPercentWidth(33.3);
        ColumnConstraints columnToolBar2 = new ColumnConstraints(Region.USE_COMPUTED_SIZE, 33.4, Double.MAX_VALUE);columnToolBar2.setHgrow(Priority.ALWAYS);columnToolBar2.setPercentWidth(33.4);
        ColumnConstraints columnToolBar3 = new ColumnConstraints(Region.USE_COMPUTED_SIZE, 33.3, Double.MAX_VALUE);columnToolBar3.setHgrow(Priority.ALWAYS);columnToolBar3.setPercentWidth(33.3);
        gridPaneToolBar.getColumnConstraints().addAll(columnToolBar1, columnToolBar2, columnToolBar3);
        HBox.setHgrow(gridPaneToolBar, Priority.ALWAYS);
        // 设置第一个网格为标题栏
        HBox toolBox = new HBox();
        toolBox.setSpacing(2);
        toolBox.setPadding(new Insets(3, 0, 2, 5));
        Image imageIcon = icon;
        ImageView imageViewIcon = new ImageView(imageIcon);
        imageViewIcon.setFitHeight(23);
        imageViewIcon.setPreserveRatio(true);
        buildMenu();
        toolBox.getChildren().add(imageViewIcon);
        toolBox.getChildren().add(menuBar);
        gridPaneToolBar.add(toolBox, 0, 0, 1, 1);
        GridPane.setHalignment(toolBox, HPos.LEFT);
        // 设置第二个网格为标题
        Label titleLabel = new Label("QuickRedTools");
        titleLabel.setFont(new Font("Consolas Bold", 20));
        updateTitleLabelStyle(titleLabel);

        gridPaneToolBar.add(titleLabel, 1, 0, 1, 1);
        GridPane.setHalignment(titleLabel, HPos.CENTER);
        GridPane.setValignment(titleLabel, VPos.CENTER);
        // 设置第三个网格为窗口操作按钮
        HBox buttonBox = new HBox();
        // 关闭按钮
        Button buttonClose = Components.getImgButton("img/CloseButton.png");
        buttonClose.setOnAction(e -> {
            primaryStage.close();
            System.exit(0);
        });
        Button buttonMin = Components.getImgButton("img/MinButton.png");
        buttonMin.setOnAction(e -> primaryStage.setIconified(true));
        Button buttonMax = Components.getImgButton("img/MaxButton.png");
        buttonMax.setOnAction(e -> {
            e.consume();
            Components.minimizeToTray(primaryStage);
        });
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(buttonClose, buttonMax, buttonMin);
        gridPaneToolBar.add(buttonBox, 2, 0, 1, 1);GridPane.setHalignment(buttonBox, HPos.RIGHT);GridPane.setValignment(buttonBox, VPos.CENTER);

        /*
          创建一个顶部模拟状态栏
         */
        HBox titleBar = new HBox();
        // 绑定拖拽事件
        titleBar.setOnMousePressed(this::handleMousePressed);
        titleBar.setOnMouseDragged(this::handleMouseDragged);
        menuBar.setOnMousePressed(this::handleMousePressed);
        menuBar.setOnMouseDragged(this::handleMouseDragged);

        // 添加一个网格视图
        titleBar.getChildren().add(gridPaneToolBar);
        titleBar.setAlignment(Pos.CENTER); // 居中布局
        titleBar.setPadding(new Insets(0, 0, 0, 0));
        titleBar.setSpacing(0);   // 设置标题栏内间距
        updateTitleBarStyle(titleBar);

        return titleBar;
    }

    /**
     * 构建顶部菜单
     */
    private void buildMenu(){
        menuBar = new MenuBar();
        updateMenuBarStyle(menuBar);
        menuBar.setPadding(new Insets(0));

        Menu settingMenu = new Menu("设置");
        Menu helpMenu = new Menu("帮助");
        menuBar.getMenus().addAll(settingMenu, helpMenu);

        // ----------设置菜单----------
        MenuItem fontSettingButton = new MenuItem("字体设置");
        darkModeMenuItem = new MenuItem("黑暗模式");

        settingMenu.getItems().addAll(fontSettingButton, darkModeMenuItem);

        fontSettingButton.setOnAction(event -> new SettingsDialog().show());

        // 黑暗模式切换功能
        updateDarkModeMenuItem();
        darkModeMenuItem.setOnAction(event -> toggleDarkMode());

        // ----------帮助菜单----------
        // ----------第一个按钮----------
        MenuItem aboutButton = new MenuItem("关于");
        helpMenu.getItems().addAll(aboutButton);
        aboutButton.setOnAction(event -> {
            new AboutDialog().show();
        });
        // ----------第二个按钮----------
        MenuItem checkUpdateButton = new MenuItem("检查更新");
        helpMenu.getItems().add(checkUpdateButton);
        checkUpdateButton.setOnAction(event -> {
            openWebPage();
        });
    }

    /**
     * 处理鼠标按下事件
     */
    private void handleMousePressed(MouseEvent event) {
        // 获取鼠标相对于窗口的坐标
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    /**
     * 处理鼠标拖动事件
     * @param event 鼠标事件
     */
    private void handleMouseDragged(MouseEvent event) {
        // 计算窗口的新位置（基于鼠标移动的距离）
        double newX = event.getScreenX() - xOffset;
        double newY = event.getScreenY() - yOffset;

        // 移动窗口到新位置
        Stage stage = (Stage) ((event.getSource() instanceof Node) ? ((Node) event.getSource()).getScene().getWindow() : null);
        if (stage != null) {
            stage.setX(newX);
            stage.setY(newY);
        }
    }

    /**
     * 打开网页链接
     */
    private void openWebPage() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("https://github.com/Y5neKO/QuickRedTools/releases"));
            } else {
                System.err.println("不支持打开网页操作");
            }
        } catch (Exception e) {
            System.err.println("无法打开网页: " + e.getMessage());
        }
    }

    /**
     * 更新标题栏样式
     */
    private void updateTitleBarStyle(HBox titleBar) {
        if (GlobalVariable.isDarkMode()) {
            titleBar.setBackground(new Background(new BackgroundFill(Color.web("#2d3748"), null, null)));
        } else {
            titleBar.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
        }
    }

    /**
     * 更新标题标签样式
     */
    private void updateTitleLabelStyle(Label titleLabel) {
        if (GlobalVariable.isDarkMode()) {
            titleLabel.setTextFill(Color.web("#e2e8f0"));
        } else {
            titleLabel.setTextFill(Color.BLACK);
        }
    }

    /**
     * 更新菜单栏样式
     */
    private void updateMenuBarStyle(MenuBar menuBar) {
        if (GlobalVariable.isDarkMode()) {
            menuBar.setStyle("-fx-background-color: transparent;");
        } else {
            menuBar.setStyle("-fx-background-color: transparent;");
        }
    }

    /**
     * 刷新Header样式
     */
    public static void refreshHeaderStyles() {
        // 静态方法用于刷新所有Header实例的样式
        // 由于Header实例在UI类中创建，我们需要通过UI类来刷新
        javafx.application.Platform.runLater(() -> {
            // 这个方法将被UI类调用，来刷新Header组件的样式
        });
    }

    /**
     * 更新标题栏样式（公共方法）
     */
    public void updateTitleBarStylePublic(HBox titleBar) {
        updateTitleBarStyle(titleBar);

        // 递归更新所有子组件中的标签
        updateAllLabels(titleBar);
    }

    /**
     * 递归更新容器中所有标签的样式
     */
    private void updateAllLabels(javafx.scene.Parent container) {
        for (javafx.scene.Node node : container.getChildrenUnmodifiable()) {
            if (node instanceof Label) {
                updateTitleLabelStyle((Label) node);
            } else if (node instanceof javafx.scene.Parent) {
                updateAllLabels((javafx.scene.Parent) node);
            }
        }
    }

    /**
     * 更新标题标签样式（公共方法）
     */
    public void updateTitleLabelStylePublic(Label titleLabel) {
        updateTitleLabelStyle(titleLabel);
    }

    /**
     * 更新菜单栏样式（公共方法）
     */
    public void updateMenuBarStylePublic(MenuBar menuBar) {
        updateMenuBarStyle(menuBar);
        // 同时更新黑暗模式菜单项文本
        updateDarkModeMenuItem();
    }

    /**
     * 更新黑暗模式菜单项文本
     */
    private void updateDarkModeMenuItem() {
        if (darkModeMenuItem != null) {
            boolean isDarkMode = GlobalVariable.isDarkMode();
            darkModeMenuItem.setText(isDarkMode ? "关闭黑暗模式" : "开启黑暗模式");
        }
    }

    /**
     * 切换黑暗模式
     */
    private void toggleDarkMode() {
        boolean currentDarkMode = GlobalVariable.isDarkMode();
        boolean newDarkMode = !currentDarkMode;

        // 更新全局变量
        GlobalVariable.setDarkMode(newDarkMode);

        // 保存设置到配置
        try {
            DataManager dataManager = DataManager.getInstance();
            dataManager.setAppConfig("darkMode", String.valueOf(newDarkMode));
        } catch (Exception e) {
            System.err.println("保存黑暗模式设置失败: " + e.getMessage());
        }

        // 更新菜单项文本
        updateDarkModeMenuItem();

        // 刷新界面 - 使用UI类的公共刷新方法
        javafx.application.Platform.runLater(() -> {
            com.y5neko.qrts.ui.common.Center.refreshAll();

            // 刷新所有UI组件（包括Header和Footer）
            com.y5neko.qrts.UI.refreshAllUIComponents();
        });
    }
}
