package aislayer.patchs;

import aislayer.panels.ConfigPanel;
import aislayer.utils.BattleStateTracker;
import aislayer.utils.CommentaryUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    @SpirePostfixPatch
    public static void Postfix(GameActionManager __instance) {
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
            if (tracker.isInBattle()) {
                tracker.endCurrentTurn();
            }
            
            // 根据配置决定是否触发解说
            boolean shouldTrigger = CommentaryUtils.shouldTriggerCommentaryByTurnEnd();
            logger.info("结束回合：解说模式检查 - commentaryByCards=" + ConfigPanel.commentaryByCards + 
                       ", shouldTrigger=" + shouldTrigger);
            
            if (shouldTrigger) {
                logger.info("结束回合：触发解说");
                CommentaryUtils.triggerCommentary("结束回合");
            } else {
                logger.info("结束回合：当前配置不触发解说（可能是按牌数模式）");
            }
        } catch (Exception e) {
            logger.error("结束回合解说处理异常", e);
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