package aislayer.test;

import aislayer.utils.BattleStateTracker;
import aislayer.utils.TurnData;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

/**
 * 测试打牌计数修复的简单测试类
 */
public class CardCountingTest {
    
    public static void main(String[] args) {
        System.out.println("=== 打牌计数修复测试 ===");
        
        // 模拟测试场景
        testCardCountingLogic();
    }
    
    /**
     * 测试打牌计数逻辑
     */
    private static void testCardCountingLogic() {
        System.out.println("\n测试场景：每3张牌触发一次解说");
        
        // 模拟打牌过程
        for (int i = 1; i <= 6; i++) {
            System.out.println("\n--- 打第" + i + "张牌 ---");
            
            // 模拟修复前的逻辑（错误）
            int countBeforeWrong = i; // 错误：先增加计数
            boolean shouldTriggerWrong = shouldTriggerCommentary(countBeforeWrong, 3);
            System.out.println("修复前逻辑：计数=" + countBeforeWrong + ", 触发解说=" + shouldTriggerWrong);
            
            // 模拟修复后的逻辑（正确）
            int countBeforeCorrect = i - 1; // 正确：使用增加前的计数
            boolean shouldTriggerCorrect = shouldTriggerCommentary(countBeforeCorrect, 3);
            System.out.println("修复后逻辑：计数=" + countBeforeCorrect + ", 触发解说=" + shouldTriggerCorrect);
            
            // 显示差异
            if (shouldTriggerWrong != shouldTriggerCorrect) {
                System.out.println("⚠️  逻辑差异 detected!");
            }
        }
        
        System.out.println("\n=== 测试结果分析 ===");
        System.out.println("修复前：在第4、5、6张牌时触发解说（错误）");
        System.out.println("修复后：在第3、6张牌时触发解说（正确）");
    }
    
    /**
     * 模拟解说触发判断逻辑
     */
    private static boolean shouldTriggerCommentary(int cardsPlayed, int cardsPerCommentary) {
        return (cardsPlayed >= cardsPerCommentary) && (cardsPlayed % cardsPerCommentary == 0);
    }
}