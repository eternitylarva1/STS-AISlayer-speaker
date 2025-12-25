package aislayer.subscribes;

import aislayer.ui.CommentaryDisplay;
import aislayer.utils.CommentaryUtils;
import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 解说系统集成类，负责初始化和管理解说系统
 */
@SpireInitializer
public class CommentarySubscribe {
    
    public static final Logger logger = LogManager.getLogger(CommentarySubscribe.class.getName());
    
    private static CommentaryDisplay commentaryDisplay;
    private static boolean initialized = false;
    
    public CommentarySubscribe() {
        initialize();
    }
    
    public static void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            // 初始化解说显示系统
            commentaryDisplay = CommentaryDisplay.getInstance();
            
            logger.info("AI解说系统已初始化");
            initialized = true;
            
        } catch (Exception e) {
            logger.error("解说系统初始化失败", e);
        }
    }
    
    /**
     * 更新解说系统（需要在游戏主循环中调用）
     */
    public static void update() {
        if (!initialized || commentaryDisplay == null) {
            return;
        }
        
        try {
            // 更新解说显示
            commentaryDisplay.update();
            
            // 检查快捷键
            checkHotkeys();
            
        } catch (Exception e) {
            logger.error("解说系统更新失败", e);
        }
    }
    
    /**
     * 渲染解说系统（需要在游戏渲染循环中调用）
     * @param sb SpriteBatch
     */
    public static void render(com.badlogic.gdx.graphics.g2d.SpriteBatch sb) {
        if (!initialized || commentaryDisplay == null) {
            return;
        }
        
        try {
            commentaryDisplay.render(sb);
        } catch (Exception e) {
            logger.error("解说系统渲染失败", e);
        }
    }
    
    /**
     * 检查快捷键（已移除所有快捷键）
     */
    private static void checkHotkeys() {
        // 所有快捷键已移除，按用户要求
    }
    
    /**
     * 在战斗开始时重置解说系统
     */
    public static void onBattleStart() {
        if (!initialized) {
            return;
        }
        
        try {
            CommentaryUtils.clearCommentaryQueue();
            commentaryDisplay.hideCurrentCommentary();
            
            // 重置战斗状态跟踪器
            aislayer.utils.BattleStateTracker.getInstance().endBattle();
            
            // 根据配置决定是否清空历史记录
            if (!aislayer.panels.ConfigPanel.showCommentaryHistory) {
                commentaryDisplay.clearHistory();
            }
            
            logger.info("战斗开始，解说系统已重置");
        } catch (Exception e) {
            logger.error("战斗开始时重置解说系统失败", e);
        }
    }
    
    /**
     * 在战斗结束时清理解说系统
     */
    public static void onBattleEnd() {
        if (!initialized) {
            return;
        }
        
        try {
            CommentaryUtils.clearCommentaryQueue();
            commentaryDisplay.hideCurrentCommentary();
            logger.info("战斗结束，解说系统已清理");
        } catch (Exception e) {
            logger.error("战斗结束时清理解说系统失败", e);
        }
    }
    
    /**
     * 获取解说显示实例
     * @return CommentaryDisplay实例
     */
    public static CommentaryDisplay getCommentaryDisplay() {
        return commentaryDisplay;
    }
    
    /**
     * 检查解说系统是否已初始化
     * @return 是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 获取解说系统状态信息
     * @return 状态信息
     */
    public static String getStatus() {
        if (!initialized) {
            return "解说系统未初始化";
        }
        
        StringBuilder status = new StringBuilder();
        status.append("解说系统状态: 已初始化\n");
        status.append("解说功能: ").append(CommentaryUtils.isCommentarySystemAvailable() ? "可用" : "不可用").append("\n");
        status.append(CommentaryUtils.getCommentaryStats()).append("\n");
        status.append(commentaryDisplay.getStats());
        
        return status.toString();
    }
}