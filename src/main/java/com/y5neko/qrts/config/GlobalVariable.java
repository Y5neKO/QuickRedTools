package com.y5neko.qrts.config;

import com.y5neko.qrts.service.DataManager;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class GlobalVariable {
    public static Image icon = new Image("img/icon.png");

    // 全局字体设置
    public static String SELECTED_FONT = "Microsoft YaHei";
    public static double TITLE_FONT_SIZE = 16;
    public static double CATEGORY_FONT_SIZE = 14;
    public static double CATEGORY_DESC_FONT_SIZE = 12;
    public static double TOOL_DESC_FONT_SIZE = 10;
    public static double BUTTON_FONT_SIZE = 12; // 独立的按钮字体大小

    /**
     * 初始化字体设置（从配置文件加载）
     */
    public static void initializeFontSettings() {
        try {
            DataManager dataManager = DataManager.getInstance();

            String savedFont = dataManager.getAppConfig("selectedFont");
            if (savedFont != null) {
                SELECTED_FONT = savedFont;
            }

            String savedTitleSize = dataManager.getAppConfig("titleFontSize");
            if (savedTitleSize != null) {
                TITLE_FONT_SIZE = Double.parseDouble(savedTitleSize);
            }

            String savedCategorySize = dataManager.getAppConfig("categoryFontSize");
            if (savedCategorySize != null) {
                CATEGORY_FONT_SIZE = Double.parseDouble(savedCategorySize);
            }

            String savedCategoryDescSize = dataManager.getAppConfig("categoryDescFontSize");
            if (savedCategoryDescSize != null) {
                CATEGORY_DESC_FONT_SIZE = Double.parseDouble(savedCategoryDescSize);
            }

            String savedToolDescSize = dataManager.getAppConfig("toolDescFontSize");
            if (savedToolDescSize != null) {
                TOOL_DESC_FONT_SIZE = Double.parseDouble(savedToolDescSize);
            }

            String savedButtonSize = dataManager.getAppConfig("buttonFontSize");
            if (savedButtonSize != null) {
                BUTTON_FONT_SIZE = Double.parseDouble(savedButtonSize);
            }
        } catch (Exception e) {
            System.err.println("加载字体设置失败，使用默认值: " + e.getMessage());
        }
    }

    /**
     * 更新全局字体设置
     */
    public static void updateFontSettings(String font, double titleSize, double categorySize,
                                        double categoryDescSize, double toolDescSize, double buttonSize) {
        SELECTED_FONT = font;
        TITLE_FONT_SIZE = titleSize;
        CATEGORY_FONT_SIZE = categorySize;
        CATEGORY_DESC_FONT_SIZE = categoryDescSize;
        TOOL_DESC_FONT_SIZE = toolDescSize;
        BUTTON_FONT_SIZE = buttonSize;
    }

    /**
     * 获取标题字体
     */
    public static Font getTitleFont() {
        return getSafeBoldFont(SELECTED_FONT, TITLE_FONT_SIZE);
    }

    /**
     * 获取分类标题字体
     */
    public static Font getCategoryFont() {
        return getSafeBoldFont(SELECTED_FONT, CATEGORY_FONT_SIZE);
    }

    /**
     * 获取分类描述字体
     */
    public static Font getCategoryDescFont() {
        return getSafeFont(SELECTED_FONT, CATEGORY_DESC_FONT_SIZE);
    }

    /**
     * 获取工具描述字体
     */
    public static Font getToolDescFont() {
        return getSafeFont(SELECTED_FONT, TOOL_DESC_FONT_SIZE);
    }

    /**
     * 获取按钮字体
     */
    public static Font getButtonFont() {
        return getSafeFont(SELECTED_FONT, BUTTON_FONT_SIZE);
    }

    /**
     * 安全获取字体，如果字体不存在则回退到系统字体
     */
    public static Font getSafeFont(String fontFamily, double fontSize) {
        try {
            Font testFont = Font.font(fontFamily, fontSize);
            // 如果字体加载失败，返回系统默认字体
            if (testFont.getFamily().equals("System")) {
                return Font.font("System", fontSize);
            }
            return testFont;
        } catch (Exception e) {
            return Font.font("System", fontSize);
        }
    }

    /**
     * 安全获取粗体字体
     */
    public static Font getSafeBoldFont(String fontFamily, double fontSize) {
        try {
            Font testFont = Font.font(fontFamily, FontWeight.BOLD, fontSize);
            // 如果字体加载失败，返回系统默认字体
            if (testFont.getFamily().equals("System")) {
                return Font.font("System", FontWeight.BOLD, fontSize);
            }
            return testFont;
        } catch (Exception e) {
            return Font.font("System", FontWeight.BOLD, fontSize);
        }
    }
}
