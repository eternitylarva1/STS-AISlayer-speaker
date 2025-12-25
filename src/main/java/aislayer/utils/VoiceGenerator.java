package aislayer.utils;

import aislayer.panels.ConfigPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.sound.sampled.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 曼波语音生成器，负责调用API生成语音并播放
 */
public class VoiceGenerator {
    
    public static final Logger logger = LogManager.getLogger(VoiceGenerator.class.getName());
    
    // 曼波API配置
    private static final String API_BASE_URL = "https://api.milorapart.top/apis/mbAIsc";
    private static final String DEFAULT_FORMAT = "wav"; // 改为WAV格式，避免MP3兼容性问题
    
    // 语音缓存目录
    private static final String CACHE_DIR = "voice_cache";
    
    // 线程池
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    
    // 语音缓存
    private static final ConcurrentHashMap<String, File> voiceCache = new ConcurrentHashMap<>();
    
    // 语音开关
    public static boolean voiceEnabled = true;
    
    // 音量控制 (0.0 - 1.0) - 从ConfigPanel同步
    public static float volume = aislayer.panels.ConfigPanel.voiceVolume;
    
    // 是否正在播放
    private static volatile boolean isPlaying = false;
    
    // 上次记录的音量，避免重复日志
    private static float lastLoggedVolume = -1.0f;
    
    /**
     * 生成并播放语音
     * @param text 要转换的文本
     */
    public static void generateAndPlayVoice(String text) {
        if (!voiceEnabled || text == null || text.trim().isEmpty()) {
            return;
        }
        
        // 异步处理，避免阻塞游戏线程
        CompletableFuture.runAsync(() -> {
            try {
                generateAndPlayVoiceSync(text);
            } catch (Exception e) {
                logger.error("语音生成播放失败", e);
            }
        }, executor);
    }
    
    /**
     * 同步生成并播放语音
     * @param text 要转换的文本
     */
    private static void generateAndPlayVoiceSync(String text) {
        try {
            // 检查缓存
            String cacheKey = generateCacheKey(text);
            File audioFile = voiceCache.get(cacheKey);
            
            if (audioFile == null || !audioFile.exists()) {
                // 生成新的语音
                audioFile = generateVoice(text);
                if (audioFile != null) {
                    voiceCache.put(cacheKey, audioFile);
                }
            }
            
            if (audioFile != null && audioFile.exists()) {
                // 播放语音
                playAudio(audioFile);
            }
            
        } catch (Exception e) {
            logger.error("语音生成播放失败: " + text, e);
        }
    }
    
    /**
     * 调用API生成语音
     * @param text 要转换的文本
     * @return 音频文件
     */
    private static File generateVoice(String text) {
        try {
            // 构建请求URL
            String encodedText = URLEncoder.encode(text, "UTF-8");
            String requestUrl = String.format("%s?text=%s&format=%s", API_BASE_URL, encodedText, DEFAULT_FORMAT);
            
            logger.info("请求曼波API: " + requestUrl);
            
            // 发送HTTP请求
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "AISlayer-Mod/1.0");
            
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // 读取响应
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // 解析JSON响应
                JSONObject jsonResponse = new JSONObject(response.toString());
                if (jsonResponse.getInt("code") == 200) {
                    String audioUrl = jsonResponse.getString("url");
                    logger.info("获取到音频URL: " + audioUrl);
                    
                    // 下载音频文件
                    return downloadAudioFile(audioUrl, text);
                } else {
                    logger.error("API返回错误: " + jsonResponse.getString("msg"));
                }
            } else {
                logger.error("API请求失败，状态码: " + responseCode);
            }
            
