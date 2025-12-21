package aislayer.patchs;

import aislayer.utils.CommentaryUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

/**
 * 监听房间更新的Patch，用于检测各种选择操作
 */
@SpirePatch(
        clz = AbstractRoom.class,
        method = "update",
        optional = true
)
public class SelectionPatch {

    @SpirePostfixPatch
    public static void Postfix(AbstractRoom __instance) {
        try {
            // 检查是否在事件房间且有选项
            if (__instance.event != null && AbstractDungeon.screen.toString().equals("EVENT")) {
                CommentaryUtils.triggerCommentary("选择", "事件选择", "面临重要的抉择");
            }
            
            // 检查是否在商店
            if (__instance.getClass().getSimpleName().equals("ShopRoom") && 
                AbstractDungeon.screen.toString().equals("SHOP")) {
                CommentaryUtils.triggerCommentary("选择", "商店购买", "考虑购买卡牌或遗物");
            }
        } catch (Exception e) {
            // 静默处理异常
        }
    }
}