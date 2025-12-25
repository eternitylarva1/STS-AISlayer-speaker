package aislayer.test;

import aislayer.utils.VoiceGenerator;

/**
 * 语音生成测试类
 */
public class VoiceTest {
    public static void main(String[] args) {
        System.out.println("开始语音生成测试...");
        
        // 测试语音生成
        System.out.println("测试语音生成功能...");
        VoiceGenerator.generateAndPlayVoice("这是一个测试语音，验证修复后的功能是否正常工作");
        
        // 等待一段时间让语音播放完成
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // 显示缓存统计
        System.out.println("缓存统计: " + VoiceGenerator.getCacheStats());
        
        System.out.println("语音生成测试完成");
    }
}