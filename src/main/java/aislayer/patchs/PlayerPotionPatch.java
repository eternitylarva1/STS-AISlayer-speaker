package aislayer.patchs;

import aislayer.utils.CommentaryUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

/**
 * 监听玩家用药水行动的Patch
 */
@SpirePatch(
        clz = AbstractPotion.class,
        method = "use",
        paramtypez = {AbstractCreature.class},
        optional = true
)
public class PlayerPotionPatch {

    @SpirePostfixPatch
    public static void Postfix(AbstractPotion __instance, AbstractCreature target) {
        // 只在战斗中触发解说
        if (!isInCombat()) {
            return;
        }
        
        // 检查药水是否成功使用
        if (!wasPotionSuccessfullyUsed(__instance, target)) {
            return;
        }
        
        try {
            // 触发解说
            CommentaryUtils.triggerCommentary("用药水", __instance, target);
        } catch (Exception e) {
            // 静默处理异常，避免影响游戏正常进行
        }
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
    
    /**
     * 检查药水是否成功使用
     * @param potion 使用的药水
     * @param target 目标
     * @return 是否成功使用
     */
    private static boolean wasPotionSuccessfullyUsed(AbstractPotion potion, AbstractCreature target) {
        // 检查药水是否为null
        if (potion == null) {
            return false;
        }
        
        // 检查玩家是否为null
        if (AbstractDungeon.player == null) {
            return false;
        }
        
        // 检查目标是否有效
        if (target == null) {
            return false;
        }
        
        // 检查是否是玩家使用的药水（通过检查药水是否在玩家药水栏中）
        boolean isPlayerPotion = false;
        for (AbstractPotion playerPotion : AbstractDungeon.player.potions) {
            if (playerPotion == potion) {
                isPlayerPotion = true;
                break;
            }
        }
        
        return isPlayerPotion;
    }
}