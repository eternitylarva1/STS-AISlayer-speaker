package aislayer.utils;

import aislayer.AISlayer;
import aislayer.panels.ConfigPanel;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.SpeechBubble;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 解说工具类，专门处理AI解说功能
 */
public class CommentaryUtils {
    
    public static final Logger logger = LogManager.getLogger(CommentaryUtils.class.getName());
    
    // 解说冷却时间（毫秒）
    private static final long COMMENTARY_COOLDOWN = 3000; // 3秒冷却
    
    // 上次解说时间
    private static long lastCommentaryTime = 0;
    
    // 解说队列
    private static final ArrayList<String> commentaryQueue = new ArrayList<>();
    
    // 解说缓存
    private static final Map<String, String> commentaryCache = new HashMap<>();
    
    // 解说开关
    public static boolean commentaryEnabled = true;
    
    // 解说频率控制
    public static int commentaryFrequency = 1; // 1=每次都解说，2=每2次解说一次，以此类推
    
    // 当前行动计数
    private static int actionCounter = 0;
    
    /**
     * 触发解说
     * @param actionType 行动类型
     * @param params 行动参数
     */
    public static void triggerCommentary(String actionType, Object... params) {
        if (!shouldTriggerCommentary()) {
            return;
        }
        
        try {
            JSONObject actionInfo = AISlayer.getActionInfo(actionType, params);
            String cacheKey = generateCacheKey(actionType, params);
            
            // 检查缓存
            if (commentaryCache.containsKey(cacheKey)) {
                showCommentary(commentaryCache.get(cacheKey));
                return;
            }
            
            // 调用AI获取解说
            AIUtils.getCommentary(actionInfo);
            
        } catch (Exception e) {
            logger.error("触发解说失败", e);
            showFallbackCommentary(actionType);
        }
    }
    
    /**
     * 检查是否应该触发解说
     * @return 是否应该触发解说
     */
    private static boolean shouldTriggerCommentary() {
        // 检查解说开关
        if (!commentaryEnabled) {
            return false;
        }
        
        // 检查API配置
        if (!AISlayer.isAIStart()) {
            return false;
        }
        
        // 检查冷却时间
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCommentaryTime < COMMENTARY_COOLDOWN) {
            return false;
        }
        
        // 检查频率控制
        actionCounter++;
        if (actionCounter % commentaryFrequency != 0) {
            return false;
        }
        
        // 检查游戏状态
        if (AbstractDungeon.getCurrRoom() == null) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 显示解说内容
     * @param commentary 解说内容
     */
    public static void showCommentary(String commentary) {
        if (commentary == null || commentary.trim().isEmpty()) {
            return;
        }
        
        // 格式化解说内容
        String formattedCommentary = AISlayer.formatCommentary(commentary);
        
        // 添加到队列
        commentaryQueue.add(formattedCommentary);
        
        // 如果队列只有一条，立即显示
        if (commentaryQueue.size() == 1) {
            displayNextCommentary();
        }
        
        // 更新冷却时间
        lastCommentaryTime = System.currentTimeMillis();
    }
    
    /**
     * 显示下一条解说
     */
    private static void displayNextCommentary() {
        if (commentaryQueue.isEmpty()) {
            return;
        }
        
        String commentary = commentaryQueue.get(0);
        
        // 直接使用SpeechBubble Effect（不使用Action）
        if (AbstractDungeon.player != null) {
            AbstractDungeon.effectList.add(new SpeechBubble(
                AbstractDungeon.player.dialogX,
                AbstractDungeon.player.dialogY,
                3.0f, // 显示时长3秒
                commentary,
                true // 是玩家
            ));
        }
        
        // 延迟移除已显示的解说
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(3500); // 等待3.5秒（比Effect稍长一点）
                if (!commentaryQueue.isEmpty()) {
                    commentaryQueue.remove(0);
                    // 显示下一条
                    displayNextCommentary();
                }
            } catch (InterruptedException e) {
                logger.error("解说队列处理失败", e);
            }
        });
        thread.start();
    }
    
    /**
     * 生成缓存键
     * @param actionType 行动类型
     * @param params 参数
     * @return 缓存键
     */
    private static String generateCacheKey(String actionType, Object... params) {
        StringBuilder key = new StringBuilder(actionType);
        for (Object param : params) {
            if (param != null) {
                key.append("_").append(param.toString());
            }
        }
        return key.toString();
    }
    
    /**
     * 显示备用解说
     * @param actionType 行动类型
     */
    private static void showFallbackCommentary(String actionType) {
        String fallbackCommentary = getFallbackCommentary(actionType);
        showCommentary(fallbackCommentary);
    }
    
    /**
     * 获取备用解说
     * @param actionType 行动类型
     * @return 备用解说
     */
    private static String getFallbackCommentary(String actionType) {
        switch (actionType) {
            case "打牌":
                return "精彩的出牌！";
            case "用药水":
                return "明智的药水使用！";
            case "结束回合":
                return "回合结束，期待下一轮！";
            case "选择":
                return "有趣的选择！";
            case "火堆选择":
                return "重要的休息时间！";
            case "地图选择":
                return "新的冒险即将开始！";
            default:
                return "精彩的行动！";
        }
    }
    
    /**
     * 清空解说队列
     */
    public static void clearCommentaryQueue() {
        commentaryQueue.clear();
    }
    
    /**
     * 清空解说缓存
     */
    public static void clearCommentaryCache() {
        commentaryCache.clear();
    }
    
    /**
     * 添加解说到缓存
     * @param key 缓存键
     * @param commentary 解说内容
     */
    public static void addToCache(String key, String commentary) {
        if (commentaryCache.size() < 100) { // 限制缓存大小
            commentaryCache.put(key, commentary);
        }
    }
    
    /**
     * 获取解说统计信息
     * @return 统计信息
     */
    public static String getCommentaryStats() {
        return String.format("解说队列: %d, 缓存: %d, 行动计数: %d", 
                commentaryQueue.size(), commentaryCache.size(), actionCounter);
    }
    
    /**
     * 重置解说系统
     */
    public static void resetCommentarySystem() {
        clearCommentaryQueue();
        clearCommentaryCache();
        lastCommentaryTime = 0;
        actionCounter = 0;
        logger.info("解说系统已重置");
    }
    
    /**
     * 检查解说系统是否可用
     * @return 是否可用
     */
    public static boolean isCommentarySystemAvailable() {
        return commentaryEnabled && AISlayer.isAIStart();
    }
}