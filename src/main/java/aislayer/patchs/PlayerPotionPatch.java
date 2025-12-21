package aislayer.patchs;

import aislayer.utils.CommentaryUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

/**
 * 监听玩家用药水行动的Patch
 * 监听AbstractPlayer.removePotion方法，这是药水使用的关键步骤
 */
@SpirePatch(
        clz = AbstractPlayer.class,
        method = "removePotion",
        paramtypez = {AbstractPotion.class},
        optional = true
)
public class PlayerPotionPatch {

    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance, AbstractPotion potion) {
        // 只在战斗中触发解说
        if (!isInCombat()) {
            return;
        }
        
        // 检查药水是否有效
        if (!isPotionValid(potion)) {
            return;
        }
        
        try {
            // 获取药水目标（通常是玩家或敌人）
            AbstractCreature target = getPotionTarget(potion);
            
            // 触发解说
            CommentaryUtils.triggerCommentary("用药水", potion, target);
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
     * 检查药水是否有效
     * @param potion 药水
     * @return 是否有效
     */
    private static boolean isPotionValid(AbstractPotion potion) {
        // 检查药水是否为null
        if (potion == null) {
            return false;
        }
        
        // 检查玩家是否为null
        if (AbstractDungeon.player == null) {
            return false;
        }
        
        // 检查药水名称是否有效
        if (potion.name == null || potion.name.isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取药水目标
     * @param potion 药水
     * @return 目标生物
     */
    private static AbstractCreature getPotionTarget(AbstractPotion potion) {
        // 大多数药水以玩家为目标，除非是攻击性药水
        // 这里简化处理，返回玩家作为默认目标
        // 在实际使用中，药水的目标会在use方法中确定
        return AbstractDungeon.player;
    }
}