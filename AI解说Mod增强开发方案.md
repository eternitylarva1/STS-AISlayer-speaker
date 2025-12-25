# AI解说Mod增强开发方案

## 项目概述

基于现有的AI爬塔解说Mod，实现以下两个核心功能增强：

1. **战斗开始时怪物介绍**：替换"让俺寻思寻思"等thinking文本，改为介绍场上的怪物信息
2. **解说频率控制**：添加按牌数解说和回合结束解说两种模式

## 需求分析

### 需求1：战斗开始时怪物介绍
- 当前在`PlayFirstCardPatch.java`中显示thinking文本
- 需要改为调用AI API生成怪物介绍解说
- 介绍内容应包括怪物名称、血量、威胁等级、应对建议

### 需求2：解说频率控制
- **按牌数解说模式**：记录本回合打出的牌，达到设定数量时触发解说
- **回合结束解说模式**：在回合结束时收集完整回合数据，生成总结解说
- 需要在ConfigPanel中添加相关配置选项

## 技术架构设计

### 核心组件

```
解说增强架构
├── 配置管理层 (ConfigPanel扩展)
├── 数据跟踪层 (TurnData + BattleStateTracker)
├── 事件监听层 (Patch文件修改)
├── 解说处理层 (CommentaryUtils增强)
└── AI接口层 (AISlayer + AIUtils扩展)
```

### 数据流设计

```
游戏事件 → Patch监听 → 数据收集 → 状态跟踪 → 解说触发 → AI调用 → 内容显示
```

## 详细实现方案

### 第一阶段：配置系统扩展

#### 1.1 修改ConfigPanel.java
```java
// 解说模式：0=按牌数解说，1=回合结束解说
public static int commentaryMode = 0; 
// 每次解说需要的牌数（按牌数模式）
public static int cardsPerCommentary = 3;
// 是否在战斗开始时介绍怪物
public static boolean introduceMonsters = true;
// 怪物介绍详细程度：0=简单，1=详细
public static int monsterIntroDetail = 1;
```

#### 1.2 更新本地化文件
在`src/main/resources/aislayerResources/localization/zhs/ui.json`中添加：
```json
"commentaryMode": "解说模式",
"cardsPerCommentary": "每几张牌解说一次",
"introduceMonsters": "战斗开始时介绍怪物",
"monsterIntroDetail": "怪物介绍详细程度"
```

在`text.json`中添加怪物介绍相关文本：
```json
"monsterIntro": [
    "面对{monsterName}，血量{hp}，{threat}！",
    "小心{monsterName}({hp})，它准备{intent}！",
    "遭遇{monsterName}，建议{strategy}！"
]
```

### 第二阶段：数据跟踪系统

#### 2.1 创建TurnData.java
```java
package aislayer.utils;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import java.util.ArrayList;

public class TurnData {
    public int turnNumber;
    public ArrayList<AbstractCard> playedCards;
    public ArrayList<AbstractMonster> cardTargets;
    public int startHealth;
    public int endHealth;
    public int startBlock;
    public int endBlock;
    public int startEnergy;
    public int energyUsed;
    public int damageReceived;
    public int damageDealt;
    public ArrayList<String> monsterIntents;
    public ArrayList<String> gainedPowers;
    public ArrayList<String> lostPowers;
    public long startTime;
    public long endTime;
    
    public TurnData(int turnNumber) {
        this.turnNumber = turnNumber;
        this.playedCards = new ArrayList<>();
        this.cardTargets = new ArrayList<>();
        this.monsterIntents = new ArrayList<>();
        this.gainedPowers = new ArrayList<>();
        this.lostPowers = new ArrayList<>();
        this.startTime = System.currentTimeMillis();
    }
    
    public void recordCardPlay(AbstractCard card, AbstractMonster target) {
        playedCards.add(card);
        cardTargets.add(target);
    }
    
    public void finalizeTurn() {
        this.endTime = System.currentTimeMillis();
    }
}
```

