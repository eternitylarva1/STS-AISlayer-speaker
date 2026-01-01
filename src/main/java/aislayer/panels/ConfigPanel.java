package aislayer.panels;

import basemod.EasyConfigPanel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.UIStrings;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Map;

import static aislayer.AISlayer.loadJson;

public class ConfigPanel extends EasyConfigPanel {

    public static boolean selectedPlatform_1 = true;
    public static String apiKey_1 = "sk-...";
    public static String apiUrl_1 = "api.deepseek.com/v1";
    public static boolean selectedModel_1_1 = true;
    public static String model_1_1 = "deepseek-chat";
    public static boolean selectedModel_1_2 = false;
    public static String model_1_2 = "deepseek-chat";

    public static boolean selectedPlatform_2 = false;
    public static String apiKey_2 = "sk-...";
    public static String apiUrl_2 = "api.openai.com/v1";
    public static boolean selectedModel_2_1 = false;
    public static String model_2_1 = "gpt-3.5-turbo";
    public static boolean selectedModel_2_2 = false;
    public static String model_2_2 = "gpt-4";

    public static String language = "中文";
    public static boolean handleApiUrl = true;

    // 解说相关配置
    public static boolean commentaryEnabled = true;
    public static int commentaryFrequency = 1;
    public static String commentaryStyle = "幽默";
    public static boolean showCommentaryHistory = true;
    public static int commentaryTimeout = 10; // API调用超时时间（秒）
    
    // 新增解说增强配置
    public static boolean commentaryByCards = true; // 解说模式：true=按牌数解说，false=回合结束解说
    public static int cardsPerCommentary = 3; // 每次解说需要的牌数（按牌数模式）
    public static boolean introduceMonsters = true; // 是否在战斗开始时介绍怪物
    public static boolean detailedMonsterIntro = true; // 怪物介绍详细程度：true=详细，false=简单
    
    // 语音配置
    public static boolean voiceEnabled = true; // 是否启用语音解说
    public static float voiceVolume = 0.8f; // 语音音量 (0.0 - 1.0)
    public static boolean autoClearVoiceCache = false; // 是否自动清理语音缓存
    public static String voiceApiToken = ""; // 语音API Token
    
    // 关键词配置
    public static float keywordTriggerProbability = 0.1f; // 关键词触发概率，默认0.1
    
    /**
     * 检查是否为按牌数解说模式
     * @return 是否为按牌数解说模式
     */
    public static boolean isByCardsMode() {
        return commentaryByCards;
    }
    
    /**
     * 检查是否为回合结束解说模式
     * @return 是否为回合结束解说模式
     */
    public static boolean isByTurnEndMode() {
        return !commentaryByCards;
    }
    
    /**
     * 设置关键词触发概率
     * @param probability 概率值 (0.0 - 1.0)
     */
    public static void setKeywordTriggerProbability(float probability) {
        // 确保概率在 0.0 - 1.0 范围内
        keywordTriggerProbability = Math.max(0.0f, Math.min(1.0f, probability));
    }
    
    /**
     * 获取关键词触发概率
     * @return 概率值 (0.0 - 1.0)
     */
    public static float getKeywordTriggerProbability() {
        return keywordTriggerProbability;
    }

    public ConfigPanel() {
        super("aislayer", getUIStrings(), "config");
        setupTextField("apiUrl_1", 500, 100);
        setupTextField("apiKey_1", 750, 100);
        setupTextField("apiUrl_2", 500, 100);
        setupTextField("apiKey_2", 750, 100);
        setupTextField("voiceApiToken", 750, 100);
        
        // 设置语音音量范围 (0.0 - 1.0)
        setNumberRange("voiceVolume", 0.0f, 1.0f);
        
        // 设置关键词触发概率范围 (0.0 - 1.0)
        setNumberRange("keywordTriggerProbability", 0.0f, 1.0f);
        
        // 确保初始值在有效范围内
        setKeywordTriggerProbability(keywordTriggerProbability);
        
        setPadding(30.0F);
    }
    
    /**
     * 设置语音音量，确保在有效范围内
     * @param volume 音量值
     */
    public static void setVoiceVolume(float volume) {
        // 确保音量在 0.0 - 1.0 范围内
        voiceVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }
    
    /**
     * 获取语音音量
     * @return 音量值 (0.0 - 1.0)
     */
    public static float getVoiceVolume() {
        return voiceVolume;
    }

    private static UIStrings getUIStrings() {
        String langPackDir = "aislayerResources" + File.separator + "localization" + File.separator + Settings.language.toString().toLowerCase();
        String uiPath = langPackDir + File.separator + "ui.json";
        Gson gson = new Gson();
        Type uiType = (new TypeToken<Map<String, UIStrings>>() {
        }).getType();
        Map<String, UIStrings> ui = gson.fromJson(loadJson(uiPath), uiType);
        return ui.get("aislayer:Config");
    }

}
