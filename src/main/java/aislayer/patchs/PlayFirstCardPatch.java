package aislayer.patchs;

import aislayer.AISlayer;
import aislayer.panels.ConfigPanel;
import aislayer.utils.BattleStateTracker;
import aislayer.utils.CommentaryUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.actions.animations.TalkAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

import static aislayer.AISlayer.isAIStart;

@SpirePatch(
        clz = AbstractMonster.class,
        method = "update"
)
public class PlayFirstCardPatch {

    @SpirePostfixPatch
    public static void Postfix(AbstractMonster __instance) {
        if (
                __instance.intent != AbstractMonster.Intent.DEBUG
                        && AbstractDungeon.actionManager.phase == GameActionManager.Phase.WAITING_ON_USER
                        && AbstractDungeon.overlayMenu.endTurnButton.enabled
                        && !AISlayer.intentUpdated
                        && isAIStart()
        ) {
            AISlayer.intentUpdated = true;

            // 检查是否需要介绍怪物
            if (ConfigPanel.introduceMonsters && isInCombat()) {
                // 初始化战斗状态跟踪器
                BattleStateTracker tracker = BattleStateTracker.getInstance();
                if (!tracker.isInBattle()) {
                    tracker.startBattle();
                    tracker.updateConfig(ConfigPanel.cardsPerCommentary,
                                       ConfigPanel.introduceMonsters,
                                       ConfigPanel.detailedMonsterIntro);
                    
                    // 触发怪物介绍
                    CommentaryUtils.triggerMonsterIntroduction();
                }
            } else {
                // 如果不介绍怪物，显示思考文本
                String langPackDir = "aislayerResources" + File.separator + "localization" + File.separator + Settings.language.toString().toLowerCase();
                String textPath = langPackDir + File.separator + "text.json";
                JSONArray text = (new JSONObject(AISlayer.loadJson(textPath))).getJSONArray("thinking");
                String thinking = text.getString((int) (Math.random() * text.length()));
                AbstractDungeon.actionManager.addToBottom(new TalkAction(true, thinking, 4.0F, 4.0F));
            }
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
