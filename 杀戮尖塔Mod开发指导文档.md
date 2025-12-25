# 杀戮尖塔 Mod 开发指导文档

## 目录
1. [游戏架构概述](#游戏架构概述)
2. [核心系统分析](#核心系统分析)
3. [Mod开发基础](#mod开发基础)
4. [常见Mod类型开发指南](#常见mod类型开发指南)
5. [最佳实践与注意事项](#最佳实践与注意事项)
6. [调试与测试](#调试与测试)

## 游戏架构概述

### 核心类结构

杀戮尖塔游戏采用模块化架构，主要包含以下核心类：

- **CardCrawlGame**: 主游戏类，实现ApplicationListener接口，管理游戏状态和生命周期
- **AbstractDungeon**: 地城管理核心类，处理地图生成、房间转换、游戏流程
- **AbstractCreature**: 所有生物（玩家和怪物）的基类
- **AbstractPlayer**: 玩家角色基类
- **AbstractMonster**: 怪物基类
- **AbstractCard**: 卡牌基类
- **AbstractRelic**: 遗物基类
- **AbstractPower**: 能力/状态效果基类
- **AbstractPotion**: 药水基类
- **AbstractRoom**: 房间基类
- **AbstractEvent**: 事件基类
- **AbstractGameAction**: 游戏动作基类
- **GameActionManager**: 动作管理器，处理动作队列和回合制执行

## 核心系统分析

### 1. 动作系统 (Action System)

动作系统是游戏的核心机制，所有游戏操作都通过动作实现：

```java
public abstract class AbstractGameAction {
    public ActionType actionType;
    public float duration;
    public AbstractCreature target;
    public AbstractCreature source;
    
    public abstract void update();
}
```

**关键特性**：
- 所有动作通过GameActionManager.addToBottom()添加到队列
- 动作按顺序执行，支持动画和延迟
- 回合制系统通过动作队列实现

**常用动作类型**：
- AttackAction: 攻击动作
- DrawCardAction: 抽卡动作
- GainBlockAction: 获得格挡
- ApplyPowerAction: 应用能力效果
- DamageAction: 造成伤害

### 2. 卡牌系统 (Card System)

卡牌是游戏的主要交互元素：

```java
public abstract class AbstractCard {
    public String cardID;
    public String name;
    public String rawDescription;
    public int cost;
    public CardType type;
    public CardColor color;
    public CardRarity rarity;
    public CardTarget target;
    
    public abstract void use(AbstractPlayer p, AbstractMonster m);
    public abstract void upgrade();
}
```

**卡牌类型**：
- ATTACK: 攻击卡
- SKILL: 技能卡
- POWER: 能力卡
- CURSE: 诅咒卡
- STATUS: 状态卡

**关键方法**：
- use(): 卡牌使用效果
- upgrade(): 卡牌升级效果
- calculateCardDamage(): 计算伤害

### 3. 角色系统 (Character System)

#### 玩家角色
```java
public abstract class AbstractPlayer extends AbstractCreature {
    public CardGroup masterDeck;      // 主牌组
    public CardGroup drawPile;        // 抽牌堆
    public CardGroup hand;            // 手牌
    public CardGroup discardPile;     // 弃牌堆
    public CardGroup exhaustPile;     // 消耗牌堆
    public ArrayList<AbstractRelic> relics; // 遗物列表
    public EnergyManager energy;      // 能量管理
}
```

#### 怪物系统
```java
public abstract class AbstractMonster extends AbstractCreature {
    public enum Intent {
        ATTACK, ATTACK_BUFF, ATTACK_DEBUFF, ATTACK_DEFEND,
        BUFF, DEBUFF, STRONG_DEBUFF, DEBUG,
        DEFEND, DEFEND_DEBUFF, DEFEND_BUFF,
        ESCAPE, MAGIC, NONE, SLEEP, STUN, UNKNOWN;
    }
    
    public abstract void takeTurn();
    protected abstract void getMove(int paramInt);
}
```

### 4. 能力系统 (Power System)

能力系统处理所有状态效果：

```java
public abstract class AbstractPower {
    public String ID;
    public String name;
    public AbstractCreature owner;
    public PowerType type;
    public int amount;
    
    // 生命周期钩子方法
    public void atStartOfTurn() {}
    public void atEndOfTurn(boolean isPlayer) {}
    public float atDamageGive(float damage, DamageInfo.DamageType type) { return damage; }
    public void onAttack(DamageInfo info, int damageAmount, AbstractCreature target) {}
    public void onUseCard(AbstractCard card, UseCardAction action) {}
}
```

**能力类型**：
- BUFF: 增益效果
- DEBUFF: 减益效果

### 5. 遗物系统 (Relic System)

遗物提供永久性效果：

```java
public abstract class AbstractRelic {
    public String relicId;
    public String name;
    public RelicTier tier;
    public int counter;
    
    public abstract AbstractRelic makeCopy();
    
    // 生命周期钩子方法
    public void onEquip() {}
    public void onUnequip() {}
    public void atBattleStart() {}
    public void atBattleStartPreDraw() {}
    public void atTurnStart() {}
    public void atTurnStartPostDraw() {}
    public void onPlayCard(AbstractCard c) {}
    public void onVictory() {}
}
```

**遗物稀有度**：
- STARTER: 初始遗物
- COMMON: 普通遗物
- UNCOMMON: 稀有遗物
- RARE: 极稀有遗物
- BOSS: Boss遗物
- SHOP: 商店遗物
- SPECIAL: 特殊遗物

### 6. 药水系统 (Potion System)

药水提供一次性效果：

```java
public abstract class AbstractPotion {
    public String ID;
    public String name;
    public PotionRarity rarity;
    public PotionSize size;
    public PotionColor color;
    public int potency;
    
    public abstract void use(AbstractCreature target);
    public abstract AbstractPotion makeCopy();
}
```

**药水稀有度**：
- COMMON: 普通药水
- UNCOMMON: 稀有药水
- RARE: 极稀有药水

### 7. 房间系统 (Room System)

房间是游戏的基本场景单元：

```java
public abstract class AbstractRoom {
    public enum RoomPhase {
        COMBAT, EVENT, COMPLETE, INCOMPLETE;
    }
    
    public enum RoomType {
        SHOP, MONSTER, SHRINE, TREASURE, EVENT, BOSS;
    }
    
    public ArrayList<AbstractPotion> potions;
    public ArrayList<AbstractRelic> relics;
    public ArrayList<RewardItem> rewards;
    public MonsterGroup monsters;
    public AbstractEvent event;
    
    public abstract void onPlayerEntry();
}
```

**房间类型**：
- MonsterRoom: 普通怪物房间
- MonsterRoomElite: 精英怪物房间
- MonsterRoomBoss: Boss房间
- EventRoom: 事件房间
- ShopRoom: 商店房间
- RestRoom: 休息房间
- TreasureRoom: 宝箱房间

### 8. 事件系统 (Event System)

事件提供叙事选择：

```java
public abstract class AbstractEvent {
    public RoomEventDialog roomEventText;
    public GenericEventDialog imageEventText;
    
    public abstract void buttonEffect(int buttonPressed);
    
    // 日志记录方法
    public static void logMetric(String eventName, String playerChoice, 
                                List<String> cardsObtained, List<String> cardsRemoved, 
                                List<String> relicsObtained, int damageTaken, 
                                int goldGain, int goldLoss);
}
```

## Mod开发基础

### 1. 项目结构

典型的Mod项目结构：
```
mod-project/
├── src/main/java/
│   └── yourmod/
│       ├── YourMod.java           # Mod主类
│       ├── cards/                 # 自定义卡牌
│       ├── characters/            # 自定义角色
│       ├── relics/                # 自定义遗物
│       ├── powers/                # 自定义能力
│       ├── potions/               # 自定义药水
│       ├── events/                # 自定义事件
│       ├── monsters/              # 自定义怪物
│       ├── patches/               # SpirePatch修改
│       └── utils/                 # 工具类
├── src/main/resources/
│   ├── ModTheSpire.json          # Mod配置文件
│   └── localization/             # 本地化文件
└── pom.xml                       # Maven配置
```

### 2. ModTheSpire配置

ModTheSpire.json是Mod的配置文件：
```json
{
  "modid": "yourmod",
  "name": "Your Mod Name",
  "author_list": ["Your Name"],
  "description": "Mod description",
  "version": "1.0.0",
  "sts_version": "12-22-2020",
  "dependencies": ["basemod"],
  "update_json": ""
}
```

### 3. 主类设置

Mod主类需要继承BaseMod并实现必要方法：
```java
@SpireInitializer
public class YourMod implements BaseMod {
    public static final String MOD_ID = "yourmod";
    public static final Logger logger = LogManager.getLogger(YourMod.class.getName());
    
    public YourMod() {
        BaseMod.subscribe(this);
    }
    
    public static void initialize() {
        new YourMod();
    }
    
    @Override
    public void receivePostInitialize() {
        // Mod初始化后调用
    }
    
    @Override
    public void receiveEditCards() {
        // 添加自定义卡牌
    }
    
    @Override
    public void receiveEditRelics() {
        // 添加自定义遗物
    }
    
    @Override
    public void receiveEditCharacters() {
        // 添加自定义角色
    }
    
    @Override
    public void receiveEditPotions() {
        // 添加自定义药水
    }
    
    @Override
    public void receiveEditStrings() {
        // 加载本地化文件
    }
}
```

## 常见Mod类型开发指南

### 1. 自定义卡牌

创建自定义卡牌需要继承AbstractCard：
```java
public class YourCard extends AbstractCard {
    public static final String ID = "yourmod:YourCard";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardString(ID);
    public static final String NAME = cardStrings.NAME;
    public static final String DESCRIPTION = cardStrings.DESCRIPTION;
    private static final int COST = 1;
    private static final int DAMAGE = 8;
    private static final int UPGRADE_PLUS_DMG = 3;
    
    public YourCard() {
        super(ID, NAME, "yourmodResources/images/cards/YourCard.png", COST, DESCRIPTION, 
              CardType.ATTACK, CardColor.RED, CardRarity.COMMON, CardTarget.ENEMY);
        
        this.baseDamage = DAMAGE;
        this.tags.add(CardTags.STRIKE);
    }
    
    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        addToBot(new DamageAction(m, new DamageInfo(p, this.damage, DamageInfo.DamageType.NORMAL)));
    }
    
    @Override
    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeDamage(UPGRADE_PLUS_DMG);
            initializeDescription();
        }
    }
    
    @Override
    public AbstractCard makeCopy() {
        return new YourCard();
    }
}
```

在Mod主类中注册卡牌：
```java
@Override
public void receiveEditCards() {
    BaseMod.addCard(new YourCard());
}
```

### 2. 自定义遗物

创建自定义遗物需要继承AbstractRelic：
```java
public class YourRelic extends AbstractRelic {
    public static final String ID = "yourmod:YourRelic";
    
    public YourRelic() {
        super(ID, "yourmodResources/images/relics/YourRelic.png", RelicTier.COMMON, LandingSound.CLINK);
    }
    
    @Override
    public String getUpdatedDescription() {
        return DESCRIPTIONS[0];
    }
    
    @Override
    public void atBattleStart() {
        flash();
        addToBot(new GainEnergyAction(1));
    }
    
    @Override
    public AbstractRelic makeCopy() {
        return new YourRelic();
    }
}
```

在Mod主类中注册遗物：
```java
@Override
public void receiveEditRelics() {
    BaseMod.addRelic(new YourRelic(), RED);
}
```

### 3. 自定义能力

创建自定义能力需要继承AbstractPower：
```java
public class YourPower extends AbstractPower {
    public static final String POWER_ID = "yourmod:YourPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings(POWER_ID);
    public static final String NAME = powerStrings.NAME;
    public static final String[] DESCRIPTIONS = powerStrings.DESCRIPTIONS;
    
    public YourPower(AbstractCreature owner, int amount) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = amount;
        this.type = PowerType.BUFF;
        this.isTurnBased = false;
        
        updateDescription();
        loadRegion("yourmod:YourPower");
    }
    
    @Override
    public void updateDescription() {
        this.description = DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1];
    }
    
    @Override
    public float atDamageGive(float damage, DamageInfo.DamageType type) {
        return damage + this.amount;
    }
    
    @Override
    public void atEndOfTurn(boolean isPlayer) {
        if (isPlayer) {
            addToBot(new ReducePowerAction(this.owner, this.owner, this, 1));
        }
    }
}
```

### 4. 自定义药水

创建自定义药水需要继承AbstractPotion：
```java
public class YourPotion extends AbstractPotion {
    public YourPotion() {
        super(NAME, POTION_ID, PotionRarity.COMMON, PotionSize.BOTTLE, PotionColor.FIRE);
        
        this.isThrown = true;
        this.targetRequired = true;
        this.labOutlineColor = Settings.RED_TEXT_COLOR;
    }
    
    @Override
    public void use(AbstractCreature target) {
        if (target instanceof AbstractMonster) {
            addToTop(new DamageAction(target, new DamageInfo(AbstractDungeon.player, this.potency, DamageInfo.DamageType.NORMAL)));
        }
    }
    
    @Override
    public void initializeData() {
        this.potency = getPotency();
        this.description = DESCRIPTIONS[0] + this.potency + DESCRIPTIONS[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
    }
    
    @Override
    public AbstractPotion makeCopy() {
        return new YourPotion();
    }
}
```

### 5. 自定义事件

创建自定义事件需要继承AbstractEvent：
```java
public class YourEvent extends AbstractEvent {
    public static final String ID = "yourmod:YourEvent";
    
    public YourEvent() {
        this.title = NAME;
        this.body = DESCRIPTIONS[0];
        this.roomEventText.addDialogOption(OPTIONS[0]);
        this.roomEventText.addDialogOption(OPTIONS[1]);
        this.hasDialog = true;
    }
    
    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (buttonPressed) {
            case 0:
                this.roomEventText.updateBodyText(DESCRIPTIONS[1]);
                this.roomEventText.updateDialogOption(0, OPTIONS[2]);
                this.roomEventText.clearRemainingOptions();
                logMetric(ID, "Option 1");
                break;
            case 1:
                this.roomEventText.updateBodyText(DESCRIPTIONS[2]);
                this.roomEventText.updateDialogOption(0, OPTIONS[2]);
                this.roomEventText.clearRemainingOptions();
                logMetric(ID, "Option 2");
                break;
            case 2:
                openMap();
                break;
        }
    }
}
```

### 6. SpirePatch修改

使用SpirePatch修改游戏原有方法：
```java
@SpirePatch(clz = AbstractPlayer.class, method = "damage")
public static class YourPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AbstractPlayer __instance, DamageInfo info) {
        // 在原方法执行前添加逻辑
        if (__instance.hasPower("yourmod:YourPower")) {
            // 修改伤害计算
        }
        return SpireReturn.Continue();
    }
    
    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance, DamageInfo info) {
        // 在原方法执行后添加逻辑
    }
}
```

## 最佳实践与注意事项

### 1. 代码规范

- 遵循Java命名规范
- 使用有意义的变量和方法名
- 添加适当的注释
- 保持代码简洁和可读性

### 2. 性能优化

- 避免在update()方法中进行重计算
- 使用对象池减少GC压力
- 合理使用静态变量
- 避免不必要的对象创建

### 3. 兼容性考虑

- 检查Mod之间的兼容性
- 使用BaseMod提供的安全方法
- 避免直接修改游戏核心类
- 提供配置选项让用户调整

### 4. 本地化支持

- 使用CardCrawlGame.languagePack获取本地化文本
- 提供多语言支持
- 避免硬编码文本

### 5. 错误处理

- 添加适当的错误检查
- 使用try-catch处理异常
- 记录错误日志
- 提供友好的错误信息

### 6. 资源管理

- 正确释放资源
- 使用相对路径
- 避免资源泄漏
- 优化图片和音频文件大小

## 调试与测试

### 1. 调试技巧

- 使用logger记录调试信息
- 利用游戏内调试模式
- 添加调试命令
- 使用断点调试

### 2. 测试方法

- 测试各种游戏场景
- 验证与其他Mod的兼容性
- 测试不同游戏设置
- 进行性能测试

### 3. 常见问题解决

- 检查Mod加载顺序
- 验证资源路径
- 确认依赖关系
- 查看错误日志

### 4. 发布准备

- 创建完整的Mod包
- 编写详细的说明文档
- 提供安装指南
- 准备示例和教程

## 总结

杀戮尖塔Mod开发需要对游戏架构有深入理解，特别是动作系统、卡牌系统和各种钩子方法的使用。通过遵循本指南的最佳实践，可以开发出高质量、兼容性好的Mod。

记住，Mod开发是一个持续学习和改进的过程，不断尝试和探索新的可能性，为游戏社区带来更多有趣的内容。