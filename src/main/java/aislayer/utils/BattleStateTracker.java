package aislayer.utils;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 战斗状态跟踪器
 * 用于跟踪整个战斗过程中的状态变化
 */
public class BattleStateTracker {
    private static BattleStateTracker instance;
    
    // 战斗状态
    private boolean inBattle = false;
    private int currentTurn = 0;
    private TurnData currentTurnData;
    private List<TurnData> turnHistory;
    private Map<String, MonsterState> monsterStates;
    private long battleStartTime;
    private long battleEndTime;
    
    // 配置相关
    private int cardsPerCommentary;
    private boolean introduceMonsters;
    private boolean detailedMonsterIntro;
    
    private BattleStateTracker() {
        this.turnHistory = new ArrayList<>();
        this.monsterStates = new HashMap<>();
        this.cardsPerCommentary = 3; // 默认值
        this.introduceMonsters = true; // 默认值
        this.detailedMonsterIntro = true; // 默认值
    }
    
    public static BattleStateTracker getInstance() {
        if (instance == null) {
            instance = new BattleStateTracker();
        }
        return instance;
    }
    
    /**
     * 更新配置
     */
    public void updateConfig(int cardsPerCommentary, boolean introduceMonsters, boolean detailedMonsterIntro) {
        this.cardsPerCommentary = cardsPerCommentary;
        this.introduceMonsters = introduceMonsters;
        this.detailedMonsterIntro = detailedMonsterIntro;
    }
    
    /**
     * 开始战斗
     */
    public void startBattle() {
        this.inBattle = true;
        this.currentTurn = 0;
        this.turnHistory.clear();
        this.monsterStates.clear();
        this.battleStartTime = System.currentTimeMillis();
        this.battleEndTime = 0;
        
        // 记录初始怪物状态
        if (AbstractDungeon.getCurrRoom() != null && AbstractDungeon.getCurrRoom().monsters != null) {
            for (AbstractMonster monster : AbstractDungeon.getCurrRoom().monsters.monsters) {
                if (!monster.isDead && !monster.isDying && !monster.isEscaping) {
                    monsterStates.put(monster.id, new MonsterState(monster));
                }
            }
        }
    }
    
    /**
     * 结束战斗
     */
    public void endBattle() {
        this.inBattle = false;
        this.battleEndTime = System.currentTimeMillis();
        
        // 结束当前回合
        if (currentTurnData != null) {
            currentTurnData.endTurn();
            turnHistory.add(currentTurnData);
            currentTurnData = null;
        }
    }
    
    /**
     * 开始新回合
     */
    public void startNewTurn() {
        if (!inBattle) return;
        
        // 结束上一回合
        if (currentTurnData != null) {
            currentTurnData.endTurn();
            turnHistory.add(currentTurnData);
        }
        
        // 开始新回合
        currentTurn++;
        currentTurnData = new TurnData(currentTurn);
        
        // 记录玩家初始状态
        if (AbstractDungeon.player != null) {
            currentTurnData.playerEnergyStart = AbstractDungeon.player.energy.energy;
            currentTurnData.playerHealthStart = AbstractDungeon.player.currentHealth;
        }
        
        // 更新怪物状态
        updateMonsterStates();
    }
    
    /**
     * 结束当前回合
     */
    public void endCurrentTurn() {
        if (!inBattle || currentTurnData == null) return;
        
        // 记录玩家结束状态
        if (AbstractDungeon.player != null) {
            currentTurnData.playerEnergyEnd = AbstractDungeon.player.energy.energy;
            currentTurnData.playerHealthEnd = AbstractDungeon.player.currentHealth;
        }
        
        currentTurnData.endTurn();
        turnHistory.add(currentTurnData);
        currentTurnData = null;
    }
    
    /**
     * 记录出牌
     */
    public void recordCardPlay(com.megacrit.cardcrawl.cards.AbstractCard card, AbstractMonster target) {
        if (!inBattle || currentTurnData == null) return;
        
        currentTurnData.addCardPlay(card, target);
    }
    
    /**
     * 更新怪物意图
     */
    public void updateMonsterIntents() {
        if (!inBattle || currentTurnData == null) return;
        
        if (AbstractDungeon.getCurrRoom() != null && AbstractDungeon.getCurrRoom().monsters != null) {
            for (AbstractMonster monster : AbstractDungeon.getCurrRoom().monsters.monsters) {
                if (!monster.isDead && !monster.isDying && !monster.isEscaping) {
                    currentTurnData.addMonsterIntent(monster);
                }
            }
        }
    }
    
    /**
     * 更新怪物状态
     */
    private void updateMonsterStates() {
        if (AbstractDungeon.getCurrRoom() != null && AbstractDungeon.getCurrRoom().monsters != null) {
            for (AbstractMonster monster : AbstractDungeon.getCurrRoom().monsters.monsters) {
                if (!monster.isDead && !monster.isDying && !monster.isEscaping) {
                    monsterStates.put(monster.id, new MonsterState(monster));
                }
            }
        }
    }
    
    /**
     * 检查是否应该触发解说（按牌数模式）
     */
    public boolean shouldTriggerCommentaryByCards() {
        if (!inBattle || currentTurnData == null) return false;
        
        return currentTurnData.getPlayedCardsCount() >= cardsPerCommentary;
    }
    
    /**
     * 获取当前回合数据
     */
    public TurnData getCurrentTurnData() {
        return currentTurnData;
    }
    
    /**
     * 获取回合历史
     */
    public List<TurnData> getTurnHistory() {
        return new ArrayList<>(turnHistory);
    }
    
    /**
     * 获取怪物状态
     */
    public Map<String, MonsterState> getMonsterStates() {
        return new HashMap<>(monsterStates);
    }
    
    /**
     * 获取战斗持续时间
     */
    public long getBattleDuration() {
        if (battleEndTime == 0) {
            return System.currentTimeMillis() - battleStartTime;
        }
        return battleEndTime - battleStartTime;
    }
    
    /**
     * 是否在战斗中
     */
    public boolean isInBattle() {
        return inBattle;
    }
    
    /**
     * 获取当前回合数
     */
    public int getCurrentTurn() {
        return currentTurn;
    }
    
    /**
     * 怪物状态内部类
     */
    public static class MonsterState {
        public String monsterId;
        public String monsterName;
        public int currentHealth;
        public int maxHealth;
        public int currentBlock;
        public String intent;
        public int intentDamage;
        public boolean isDead;
        public boolean isDying;
        public boolean isEscaping;
        
        public MonsterState(AbstractMonster monster) {
            this.monsterId = monster.id;
            this.monsterName = monster.name;
            this.currentHealth = monster.currentHealth;
            this.maxHealth = monster.maxHealth;
            this.currentBlock = monster.currentBlock;
            this.intent = monster.intent.name();
            this.intentDamage = monster.getIntentDmg();
            this.isDead = monster.isDead;
            this.isDying = monster.isDying;
            this.isEscaping = monster.isEscaping;
        }
    }
}