package aislayer.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.TalkAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rewards.chests.AbstractChest;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.TreasureRoom;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import com.megacrit.cardcrawl.vfx.combat.LightBulbEffect;

import static aislayer.AISlayer.allCards;
import static aislayer.AISlayer.allDescriptions;
import static aislayer.AISlayer.allPotions;
import static aislayer.AISlayer.allRelics;
import static aislayer.AISlayer.apiKey;
import static aislayer.AISlayer.apiUrl;
import static aislayer.AISlayer.getCardInfo;
import static aislayer.AISlayer.getMapPaths;
import static aislayer.AISlayer.handleDescription;
import static aislayer.AISlayer.knownCards;
import static aislayer.AISlayer.knownKeywords;
import static aislayer.AISlayer.knownPotions;
import static aislayer.AISlayer.knownRelics;
import static aislayer.AISlayer.model;
import aislayer.actions.AIEndTurnAction;
import aislayer.actions.AIThinkAction;
import aislayer.actions.AIUseCardAction;
import aislayer.actions.AIUsePotionAction;
import aislayer.patchs.SelectCampfirePatch;

public class AIUtils {

    public static final Logger logger = LogManager.getLogger(AIUtils.class.getName());

    public static JSONArray messagesArray = new JSONArray();

    public static Hitbox lockedHoveredHitbox = null;

    public static void action (JSONObject info) {

        Thread thread = new Thread(() -> {
            
            addToBot(new AIThinkAction());

            JSONObject tool = AIUtils.getTool(apiKey, apiUrl, model, stringify(info));

            if (tool.has("error")) {
                logger.info(tool.getString("error"));
                return;
            }

            JSONObject message = new JSONObject();
            JSONArray tool_calls = new JSONArray();
            tool_calls.put(tool);
            message.put("role", "assistant");
            message.put("tool_calls", tool_calls);
            messagesArray.put(message);

            JSONObject response = new JSONObject();
            response.put("tool_call_id", tool.getString("id"));
            response.put("role", "tool");
            response.put("name", tool.getJSONObject("function").getString("name"));
            response.put("content", tool.getJSONObject("function").getString("arguments"));
            messagesArray.put(response);

            JSONObject function = tool.getJSONObject("function");

            logger.info(function);

            String functionName = function.getString("name");
            JSONObject arguments = new JSONObject(function.getString("arguments"));

            addToBot(new VFXAction(new LightBulbEffect(AbstractDungeon.player.hb), 0.5F));

            addToBot(new TalkAction(true, arguments.getString("reason"), 4.0F, 4.0F));

            executeAction(functionName, arguments);
        });
        thread.start();
    }

