package aislayer.patchs;

import aislayer.AISlayer;
import aislayer.panels.ConfigPanel;
import aislayer.utils.AIUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.neow.NeowRoom;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import static aislayer.AISlayer.*;
import static com.megacrit.cardcrawl.shop.Merchant.NAMES;

public class EnterRoomPatch {

    public static final Logger logger = LogManager.getLogger(EnterRoomPatch.class.getName());

    private static void preEnter() {
        if (isAIStart()) {
            AISlayer.knownCards.clear();
            AISlayer.knownPotions.clear();
            AISlayer.knownRelics.clear();
            AISlayer.knownKeywords.clear();
            AISlayer.allDescriptions.clear();
            AISlayer.allCards.clear();
            AISlayer.allPotions.clear();
            AISlayer.allRelics.clear();

            AIUtils.messagesArray = new JSONArray();
        }
    }

    private  static void postEnter(MapRoomNode roomNode) {
        if (isAIStart()) {
            AbstractRoom room = roomNode.getRoom();
            String roomName = room.getClass().getSimpleName();
            String todo = "";
            switch (roomName) {
                case "TreasureRoom":
                    todo = "用boolean选择是否打开宝箱(可能有遗物、蓝宝石)";
                    break;
                case "TreasureRoomBoss":
                    todo = "用boolean选择是否打开BOSS宝箱(BOSS遗物三选一)";
                    break;
                case "ShopRoom":
                    AbstractDungeon.overlayMenu.proceedButton.setLabel(NAMES[0]);
                    AbstractDungeon.shopScreen.open();
                    break;
                case "NeowRoom":
                case "EventRoom":
                default:
                    logger.info("当前房间: {}({})", roomName, roomNode);
                    break;
            }
            // 禁用AI自动操作，只保留解说功能
            // if (!todo.isEmpty()) {
            //     AIUtils.action(getInfo(todo));
            // }
        }
    }

    @SpirePatch(
            clz = AbstractDungeon.class,
            method = "populatePathTaken"
    )
    public static class EnterRoomLoad {
        @SpirePrefixPatch
        public static void Prefix() {
            preEnter();
        }
        @SpirePostfixPatch
        public static void Postfix() {
            postEnter(AbstractDungeon.getCurrMapNode());
        }
    }

    @SpirePatch(
            clz = SaveFile.class,
            method = SpirePatch.CONSTRUCTOR,
            paramtypez = {
                    SaveFile.SaveType.class
            }
    )
    public static class EnterRoomSave {
        @SpirePrefixPatch
        public static void Prefix() {
            preEnter();
        }
        @SpirePostfixPatch
        public static void Postfix() {
            if (AbstractDungeon.nextRoom != null && !(AbstractDungeon.getCurrRoom() instanceof NeowRoom)) {
                postEnter(AbstractDungeon.nextRoom);
            } else {
                postEnter(AbstractDungeon.getCurrMapNode());
            }
        }
    }

}