#### 2.2 创建BattleStateTracker.java
```java
package aislayer.utils;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class BattleStateTracker {
    private static final Logger logger = LogManager.getLogger(BattleStateTracker.class.getName());
    
    private static TurnData currentTurn;
    private static ArrayList<TurnData> turnHistory;
    private static int battleStartHealth;
    private static int battleStartBlock;
    
    public static void initializeBattle() {
        turnHistory = new ArrayList<>();
        battleStartHealth = AbstractDungeon.player.currentHealth;
        battleStartBlock = AbstractDungeon.player.currentBlock;
        startTurn(1);
    }
    
    public static void startTurn(int turnNumber) {
        if (currentTurn != null) {
            currentTurn.finalizeTurn();
            turnHistory.add(currentTurn);
        }
        
        currentTurn = new TurnData(turnNumber);
        currentTurn.startHealth = AbstractDungeon.player.currentHealth;
        currentTurn.startBlock = AbstractDungeon.player.currentBlock;
        currentTurn.startEnergy = EnergyPanel.getCurrentEnergy();
        
        // 记录怪物意图
        recordMonsterIntents();
    }
    
    public static void recordCardPlay(AbstractCard card, AbstractMonster target) {
        if (currentTurn != null) {
            currentTurn.recordCardPlay(card, target);
            currentTurn.energyUsed += card.costForTurn;
        }
    }
    
    public static void recordDamageReceived(int damage) {
        if (currentTurn != null) {
            currentTurn.damageReceived += damage;
        }
    }
    
    public static void recordDamageDealt(int damage) {
        if (currentTurn != null) {
            currentTurn.damageDealt += damage;
        }
    }
    
    public static void recordPowerChange(AbstractPower power, boolean gained) {
        if (currentTurn != null) {
            if (gained) {
                currentTurn.gainedPowers.add(power.name);
            } else {
                currentTurn.lostPowers.add(power.name);
            }
        }
    }
    
    public static void endTurn() {
        if (currentTurn != null) {
            currentTurn.endHealth = AbstractDungeon.player.currentHealth;
            currentTurn.endBlock = AbstractDungeon.player.currentBlock;
            currentTurn.finalizeTurn();
            
            // 计算本回合受到的伤害
            int healthChange = currentTurn.startHealth - currentTurn.endHealth;
            int blockChange = currentTurn.startBlock - currentTurn.endBlock;
            currentTurn.damageReceived = Math.max(0, healthChange + blockChange);
            
            turnHistory.add(currentTurn);
            logger.info("回合 " + currentTurn.turnNumber + " 结束，打出 " + 
                       currentTurn.playedCards.size() + " 张牌");
        }
    }
    
    private static void recordMonsterIntents() {
        if (AbstractDungeon.getCurrRoom().monsters != null) {
            currentTurn.monsterIntents.clear();
            for (AbstractMonster monster : AbstractDungeon.getCurrRoom().monsters.monsters) {
                if (!monster.isDeadOrEscaped()) {
                    String intent = getMonsterIntentDescription(monster);
                    currentTurn.monsterIntents.add(intent);
                }
            }
        }
    }
    
    private static String getMonsterIntentDescription(AbstractMonster monster) {
        String intent = monster.intent.toString();
        int damage = monster.getIntentDmg();
        
        switch (intent) {
            case "ATTACK":
                return monster.name + " 将造成 " + damage + " 点伤害";
            case "ATTACK_BUFF":
                return monster.name + " 将造成 " + damage + " 点伤害并获得强化";
            case "ATTACK_DEBUFF":
                return monster.name + " 将造成 " + damage + " 点伤害并施加负面效果";
            case "DEFEND":
                return monster.name + " 将获得格挡";
            case "BUFF":
                return monster.name + " 将获得强化效果";
            case "DEBUFF":
                return monster.name + " 将施加负面效果";
            default:
                return monster.name + " 准备行动";
        }
    }
    
    public static TurnData getCurrentTurn() {
        return currentTurn;
    }
    
    public static ArrayList<TurnData> getTurnHistory() {
        return new ArrayList<>(turnHistory);
    }
    
    public static int getCardsPlayedThisTurn() {
        return currentTurn != null ? currentTurn.playedCards.size() : 0;
    }
    
    public static void reset() {
        currentTurn = null;
        turnHistory = null;
    }
}
```

