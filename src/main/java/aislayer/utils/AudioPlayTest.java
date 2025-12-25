package aislayer.utils;

import java.io.File;

/**
 * 音频播放修复测试
 */
public class AudioPlayTest {
    
    public static void main(String[] args) {
        System.out.println("=== 音频播放修复测试 ===");
        
        // 测试语音生成和播放
        System.out.println("测试语音生成和播放...");
        VoiceGenerator.generateAndPlayVoice("这是一个测试语音，验证音频播放是否正常工作");
        
        // 等待一段时间
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 测试音量控制
        System.out.println("测试音量控制...");
        VoiceGenerator.setVolume(0.5f);
        VoiceGenerator.generateAndPlayVoice("音量已调低到50%");
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 恢复音量
        VoiceGenerator.setVolume(0.8f);
        
        System.out.println("=== 测试完成 ===");
    }
}