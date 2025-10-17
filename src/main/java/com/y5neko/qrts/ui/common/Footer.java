package com.y5neko.qrts.ui.common;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class Footer {
    public HBox getBottomBar(){
        HBox bottomBox = new HBox();
        bottomBox.setPadding(new Insets(2, 5, 2, 5));
        bottomBox.setStyle("-fx-background-color: #99ccff;");
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        Label bottomLabel = new Label("Powered by Y5neKO");
        bottomLabel.setFont(Font.font(bottomLabel.getFont().getFamily(), FontWeight.BOLD, bottomLabel.getFont().getSize()));
        bottomBox.getChildren().add(bottomLabel);
        return bottomBox;
    }
}