### 第三阶段：解说逻辑重构

#### 3.1 修改PlayFirstCardPatch.java
```java
package aislayer.patchs;

import aislayer.AISlayer;
import aislayer.utils.AIUtils;
import aislayer.utils.BattleStateTracker;
import aislayer.panels.ConfigPanel;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.actions.animations.TalkAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import org.json.JSONObject;

import java.io.File;

import static aislayer.AISlayer.getInfo;
import static aislayer.AISlayer.isAIStart;

@SpirePatch(
        clz = AbstractMonster.class,
        method = "update"
)
public class PlayFirstCardPatch {

    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (
                __instance.intent != AbstractMonster.Intent.DEBUG
                        && AbstractDungeon.actionManager.phase == GameActionManager.Phase.WAITING_ON_USER
                        && AbstractDungeon.overlayMenu.endTurnButton.enabled
                        && !AISlayer.intentUpdated
                        && isAIStart()
        ) {
            AISlayer.intentUpdated = true;

            // 初始化战斗状态跟踪
            BattleStateTracker.initializeBattle();

            // 如果启用了怪物介绍，生成怪物介绍解说
            if (ConfigPanel.introduceMonsters) {
                generateMonsterIntroduction();
            }
        }
    }
    
    private static void generateMonsterIntroduction() {
        Thread thread = new Thread(() -> {
            try {
                JSONObject monsterInfo = AISlayer.getMonsterIntro();
                String commentary = AIUtils.getMonsterCommentary(monsterInfo);
                if (commentary != null && !commentary.trim().isEmpty()) {
                    AbstractDungeon.actionManager.addToBottom(new TalkAction(true, commentary, 4.0F, 4.0F));
                }
            } catch (Exception e) {
                // 降级处理：显示默认怪物介绍
                String fallbackIntro = generateFallbackMonsterIntro();
                AbstractDungeon.actionManager.addToBottom(new TalkAction(true, fallbackIntro, 4.0F, 4.0F));
            }
        });
        thread.start();
    }
    
    private static String generateFallbackMonsterIntro() {
        StringBuilder intro = new StringBuilder("面对");
        if (AbstractDungeon.getCurrRoom().monsters != null) {
            int monsterCount = 0;
            for (AbstractMonster monster : AbstractDungeon.getCurrRoom().monsters.monsters) {
                if (!monster.isDeadOrEscaped()) {
                    if (monsterCount > 0) {
                        intro.append("和");
                    }
                    intro.append(monster.name);
                    monsterCount++;
                }
            }
            intro.append("，小心应对！");
        }
        return intro.toString();
    }
}
```

