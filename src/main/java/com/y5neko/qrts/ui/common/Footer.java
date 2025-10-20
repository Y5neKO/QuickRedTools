package com.y5neko.qrts.ui.common;

import com.y5neko.qrts.config.GlobalVariable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class Footer {
    public HBox getBottomBar(){
        HBox bottomBox = new HBox();
        bottomBox.setPadding(new Insets(2, 5, 2, 5));
        updateFooterStyle(bottomBox);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);

        Label bottomLabel = new Label("Powered by Y5neKO");
        bottomLabel.setFont(Font.font(bottomLabel.getFont().getFamily(), FontWeight.BOLD, bottomLabel.getFont().getSize()));
        updateLabelStyle(bottomLabel);
        bottomBox.getChildren().add(bottomLabel);
        return bottomBox;
    }

    /**
     * 更新底部栏样式
     */
    private void updateFooterStyle(HBox bottomBox) {
        if (GlobalVariable.isDarkMode()) {
            bottomBox.setStyle("-fx-background-color: #2d3748;");
        } else {
            bottomBox.setStyle("-fx-background-color: #99ccff;");
        }
    }

    /**
     * 更新标签样式
     */
    private void updateLabelStyle(Label label) {
        if (GlobalVariable.isDarkMode()) {
            label.setTextFill(Color.web("#e2e8f0"));
        } else {
            label.setTextFill(Color.BLACK);
        }
    }

    /**
     * 刷新Footer样式
     */
    public static void refreshFooterStyles() {
        // 静态方法用于刷新所有Footer实例的样式
        // 由于Footer实例在UI类中创建，我们需要通过UI类来刷新
        javafx.application.Platform.runLater(() -> {
            // 这个方法将被UI类调用，来刷新Footer组件的样式
        });
    }

    /**
     * 更新Footer样式（实例方法）
     */
    public void updateFooterStyles(HBox bottomBox, Label bottomLabel) {
        updateFooterStyle(bottomBox);
        updateLabelStyle(bottomLabel);
    }
}
