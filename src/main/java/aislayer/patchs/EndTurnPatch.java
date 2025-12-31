package aislayer.patchs;

import aislayer.panels.ConfigPanel;
import aislayer.utils.AIUtils;
import aislayer.utils.BattleStateTracker;
import aislayer.utils.CommentaryUtils;
import aislayer.utils.TurnData;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * 监听玩家结束回合行动的Patch
 */
@SpirePatch(
        clz = GameActionManager.class,
        method = "endTurn",
        optional = true
)
public class EndTurnPatch {

    public static final Logger logger = LogManager.getLogger(EndTurnPatch.class.getName());

    @SpirePrefixPatch
    public static void Prefix(GameActionManager __instance) {
        // 只在战斗中触发解说
        if (!isInCombat()) {
            logger.info("结束回合：不在战斗中，跳过解说");
            return;
        }
        
        // 移除玩家主动结束回合的检查，因为endTurn方法本身就是玩家主动调用的
        logger.info("结束回合：玩家主动结束回合，继续处理");
        
        try {
            logger.info("结束回合：检查是否触发解说...");
            
            // 更新战斗状态跟踪器
            BattleStateTracker tracker = BattleStateTracker.getInstance();
            
            // 根据配置决定是否触发解说
            boolean shouldTrigger = CommentaryUtils.shouldTriggerCommentaryByTurnEnd();
            logger.info("结束回合：解说模式检查 - commentaryByCards=" + ConfigPanel.commentaryByCards +
                       ", shouldTrigger=" + shouldTrigger);
            
            if (shouldTrigger) {
                logger.info("结束回合：触发解说");
                
                // 在游戏清空数据前，先构建包含完整回合数据的actionInfo
                try {
                    org.json.JSONObject actionInfo = new org.json.JSONObject();
                    actionInfo.put("行动类型", "结束回合");
                    
                    // 保存当前回合数据到actionInfo中
                    TurnData currentTurn = tracker.getCurrentTurnData();
                    if (currentTurn != null) {
                        logger.info("结束回合：保存回合数据到actionInfo，playedCards=" + currentTurn.getPlayedCardsCount());
                        
                        // 保存打牌数据
                        org.json.JSONArray playedCardsArray = new org.json.JSONArray();
                        if (currentTurn.playedCards != null) {
                            for (TurnData.CardPlayRecord cardRecord : currentTurn.playedCards) {
                                org.json.JSONObject cardObj = new org.json.JSONObject();
                                cardObj.put("cardName", cardRecord.cardName);
                                cardObj.put("cardId", cardRecord.cardId);
                                cardObj.put("cardType", cardRecord.cardType);
                                cardObj.put("cost", cardRecord.cost);
                                
                                // 获取卡牌描述
                                String cardDescription = getCardDescription(cardRecord.cardId);
                                if (cardDescription != null && !cardDescription.isEmpty()) {
                                    cardObj.put("description", cardDescription);
                                    logger.info("结束回合：为卡牌 " + cardRecord.cardName + " 添加描述: " + cardDescription);
                                } else {
                                    logger.warn("结束回合：无法获取卡牌描述: " + cardRecord.cardName + " (ID: " + cardRecord.cardId + ")");
                                }
                                
                                if (cardRecord.targetMonsterName != null) {
                                    cardObj.put("targetMonster", cardRecord.targetMonsterName);
                                }
                                if (cardRecord.targetMonsterId != null) {
                                    cardObj.put("targetMonsterId", cardRecord.targetMonsterId);
                                }
                                playedCardsArray.put(cardObj);
                            }
                        }
                        actionInfo.put("本回合打牌", playedCardsArray);
                        actionInfo.put("本回合打牌数", currentTurn.getPlayedCardsCount());
                        
                        // 保存玩家状态
                        if (AbstractDungeon.player != null) {
                            org.json.JSONObject playerState = new org.json.JSONObject();
                            playerState.put("currentHealth", AbstractDungeon.player.currentHealth);
                            playerState.put("maxHealth", AbstractDungeon.player.maxHealth);
                            
                            // 使用回合结束时的能量，而不是当前能量（可能已被清空）
                            int endEnergy = currentTurn.playerEnergyEnd;
                            if (endEnergy == 0 && currentTurn.playerEnergyStart > 0) {
                                // 如果endEnergy为0但startEnergy大于0，说明能量被清空了，使用startEnergy作为参考
                                endEnergy = currentTurn.playerEnergyStart;
                            }
                            playerState.put("energy", endEnergy);
                            actionInfo.put("玩家状态", playerState);
                        }
                        
                        // 保存怪物状态 - 直接从当前战斗中获取所有存活的怪物
                        if (AbstractDungeon.getCurrRoom() != null && AbstractDungeon.getCurrRoom().monsters != null) {
                            org.json.JSONArray monstersArray = new org.json.JSONArray();
                            int aliveMonsters = 0;
                            
                            // 创建怪物列表的副本，避免ConcurrentModificationException
                            java.util.ArrayList<com.megacrit.cardcrawl.monsters.AbstractMonster> monstersCopy;
                            try {
                                monstersCopy = new java.util.ArrayList<>(AbstractDungeon.getCurrRoom().monsters.monsters);
                                logger.info("结束回合：开始处理怪物状态，总怪物数: " + monstersCopy.size());
                            } catch (Exception e) {
                                logger.error("结束回合：无法创建怪物列表副本", e);
                                return;
                            }
                            
                            for (com.megacrit.cardcrawl.monsters.AbstractMonster monster : monstersCopy) {
                                try {
                                    logger.info("结束回合：处理怪物 " + monster.name + " - 死亡:" + monster.isDead + " 濒死:" + monster.isDying + " 逃跑:" + monster.isEscaping + " 意图:" + monster.intent.name());
                                    
                                    if (!monster.isDead && !monster.isDying && !monster.isEscaping) {
                                        aliveMonsters++;
                                        org.json.JSONObject monsterObj = new org.json.JSONObject();
                                        monsterObj.put("name", monster.name);
                                        monsterObj.put("id", monster.id);
                                        monsterObj.put("currentHealth", monster.currentHealth);
                                        monsterObj.put("maxHealth", monster.maxHealth);
                                        monsterObj.put("currentBlock", monster.currentBlock);
                                        monsterObj.put("intent", monster.intent.name());
                                        monsterObj.put("intentDamage", monster.getIntentDmg());
                                        monstersArray.put(monsterObj);
                                        
                                        logger.info("结束回合：记录存活怪物 " + monster.name + " - 血量:" + monster.currentHealth + "/" + monster.maxHealth + " 意图:" + monster.intent.name() + " 伤害:" + monster.getIntentDmg());
                                    }
                                } catch (Exception e) {
                                    logger.error("结束回合：处理怪物 " + monster.name + " 时发生异常", e);
                                    // 继续处理下一个怪物
                                }
                            }
                            
                            if (aliveMonsters > 0) {
                                actionInfo.put("怪物状态", monstersArray);
                                logger.info("结束回合：成功记录了 " + aliveMonsters + " 个存活的怪物，共 " + monstersCopy.size() + " 个怪物");
                            } else {
                                logger.info("结束回合：没有存活的怪物，共 " + monstersCopy.size() + " 个怪物");
                            }
                        } else {
                            logger.warn("结束回合：无法获取怪物信息 - 房间或怪物列表为空");
                        }
                    }
                    
                    // 直接调用AIUtils.getCommentary，绕过异步问题
                    AIUtils.getCommentary(actionInfo);
                    
                } catch (Exception e) {
                    logger.error("构建结束回合解说信息失败", e);
                }
                
                // 解说触发后立即结束当前回合，保存数据
                if (tracker.isInBattle()) {
                    logger.info("结束回合：保存当前回合数据");
                    tracker.endCurrentTurn();
                }
            } else {
                logger.info("结束回合：当前配置不触发解说（可能是按牌数模式）");
                // 即使不触发解说也要正常结束回合，保存数据
                if (tracker.isInBattle()) {
                    logger.info("结束回合：保存当前回合数据（无解说）");
                    tracker.endCurrentTurn();
                }
            }
        } catch (Exception e) {
            logger.error("结束回合解说处理异常", e);
        }
    }
    