#### 3.2 修改PlayerActionPatch.java
```java
package aislayer.patchs;

import aislayer.utils.BattleStateTracker;
import aislayer.utils.CommentaryUtils;
import aislayer.panels.ConfigPanel;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

@SpirePatch(
        clz = AbstractPlayer.class,
        method = "useCard",
        paramtypez = {AbstractCard.class, AbstractMonster.class, int.class},
        optional = true
)
public class PlayerActionPatch {

    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance, AbstractCard card, AbstractMonster target, int energyOnUse) {
        // 只在战斗中触发
        if (!isInCombat()) {
            return;
        }
        
        // 检查卡牌是否成功使用
        if (!wasCardSuccessfullyUsed(card, target)) {
            return;
        }
        
        try {
            // 记录卡牌使用
            BattleStateTracker.recordCardPlay(card, target);
            
            // 根据解说模式决定是否立即触发解说
            if (ConfigPanel.commentaryMode == 0) { // 按牌数解说模式
                checkAndTriggerCardBasedCommentary(card, target);
            } else { // 回合结束解说模式，不立即触发
                // 可以选择显示简短的即时反馈
                showImmediateFeedback(card, target);
            }
        } catch (Exception e) {
            // 静默处理异常
        }
    }
    
    private static void checkAndTriggerCardBasedCommentary(AbstractCard card, AbstractMonster target) {
        int cardsPlayed = BattleStateTracker.getCardsPlayedThisTurn();
        
        if (cardsPlayed >= ConfigPanel.cardsPerCommentary) {
            // 达到解说阈值，触发解说
            TurnData turnData = BattleStateTracker.getCurrentTurn();
            CommentaryUtils.triggerCardBasedCommentary(turnData);
            
            // 重置计数（可选：或者继续累积）
            // BattleStateTracker.resetCurrentTurnCards();
        }
    }
    
    private static void showImmediateFeedback(AbstractCard card, AbstractMonster target) {
        // 在回合结束模式下，可以显示简短的即时反馈
        // 这里可以实现一个简化的反馈机制
    }
    
    private static boolean isInCombat() {
        if (AbstractDungeon.getCurrRoom() == null) {
            return false;
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        return room.phase == AbstractRoom.RoomPhase.COMBAT;
    }
    
    private static boolean wasCardSuccessfullyUsed(AbstractCard card, AbstractMonster target) {
        if (card == null || AbstractDungeon.player == null) {
            return false;
        }
        
        // 检查卡牌是否还在手牌中（已打出的牌应该不在手牌中）
        if (AbstractDungeon.player.hand.group.contains(card)) {
            return false;
        }
        
        // 对于需要目标的卡牌，检查目标是否有效
        if (card.target != AbstractCard.CardTarget.NONE && target == null) {
            return false;
        }
        
        return true;
    }
}
```

#### 3.3 修改EndTurnPatch.java
```java
package aislayer.patchs;

import aislayer.utils.BattleStateTracker;
import aislayer.utils.CommentaryUtils;
import aislayer.panels.ConfigPanel;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

@SpirePatch(
        clz = GameActionManager.class,
        method = "endTurn",
        optional = true
)
public class EndTurnPatch {

    @SpirePostfixPatch
    public static void Postfix(GameActionManager __instance) {
        // 只在战斗中触发
        if (!isInCombat()) {
            return;
        }
        
        // 检查是否是玩家主动结束回合
        if (!wasPlayerEndTurn()) {
            return;
        }
        
        try {
            // 结束当前回合的数据收集
            BattleStateTracker.endTurn();
            
            // 根据解说模式决定是否触发解说
            if (ConfigPanel.commentaryMode == 1) { // 回合结束解说模式
                TurnData turnData = BattleStateTracker.getTurnHistory()
                    .get(BattleStateTracker.getTurnHistory().size() - 1);
                CommentaryUtils.triggerTurnEndCommentary(turnData);
            }
            
            // 开始新回合
            BattleStateTracker.startTurn(GameActionManager.turn + 1);
            
        } catch (Exception e) {
            // 静默处理异常
        }
    }
    
    private static boolean isInCombat() {
        if (AbstractDungeon.getCurrRoom() == null) {
            return false;
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        return room.phase == AbstractRoom.RoomPhase.COMBAT;
    }
    
    private static boolean wasPlayerEndTurn() {
        if (AbstractDungeon.actionManager == null) {
            return false;
        }
        return AbstractDungeon.actionManager.phase == GameActionManager.Phase.WAITING_ON_USER;
    }
}
```

### 第四阶段：AI解说增强

