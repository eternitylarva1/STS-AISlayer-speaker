package aislayer.utils;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import java.util.ArrayList;
import java.util.List;

/**
 * 回合数据跟踪类
 * 用于记录单个回合内的游戏状态变化
 */
public class TurnData {
    public int turnNumber;
    public List<CardPlayRecord> playedCards;
    public List<MonsterIntentRecord> monsterIntents;
    public int playerEnergyStart;
    public int playerEnergyEnd;
    public int playerHealthStart;
    public int playerHealthEnd;
    public boolean turnEnded;
    public long startTime;
    public long endTime;

    public TurnData(int turnNumber) {
        this.turnNumber = turnNumber;
        this.playedCards = new ArrayList<>();
        this.monsterIntents = new ArrayList<>();
        this.playerEnergyStart = 0;
        this.playerEnergyEnd = 0;
        this.playerHealthStart = 0;
        this.playerHealthEnd = 0;
        this.turnEnded = false;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
    }

    /**
     * 添加出牌记录
     */
    public void addCardPlay(AbstractCard card, AbstractMonster target) {
        CardPlayRecord record = new CardPlayRecord(card, target);
        playedCards.add(record);
    }

    /**
     * 添加怪物意图记录
     */
    public void addMonsterIntent(AbstractMonster monster) {
        MonsterIntentRecord record = new MonsterIntentRecord(monster);
        monsterIntents.add(record);
    }

    /**
     * 结束回合
     */
    public void endTurn() {
        this.turnEnded = true;
        this.endTime = System.currentTimeMillis();
    }

    /**
     * 获取本回合出牌数量
     */
    public int getPlayedCardsCount() {
        return playedCards.size();
    }

    /**
     * 获取回合持续时间（毫秒）
     */
    public long getTurnDuration() {
        if (endTime == 0) {
            return System.currentTimeMillis() - startTime;
        }
        return endTime - startTime;
    }

    /**
     * 出牌记录内部类
     */
    public static class CardPlayRecord {
        public String cardId;
        public String cardName;
        public String cardType;
        public int cost;
        public String targetMonsterId;
        public String targetMonsterName;
        public long timestamp;

        public CardPlayRecord(AbstractCard card, AbstractMonster target) {
            this.cardId = card.cardID;
            this.cardName = card.name;
            this.cardType = card.type.toString();
            this.cost = card.costForTurn;
            this.timestamp = System.currentTimeMillis();
            
            if (target != null) {
                this.targetMonsterId = target.id;
                this.targetMonsterName = target.name;
            } else {
                this.targetMonsterId = null;
                this.targetMonsterName = null;
            }
        }
    }

    /**
     * 怪物意图记录内部类
     */
    public static class MonsterIntentRecord {
        public String monsterId;
        public String monsterName;
        public String intent;
        public int intentBaseDamage;
        public int intentDamage;
        public int intentBlock;
        public int intentMagicNumber;
        public long timestamp;

        public MonsterIntentRecord(AbstractMonster monster) {
            this.monsterId = monster.id;
            this.monsterName = monster.name;
            this.intent = monster.intent.name();
            this.intentBaseDamage = monster.getIntentBaseDmg();
            this.intentDamage = monster.getIntentDmg();
            this.intentBlock = -1; // AbstractMonster没有直接的getBlock方法，设为默认值
            this.intentMagicNumber = -1; // AbstractMonster没有直接的getMagicNumber方法，设为默认值
            this.timestamp = System.currentTimeMillis();
        }
    }
}