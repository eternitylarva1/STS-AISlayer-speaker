package aislayer.patchs;

import aislayer.subscribes.CommentarySubscribe;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

/**
 * 渲染Patch，用于在游戏渲染循环中调用解说系统渲染
 */
@SpirePatch(
        clz = AbstractDungeon.class,
        method = "render",
        paramtypez = {SpriteBatch.class},
        optional = true
)
public class RenderPatch {

    @SpirePostfixPatch
    public static void Postfix(AbstractDungeon __instance, SpriteBatch sb) {
        try {
            // 调用解说系统渲染
            CommentarySubscribe.render(sb);
        } catch (Exception e) {
            // 静默处理异常，避免影响游戏渲染
        }
    }
}