package aislayer.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 语音功能测试类
 */
public class VoiceTest {
    
    private static final Logger logger = LogManager.getLogger(VoiceTest.class.getName());
    
    /**
     * 测试曼波语音生成功能
     */
    public static void testVoiceGeneration() {
        logger.info("开始测试曼波语音生成功能...");
        
        // 测试文本
        String[] testTexts = {
            "精彩的出牌！",
            "明智的药水使用！",
            "回合结束，期待下一轮！",
            "前方出现了强大的敌人！",
            "这是一个有趣的战斗！"
        };
        
        for (String text : testTexts) {
            logger.info("测试语音生成: " + text);
            VoiceGenerator.generateAndPlayVoice(text);
            
            // 等待一段时间再测试下一个
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                logger.error("测试被中断", e);
                break;
            }
        }
        
        logger.info("语音测试完成");
    }
    
    /**
     * 测试语音配置功能
     */
    public static void testVoiceSettings() {
        logger.info("测试语音配置功能...");
        
        // 测试音量调节
        logger.info("测试音量调节");
        VoiceGenerator.setVolume(0.5f);
        logger.info("当前音量: " + VoiceGenerator.getVolume());
        
        VoiceGenerator.setVolume(1.0f);
        logger.info("当前音量: " + VoiceGenerator.getVolume());
        
        // 测试开关控制
        logger.info("测试语音开关");
        boolean originalState = VoiceGenerator.voiceEnabled;
        
        VoiceGenerator.voiceEnabled = false;
        logger.info("语音已禁用，测试语音生成（应该没有声音）");
        VoiceGenerator.generateAndPlayVoice("这条语音不应该播放");
        
        VoiceGenerator.voiceEnabled = true;
        logger.info("语音已启用");
        
        // 恢复原始状态
        VoiceGenerator.voiceEnabled = originalState;
        
        logger.info("语音配置测试完成");
    }
    
    /**
     * 测试语音缓存功能
     */
    public static void testVoiceCache() {
        logger.info("测试语音缓存功能...");
        
        // 获取缓存统计
        String stats = VoiceGenerator.getCacheStats();
        logger.info("缓存统计: " + stats);
        
        // 清理缓存
        logger.info("清理语音缓存");
        VoiceGenerator.clearCache();
        
        // 再次获取统计
        stats = VoiceGenerator.getCacheStats();
        logger.info("清理后缓存统计: " + stats);
        
        logger.info("语音缓存测试完成");
    }
    
    /**
     * 运行所有测试
     */
    public static void runAllTests() {
        logger.info("=== 开始语音功能全面测试 ===");
        
        try {
            testVoiceSettings();
            Thread.sleep(1000);
            
            testVoiceCache();
            Thread.sleep(1000);
            
            testVoiceGeneration();
            
        } catch (Exception e) {
            logger.error("测试过程中出现异常", e);
        }
        
        logger.info("=== 语音功能测试完成 ===");
    }
}