#### 4.1 扩展AISlayer.java
```java
// 在AISlayer.java中添加以下方法

/**
 * 获取怪物介绍信息
 */
public static JSONObject getMonsterIntro() {
    JSONObject infoJson = new JSONObject();
    
    if (AbstractDungeon.getCurrRoom() == null || AbstractDungeon.getCurrRoom().monsters == null) {
        return infoJson;
    }
    
    JSONArray monstersJson = new JSONArray();
    int totalThreat = 0;
    
    for (AbstractMonster monster : AbstractDungeon.getCurrRoom().monsters.monsters) {
        if (!monster.isDeadOrEscaped()) {
            JSONObject monsterJson = new JSONObject();
            monsterJson.put("名称", monster.name);
            monsterJson.put("血量", monster.currentHealth + "/" + monster.maxHealth);
            
            // 计算威胁等级
            int threat = calculateMonsterThreat(monster);
            monsterJson.put("威胁等级", threat);
            totalThreat += threat;
            
            // 怪物意图
            String intent = getMonsterIntentDescription(monster);
            monsterJson.put("意图", intent);
            
            // 推荐策略
            String strategy = getRecommendedStrategy(monster);
            monsterJson.put("建议策略", strategy);
            
            monstersJson.put(monsterJson);
        }
    }
    
    infoJson.put("怪物", monstersJson);
    infoJson.put("总威胁等级", totalThreat);
    infoJson.put("玩家血量", AbstractDungeon.player.currentHealth + "/" + AbstractDungeon.player.maxHealth);
    infoJson.put("玩家能量", EnergyPanel.getCurrentEnergy() + "/" + AbstractDungeon.player.energy.energyMaster);
    
    return infoJson;
}

/**
 * 获取回合总结信息
 */
public static JSONObject getTurnSummary(TurnData turnData) {
    JSONObject summaryJson = new JSONObject();
    
    summaryJson.put("回合数", turnData.turnNumber);
    summaryJson.put("打出牌数", turnData.playedCards.size());
    
    // 打出的牌
    JSONArray cardsJson = new JSONArray();
    for (int i = 0; i < turnData.playedCards.size(); i++) {
        AbstractCard card = turnData.playedCards.get(i);
        AbstractMonster target = turnData.cardTargets.get(i);
        
        JSONObject cardJson = new JSONObject();
        cardJson.put("名称", card.name);
        cardJson.put("类型", getType(card.type));
        cardJson.put("能耗", card.costForTurn);
        if (target != null) {
            cardJson.put("目标", target.name);
        }
        cardsJson.put(cardJson);
    }
    summaryJson.put("打出的牌", cardsJson);
    
    // 状态变化
    summaryJson.put("开始血量", turnData.startHealth);
    summaryJson.put("结束血量", turnData.endHealth);
    summaryJson.put("血量变化", turnData.endHealth - turnData.startHealth);
    
    summaryJson.put("开始护甲", turnData.startBlock);
    summaryJson.put("结束护甲", turnData.endBlock);
    summaryJson.put("护甲变化", turnData.endBlock - turnData.startBlock);
    
    summaryJson.put("使用能量", turnData.energyUsed);
    summaryJson.put("受到伤害", turnData.damageReceived);
    summaryJson.put("造成伤害", turnData.damageDealt);
    
    // 怪物意图
    JSONArray intentsJson = new JSONArray();
    for (String intent : turnData.monsterIntents) {
        intentsJson.put(intent);
    }
    summaryJson.put("怪物意图", intentsJson);
    
    // 能力变化
    if (!turnData.gainedPowers.isEmpty()) {
        JSONArray gainedJson = new JSONArray();
        for (String power : turnData.gainedPowers) {
            gainedJson.put(power);
        }
        summaryJson.put("获得能力", gainedJson);
    }
    
    if (!turnData.lostPowers.isEmpty()) {
        JSONArray lostJson = new JSONArray();
        for (String power : turnData.lostPowers) {
            lostJson.put(power);
        }
        summaryJson.put("失去能力", lostJson);
    }
    
    return summaryJson;
}

private static int calculateMonsterThreat(AbstractMonster monster) {
    int threat = 0;
    
    // 基于血量的威胁
    double hpRatio = (double) monster.currentHealth / monster.maxHealth;
    threat += (int) (50 * (1 - hpRatio));
    
    // 基于意图的威胁
    if (monster.intent.toString().contains("ATTACK")) {
        threat += monster.getIntentDmg() * 2;
    }
    
    // 基于怪物类型的威胁
    if (monster.type == AbstractMonster.EnemyType.ELITE) {
        threat += 30;
    } else if (monster.type == AbstractMonster.EnemyType.BOSS) {
        threat += 50;
    }
    
    return Math.min(100, threat);
}

private static String getMonsterIntentDescription(AbstractMonster monster) {
    String intent = monster.intent.toString();
    int damage = monster.getIntentDmg();
    
    switch (intent) {
        case "ATTACK":
            return "将造成" + damage + "点伤害";
        case "ATTACK_BUFF":
            return "将造成" + damage + "点伤害并获得强化";
        case "ATTACK_DEBUFF":
            return "将造成" + damage + "点伤害并施加负面效果";
        case "DEFEND":
            return "将获得格挡";
        case "BUFF":
            return "将获得强化效果";
        case "DEBUFF":
            return "将施加负面效果";
        default:
            return "准备行动";
    }
}

private static String getRecommendedStrategy(AbstractMonster monster) {
    String intent = monster.intent.toString();
    
    if (intent.contains("ATTACK")) {
        return "优先获得格挡或使用防御牌";
    } else if (intent.contains("DEBUFF")) {
        return "考虑使用净化或免疫效果";
    } else if (intent.contains("BUFF")) {
        return "尽快打断或使用弱化效果";
    } else {
        return "集中火力输出";
    }
}
```

