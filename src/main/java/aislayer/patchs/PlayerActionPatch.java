package aislayer.patchs;

import aislayer.panels.ConfigPanel;
import aislayer.utils.BattleStateTracker;
import aislayer.utils.CommentaryUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

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
                tracker.recordCardPlay(card, target);
            }
            
            // 根据配置决定是否触发解说
            if (CommentaryUtils.shouldTriggerCommentaryByCards()) {
                CommentaryUtils.triggerCommentary("打牌", card, target);
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
        
        // 检查卡牌是否在手牌中（已经打出的卡牌应该不在手牌中）
        if (com.megacrit.cardcrawl.dungeons.AbstractDungeon.player.hand.group.contains(card)) {
            return false;
        }
        
        // 对于需要目标的卡牌，检查目标是否有效
        if (card.target != AbstractCard.CardTarget.NONE && target == null) {
            return false;
        }
        
        return true;
    }
}