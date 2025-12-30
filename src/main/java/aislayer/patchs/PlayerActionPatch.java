package aislayer.patchs;

import aislayer.panels.ConfigPanel;
import aislayer.utils.BattleStateTracker;
import aislayer.utils.CommentaryUtils;
import aislayer.utils.TurnData;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 监听玩家打牌行动的Patch
 */
@SpirePatch(
        clz = AbstractPlayer.class,
        method = "useCard",
        paramtypez = {AbstractCard.class, AbstractMonster.class, int.class},
        optional = true
)
public class PlayerActionPatch {
    
    private static final Logger logger = LogManager.getLogger(PlayerActionPatch.class.getName());

    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance, AbstractCard card, AbstractMonster target, int energyOnUse) {
        // 只在战斗中触发解说
        if (!isInCombat()) {
            return;
        }
        
        // 检查卡牌是否成功使用（有些卡牌可能因为各种原因无法使用）
        if (!wasCardSuccessfullyUsed(card, target)) {
            return;
        }
        
        try {
            // 更新战斗状态跟踪器
            BattleStateTracker tracker = BattleStateTracker.getInstance();
            if (tracker.isInBattle()) {
                // 先获取当前计数（增加前的计数）
                TurnData currentTurn = tracker.getCurrentTurnData();
                int cardsPlayedBefore = currentTurn != null ? currentTurn.getPlayedCardsCount() : 0;
                
                // 根据配置决定是否触发解说
                // 使用增加前的牌数进行判断，确保计数准确
                if (CommentaryUtils.shouldTriggerCommentaryByCardsWithCount(cardsPlayedBefore)) {
                    CommentaryUtils.triggerCommentary("打牌", card, target);
                }
                
                // 然后再增加计数
                tracker.recordCardPlay(card, target);
                
                // 添加调试日志
                logger.info("打牌计数更新：增加前=" + cardsPlayedBefore +
                           ", 增加后=" + (currentTurn != null ? currentTurn.getPlayedCardsCount() : 0) +
                           ", 卡牌=" + (card != null ? card.name : "null"));
            }
        } catch (Exception e) {
            // 静默处理异常，避免影响游戏正常进行
        }
    }
    
    /**
     * 检查是否在战斗中
     * @return 是否在战斗中
     */
    private static boolean isInCombat() {
        if (com.megacrit.cardcrawl.dungeons.AbstractDungeon.getCurrRoom() == null) {
            return false;
        }
        
        AbstractRoom room = com.megacrit.cardcrawl.dungeons.AbstractDungeon.getCurrRoom();
        return room.phase == AbstractRoom.RoomPhase.COMBAT;
    }
    
    /**
     * 检查卡牌是否成功使用
     * @param card 使用的卡牌
     * @param target 目标
     * @return 是否成功使用
     */
    private static boolean wasCardSuccessfullyUsed(AbstractCard card, AbstractMonster target) {
        // 检查卡牌是否为null
        if (card == null) {
            return false;
        }
        
        // 检查玩家是否为null
        if (com.megacrit.cardcrawl.dungeons.AbstractDungeon.player == null) {
            return false;
        }
        
        // 移除目标检查，因为：
        // 1. 任何类型的卡牌目标都可能为null
        // 2. 后续代码（BattleStateTracker和TurnData）已经正确处理null目标
        // 3. 过于严格的目标检查会阻止某些卡牌被统计
        
        return true;
    }
}