    /**
     * 获取卡牌描述
     * @param cardId 卡牌ID
     * @return 卡牌描述，如果获取失败返回空字符串
     */
    private static String getCardDescription(String cardId) {
        try {
            logger.info("结束回合：尝试获取卡牌描述，cardId=" + cardId);
            
            com.megacrit.cardcrawl.cards.AbstractCard foundCard = null;
            
            // 首先尝试从CardLibrary获取
            com.megacrit.cardcrawl.cards.AbstractCard card = com.megacrit.cardcrawl.helpers.CardLibrary.getCard(cardId);
            if (card != null) {
                foundCard = card;
                logger.info("结束回合：从CardLibrary找到卡牌 " + cardId);
            }
            
            // 如果CardLibrary找不到，尝试从各个牌堆中查找
            if (foundCard == null && AbstractDungeon.player != null) {
                try {
                    // 从手牌中查找 - 创建副本避免并发修改
                    java.util.ArrayList<com.megacrit.cardcrawl.cards.AbstractCard> handCopy =
                        new java.util.ArrayList<>(AbstractDungeon.player.hand.group);
                    for (com.megacrit.cardcrawl.cards.AbstractCard handCard : handCopy) {
                        if (cardId.equals(handCard.cardID)) {
                            foundCard = handCard;
                            logger.info("结束回合：从手牌找到卡牌 " + cardId);
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("结束回合：从手牌查找卡牌时发生异常: " + cardId, e);
                }
            }
            
            if (foundCard == null && AbstractDungeon.player != null) {
                try {
                    // 从抽牌堆中查找 - 创建副本避免并发修改
                    java.util.ArrayList<com.megacrit.cardcrawl.cards.AbstractCard> drawCopy =
                        new java.util.ArrayList<>(AbstractDungeon.player.drawPile.group);
                    for (com.megacrit.cardcrawl.cards.AbstractCard drawCard : drawCopy) {
                        if (cardId.equals(drawCard.cardID)) {
                            foundCard = drawCard;
                            logger.info("结束回合：从抽牌堆找到卡牌 " + cardId);
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("结束回合：从抽牌堆查找卡牌时发生异常: " + cardId, e);
                }
            }
            
            if (foundCard == null && AbstractDungeon.player != null) {
                try {
                    // 从弃牌堆中查找 - 创建副本避免并发修改
                    java.util.ArrayList<com.megacrit.cardcrawl.cards.AbstractCard> discardCopy =
                        new java.util.ArrayList<>(AbstractDungeon.player.discardPile.group);
                    for (com.megacrit.cardcrawl.cards.AbstractCard discardCard : discardCopy) {
                        if (cardId.equals(discardCard.cardID)) {
                            foundCard = discardCard;
                            logger.info("结束回合：从弃牌堆找到卡牌 " + cardId);
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("结束回合：从弃牌堆查找卡牌时发生异常: " + cardId, e);
                }
            }
            
            if (foundCard == null && AbstractDungeon.player != null) {
                try {
                    // 从总牌组中查找 - 创建副本避免并发修改
                    java.util.ArrayList<com.megacrit.cardcrawl.cards.AbstractCard> deckCopy =
                        new java.util.ArrayList<>(AbstractDungeon.player.masterDeck.group);
                    for (com.megacrit.cardcrawl.cards.AbstractCard deckCard : deckCopy) {
                        if (cardId.equals(deckCard.cardID)) {
                            foundCard = deckCard;
                            logger.info("结束回合：从总牌组找到卡牌 " + cardId);
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("结束回合：从总牌组查找卡牌时发生异常: " + cardId, e);
                }
            }
            
            if (foundCard != null) {
                // 使用AISlayer.handleDescription处理字符转换
                String rawDescription = foundCard.rawDescription;
                String processedDescription = aislayer.AISlayer.handleDescription(rawDescription, foundCard);
                logger.info("结束回合：获取到卡牌描述 " + cardId + " - 原始: " + rawDescription + " - 处理后: " + processedDescription);
                return processedDescription;
            }
            
            logger.warn("结束回合：无法找到卡牌 " + cardId);
        } catch (Exception e) {
            logger.error("结束回合：获取卡牌描述时发生异常: " + cardId, e);
        }
        return "";
    }
    
    /**
     * 检查是否在战斗中
     * @return 是否在战斗中
     */
    private static boolean isInCombat() {
        if (AbstractDungeon.getCurrRoom() == null) {
            return false;
        }
        
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        return room.phase == AbstractRoom.RoomPhase.COMBAT;
    }
}