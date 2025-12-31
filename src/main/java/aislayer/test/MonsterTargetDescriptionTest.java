package aislayer.test;

import aislayer.utils.AIUtils;
import aislayer.utils.TurnData;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.red.Strike_Red;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.Cultist;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 * 测试怪物目标描述功能
 */
public class MonsterTargetDescriptionTest {
    
    public static final Logger logger = LogManager.getLogger(MonsterTargetDescriptionTest.class.getName());
    
    public static void main(String[] args) {
        logger.info("=== 开始测试怪物目标描述功能 ===");
        
        // 测试1: 打牌解说中的怪物描述
        testCardPlayCommentary();
        
        // 测试2: 回合结束解说中的怪物描述
        testTurnEndCommentary();
        
        logger.info("=== 怪物目标描述功能测试完成 ===");
    }
    
    /**
     * 测试打牌解说中的怪物描述
     */
    private static void testCardPlayCommentary() {
        logger.info("--- 测试打牌解说中的怪物描述 ---");
        
        try {
            // 创建模拟的卡牌和怪物
            AbstractCard card = new Strike_Red();
            AbstractMonster monster = new Cultist(0.0f, 0.0f);
            
            // 创建CardPlayRecord
            TurnData.CardPlayRecord cardRecord = new TurnData.CardPlayRecord(card, monster);
            
            // 创建actionInfo
            JSONObject actionInfo = new JSONObject();
            actionInfo.put("行动类型", "打牌");
            
            JSONObject cardInfo = new JSONObject();
            cardInfo.put("名称", card.name);
            cardInfo.put("描述", "造成6点伤害。");
            actionInfo.put("使用的卡牌", cardInfo);
            
            // 测试buildCommentaryPrompt方法
            // 注意：这里我们无法直接测试私有方法，但可以通过日志验证功能
            logger.info("卡牌记录: " + cardRecord.cardName);
            logger.info("目标怪物ID: " + cardRecord.targetMonsterId);
            logger.info("目标怪物名称: " + cardRecord.targetMonsterName);
            
            // 验证怪物描述是否能正确获取
            String monsterDesc = getMonsterDescriptionForTest(cardRecord.targetMonsterId);
            if (monsterDesc != null) {
                logger.info("怪物描述获取成功: " + monsterDesc);
            } else {
                logger.warn("怪物描述获取失败");
            }
            
        } catch (Exception e) {
            logger.error("测试打牌解说失败", e);
        }
    }
    
    /**
     * 测试回合结束解说中的怪物描述
     */
    private static void testTurnEndCommentary() {
        logger.info("--- 测试回合结束解说中的怪物描述 ---");
        
        try {
            // 创建模拟的actionInfo（模拟EndTurnPatch中的数据结构）
            JSONObject actionInfo = new JSONObject();
            actionInfo.put("行动类型", "结束回合");
            actionInfo.put("本回合打牌数", 2);
            
            // 创建打牌数组
            org.json.JSONArray playedCards = new org.json.JSONArray();
            
            // 第一张牌：攻击Cultist
            JSONObject card1 = new JSONObject();
            card1.put("cardName", "打击");
            card1.put("cardId", "Strike_Red");
            card1.put("cost", 1);
            card1.put("description", "造成6点伤害。");
            card1.put("targetMonster", "Cultist");
            card1.put("targetMonsterId", "Cultist");
            playedCards.put(card1);
            
            // 第二张牌：攻击Cultist
            JSONObject card2 = new JSONObject();
            card2.put("cardName", "防御");
            card2.put("cardId", "Defend_Red");
            card2.put("cost", 1);
            card2.put("description", "获得5点格挡。");
            // 这张牌没有目标
            playedCards.put(card2);
            
            actionInfo.put("本回合打牌", playedCards);
            
            // 验证数据结构
            logger.info("回合数据构建完成，打牌数: " + actionInfo.getInt("本回合打牌数"));
            
            for (int i = 0; i < playedCards.length(); i++) {
                JSONObject card = playedCards.getJSONObject(i);
                logger.info("卡牌" + (i+1) + ": " + card.getString("cardName"));
                
                if (card.has("targetMonsterId")) {
                    String monsterId = card.getString("targetMonsterId");
                    String monsterDesc = getMonsterDescriptionForTest(monsterId);
                    if (monsterDesc != null) {
                        logger.info("  目标怪物描述: " + monsterDesc);
                    } else {
                        logger.warn("  无法获取目标怪物描述");
                    }
                } else {
                    logger.info("  无目标怪物");
                }
            }
            
        } catch (Exception e) {
            logger.error("测试回合结束解说失败", e);
        }
    }
    
    /**
     * 测试用的怪物描述获取方法（从AIUtils复制）
     */
    private static String getMonsterDescriptionForTest(String monsterId) {
        try {
            String langPackDir = "aislayerResources" + java.io.File.separator + "localization" + java.io.File.separator + "zhs";
            String descriptionPath = langPackDir + java.io.File.separator + "monsterDescription.json";
            
            JSONObject descriptions = new JSONObject(aislayer.AISlayer.loadJson(descriptionPath));
            
            if (descriptions.has(monsterId)) {
                JSONObject monsterDesc = descriptions.getJSONObject(monsterId);
                if (monsterDesc.has("TEXT")) {
                    org.json.JSONArray textArray = monsterDesc.getJSONArray("TEXT");
                    if (textArray.length() > 0) {
                        // 随机选择一个描述（如果有多个）
                        int index = (int) (Math.random() * textArray.length());
                        return textArray.getString(index);
                    }
                }
            }
            
            logger.info("未找到怪物ID " + monsterId + " 的描述");
            return null;
        } catch (Exception e) {
            logger.error("读取怪物描述失败，怪物ID: " + monsterId, e);
            return null;
        }
    }
}