#### 4.2 扩展AIUtils.java
```java
// 在AIUtils.java中添加以下方法

/**
 * 获取怪物介绍解说
 */
public static String getMonsterCommentary(JSONObject monsterInfo) throws IOException {
    JSONObject requestBody = new JSONObject();
    
    try {
        requestBody.put("model", model);
        
        // 构建怪物介绍提示词
        String prompt = buildMonsterIntroPrompt(monsterInfo);
        
        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是游戏主播，用30字左右的主播口吻介绍怪物，要有紧张感和策略建议");
        messages.put(systemMessage);
        
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.put(userMessage);
        
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 100);
        requestBody.put("temperature", 0.8);
        
    } catch (Exception e) {
        logger.error("构建怪物介绍请求失败", e);
        return "小心前方的敌人！";
    }
    
    // 调用API（复用现有的API调用逻辑）
    return callCommentaryAPI(requestBody);
}

/**
 * 获取回合总结解说
 */
public static String getTurnSummaryCommentary(JSONObject turnSummary) throws IOException {
    JSONObject requestBody = new JSONObject();
    
    try {
        requestBody.put("model", model);
        
        // 构建回合总结提示词
        String prompt = buildTurnSummaryPrompt(turnSummary);
        
        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是游戏主播，用40字左右的主播口吻总结回合，要有战术分析和情绪表达");
        messages.put(systemMessage);
        
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.put(userMessage);
        
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 120);
        requestBody.put("temperature", 0.8);
        
    } catch (Exception e) {
        logger.error("构建回合总结请求失败", e);
        return "这回合打得不错！";
    }
    
    return callCommentaryAPI(requestBody);
}

private static String buildMonsterIntroPrompt(JSONObject monsterInfo) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("噶人们，看我们遇到了什么敌人：");
    
    JSONArray monsters = monsterInfo.getJSONArray("怪物");
    for (int i = 0; i < monsters.length(); i++) {
        JSONObject monster = monsters.getJSONObject(i);
        prompt.append(monster.getString("名称"))
              .append("(").append(monster.getString("血量")).append(")");
        
        if (monster.getInt("威胁等级") > 50) {
            prompt.append("是个硬茬！");
        }
        
        if (i < monsters.length() - 1) {
            prompt.append("，还有");
        }
    }
    
    prompt.append("。总威胁等级").append(monsterInfo.getInt("总威胁等级"));
    prompt.append("。请用主播口吻介绍这个局面：");
    
    return prompt.toString();
}

private static String buildTurnSummaryPrompt(JSONObject turnSummary) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("第").append(turnSummary.getInt("回合数")).append("回合结束了！");
    
    prompt.append("我打出了").append(turnSummary.getInt("打出牌数")).append("张牌，");
    
    int hpChange = turnSummary.getInt("血量变化");
    if (hpChange < 0) {
        prompt.append("掉了").append(Math.abs(hpChange)).append("点血，");
    } else if (hpChange > 0) {
        prompt.append("回了").append(hpChange).append("点血，");
    }
    
    int blockChange = turnSummary.getInt("护甲变化");
    if (blockChange > 0) {
        prompt.append("获得了").append(blockChange).append("点护甲，");
    }
    
    prompt.append("用了").append(turnSummary.getInt("使用能量")).append("点能量。");
    
    if (turnSummary.has("怪物意图")) {
        JSONArray intents = turnSummary.getJSONArray("怪物意图");
        if (intents.length() > 0) {
            prompt.append("怪物们准备：");
            for (int i = 0; i < intents.length(); i++) {
                if (i > 0) prompt.append("，");
                prompt.append(intents.getString(i));
            }
            prompt.append("。");
        }
    }
    
    prompt.append("请用主播口吻总结这回合的表现：");
    
    return prompt.toString();
}
```

