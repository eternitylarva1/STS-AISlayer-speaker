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
        org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(StartTurnPatch.class.getName());
        logger.info("StartTurnPatch.Postfix调用");
        
        // 检查是否进入战斗房间
        boolean inCombat = isInCombat();
        logger.info("战斗状态检查：" + inCombat);
        
        if (!inCombat) {
            return;
        }
        
        try {
            // 初始化战斗状态跟踪器
            BattleStateTracker tracker = BattleStateTracker.getInstance();
            logger.info("BattleStateTracker状态：inBattle=" + tracker.isInBattle());
            
            if (!tracker.isInBattle()) {
                logger.info("开始新战斗");
                tracker.startBattle();
                tracker.updateConfig(ConfigPanel.cardsPerCommentary,
                                   ConfigPanel.introduceMonsters,
                                   ConfigPanel.detailedMonsterIntro);
            }
            
            // 开始新回合
            logger.info("开始新回合");
            tracker.startNewTurn();
            logger.info("新回合开始完成，当前回合数：" + tracker.getCurrentTurn());
            
        } catch (Exception e) {
            logger.error("StartTurnPatch异常", e);
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