    /**
     * 执行AI决策的动作
     * @param functionName 动作名称
     * @param arguments 动作参数
     */
    private static void executeAction(String functionName, JSONObject arguments) {
        switch (functionName) {
            case "playCard":
                int cardIndex = arguments.getInt("index");
                int cardTargetIndex = arguments.getInt("target");

                AbstractCard card = AbstractDungeon.player.hand.group.get(cardIndex);

                ArrayList<AbstractCreature> cardTargets = new ArrayList<>();
                cardTargets.add(AbstractDungeon.player);
                cardTargets.addAll(AbstractDungeon.getMonsters().monsters);
                AbstractCreature cardTarget = cardTargets.get(cardTargetIndex);

                if (cardTarget == AbstractDungeon.player) {
                    cardTarget = cardTargets.get(1);
                }
                addToBot(new AIUseCardAction(card, cardTarget));
                break;
            case "endTurn":
                boolean suicide = arguments.getBoolean("suicide");
                if (suicide) {
                    addToBot(new LoseHPAction(AbstractDungeon.player, AbstractDungeon.player, 99999));
                } else {
                    if (EnergyPanel.getCurrentEnergy() > 0 && !AbstractDungeon.player.hasRelic("Ice Cream")) {
                        addTip("游戏", "如果没有相关的遗物或者效果,能量和手牌就不会保留到下个回合");
                    }
                    addToBot(new AIEndTurnAction());
                }
                break;
            case "usePotion":
                int potionIndex = arguments.getInt("index");
                int potionTargetIndex = arguments.getInt("target");

                AbstractPotion potion = AbstractDungeon.player.potions.get(potionIndex);

                ArrayList<AbstractCreature> potionTargets = new ArrayList<>();
                potionTargets.add(AbstractDungeon.player);
                potionTargets.addAll(AbstractDungeon.getMonsters().monsters);

                AbstractCreature potionTarget = potionTargets.get(potionTargetIndex);

                addToBot(new AIUsePotionAction(potion, potionTarget));
                break;
            case "select":
                JSONArray selectIndexes = arguments.getJSONArray("Indexes");

                if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.GRID) {
                    for (int i = 0; i < selectIndexes.length(); i++) {
                        int selectIndex = selectIndexes.getInt(i);
                        AbstractCard selectedCard = AbstractDungeon.gridSelectScreen.targetGroup.group.get(selectIndex);
                        selectedCard.hb.clicked = true;
                        lockedHoveredHitbox = selectedCard.hb;
                    }
                    AbstractDungeon.gridSelectScreen.confirmButton.hb.clicked = true;
                    break;
                } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.HAND_SELECT) {
                    for (int i = 0; i < selectIndexes.length(); i++) {
                        int selectIndex = selectIndexes.getInt(i);
                        AbstractCard selectedCard = AbstractDungeon.player.hand.group.get(selectIndex);
                        AbstractDungeon.player.hand.removeCard(selectedCard);
                        AbstractDungeon.player.hand.refreshHandLayout();
                        AbstractDungeon.handCardSelectScreen.selectedCards.addToBottom(selectedCard);
                    }
                    AbstractDungeon.handCardSelectScreen.button.hb.clicked = true;
                    break;
                } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.CARD_REWARD) {
                    AbstractCard selectedCard = AbstractDungeon.cardRewardScreen.rewardGroup.get(selectIndexes.getInt(0));
                    selectedCard.hb.clicked = true;
                    lockedHoveredHitbox = selectedCard.hb;
                    break;
                } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) {
                    if (!selectIndexes.isEmpty()) {
                        Hitbox hb = AbstractDungeon.bossRelicScreen.relics.get(selectIndexes.getInt(0)).hb;
                        hb.clicked = true;
                        lockedHoveredHitbox = hb;
                    }
                    pressProceedButton();
                } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.COMBAT_REWARD) {
                    ArrayList<RewardItem> rewards = new ArrayList<>(AbstractDungeon.combatRewardScreen.rewards);
                    for (int i = 0; i < selectIndexes.length(); i++) {
                        int selectIndex = selectIndexes.getInt(i);
                        RewardItem selectedReward = rewards.get(selectIndex);
                        selectedReward.hb.clicked = true;
                        lockedHoveredHitbox = selectedReward.hb;
                        int fuckNum = 0;
                        while (true) {
                            if (fuckNum > 10 && lockedHoveredHitbox == null) {
                                break;
                            } else {
                                if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.COMBAT_REWARD) {
                                    fuckNum++;
                                } else {
                                    fuckNum = 0;
                                }
                                logger.info("等待{}", AbstractDungeon.screen);
                            }
                        }
                    }
                    pressProceedButton();
                    break;
                } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP) {
                    ArrayList<ArrayList<MapRoomNode>> mapPaths = getMapPaths(AbstractDungeon.getCurrMapNode());
                    ArrayList<MapRoomNode> mapPath = mapPaths.get(selectIndexes.getInt(0));
                    MapRoomNode mapNode = mapPath.get(0);
                    mapNode.hb.clicked = true;
                    lockedHoveredHitbox = mapNode.hb;
                    AbstractDungeon.dungeonMapScreen.clicked = true;
                    InputHelper.justClickedLeft = true;
                    break;
                } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NONE) {
                    String roomName = AbstractDungeon.getCurrRoom().getClass().getSimpleName();
                    switch (roomName) {
                        case "RestRoom":
                            if (!selectIndexes.isEmpty()) {
                                int selectIndex = selectIndexes.getInt(0);
                                AbstractCampfireOption selectedOption = SelectCampfirePatch.buttons.get(selectIndex);
                                selectedOption.hb.clicked = true;
                                pressProceedButton();
                            } else {
                                pressProceedButton();
                            }
                            break;
                    }
                }
                break;
            case "boolean":
                boolean select = arguments.getBoolean("boolean");
                AbstractRoom room = AbstractDungeon.getCurrRoom();
                String roomName = room.getClass().getSimpleName();
                switch (roomName) {
                    case "TreasureRoomBoss":
                    case "TreasureRoom":
                        if (select) {
                            AbstractChest chest = ((TreasureRoom) room).chest;
                            chest.isOpen = true;
                            chest.open(false);
                        } else {
                            pressProceedButton();
                        }
                        break;
                }
                break;
            default:
                break;
        }
    }

    /**
     * 获取AI解说内容
     * @param actionInfo 行动信息
     */
    public static void getCommentary(JSONObject actionInfo) {
        Thread thread = new Thread(() -> {
            try {
                String commentary = callCommentaryAPI(actionInfo);
                if (commentary != null && !commentary.trim().isEmpty()) {
                    // 使用CommentaryUtils显示解说（Effect方式）
                    CommentaryUtils.showCommentary(commentary);
                }
            } catch (Exception e) {
                logger.error("获取解说失败", e);
            }
        });
        thread.start();
    }

    /**
     * 调用AI解说API
     * @param actionInfo 行动信息
     * @return 解说内容
     */
    private static String callCommentaryAPI(JSONObject actionInfo) throws IOException {
        JSONObject requestBody = new JSONObject();
        
        try {
            requestBody.put("model", model);
            
            // 构建解说提示词
            String prompt = buildCommentaryPrompt(actionInfo);
            
            JSONArray messages = new JSONArray();
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是游戏主播，用30字左右的主播口吻解说打牌，要有实际内容，比如：噶人们，看这波打击打！直接秒杀！");
            messages.put(systemMessage);
            
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);
            
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 100);
            requestBody.put("temperature", 0.8);
            
        } catch (Exception e) {
            logger.error("构建解说请求失败", e);
            return "精彩的行动！";
        }
        
        logger.info("请求AI解说...");
        
        // 创建连接
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);
        
        // 设置超时时间（从配置中读取，单位：毫秒）
        int timeoutMs = aislayer.panels.ConfigPanel.commentaryTimeout * 1000;
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        
        // 发送请求
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        } catch (IOException e) {
            logger.error("发送解说请求失败", e);
            return "精彩的行动！";
        }
        
        // 获取响应
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    JSONObject responseJSON = new JSONObject(response.toString());
                    return responseJSON
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim();
                } catch (Exception e) {
                    logger.error("解析解说响应失败", e);
                    return "精彩的行动！";
                }
            } else {
                logger.error("解说API调用失败，响应码: " + responseCode);
                return "精彩的行动！";
            }
        } catch (SocketTimeoutException e) {
            logger.info("API调用超时，使用默认解说");
            return "精彩的行动！";
        } catch (Exception e) {
            logger.error("获取解说响应时发生错误", e);
            return "精彩的行动！";
        }
    }

    /**
     * 构建解说提示词
     * @param actionInfo 行动信息
     * @return 提示词
     */
    private static String buildCommentaryPrompt(JSONObject actionInfo) {
        StringBuilder prompt = new StringBuilder();
        
        String actionType = actionInfo.getString("行动类型");
        
        // 获取战斗状态信息
        BattleStateTracker tracker = BattleStateTracker.getInstance();
        TurnData currentTurn = tracker.getCurrentTurnData();
        
        // 根据行动类型构建不同的提示词
        if ("怪物介绍".equals(actionType)) {
            // 怪物介绍的特殊处理
            prompt.append("战斗开始了！前方出现了");
            if (actionInfo.has("怪物信息")) {
                prompt.append(actionInfo.getString("怪物信息"));
            }
            prompt.append("。噶人们，准备迎接挑战！请用主播口吻介绍这个敌人：");
        } else if ("打牌".equals(actionType)) {
            // 打牌行动的详细解说
            prompt.append("我打出了");
            if (actionInfo.has("使用的卡牌")) {
                JSONObject cardInfo = actionInfo.getJSONObject("使用的卡牌");
                prompt.append(cardInfo.getString("名称"));
                if (cardInfo.has("描述")) {
                    prompt.append("（").append(cardInfo.getString("描述")).append("）");
                }
            }
            
            // 目标信息
            if (actionInfo.has("目标")) {
                prompt.append("攻击").append(actionInfo.getString("目标"));
            }
            prompt.append("。");
            
            // 添加战斗状态信息
            if (currentTurn != null) {
                prompt.append("这是本回合第").append(currentTurn.getPlayedCardsCount()).append("张牌。");
                
                // 添加怪物意图信息
                if (actionInfo.has("怪物意图")) {
                    prompt.append("怪物意图：").append(actionInfo.getString("怪物意图")).append("。");
                }
                
                // 添加玩家状态信息
                if (AbstractDungeon.player != null) {
                    prompt.append("我还有").append(AbstractDungeon.player.energy.energy).append("点能量，")
                          .append(AbstractDungeon.player.currentHealth).append("/").append(AbstractDungeon.player.maxHealth)
                          .append("血量。");
                }
            }
            
            // 明确要求解说动机
            prompt.append("噶人们，看这波操作！请用主播口吻解说我为什么这样打：");
        } else if ("结束回合".equals(actionType)) {
            // 回合结束的解说
            prompt.append("回合结束了！");
            
            // 添加回合总结信息
            if (currentTurn != null) {
                prompt.append("本回合我打了").append(currentTurn.getPlayedCardsCount()).append("张牌。");
                
                // 添加玩家状态变化
                if (AbstractDungeon.player != null) {
                    prompt.append("当前状态：").append(AbstractDungeon.player.currentHealth).append("/")
                          .append(AbstractDungeon.player.maxHealth).append("血量，")
                          .append(AbstractDungeon.player.energy.energy).append("点能量。");
                }
                
                // 添加怪物状态信息
                Map<String, BattleStateTracker.MonsterState> monsters = tracker.getMonsterStates();
                if (!monsters.isEmpty()) {
                    prompt.append("敌人状态：");
                    for (BattleStateTracker.MonsterState monster : monsters.values()) {
                        if (!monster.isDead && !monster.isDying) {
                            prompt.append(monster.monsterName).append("(").append(monster.currentHealth)
                                  .append("/").append(monster.maxHealth).append("HP) ");
                        }
                    }
                }
            }
            
            prompt.append("噶人们，这回合打得怎么样？请用主播口吻总结：");
        } else {
            // 其他行动类型的简化处理
            prompt.append("噶人们，看这波").append(actionType).append("操作！请用主播口吻解说：");
        }
        
        return prompt.toString();
    }

    public static void pressProceedButton() {
        try {
            Field hbField = AbstractDungeon.overlayMenu.proceedButton.getClass().getDeclaredField("hb");
            hbField.setAccessible(true);
            Hitbox hb = (Hitbox) hbField.get(AbstractDungeon.overlayMenu.proceedButton);
            AbstractDungeon.overlayMenu.proceedButton.show();
            hb.clicked = true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("无法访问 proceedButton 的 hb 字段", e);
        }
    }

    private static String stringify(Object info) {
        return info.toString()
                .replace("\\", "")
                .replace("\"", "");
    }

    private static void addTip(String type, String tip) {
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "[" + type + "提示]: " + tip);
        messagesArray.put(systemMessage);

        logger.info(systemMessage.get("content"));
    }

    private static void addToBot(AbstractGameAction action) {
        AbstractDungeon.actionManager.addToBottom(action);
    }

    /**
     *
     * @param apiKey API密钥
     * @param apiUrl API地址
     * @param model 模型名称
     * @param info 游戏信息
     * @return 生成的内容，异常返回null
     */
    private static JSONObject getTool(String apiKey, String apiUrl, String model, String info) {
        try {
            return callChatAPI(apiKey, apiUrl, model, info);
        } catch (Exception e) {
            return new JSONObject().put("error", e.getMessage());
        }
    }

    /**
     * 调用AI聊天API
     */
    private static JSONObject callChatAPI(String apiKey, String apiUrl, String model, String info)
            throws IOException {
        JSONObject requestBody = new JSONObject();
        try{

            requestBody.put("model", model);

            JSONObject unknownPotions = getUnknownPotions();
            if (!unknownPotions.isEmpty()) {
                addTip("药水", stringify(unknownPotions));
            }

            JSONObject unknownRelics = getUnknownRelics();
            if (!unknownRelics.isEmpty()) {
                addTip("遗物", stringify(unknownRelics));
            }

            JSONArray unknownCards = getUnknownCards();
            if (!unknownCards.isEmpty()) {
                addTip("卡牌", stringify(unknownCards));
            }

            JSONObject unknownKeywords = new JSONObject();
            while (!allDescriptions.isEmpty()) {
                JSONObject unknownKeywordsTemp = getUnknownKeywords();
                for (String keyword : unknownKeywordsTemp.keySet()) {
                    unknownKeywords.put(keyword, unknownKeywordsTemp.getString(keyword));
                }
            }
            if (!unknownKeywords.isEmpty()) {
                addTip("关键词", stringify(unknownKeywords));
            }

            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", "[当前信息]: " + info);
            messagesArray.put(message);

            logger.info(message.get("content"));

            requestBody.put("messages", messagesArray);

            JSONArray toolsArray = new JSONArray();

            JSONObject toolPlayCard = new JSONObject();
            toolPlayCard.put("type", "function");
            JSONObject funcPlayCard = funcPlayCard();
            toolPlayCard.put("function", funcPlayCard);
            toolsArray.put(toolPlayCard);

            JSONObject toolEndTurn = new JSONObject();
            toolEndTurn.put("type", "function");
            JSONObject funcEndTurn = funcEndTurn();
            toolEndTurn.put("function", funcEndTurn);
            toolsArray.put(toolEndTurn);

            JSONObject toolUsePotion = new JSONObject();
            toolUsePotion.put("type", "function");
            JSONObject funcUsePotion = funcUsePotion();
            toolUsePotion.put("function", funcUsePotion);
            toolsArray.put(toolUsePotion);

            JSONObject toolSelect = new JSONObject();
            toolSelect.put("type", "function");
            JSONObject funcSelect = funcSelect();
            toolSelect.put("function", funcSelect);
            toolsArray.put(toolSelect);

            JSONObject toolBoolean = new JSONObject();
            toolBoolean.put("type", "function");
            JSONObject funcBoolean = funcBoolean();
            toolBoolean.put("function", funcBoolean);
            toolsArray.put(toolBoolean);

            requestBody.put("tools", toolsArray);
        } catch (Exception e){
            return new JSONObject().put("error", e.getMessage());
        }

        logger.info("AI思考中...");
        
        // 创建连接
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);

        // 发送请求
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        } catch (IOException e) {
            return new JSONObject().put("error", e.getMessage());
        }

        // 获取响应
        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject responseJSON = new JSONObject(response.toString());
                return responseJSON
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getJSONArray("tool_calls")
                        .getJSONObject(0);
            } catch (Exception e){
                return new JSONObject().put("error", e.getMessage());
            }
        } else {
            try (
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(
                                    connection.getErrorStream(),
                                    StandardCharsets.UTF_8
                            )
                    )
            ) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                throw new IOException("\nAPI调用失败，响应：" + response);
            }
        }
    }

    private static JSONObject getUnknownRelics() {
        ArrayList<AbstractRelic> allRelics = getAllRelics();
        JSONObject unknownRelics = new JSONObject();
        for (AbstractRelic relic : allRelics) {
            if (!knownRelics.contains(relic.name)) {
                knownRelics.add(relic.name);
                unknownRelics.put(relic.name, handleDescription(relic.description));
            }
        }
        return unknownRelics;
    }

    private static ArrayList<AbstractRelic> getAllRelics() {
        return allRelics;
    }

    private static JSONObject getUnknownPotions() {
        ArrayList<AbstractPotion> allPotions = getAllPotions();
        JSONObject unknownPotions = new JSONObject();
        for (AbstractPotion potion : allPotions) {
            if (!knownPotions.contains(potion.name)) {
                knownPotions.add(potion.name);
                unknownPotions.put(potion.name, handleDescription(potion.description));
            }
        }
        return unknownPotions;
    }

    private static ArrayList<AbstractPotion> getAllPotions() {
        return allPotions;
    }

    private static JSONObject getUnknownKeywords() {
        JSONObject allKeywords = getAllKeywords();
        JSONObject unknownKeywords = new JSONObject();
        for (String keywordString : allKeywords.keySet()) {
            if (!knownKeywords.contains(keywordString)) {
                knownKeywords.add(keywordString);
                unknownKeywords.put(keywordString, handleDescription(allKeywords.getString(keywordString)));
            }
        }
        return unknownKeywords;
    }

    private static JSONObject getAllKeywords() {
        JSONObject allKeywords = new JSONObject();
        JSONObject keywords = new JSONObject(GameDictionary.keywords);
        for (String keywordString : keywords.keySet()) {
            if (allDescriptions.toString().contains(keywordString)) {
                allKeywords.put(keywordString, keywords.getString(keywordString));
            }
        }
        allDescriptions.clear();
        return allKeywords;
    }

    private static JSONArray getUnknownCards() {
        ArrayList<AbstractCard> allCards = getAllCards();
        JSONArray unknownCards = new JSONArray();
        for (AbstractCard card : allCards) {
            if (!knownCards.contains(card.name)) {
                unknownCards.put(getCardInfo(card));
                knownCards.add(card.name);
            }
        }
        return unknownCards;
    }

    private static ArrayList<AbstractCard> getAllCards() {
        return allCards;
    }

    private static JSONObject funcSelect() {
        JSONObject funcSelect = new JSONObject();
        funcSelect.put("name", "select");
        funcSelect.put("description", "从要求的列表中选择一组物品(卡牌、遗物等等),用于弃牌、升级或者拿取等等");

        JSONObject parameters = new JSONObject();
        parameters.put("type", "object");
        JSONObject properties = new JSONObject();

        addParameter(
                properties,
                "Indexes",
                "array",
                "你选择的物品在列表中的序号,序号从0开始"
        );

        addParameter(
                properties,
                "reason",
                "string",
                "你选择这组物品的理由,一句简单幽默的话陈述"
        );

        parameters.put("properties", properties);
        parameters.put("required", new JSONArray().put("Indexes").put("reason"));

        funcSelect.put("parameters", parameters);
        return funcSelect;
    }

    private static JSONObject funcBoolean() {
        JSONObject funcBoolean = new JSONObject();
        funcBoolean.put("name", "boolean");
        funcBoolean.put("description", "选择是或否，在明确询问是否时才用，用于确定是否打开宝箱等等");

        JSONObject parameters = new JSONObject();
        parameters.put("type", "object");
        JSONObject properties = new JSONObject();

        addParameter(
                properties,
                "boolean",
                "boolean",
                "你选择的布尔值"
        );

        addParameter(
                properties,
                "reason",
                "string",
                "你选择这个布尔值的理由,一句简单幽默的话陈述"
        );

        parameters.put("properties", properties);
        parameters.put("required", new JSONArray().put("boolean").put("reason"));

        funcBoolean.put("parameters", parameters);
        return funcBoolean;
    }

    private static JSONObject funcUsePotion() {
        JSONObject funcUsePotion = new JSONObject();
        funcUsePotion.put("name", "usePotion");
        funcUsePotion.put("description", "使用一瓶药水,药水使用后会消耗");

        JSONObject parameters = new JSONObject();
        parameters.put("type", "object");
        JSONObject properties = new JSONObject();

        addParameter(
                properties,
                "index",
                "integer",
                "这瓶药水的序号(包括药水栏),序号从0开始"
        );
        addParameter(
                properties,
                "target",
                "integer",
                "这瓶药水的目标生物的序号，如果是针对全部敌人就选择随便一个敌人"
        );
        addParameter(
                properties,
                "reason",
                "string",
                "你选择这瓶药水和目标的理由,一句简单幽默的话陈述"
        );

        parameters.put("properties", properties);
        parameters.put("required", new JSONArray().put("index").put("target").put("reason"));

        funcUsePotion.put("parameters", parameters);
        return funcUsePotion;
    }

    private static JSONObject funcEndTurn() {
        JSONObject funcPlayCard = new JSONObject();
        funcPlayCard.put("name", "endTurn");
        funcPlayCard.put("description", "结束当前回合，只能战斗中使用，结束后手牌会被弃掉，能量也不会保留");

        JSONObject parameters = new JSONObject();
        parameters.put("type", "object");
        JSONObject properties = new JSONObject();

        addParameter(
                properties,
                "suicide",
                "boolean",
                "如果你觉得你已经没有任何机会了，那就大喊一声'烦死人了！'，然后自杀放弃吧"
        );
        addParameter(
                properties,
                "reason",
                "string",
                "你选择结束这个回合的理由,一句简单幽默的话陈述"
        );

        parameters.put("properties", properties);
        parameters.put("required", new JSONArray().put("suicide").put("reason"));

        funcPlayCard.put("parameters", parameters);
        return funcPlayCard;
    }

    private static JSONObject funcPlayCard() {
        JSONObject funcPlayCard = new JSONObject();
        funcPlayCard.put("name", "playCard");
        funcPlayCard.put("description", "选择一张手牌打出，只能战斗中使用，会消耗能量，能量小于能耗时禁止打出这张牌，这张牌的效果可能会受敌我的效果影响，你需要根据效果计算");

        JSONObject parameters = new JSONObject();
        parameters.put("type", "object");
        JSONObject properties = new JSONObject();

        addParameter(
                properties,
                "index",
                "integer",
                "这张牌在手牌中的序号,序号从0开始"
        );
        addParameter(
                properties,
                "target",
                "integer",
                "这张牌的目标生物的序号，如果是针对全部敌人就选择随便一个敌人"
        );
        addParameter(
                properties,
                "reason",
                "string",
                "你选择这张牌和目标的理由,一句简单幽默的话陈述"
        );

        parameters.put("properties", properties);
        parameters.put("required", new JSONArray().put("index").put("target").put("reason"));

        funcPlayCard.put("parameters", parameters);
        return funcPlayCard;
    }

    private static void addParameter(JSONObject parameters, String name, String type, String description) {
        JSONObject param = new JSONObject();
        param.put("type", type);
        param.put("description", description);
        parameters.put(name, param);
    }

}