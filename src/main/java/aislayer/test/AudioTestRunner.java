package aislayer.test;

import aislayer.utils.AudioTest;

/**
 * 音频测试运行器
 */
public class AudioTestRunner {
    public static void main(String[] args) {
        System.out.println("开始音频系统测试...");
        
        // 测试MP3支持
        AudioTest.testMP3Support();
        
        // 测试音频播放
        AudioTest.testAudioPlayback();
        
        System.out.println("音频系统测试完成");
    }
}