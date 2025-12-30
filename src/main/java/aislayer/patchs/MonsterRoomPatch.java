package aislayer.patchs;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import aislayer.utils.BattleStateTracker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MonsterRoom进入补丁
 * 用于在玩家进入怪物房间（战斗开始）时初始化战斗状态跟踪
 */
@SpirePatch(clz = MonsterRoom.class, method = "onPlayerEntry")
public class MonsterRoomPatch {
    private static final Logger logger = LogManager.getLogger(MonsterRoomPatch.class.getName());
    
    @SpirePostfixPatch
    public static void onPlayerEntry(MonsterRoom __instance) {
        logger.info("MonsterRoom.onPlayerEntry被调用，初始化战斗状态跟踪");
        
        try {
            // 初始化战斗状态跟踪
            BattleStateTracker.getInstance().startBattle();
            logger.info("战斗状态跟踪初始化完成");
        } catch (Exception e) {
            logger.error("初始化战斗状态跟踪时发生错误", e);
        }
    }
}