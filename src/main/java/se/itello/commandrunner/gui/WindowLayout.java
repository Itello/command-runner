package se.itello.commandrunner.gui;

import org.json.JSONException;
import org.json.JSONObject;

public class WindowLayout {
    public static final String DARK_THEME = "DARK_THEME";
    public static final String LIGHT_THEME = "LIGHT_THEME";
    public static final WindowLayout DEFAULT_LAYOUT =
            new WindowLayout()
                    .setWindowWidth(1040)
                    .setWindowHeight(600)
                    .setVerticalDividerPosition(0.75f)
                    .setHorizontalDividerPosition(0.83f)
                    .setTableCommandColumnWidth(490)
                    .setTableDirectoryColumnWidth(250)
                    .setTableCommentColumnWidth(100)
                    .setMaximized(false)
                    .setTheme(LIGHT_THEME)
                    .setShowStatusBar(true);

    private static final String WINDOW_WIDTH = "WINDOW_WIDTH";
    private static final String WINDOW_HEIGHT = "WINDOW_HEIGHT";
    private static final String IS_MAXIMIZED = "IS_MAXIMIZED";
    private static final String VERTICAL_DIVIDER_POSITION = "VERTICAL_DIVIDER_POSITION";
    private static final String HORIZONTAL_DIVIDER_POSITION = "HORIZONTAL_DIVIDER_POSITION";
    private static final String TABLE_COMMAND_COLUMN_WIDTH = "TABLE_COMMAND_COLUMN_WIDTH";
    private static final String TABLE_COMMENT_COLUMN_WIDTH = "TABLE_COMMENT_COLUMN_WIDTH";
    private static final String TABLE_DIRECTORY_COLUMN_WIDTH = "TABLE_DIRECTORY_COLUMN_WIDTH";
    private static final String THEME = "THEME";
    private static final String SHOW_STATUS_BAR = "SHOW_STATUS_BAR";

    private int windowWidth;
    private int windowHeight;
    private double verticalDividerPosition;
    private double horizontalDividerPosition;
    private int tableCommandColumnWidth;
    private int tableDirectoryColumnWidth;
    private int tableCommentColumnWidth;
    private boolean isMaximized;
    private String theme;
    private boolean showStatusBar;

    private WindowLayout() {
    }

    public WindowLayout(JSONObject jsonObject) throws JSONException {
        setWindowWidth(jsonObject.getInt(WINDOW_WIDTH))
                .setWindowHeight(jsonObject.getInt(WINDOW_HEIGHT))
                .setVerticalDividerPosition((float) jsonObject.getDouble(VERTICAL_DIVIDER_POSITION))
                .setHorizontalDividerPosition((float) jsonObject.getDouble(HORIZONTAL_DIVIDER_POSITION))
                .setTableCommandColumnWidth(jsonObject.getInt(TABLE_COMMAND_COLUMN_WIDTH))
                .setTableDirectoryColumnWidth(jsonObject.getInt(TABLE_DIRECTORY_COLUMN_WIDTH))
                .setTableCommentColumnWidth(jsonObject.getInt(TABLE_COMMENT_COLUMN_WIDTH))
                .setMaximized(jsonObject.getBoolean(IS_MAXIMIZED))
                .setTheme(jsonObject.getString(THEME))
                .setShowStatusBar(jsonObject.getBoolean(SHOW_STATUS_BAR));
    }

    public WindowLayout setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
        return this;
    }

    public WindowLayout setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
        return this;
    }

    public WindowLayout setVerticalDividerPosition(double verticalDividerPosition) {
        this.verticalDividerPosition = verticalDividerPosition;
        return this;
    }

    public WindowLayout setHorizontalDividerPosition(double horizontalDividerPosition) {
        this.horizontalDividerPosition = horizontalDividerPosition;
        return this;
    }

    public WindowLayout setTableCommandColumnWidth(int tableCommandColumnWidth) {
        this.tableCommandColumnWidth = tableCommandColumnWidth;
        return this;
    }

    public WindowLayout setTableDirectoryColumnWidth(int tableDirectoryColumnWidth) {
        this.tableDirectoryColumnWidth = tableDirectoryColumnWidth;
        return this;
    }

    public WindowLayout setTableCommentColumnWidth(int tableCommentColumnWidth) {
        this.tableCommentColumnWidth = tableCommentColumnWidth;
        return this;
    }

    public WindowLayout setMaximized(boolean maximized) {
        isMaximized = maximized;
        return this;
    }

    public WindowLayout setTheme(String theme) {
        this.theme = theme;
        return this;
    }

    public WindowLayout setShowStatusBar(boolean showStatusBar) {
        this.showStatusBar = showStatusBar;
        return this;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    public double getVerticalDividerPosition() {
        return verticalDividerPosition;
    }

    public double getHorizontalDividerPosition() {
        return horizontalDividerPosition;
    }

    public int getTableCommandColumnWidth() {
        return tableCommandColumnWidth;
    }

    public int getTableDirectoryColumnWidth() {
        return tableDirectoryColumnWidth;
    }

    public int getTableCommentColumnWidth() {
        return tableCommentColumnWidth;
    }

    public boolean isMaximized() {
        return isMaximized;
    }

    public String getTheme() {
        return theme;
    }

    public boolean isShowStatusBar() {
        return showStatusBar;
    }

    public JSONObject jsonObject() throws JSONException {
        return new JSONObject()
                .put(WINDOW_WIDTH, windowWidth)
                .put(WINDOW_HEIGHT, windowHeight)
                .put(VERTICAL_DIVIDER_POSITION, verticalDividerPosition)
                .put(HORIZONTAL_DIVIDER_POSITION, horizontalDividerPosition)
                .put(TABLE_COMMAND_COLUMN_WIDTH, tableCommandColumnWidth)
                .put(TABLE_DIRECTORY_COLUMN_WIDTH, tableDirectoryColumnWidth)
                .put(TABLE_COMMENT_COLUMN_WIDTH, tableCommentColumnWidth)
                .put(IS_MAXIMIZED, isMaximized)
                .put(THEME, theme)
                .put(SHOW_STATUS_BAR, showStatusBar);
    }
}