### 第五阶段：CommentaryUtils增强

#### 5.1 修改CommentaryUtils.java
```java
// 在CommentaryUtils.java中添加以下方法

/**
 * 触发基于牌数的解说
 */
public static void triggerCardBasedCommentary(aislayer.utils.TurnData turnData) {
    if (!shouldTriggerCommentary()) {
        return;
    }
    
    try {
        JSONObject actionInfo = aislayer.AISlayer.getActionInfo("按牌数解说", turnData);
        String cacheKey = generateCacheKey("card_based", turnData.turnNumber, turnData.playedCards.size());
        
        // 检查缓存
        if (commentaryCache.containsKey(cacheKey)) {
            showCommentary(commentaryCache.get(cacheKey));
            return;
        }
        
        // 调用AI获取解说
        getCardBasedCommentary(actionInfo);
        
    } catch (Exception e) {
        logger.error("触发按牌数解说失败", e);
        showFallbackCommentary("按牌数解说");
    }
}

/**
 * 触发回合结束解说
 */
public static void triggerTurnEndCommentary(aislayer.utils.TurnData turnData) {
    if (!shouldTriggerCommentary()) {
        return;
    }
    
    try {
        JSONObject turnSummary = aislayer.AISlayer.getTurnSummary(turnData);
        String cacheKey = generateCacheKey("turn_end", turnData.turnNumber);
        
        // 检查缓存
        if (commentaryCache.containsKey(cacheKey)) {
            showCommentary(commentaryCache.get(cacheKey));
            return;
        }
        
        // 调用AI获取解说
        getTurnEndCommentary(turnSummary);
        
    } catch (Exception e) {
        logger.error("触发回合结束解说失败", e);
        showFallbackCommentary("回合结束");
    }
}

/**
 * 获取基于牌数的解说
 */
private static void getCardBasedCommentary(JSONObject actionInfo) {
    Thread thread = new Thread(() -> {
        try {
            String commentary = aislayer.AIUtils.getCardBasedCommentary(actionInfo);
            if (commentary != null && !commentary.trim().isEmpty()) {
                showCommentary(commentary);
                // 添加到缓存
                String cacheKey = generateCacheKey("card_based", 
                    actionInfo.optInt("回合数", 0), 
                    actionInfo.optJSONArray("打出的牌").length());
                addToCache(cacheKey, commentary);
            }
        } catch (Exception e) {
            logger.error("获取按牌数解说失败", e);
            showFallbackCommentary("按牌数解说");
        }
    });
    thread.start();
}

/**
 * 获取回合结束解说
 */
private static void getTurnEndCommentary(JSONObject turnSummary) {
    Thread thread = new Thread(() -> {
        try {
            String commentary = aislayer.AIUtils.getTurnSummaryCommentary(turnSummary);
            if (commentary != null && !commentary.trim().isEmpty()) {
                showCommentary(commentary);
                // 添加到缓存
                String cacheKey = generateCacheKey("turn_end", turnSummary.optInt("回合数", 0));
                addToCache(cacheKey, commentary);
            }
        } catch (Exception e) {
            logger.error("获取回合结束解说失败", e);
            showFallbackCommentary("回合结束");
        }
    });
    thread.start();
}

/**
 * 更新shouldTriggerCommentary方法，考虑新的解说模式
 */
private static boolean shouldTriggerCommentary() {
    // 检查解说开关
    if (!commentaryEnabled) {
        return false;
    }
    
    // 检查API配置
    if (!aislayer.AISlayer.isAIStart()) {
        return false;
    }
    
    // 检查冷却时间
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastCommentaryTime < COMMENTARY_COOLDOWN) {
        return false;
    }
    
    // 检查频率控制（主要对按牌数模式有效）
    if (aislayer.panels.ConfigPanel.commentaryMode == 0) {
        actionCounter++;
        if (actionCounter % commentaryFrequency != 0) {
            return false;
        }
    }
    
    // 检查游戏状态
    if (AbstractDungeon.getCurrRoom() == null) {
        return false;
    }
    
    return true;
}
```

