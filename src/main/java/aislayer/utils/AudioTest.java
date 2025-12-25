package aislayer.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * 音频播放测试类
 */
public class AudioTest {
    
    public static final Logger logger = LogManager.getLogger(AudioTest.class.getName());
    
    /**
     * 测试音频播放功能
     */
    public static void testAudioPlayback() {
        logger.info("开始音频播放测试");
        
        // 测试系统支持的音频格式
        testSupportedFormats();
        
        // 创建测试音频文件
        File testFile = createTestAudioFile();
        if (testFile != null) {
            // 测试播放
            testPlayAudioFile(testFile);
        }
    }
    
    /**
     * 测试系统支持的音频格式
     */
    private static void testSupportedFormats() {
        logger.info("=== 测试系统支持的音频格式 ===");
        
        AudioFormat[] formats = {
            new AudioFormat(44100, 16, 2, true, false), // 标准立体声
            new AudioFormat(22050, 16, 1, true, false), // 单声道
            new AudioFormat(44100, 8, 2, true, false),  // 8位
            new AudioFormat(44100, 16, 2, true, true)   // 大端序
        };
        
        for (AudioFormat format : formats) {
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            boolean supported = AudioSystem.isLineSupported(info);
            logger.info("格式支持测试: " + format + " -> " + (supported ? "支持" : "不支持"));
        }
    }
    
    /**
     * 创建测试音频文件
     */
    private static File createTestAudioFile() {
        try {
            logger.info("创建测试音频文件");
            
            // 生成1秒的正弦波音频
            float sampleRate = 44100;
            int duration = 1; // 1秒
            int sampleCount = (int) (sampleRate * duration);
            byte[] buffer = new byte[sampleCount * 2]; // 16位单声道
            
            for (int i = 0; i < sampleCount; i++) {
                double angle = 2.0 * Math.PI * i / (sampleRate / 440.0); // 440Hz正弦波
                short value = (short) (Short.MAX_VALUE * Math.sin(angle));
                buffer[i * 2] = (byte) (value & 0xFF);
                buffer[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
            }
            
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(buffer);
            AudioInputStream ais = new AudioInputStream(bais, format, buffer.length / 2);
            
            File testFile = new File("test_audio.wav");
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, testFile);
            
            logger.info("测试音频文件创建成功: " + testFile.getAbsolutePath());
            return testFile;
            
        } catch (Exception e) {
            logger.error("创建测试音频文件失败", e);
            return null;
        }
    }
    
    /**
     * 测试播放音频文件
     */
    private static void testPlayAudioFile(File audioFile) {
        logger.info("=== 测试播放音频文件 ===");
        
        try {
            // 获取音频输入流
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioStream.getFormat();
            logger.info("音频格式: " + format);
            
            // 检查是否支持
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                logger.error("不支持的音频格式: " + format);
                return;
            }
            
            // 创建并播放Clip
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(audioStream);
            
            logger.info("开始播放测试音频...");
            clip.start();
            
            // 等待播放完成
            while (clip.isRunning()) {
                Thread.sleep(100);
            }
            
            clip.close();
            audioStream.close();
            logger.info("音频播放测试完成");
            
        } catch (Exception e) {
            logger.error("音频播放测试失败", e);
        }
    }
    
    /**
     * 测试MP3文件支持
     */
    public static void testMP3Support() {
        logger.info("=== 测试MP3文件支持 ===");
        
        try {
            // 列出所有支持的音频文件类型
            AudioFileFormat.Type[] types = AudioSystem.getAudioFileTypes();
            for (AudioFileFormat.Type type : types) {
                logger.info("支持的音频类型: " + type.getExtension());
            }
            
            // 检查MP3支持
            boolean mp3ReadingSupported = false;
            boolean mp3WritingSupported = false;
            
            for (AudioFileFormat.Type type : types) {
                if ("mp3".equalsIgnoreCase(type.getExtension())) {
                    mp3ReadingSupported = true;
                    break;
                }
            }
            
            logger.info("MP3读取支持: " + (mp3ReadingSupported ? "是" : "否"));
            logger.info("MP3写入支持: " + (mp3WritingSupported ? "是" : "否"));
            
        } catch (Exception e) {
            logger.error("测试MP3支持失败", e);
        }
    }
}