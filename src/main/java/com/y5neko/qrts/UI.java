package com.y5neko.qrts;

import com.y5neko.qrts.config.GlobalVariable;
import com.y5neko.qrts.ui.common.Center;
import com.y5neko.qrts.ui.common.Footer;
import com.y5neko.qrts.ui.common.Header;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import static com.y5neko.qrts.config.GlobalVariable.icon;

public class UI extends Application {
    // 设置一个BorderPane作为根视图
    BorderPane root = new BorderPane();

    private double dragStartX, dragStartY;
    private double initWidth, initHeight;

    // 保存组件实例用于黑暗模式切换
    private static Header headerInstance;
    private static Footer footerInstance;
    private static Scene currentScene;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 初始化字体设置
        GlobalVariable.initializeFontSettings();

        // 初始化黑暗模式设置
        initializeDarkMode();

        // =============================================================Step 1: 创建一个菜单栏=============================================================
        headerInstance = new Header();
        HBox titleBar = headerInstance.getTitleBar(primaryStage);
        root.setTop(titleBar);


        // =============================================================Step 2: 创建一个中间容器=============================================================
        // 设置一个VBox作为中间主要展示内容
        Center centerComponent = new Center();
        VBox centerBox = centerComponent.getCenterBox();
        Center.setMainCenterBox(centerBox); // 设置主界面引用
        root.setCenter(centerBox);


        // =============================================================Step 3: 创建一个底部栏=============================================================
        footerInstance = new Footer();
        HBox bottomBar = footerInstance.getBottomBar();
        root.setBottom(bottomBar);


        // =============================================================Step 4: 外观设计=============================================================
        // 圆角设计
        // 动态绑定 clip 的宽高到 root 的宽高（自动适应窗口大小变化）
        Rectangle clipRectangle = new Rectangle();
        clipRectangle.setArcWidth(20);
        clipRectangle.setArcHeight(20);

        // 创建一个 StackPane 来做边框容器，达成边框效果（root现在不是根容器了）
        StackPane borderedPane = new StackPane();
        borderedPane.setPadding(new Insets(1)); // 设置边框宽度（内边距）
        updateBorderedPaneStyle(borderedPane);
        borderedPane.getChildren().add(root);

        // 绑定宽度和高度到 root 的尺寸
        clipRectangle.widthProperty().bind(root.widthProperty());
        clipRectangle.heightProperty().bind(root.heightProperty());
        root.setClip(clipRectangle);


        // =============================================================Step 5: 处理Scene和Stage=============================================================
        // 开始处理Stage和放入Scene（root现在不是根容器了）
        currentScene = new Scene(borderedPane);
        currentScene.setFill(Color.TRANSPARENT);   // 圆角|透明
        currentScene.getStylesheets().add("css/Style.css");
        currentScene.getStylesheets().add("css/Tabs.css"); // 加载CSS

        // 应用黑暗模式
        applyDarkMode(currentScene);
        // 设置stage为无状态栏型
        primaryStage.initStyle(StageStyle.TRANSPARENT); // 圆角|透明
        primaryStage.setScene(currentScene);
        primaryStage.setTitle("template");

        // 设置最小窗口大小
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);

        // 设置初始窗口大小
        primaryStage.setWidth(1300);
        primaryStage.setHeight(800);
        primaryStage.getIcons().add(icon);
        primaryStage.show();