## 测试计划

### 单元测试
1. **配置系统测试**
   - 验证新配置项的保存和加载
   - 测试配置界面的显示和交互

2. **数据跟踪测试**
   - 验证TurnData的数据收集准确性
   - 测试BattleStateTracker的状态管理

3. **解说触发测试**
   - 测试按牌数解说模式的触发逻辑
   - 测试回合结束解说模式的触发时机

### 集成测试
1. **怪物介绍测试**
   - 验证战斗开始时的怪物介绍生成
   - 测试不同怪物组合的介绍内容

2. **解说模式切换测试**
   - 测试两种解说模式的切换
   - 验证模式切换后的行为变化

3. **性能测试**
   - 测试API调用的响应时间
   - 验证异步处理不影响游戏性能

### 用户验收测试
1. **游戏体验测试**
   - 邀请玩家测试新的解说功能
   - 收集对解说质量和频率的反馈

2. **兼容性测试**
   - 测试与现有Mod的兼容性
   - 验证不同游戏版本下的稳定性

## 部署和维护

### 构建和部署
- 使用现有Maven构建系统
- 保持与原版Mod相同的部署流程
- 确保向后兼容性

### 后续维护
- 根据用户反馈优化解说质量
- 持续优化性能和用户体验
- 定期更新以适配游戏版本更新

## 风险评估

### 技术风险
1. **API调用频率过高**
   - 缓解：实现智能缓存和频率控制
   - 备选：提供本地备用解说内容

2. **状态跟踪不准确**
   - 缓解：使用多个验证点确保数据准确性
   - 备选：提供手动重置功能

3. **性能影响**
   - 缓解：异步处理和智能队列管理
   - 备选：提供性能监控和优化选项

### 用户体验风险
1. **解说频率不当**
   - 缓解：提供详细的配置选项
   - 备选：实现自适应频率调整

2. **解说内容质量不稳定**
   - 缓解：优化提示词和内容过滤
   - 备选：提供内容审核和替换机制

## 总结

本开发方案提供了一个系统性的解决方案，通过分阶段实施确保功能的稳定性和可靠性。方案充分考虑了技术实现、用户体验和性能优化等多个方面，为项目的成功实施提供了详细的指导。

通过这个增强方案，AI解说Mod将能够提供更加智能和个性化的解说体验，满足不同玩家的需求，同时保持良好的性能和稳定性。