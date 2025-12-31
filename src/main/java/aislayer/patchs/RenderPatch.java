package aislayer.patchs;

import aislayer.subscribes.CommentarySubscribe;
import aislayer.utils.AIUtils;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;

/**
 * 渲染Patch，用于在游戏渲染循环中调用解说系统渲染
 */
@SpirePatch(
        clz = AbstractDungeon.class,
        method = "render",
        paramtypez = {SpriteBatch.class}
)
public class RenderPatch {

    @SpirePostfixPatch
    public static void Postfix(AbstractDungeon __instance, SpriteBatch sb) {
        try {
            // 调用解说系统渲染
            CommentarySubscribe.render(sb);
            
            // 渲染"请求解说中"文字
            if (AIUtils.isRequestingCommentary) {
                float x = Settings.WIDTH / 2.0f;
                float y = Settings.HEIGHT - 100.0f * Settings.scale;
                FontHelper.renderFontCentered(sb, FontHelper.tipBodyFont, "请求解说中...", x, y, Color.GOLD);
            }
        } catch (Exception e) {
            // 静默处理异常，避免影响游戏渲染
            System.out.println("请求异常");
        }
    }
}