//        ScenicView.show(currentScene);


        // ============================================================Step 6: 处理一些绑定事件==========================================================
        currentScene.setOnMousePressed(event -> {
            dragStartX = event.getScreenX();
            dragStartY = event.getScreenY();
            initWidth = primaryStage.getWidth();
            initHeight = primaryStage.getHeight();
        });

        currentScene.setOnMouseDragged(event -> {
            double deltaX = event.getScreenX() - dragStartX;
            double deltaY = event.getScreenY() - dragStartY;

            if (event.getSceneX() > (currentScene.getWidth() - 20) &&
                    event.getSceneY() > (currentScene.getHeight() - 20)) {
                primaryStage.setWidth(Math.max(600, initWidth + deltaX));
                primaryStage.setHeight(Math.max(400, initHeight + deltaY));
            } else if (event.getSceneX() > (currentScene.getWidth() - 5)) {
                primaryStage.setWidth(Math.max(600, initWidth + deltaX));
            } else if (event.getSceneY() > (currentScene.getHeight() - 5)) {
                primaryStage.setHeight(Math.max(400, initHeight + deltaY));
            }
        });

        currentScene.setOnMouseMoved(event -> {
            if (event.getSceneX() > (currentScene.getWidth() - 20) &&
                    event.getSceneY() > (currentScene.getHeight() - 20)) {
                currentScene.setCursor(Cursor.SE_RESIZE);
            } else {
                currentScene.setCursor(Cursor.DEFAULT);
            }
        });
    }

    /**
     * 初始化黑暗模式设置
     */
    private void initializeDarkMode() {
        // 这里可以添加额外的黑暗模式初始化逻辑
        // 比如读取配置文件中的黑暗模式设置
    }

    /**
     * 应用黑暗模式
     */
    private void applyDarkMode(Scene scene) {
        if (GlobalVariable.isDarkMode()) {
            scene.getRoot().getStyleClass().add("dark");
            // 加载黑暗模式CSS
            scene.getStylesheets().add("css/DarkMode.css");
        } else {
            scene.getRoot().getStyleClass().remove("dark");
            // 移除黑暗模式CSS
            scene.getStylesheets().remove("css/DarkMode.css");
        }
    }

    /**
     * 切换黑暗模式
     */
    public static void toggleDarkMode(Scene scene) {
        boolean isDark = GlobalVariable.isDarkMode();
        GlobalVariable.setDarkMode(!isDark);

        if (!isDark) {
            scene.getRoot().getStyleClass().add("dark");
            if (!scene.getStylesheets().contains("css/DarkMode.css")) {
                scene.getStylesheets().add("css/DarkMode.css");
            }
        } else {
            scene.getRoot().getStyleClass().remove("dark");
            scene.getStylesheets().remove("css/DarkMode.css");
        }

        // 更新边框容器样式
        updateBorderedPaneStyle(scene);

        // 刷新Header和Footer组件
        refreshAllComponents();
    }

    /**
     * 刷新当前黑暗模式样式（不切换模式，只刷新样式）
     */
    public static void refreshDarkModeStyles(Scene scene) {
        boolean isDark = GlobalVariable.isDarkMode();

        // 确保CSS类和样式表状态正确
        if (isDark) {
            if (!scene.getRoot().getStyleClass().contains("dark")) {
                scene.getRoot().getStyleClass().add("dark");
            }
            if (!scene.getStylesheets().contains("css/DarkMode.css")) {
                scene.getStylesheets().add("css/DarkMode.css");
            }
        } else {
            scene.getRoot().getStyleClass().remove("dark");
            scene.getStylesheets().remove("css/DarkMode.css");
        }

        // 更新边框容器样式
        updateBorderedPaneStyle(scene);

        // 刷新Header和Footer组件
        refreshAllComponents();
    }

    /**
     * 公共刷新方法，供外部调用
     */
    public static void refreshAllUIComponents() {
        if (currentScene != null) {
            refreshDarkModeStyles(currentScene);
        }
    }

    /**
     * 刷新所有组件样式
     */
    private static void refreshAllComponents() {
        javafx.application.Platform.runLater(() -> {
            try {
                System.out.println("开始刷新UI组件，当前黑暗模式: " + GlobalVariable.isDarkMode());

                // 刷新Header组件
                if (headerInstance != null && currentScene != null) {
                    System.out.println("刷新Header组件...");
                    // 获取根组件结构：StackPane -> BorderPane -> HBox (Header)
                    Object root = currentScene.getRoot();
                    if (root instanceof StackPane) {
                        StackPane stackPane = (StackPane) root;
                        if (!stackPane.getChildren().isEmpty()) {
                            Object borderPaneObj = stackPane.getChildren().get(0);
                            if (borderPaneObj instanceof BorderPane) {
                                BorderPane borderPane = (BorderPane) borderPaneObj;
                                Object topObj = borderPane.getTop();
                                if (topObj instanceof HBox) {
                                    HBox titleBar = (HBox) topObj;
                                    headerInstance.updateTitleBarStylePublic(titleBar);
                                    System.out.println("Header组件刷新完成");
                                } else {
                                    System.out.println("未找到Header组件");
                                }
                            } else {
                                System.out.println("未找到BorderPane组件");
                            }
                        } else {
                            System.out.println("StackPane没有子组件");
                        }
                    } else {
                        System.out.println("根组件不是StackPane");
                    }
                } else {
                    System.out.println("Header实例或Scene为空");
                }

                // 刷新Footer组件
                if (footerInstance != null && currentScene != null) {
                    System.out.println("刷新Footer组件...");
                    Object root = currentScene.getRoot();
                    if (root instanceof StackPane) {
                        StackPane stackPane = (StackPane) root;
                        if (!stackPane.getChildren().isEmpty()) {
                            Object borderPaneObj = stackPane.getChildren().get(0);
                            if (borderPaneObj instanceof BorderPane) {
                                BorderPane borderPane = (BorderPane) borderPaneObj;
                                Object bottomObj = borderPane.getBottom();
                                if (bottomObj instanceof HBox) {
                                    HBox bottomBar = (HBox) bottomObj;
                                    if (!bottomBar.getChildren().isEmpty()) {
                                        Label bottomLabel = (Label) bottomBar.getChildren().get(0);
                                        footerInstance.updateFooterStyles(bottomBar, bottomLabel);
                                        System.out.println("Footer组件刷新完成");
                                    } else {
                                        System.out.println("Footer组件没有子组件");
                                    }
                                } else {
                                    System.out.println("未找到Footer组件");
                                }
                            } else {
                                System.out.println("Footer - 未找到BorderPane组件");
                            }
                        } else {
                            System.out.println("Footer - StackPane没有子组件");
                        }
                    } else {
                        System.out.println("Footer - 根组件不是StackPane");
                    }
                } else {
                    System.out.println("Footer实例或Scene为空");
                }

                // 刷新Center组件
                System.out.println("刷新Center组件...");
                Center.refreshAll();

                // 刷新按钮和分隔符样式
                System.out.println("刷新按钮和分隔符样式...");
                Center.refreshButtonAndSeparatorStyles();

                // 再次确保边框容器样式正确
                System.out.println("再次更新边框容器样式...");
                updateBorderedPaneStyle(currentScene);

                System.out.println("所有UI组件刷新完成");

            } catch (Exception e) {
                System.err.println("刷新组件时发生错误: " + e.getMessage());
            }
        });
    }

    /**
     * 更新边框容器样式
     */
    private static void updateBorderedPaneStyle(StackPane borderedPane) {
        String newStyle;
        if (GlobalVariable.isDarkMode()) {
            newStyle = "-fx-background-color: #2d3748; -fx-background-radius: 11;";
        } else {
            newStyle = "-fx-background-color: #f0f0f0; -fx-background-radius: 11;";
        }
        borderedPane.setStyle(newStyle);
        System.out.println("边框容器样式已更新: " + newStyle);
    }

    /**
     * 更新边框容器样式（重载方法，用于Scene）
     */
    private static void updateBorderedPaneStyle(Scene scene) {
        if (scene != null && scene.getRoot() instanceof StackPane) {
            StackPane borderedPane = (StackPane) scene.getRoot();
            updateBorderedPaneStyle(borderedPane);
        }
    }
}
