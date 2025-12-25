package aislayer.ui;

import aislayer.utils.CommentaryUtils;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;

import java.util.ArrayList;

/**
 * 解说显示系统，管理解说内容的显示和历史记录
 */
public class CommentaryDisplay {
    
    private static CommentaryDisplay instance;
    
    // 解说历史记录
    private final ArrayList<String> commentaryHistory;
    private final int maxHistorySize = 20;
    
    // 显示相关
    private boolean isVisible = true;
    private float displayTimer = 0.0f;
    private final float displayDuration = 4.0f; // 显示时长4秒
    private String currentCommentary = "";
    
    // UI位置和大小
    private float x, y, width, height;
    private final float padding = 20.0f * Settings.scale;
    
    // 字体
    private BitmapFont font;
    
    // 颜色
    private final Color backgroundColor = new Color(0.0f, 0.0f, 0.0f, 0.8f);
    private final Color textColor = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    private final Color historyColor = new Color(0.8f, 0.8f, 0.8f, 0.7f);
    
    private CommentaryDisplay() {
        commentaryHistory = new ArrayList<>();
        initializeUI();
    }
    
    public static CommentaryDisplay getInstance() {
        if (instance == null) {
            instance = new CommentaryDisplay();
        }
        return instance;
    }
    
    /**
     * 初始化UI
     */
    private void initializeUI() {
        // 设置显示位置（屏幕左下角）
        width = 400.0f * Settings.scale;
        height = 60.0f * Settings.scale;
        x = padding;
        y = padding + 100.0f * Settings.scale; // 留出空间给其他UI元素
        
        // 初始化字体
        font = FontHelper.tipBodyFont;
    }
    
    /**
     * 显示解说内容
     * @param commentary 解说内容
     */
    public void showCommentary(String commentary) {
        if (commentary == null || commentary.trim().isEmpty()) {
            return;
        }
        
        currentCommentary = commentary;
        displayTimer = displayDuration;
        
        // 添加到历史记录
        addToHistory(commentary);
    }
    
    /**
     * 添加到历史记录
     * @param commentary 解说内容
     */
    private void addToHistory(String commentary) {
        commentaryHistory.add(0, commentary); // 添加到开头
        
        // 限制历史记录大小
        while (commentaryHistory.size() > maxHistorySize) {
            commentaryHistory.remove(commentaryHistory.size() - 1);
        }
    }
    
    /**
     * 更新显示
     */
    public void update() {
        if (displayTimer > 0) {
            displayTimer -= Gdx.graphics.getDeltaTime();
            if (displayTimer <= 0) {
                currentCommentary = "";
            }
        }
        
        // 快捷键已移除，按用户要求
    }
    
    /**
     * 渲染显示
     * @param sb SpriteBatch
     */
    public void render(SpriteBatch sb) {
        if (!isVisible) {
            return;
        }
        
        // 绘制当前解说
        if (!currentCommentary.isEmpty() && displayTimer > 0) {
            renderCurrentCommentary(sb);
        }
        
        // 绘制历史记录（根据配置决定）
        if (aislayer.panels.ConfigPanel.showCommentaryHistory) {
            renderHistory(sb);
        }
    }
    
    /**
     * 渲染当前解说
     * @param sb SpriteBatch
     */
    private void renderCurrentCommentary(SpriteBatch sb) {
        // 计算透明度
        float alpha = Math.min(1.0f, displayTimer);
        
        // 绘制背景
        Color bgColor = new Color(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a * alpha);
        sb.setColor(bgColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, width, height);
        
        // 绘制边框
        sb.setColor(Color.GOLD.r, Color.GOLD.g, Color.GOLD.b, alpha);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, width, 2.0f * Settings.scale);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y + height - 2.0f * Settings.scale, width, 2.0f * Settings.scale);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, y, 2.0f * Settings.scale, height);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x + width - 2.0f * Settings.scale, y, 2.0f * Settings.scale, height);
        
        // 绘制文本
        Color txtColor = new Color(textColor.r, textColor.g, textColor.b, textColor.a * alpha);
        FontHelper.renderFontLeftTopAligned(sb, font, currentCommentary, 
                x + padding, y + height - padding, txtColor);
    }
    
    /**
     * 渲染历史记录
     * @param sb SpriteBatch
     */
    private void renderHistory(SpriteBatch sb) {
        if (commentaryHistory.isEmpty()) {
            return;
        }
        
        float historyY = y + height + padding;
        float historyHeight = 25.0f * Settings.scale;
        float historyWidth = width + 100.0f * Settings.scale; // 稍微宽一点
        float maxDisplay = Math.min(8, commentaryHistory.size()); // 最多显示8条
        
        // 绘制历史记录背景
        Color historyBgColor = new Color(0.1f, 0.1f, 0.1f, 0.6f);
        sb.setColor(historyBgColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, historyY, historyWidth, maxDisplay * historyHeight);
        
        // 绘制历史记录边框
        sb.setColor(Color.LIGHT_GRAY.r, Color.LIGHT_GRAY.g, Color.LIGHT_GRAY.b, 0.8f);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, historyY, historyWidth, 1.0f * Settings.scale);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, historyY + maxDisplay * historyHeight - 1.0f * Settings.scale, historyWidth, 1.0f * Settings.scale);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x, historyY, 1.0f * Settings.scale, maxDisplay * historyHeight);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, x + historyWidth - 1.0f * Settings.scale, historyY, 1.0f * Settings.scale, maxDisplay * historyHeight);
        
        // 绘制标题
        FontHelper.renderFontLeftTopAligned(sb, font, "解说历史记录:",
                x + padding, historyY + maxDisplay * historyHeight - padding, Color.GOLD);
        
        // 绘制历史记录文本
        for (int i = 0; i < maxDisplay; i++) {
            String historyText = commentaryHistory.get(i);
            // 限制文本长度避免溢出
            if (historyText.length() > 50) {
                historyText = historyText.substring(0, 47) + "...";
            }
            
            float textY = historyY + (maxDisplay - i - 1) * historyHeight + padding;
            FontHelper.renderFontLeftTopAligned(sb, font, (i + 1) + ". " + historyText,
                    x + padding, textY, historyColor);
        }
    }
    
    /**
     * 清空历史记录
     */
    public void clearHistory() {
        commentaryHistory.clear();
    }
    
    /**
     * 获取历史记录
     * @return 历史记录列表
     */
    public ArrayList<String> getHistory() {
        return new ArrayList<>(commentaryHistory);
    }
    
    /**
     * 切换显示状态
     */
    public void toggleVisibility() {
        isVisible = !isVisible;
    }
    
    /**
     * 设置可见性
     * @param visible 是否可见
     */
    public void setVisible(boolean visible) {
        isVisible = visible;
    }
    
    /**
     * 获取可见性
     * @return 是否可见
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * 获取当前解说
     * @return 当前解说内容
     */
    public String getCurrentCommentary() {
        return currentCommentary;
    }
    
    /**
     * 强制隐藏当前解说
     */
    public void hideCurrentCommentary() {
        currentCommentary = "";
        displayTimer = 0;
    }
    
    /**
     * 获取解说统计信息
     * @return 统计信息字符串
     */
    public String getStats() {
        return String.format("解说显示: %s, 历史记录: %d, 当前: %s", 
                isVisible ? "可见" : "隐藏", 
                commentaryHistory.size(), 
                currentCommentary.isEmpty() ? "无" : "有");
    }
    
    /**
     * 重置显示系统
     */
    public void reset() {
        clearHistory();
        currentCommentary = "";
        displayTimer = 0;
        isVisible = true;
    }
}