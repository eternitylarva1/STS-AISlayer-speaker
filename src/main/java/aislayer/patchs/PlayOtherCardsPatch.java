package aislayer.patchs;

import aislayer.AISlayer;
import aislayer.utils.AIUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import static aislayer.AISlayer.getInfo;
import static aislayer.AISlayer.isAIStart;

@SpirePatch(
        clz = GameActionManager.class,
        method = "update"
)
public class PlayOtherCardsPatch {
    @SpireInsertPatch(
            rloc = 11,
            localvars = {"currentAction"}
    )
    public static void Insert(AbstractGameAction currentAction) {
        if (
                currentAction == null
                        && AbstractDungeon.overlayMenu.endTurnButton.enabled
                        && AISlayer.intentUpdated
                        && isAIStart()
        ) {

            // 禁用AI自动操作，只保留解说功能
            // String todo = "现在你可以选择使用药水、打出手牌、结束回合";
            // AIUtils.action(getInfo(todo));

        }
    }
}