package aislayer.patchs;

import aislayer.panels.ConfigPanel;
import aislayer.utils.BattleStateTracker;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

/**
 * 监听回合开始事件的Patch
 */
@SpirePatch(
        clz = AbstractDungeon.class,
        method = "nextRoomTransition",
        paramtypez = {},
        optional = true
)
public class StartTurnPatch {

    @SpirePostfixPatch
    public static void Postfix() {
        // 检查是否进入战斗房间
        if (!isInCombat()) {
            return;
        }
        
        try {
            // 初始化战斗状态跟踪器
            BattleStateTracker tracker = BattleStateTracker.getInstance();
            if (!tracker.isInBattle()) {
                tracker.startBattle();
                tracker.updateConfig(ConfigPanel.cardsPerCommentary,
                                   ConfigPanel.introduceMonsters,
                                   ConfigPanel.detailedMonsterIntro);
            }
            
            // 开始新回合
            tracker.startNewTurn();
            
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
}