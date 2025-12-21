package aislayer.patchs;

import aislayer.utils.CommentaryUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

/**
 * 监听玩家结束回合行动的Patch
 */
@SpirePatch(
        clz = GameActionManager.class,
        method = "endTurn",
        optional = true
)
public class EndTurnPatch {

    @SpirePostfixPatch
    public static void Postfix(GameActionManager __instance) {
        // 只在战斗中触发解说
        if (!isInCombat()) {
            return;
        }
        
        // 检查是否是玩家主动结束回合
        if (!wasPlayerEndTurn()) {
            return;
        }
        
        try {
            // 触发解说
            CommentaryUtils.triggerCommentary("结束回合");
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
     * 检查是否是玩家主动结束回合
     * @return 是否是玩家主动结束回合
     */
    private static boolean wasPlayerEndTurn() {
        // 检查游戏动作管理器是否在正确的状态
        if (AbstractDungeon.actionManager == null) {
            return false;
        }
        
        // 检查是否在等待玩家输入的阶段（这通常意味着玩家刚刚结束了回合）
        return AbstractDungeon.actionManager.phase == GameActionManager.Phase.WAITING_ON_USER;
    }
}