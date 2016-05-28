package CommandRunner.gui;

public interface LayoutChangedListener {
    void verticalDividerPositionChanged(double to);
    void horizontalDividerPositionChanged(double to);
    void tableCommandColumnWidthChanged(int to);
    void tableDirectoryColumnWidthChanged(int to);
    void tableCommentColumnWidthChanged(int to);
    void themeChanged(String theme);
    void showStatusBarChanged(boolean showStatusBar);
}
