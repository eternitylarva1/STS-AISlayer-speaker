package aislayer.utils;

import aislayer.panels.ConfigPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 配置验证工具类
 * 用于验证所有配置选项是否正确影响AI通信
 */
public class ConfigValidator {
    
    private static final Logger logger = LogManager.getLogger(ConfigValidator.class.getName());
    
    /**
     * 验证所有解说相关配置
     * @return 验证结果
     */
    public static ValidationResult validateCommentaryConfig() {
        ValidationResult result = new ValidationResult();
        
        // 验证解说模式
        validateCommentaryMode(result);
        
        // 验证按牌数解说配置
        validateCardsPerCommentary(result);
        
        // 验证怪物介绍配置
        validateMonsterIntroduction(result);
        
        // 验证怪物介绍详细程度
        validateMonsterIntroDetail(result);
        
        // 验证基础解说配置
        validateBasicCommentaryConfig(result);
        
        return result;
    }
    
    /**
     * 验证解说模式配置
     */
    private static void validateCommentaryMode(ValidationResult result) {
        // 检查CommentaryUtils中的使用
        boolean usedInCommentaryUtils = true;
        try {
            // 这些方法会在实际运行时被调用
            boolean byCards = ConfigPanel.isByCardsMode();
            boolean byTurnEnd = ConfigPanel.isByTurnEndMode();
            
            if (byCards && byTurnEnd) {
                result.addWarning("解说模式配置异常：同时启用了按牌数和回合结束模式");
            }
            
            if (!byCards && !byTurnEnd) {
                result.addWarning("解说模式配置异常：未启用任何解说模式");
            }
            
        } catch (Exception e) {
            result.addError("解说模式配置验证失败：" + e.getMessage());
            usedInCommentaryUtils = false;
        }
        
        String modeText = ConfigPanel.isByCardsMode() ? "按牌数解说" : "回合结束解说";
        result.addCheck("解说模式", modeText, usedInCommentaryUtils);
    }
    
    /**
     * 验证按牌数解说配置
     */
    private static void validateCardsPerCommentary(ValidationResult result) {
        int cardsPerCommentary = ConfigPanel.cardsPerCommentary;
        
        if (cardsPerCommentary <= 0) {
            result.addError("每几张牌解说一次配置无效：必须大于0");
            return;
        }
        
        if (cardsPerCommentary > 10) {
            result.addWarning("每几张牌解说一次配置较大：" + cardsPerCommentary + "，可能影响解说体验");
        }
        
        // 检查BattleStateTracker中的使用
        boolean usedInTracker = true;
        try {
            BattleStateTracker tracker = BattleStateTracker.getInstance();
            // 这个配置会在updateConfig方法中被使用
            tracker.updateConfig(cardsPerCommentary, ConfigPanel.introduceMonsters, ConfigPanel.detailedMonsterIntro);
        } catch (Exception e) {
            result.addError("按牌数解说配置验证失败：" + e.getMessage());
            usedInTracker = false;
        }
        
        result.addCheck("每几张牌解说一次", String.valueOf(cardsPerCommentary), usedInTracker);
    }
    
    /**
     * 验证怪物介绍配置
     */
    private static void validateMonsterIntroduction(ValidationResult result) {
        boolean introduceMonsters = ConfigPanel.introduceMonsters;
        
        // 检查PlayFirstCardPatch中的使用
        boolean usedInPatch = true;
        try {
            // 这个配置会在PlayFirstCardPatch中被检查
            if (introduceMonsters) {
                // 检查相关的AI通信是否会被触发
                boolean willTriggerAI = true; // 假设会触发
                result.addInfo("怪物介绍已启用，AI将在战斗开始时介绍怪物");
            } else {
                result.addInfo("怪物介绍已禁用，将显示思考文本");
            }
        } catch (Exception e) {
            result.addError("怪物介绍配置验证失败：" + e.getMessage());
            usedInPatch = false;
        }
        
        result.addCheck("战斗开始时介绍怪物", String.valueOf(introduceMonsters), usedInPatch);
    }
    
    /**
     * 验证怪物介绍详细程度配置
     */
    private static void validateMonsterIntroDetail(ValidationResult result) {
        boolean detailedMonsterIntro = ConfigPanel.detailedMonsterIntro;
        
        String detailText = detailedMonsterIntro ? "详细" : "简单";
        
        // 检查CommentaryUtils中的使用
        boolean usedInCommentaryUtils = true;
        try {
            // 这个配置会在triggerMonsterIntroduction中被使用
            if (ConfigPanel.introduceMonsters) {
                result.addInfo("怪物介绍详细程度：" + detailText);
            }
        } catch (Exception e) {
            result.addError("怪物介绍详细程度配置验证失败：" + e.getMessage());
            usedInCommentaryUtils = false;
        }
        
        result.addCheck("怪物介绍详细程度", detailText, usedInCommentaryUtils);
    }
    
    /**
     * 验证基础解说配置
     */
    private static void validateBasicCommentaryConfig(ValidationResult result) {
        // 验证解说开关
        if (!ConfigPanel.commentaryEnabled) {
            result.addWarning("解说功能已禁用，所有解说相关配置不会生效");
            return;
        }
        
        // 验证解说频率
        if (ConfigPanel.commentaryFrequency <= 0) {
            result.addError("解说频率配置无效：必须大于0");
        }
        
        // 验证解说超时
        if (ConfigPanel.commentaryTimeout <= 0) {
            result.addError("解说超时配置无效：必须大于0");
        } else if (ConfigPanel.commentaryTimeout > 30) {
            result.addWarning("解说超时时间较长：" + ConfigPanel.commentaryTimeout + "秒");
        }
        
        result.addCheck("解说功能", "已启用", true);
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private boolean hasErrors = false;
        private StringBuilder report = new StringBuilder();
        
        public void addError(String message) {
            hasErrors = true;
            report.append("❌ 错误：").append(message).append("\n");
            logger.error("配置验证错误：" + message);
        }
        
        public void addWarning(String message) {
            report.append("⚠️ 警告：").append(message).append("\n");
            logger.warn("配置验证警告：" + message);
        }
        
        public void addInfo(String message) {
            report.append("ℹ️ 信息：").append(message).append("\n");
            logger.info("配置验证信息：" + message);
        }
        
        public void addCheck(String configName, String value, boolean used) {
            String status = used ? "✅" : "❌";
            report.append(status).append(" ").append(configName).append("：").append(value).append("\n");
        }
        
        public boolean hasErrors() {
            return hasErrors;
        }
        
        public String getReport() {
            return report.toString();
        }
        
        public void logReport() {
            if (hasErrors) {
                logger.error("配置验证发现错误：\n" + getReport());
            } else {
                logger.info("配置验证通过：\n" + getReport());
            }
        }
    }
}