            connection.disconnect();
            
        } catch (Exception e) {
            logger.error("调用曼波API失败", e);
        }
        
        return null;
    }
    
    /**
     * 下载音频文件
     * @param audioUrl 音频URL
     * @param text 原始文本（用于生成文件名）
     * @return 音频文件
     */
    private static File downloadAudioFile(String audioUrl, String text) {
        try {
            // 创建缓存目录
            Path cachePath = Paths.get(CACHE_DIR);
            if (!Files.exists(cachePath)) {
                Files.createDirectories(cachePath);
            }
            
            // 生成文件名
            String fileName = generateFileName(text);
            File audioFile = cachePath.resolve(fileName).toFile();
            
            // 如果文件已存在，直接返回
            if (audioFile.exists()) {
                return audioFile;
            }
            
            // 下载文件
            URL url = new URL(audioUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(audioFile)) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            connection.disconnect();
            logger.info("音频文件下载完成: " + audioFile.getAbsolutePath());
            return audioFile;
            
        } catch (Exception e) {
            logger.error("下载音频文件失败", e);
            return null;
        }
    }
    
    /**
     * 播放音频文件
     * @param audioFile 音频文件
     */
    private static void playAudio(File audioFile) {
        try {
            if (isPlaying) {
                logger.info("正在播放其他语音，跳过当前播放");
                return;
            }
            
            isPlaying = true;
            
            // 只使用Java Sound API播放
            if (!tryPlayWithJavaSound(audioFile)) {
                logger.error("Java Sound API播放失败，无法播放音频: " + audioFile.getName());
            }
            
        } catch (Exception e) {
            logger.error("播放音频异常", e);
            isPlaying = false;
        }
    }
    
    /**
     * 尝试使用Java Sound API播放
     * @param audioFile 音频文件
     * @return 是否成功播放
     */
    private static boolean tryPlayWithJavaSound(File audioFile) {
        try {
            try (AudioInputStream audioStream = getAudioInputStream(audioFile)) {
                AudioFormat format = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.open(audioStream);
                
                // 设置音量
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
                    gainControl.setValue(dB);
                }
                
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        isPlaying = false;
                    }
                });
                
                clip.start();
                logger.info("开始播放语音: " + audioFile.getName());
                return true;
            }
        } catch (Exception e) {
            logger.warn("Java Sound API播放失败: " + e.getMessage());
            return false;
        }
    }
    
    
    /**
     * 获取音频输入流，处理MP3格式兼容性问题
     * @param audioFile 音频文件
     * @return 音频输入流
     */
    private static AudioInputStream getAudioInputStream(File audioFile) throws UnsupportedAudioFileException, IOException {
        try {
            // 首先尝试直接读取
            AudioInputStream stream = AudioSystem.getAudioInputStream(audioFile);
            logger.info("成功直接读取音频文件: " + audioFile.getName() +
                       ", 格式: " + stream.getFormat());
            return stream;
        } catch (UnsupportedAudioFileException e) {
            logger.warn("直接读取音频失败，尝试转换格式: " + e.getMessage());
            
            // 如果直接读取失败，尝试格式转换
            try (AudioInputStream originalStream = AudioSystem.getAudioInputStream(audioFile)) {
                AudioFormat originalFormat = originalStream.getFormat();
                logger.info("原始音频格式: " + originalFormat);
                
                // 转换为标准PCM格式
                AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    originalFormat.getSampleRate(),
                    16, // 16位
                    originalFormat.getChannels(),
                    originalFormat.getChannels() * 2, // 每样本2字节
                    originalFormat.getSampleRate(),
                    false // 小端序
                );
                
                logger.info("目标音频格式: " + targetFormat);
                AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, originalStream);
                logger.info("音频格式转换成功");
                return convertedStream;
            } catch (Exception conversionException) {
                logger.error("音频格式转换失败: " + conversionException.getMessage());
                throw new UnsupportedAudioFileException("无法转换音频格式: " + conversionException.getMessage());
            }
        }
    }
    
    /**
     * 生成缓存键
     * @param text 文本
     * @return 缓存键
     */
    private static String generateCacheKey(String text) {
        return String.valueOf(text.hashCode());
    }
    
    /**
     * 生成文件名
     * @param text 文本
     * @return 文件名
     */
    private static String generateFileName(String text) {
        String safeName = text.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
        if (safeName.length() > 20) {
            safeName = safeName.substring(0, 20);
        }
        return safeName + "_" + System.currentTimeMillis() + ".wav";
    }
    
    /**
     * 清理缓存
     */
    public static void clearCache() {
        try {
            Path cachePath = Paths.get(CACHE_DIR);
            if (Files.exists(cachePath)) {
                Files.walk(cachePath)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            logger.warn("删除缓存文件失败: " + file);
                        }
                    });
            }
            voiceCache.clear();
            logger.info("语音缓存已清理");
        } catch (Exception e) {
            logger.error("清理语音缓存失败", e);
        }
    }
    
    /**
     * 获取缓存统计信息
     * @return 统计信息
     */
    public static String getCacheStats() {
        try {
            Path cachePath = Paths.get(CACHE_DIR);
            if (!Files.exists(cachePath)) {
                return "缓存目录不存在";
            }
            
            long fileCount = Files.walk(cachePath)
                .filter(Files::isRegularFile)
                .count();
            
            long totalSize = Files.walk(cachePath)
                .filter(Files::isRegularFile)
                .mapToLong(file -> {
                    try {
                        return Files.size(file);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
            
            return String.format("缓存文件: %d, 总大小: %.2f MB", fileCount, totalSize / (1024.0 * 1024.0));
            
        } catch (Exception e) {
            return "获取缓存统计失败: " + e.getMessage();
        }
    }
    
    /**
     * 停止当前播放
     */
    public static void stopCurrentPlayback() {
        isPlaying = false;
    }
    
    /**
     * 设置音量，确保在有效范围内
     * @param newVolume 新音量 (0.0 - 1.0)
     */
    public static void setVolume(float newVolume) {
        // 确保音量在 0.0 - 1.0 范围内
        volume = Math.max(0.0f, Math.min(1.0f, newVolume));
        
        // 只在音量变化较大时才记录日志，避免频繁输出
        if (Math.abs(volume - lastLoggedVolume) > 0.1f) {
            logger.info("语音音量已设置为: " + volume);
            lastLoggedVolume = volume;
        }
    }
    
    /**
     * 获取当前音量
     * @return 当前音量 (0.0 - 1.0)
     */
    public static float getVolume() {
        return volume;
    }
    
    /**
     * 从ConfigPanel同步音量设置
     */
    public static void syncVolumeFromConfig() {
        setVolume(ConfigPanel.voiceVolume);
    }
    
    /**
     * 检查是否正在播放
     * @return 是否正在播放
     */
    public static boolean isPlaying() {
        return isPlaying;
    }
    
    /**
     * 关闭语音生成器
     */
    public static void shutdown() {
        executor.shutdown();
        clearCache();
    }
}