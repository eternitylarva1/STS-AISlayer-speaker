package aislayer.utils;

import aislayer.AISlayer;
import aislayer.panels.ConfigPanel;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.SpeechBubble;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
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
        logger.info("触发解说：" + actionType);
        
        if (!shouldTriggerCommentary()) {
            logger.info("解说条件不满足，跳过解说");
            return;
        }
        
        try {
            JSONObject actionInfo = AISlayer.getActionInfo(actionType, params);
            String cacheKey = generateCacheKey(actionType, params);
            
            // 检查缓存
            if (commentaryCache.containsKey(cacheKey)) {
                logger.info("使用缓存解说：" + commentaryCache.get(cacheKey));
                showCommentary(commentaryCache.get(cacheKey));
                return;
            }
            
            // 调用AI获取解说
            logger.info("调用AI获取解说：" + actionType);
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
        logger.info("检查解说条件：");
        
        // 检查解说开关
        if (!commentaryEnabled) {
            logger.info("- 解说开关：关闭");
            return false;
        }
        logger.info("- 解说开关：开启");
        
        // 检查API配置
        if (!AISlayer.isAIStart()) {
            logger.info("- AI API：未配置");
            return false;
        }
        logger.info("- AI API：已配置");
        
        // 检查冷却时间
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCommentaryTime < COMMENTARY_COOLDOWN) {
            logger.info("- 冷却时间：未到（剩余" + (COMMENTARY_COOLDOWN - (currentTime - lastCommentaryTime)) + "ms）");
            return false;
        }
        logger.info("- 冷却时间：已到");
        
        // 检查频率控制
        actionCounter++;
        if (actionCounter % commentaryFrequency != 0) {
            logger.info("- 频率控制：跳过（计数：" + actionCounter + "，频率：" + commentaryFrequency + "）");
            return false;
        }
        logger.info("- 频率控制：通过");
        
        // 检查游戏状态
        if (AbstractDungeon.getCurrRoom() == null) {
            logger.info("- 游戏状态：不在房间中");
            return false;
        }
        logger.info("- 游戏状态：正常");
        
        return true;
    }
    
    /**
     * 显示解说内容
     * @param commentary 解说内容
     */
    public static void showCommentary(String commentary) {
        if (commentary == null || commentary.trim().isEmpty()) {
            logger.warn("解说内容为空");
            return;
        }
        
        // 格式化解说内容
        String formattedCommentary = AISlayer.formatCommentary(commentary);
        
        // 输出到控制台，方便调试
        logger.info("=== AI解说 === " + formattedCommentary);
        
        // 添加到队列
        commentaryQueue.add(formattedCommentary);
        
        // 如果队列只有一条，立即显示
        if (commentaryQueue.size() == 1) {
            displayNextCommentary();
        }
        
        // 生成并播放语音
        VoiceGenerator.generateAndPlayVoice(formattedCommentary);
        
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
        logger.info("使用备用解说：" + fallbackCommentary);
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
    /**
     * 触发怪物介绍
     */
    public static void triggerMonsterIntroduction() {
        logger.info("触发怪物介绍");
        
        if (!shouldTriggerCommentary()) {
            logger.info("怪物介绍条件不满足，跳过");
            return;
        }
        
        try {
            // 构建怪物介绍信息
            JSONObject actionInfo = new JSONObject();
            actionInfo.put("行动类型", "怪物介绍");
            
            // 添加怪物信息
            StringBuilder monsterInfo = new StringBuilder();
            if (AbstractDungeon.getCurrRoom() != null && AbstractDungeon.getCurrRoom().monsters != null) {
                for (com.megacrit.cardcrawl.monsters.AbstractMonster monster : AbstractDungeon.getCurrRoom().monsters.monsters) {
                    if (monster.isDead || monster.isDying || monster.isEscaping) {
                        continue;
                    }
                    
                    if (monsterInfo.length() > 0) {
                        monsterInfo.append("和");
                    }
                    
                    if (ConfigPanel.detailedMonsterIntro) {
                        monsterInfo.append(String.format("%s(%d/%d HP)",
                            monster.name, monster.currentHealth, monster.maxHealth));
                    } else {
                        monsterInfo.append(monster.name);
                    }
                }
            }
            
            if (monsterInfo.length() == 0) {
                monsterInfo.append("未知敌人");
            }
            
            actionInfo.put("怪物信息", monsterInfo.toString());
            
            // 调用AI获取解说
            logger.info("调用AI获取怪物介绍");
            AIUtils.getCommentary(actionInfo);
            
        } catch (Exception e) {
            logger.error("触发怪物介绍失败", e);
            showFallbackCommentary("怪物介绍");
        }
    }
    
    /**
     * 生成怪物介绍文本
     * @return 怪物介绍文本
     */
    private static String generateMonsterIntroduction() {
        if (AbstractDungeon.getCurrRoom() == null || AbstractDungeon.getCurrRoom().monsters == null) {
            return "前方出现了未知的敌人！";
        }
        
        StringBuilder intro = new StringBuilder();
        boolean detailed = ConfigPanel.detailedMonsterIntro;
        
        for (AbstractMonster monster : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (monster.isDead || monster.isDying || monster.isEscaping) {
                continue;
            }
            
            if (intro.length() > 0) {
                intro.append(" ");
            }
            
            // 根据详细程度生成介绍
            if (detailed) {
                intro.append(String.format("%s(%d/%d HP)",
                    monster.name, monster.currentHealth, monster.maxHealth));
                
                // 添加意图信息
                if (monster.intent != AbstractMonster.Intent.DEBUG) {
                    intro.append(String.format(" 准备%s", getIntentDescription(monster.intent)));
                }
            } else {
                intro.append(monster.name);
            }
        }
        
        // 使用本地化模板
        String template = getMonsterIntroTemplate(detailed);
        return String.format(template, intro.toString());
    }
    
    /**
     * 获取怪物介绍模板
     * @param detailed 是否详细
     * @return 模板文本
     */
    private static String getMonsterIntroTemplate(boolean detailed) {
        try {
            String langPackDir = "aislayerResources" + File.separator + "localization" + File.separator + Settings.language.toString().toLowerCase();
            String textPath = langPackDir + File.separator + "text.json";
            JSONObject text = new JSONObject(AISlayer.loadJson(textPath));
            
            JSONArray templates = detailed ?
                text.getJSONArray("monsterIntroDetailed") :
                text.getJSONArray("monsterIntro");
            
            return templates.getString((int) (Math.random() * templates.length()));
        } catch (Exception e) {
            return detailed ? "前方出现了{0}！{1}" : "前方出现了{0}！";
        }
    }
    
    /**
     * 获取意图描述
     * @param intent 怪物意图
     * @return 意图描述
     */
    private static String getIntentDescription(AbstractMonster.Intent intent) {
        switch (intent) {
            case ATTACK:
                return "攻击";
            case ATTACK_BUFF:
                return "攻击并增益";
            case ATTACK_DEBUFF:
                return "攻击并减益";
            case ATTACK_DEFEND:
                return "攻击并防御";
            case BUFF:
                return "增益";
            case DEBUFF:
                return "减益";
            case DEFEND:
                return "防御";
            case DEFEND_DEBUFF:
                return "防御并减益";
            case ESCAPE:
                return "逃跑";
            case MAGIC:
                return "使用技能";
            case SLEEP:
                return "睡眠";
            case STUN:
                return "眩晕";
            case UNKNOWN:
                return "未知行动";
            case DEBUG:
            default:
                return "";
        }
    }
    
    /**
     * 检查是否应该按牌数触发解说
     * @return 是否应该触发
     */
    public static boolean shouldTriggerCommentaryByCards() {
        logger.info("检查按牌数解说模式");
        
        if (!shouldTriggerCommentary()) {
            logger.info("- 基础条件不满足");
            return false;
        }
        
        // 检查解说模式
        if (!ConfigPanel.isByCardsMode()) {
            logger.info("- 不是按牌数模式");
            return false; // 不是按牌数模式
        }
        logger.info("- 是按牌数模式");
        
        // 检查战斗状态跟踪器
        BattleStateTracker tracker = BattleStateTracker.getInstance();
        boolean shouldTrigger = tracker.shouldTriggerCommentaryByCards();
        logger.info("- 战斗状态检查：" + shouldTrigger);
        
        return shouldTrigger;
    }
    
    /**
     * 检查是否应该在回合结束触发解说
     * @return 是否应该触发
     */
    public static boolean shouldTriggerCommentaryByTurnEnd() {
        logger.info("检查回合结束解说模式");
        
        if (!shouldTriggerCommentary()) {
            logger.info("- 基础条件不满足");
            return false;
        }
        
        // 检查解说模式
        if (!ConfigPanel.isByTurnEndMode()) {
            logger.info("- 不是回合结束模式");
            return false; // 不是回合结束模式
        }
        logger.info("- 是回合结束模式");
        
        return true;
    }
    
    /**
     * 设置语音开关
     * @param enabled 是否启用语音
     */
    public static void setVoiceEnabled(boolean enabled) {
        VoiceGenerator.voiceEnabled = enabled;
        logger.info("语音功能已" + (enabled ? "启用" : "禁用"));
    }
    
    /**
     * 获取语音状态
     * @return 是否启用语音
     */
    public static boolean isVoiceEnabled() {
        return VoiceGenerator.voiceEnabled;
    }
    
    /**
     * 设置语音音量，确保在有效范围内
     * @param volume 音量 (0.0 - 1.0)
     */
    public static void setVoiceVolume(float volume) {
        // 确保音量在 0.0 - 1.0 范围内
        float clampedVolume = Math.max(0.0f, Math.min(1.0f, volume));
        VoiceGenerator.setVolume(clampedVolume);
        ConfigPanel.setVoiceVolume(clampedVolume);
        logger.info("语音音量已设置为: " + clampedVolume);
    }
    
    /**
     * 获取语音音量
     * @return 当前音量
     */
    public static float getVoiceVolume() {
        return VoiceGenerator.getVolume();
    }
    
    /**
     * 清理语音缓存
     */
    public static void clearVoiceCache() {
        VoiceGenerator.clearCache();
        logger.info("语音缓存已清理");
    }
    
    /**
     * 获取语音统计信息
     * @return 语音统计信息
     */
    public static String getVoiceStats() {
        return VoiceGenerator.getCacheStats();
    }
    
    /**
     * 停止当前语音播放
     */
    public static void stopVoicePlayback() {
        VoiceGenerator.stopCurrentPlayback();
        logger.info("已停止当前语音播放");
    }
    
    /**
     * 检查是否正在播放语音
     * @return 是否正在播放
     */
    public static boolean isVoicePlaying() {
        return VoiceGenerator.isPlaying();
    }
}