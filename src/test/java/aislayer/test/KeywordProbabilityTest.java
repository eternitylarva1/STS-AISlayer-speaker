package aislayer.test;

import aislayer.panels.ConfigPanel;
import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 测试关键词触发概率范围限制
 */
public class KeywordProbabilityTest extends TestCase {
    
    private static final Logger logger = LogManager.getLogger(KeywordProbabilityTest.class.getName());
    
    public void testKeywordProbabilityRange() {
        logger.info("开始测试关键词触发概率范围限制");
        
        // 测试正常范围内的值
        testKeywordProbability(0.0f, "0.0");
        testKeywordProbability(0.5f, "0.5");
        testKeywordProbability(1.0f, "1.0");
        
        // 测试超出范围的值
        testKeywordProbability(-0.5f, "-0.5");
        testKeywordProbability(1.5f, "1.5");
        testKeywordProbability(-1.0f, "-1.0");
        testKeywordProbability(2.0f, "2.0");
        
        logger.info("关键词触发概率范围限制测试完成");
    }
    
    private void testKeywordProbability(float inputValue, String testName) {
        logger.info("测试值: " + testName);
        
        // 设置概率值
        ConfigPanel.setKeywordTriggerProbability(inputValue);
        
        // 获取实际值
        float actualValue = ConfigPanel.getKeywordTriggerProbability();
        
        // 验证范围
        boolean inRange = (actualValue >= 0.0f && actualValue <= 1.0f);
        
        logger.info("输入值: " + inputValue + ", 实际值: " + actualValue + ", 范围正确: " + inRange);
        
        if (!inRange) {
            logger.error("错误：概率值不在0-1范围内！");
            fail("概率值不在0-1范围内：输入值=" + inputValue + ", 实际值=" + actualValue);
        } else {
            logger.info("✓ 概率值范围限制正常");
        }
        
        // 验证边界值
        if (inputValue < 0.0f) {
            assertEquals("负值应被限制为0.0", 0.0f, actualValue, 0.001f);
        } else if (inputValue > 1.0f) {
            assertEquals("超过1的值应被限制为1.0", 1.0f, actualValue, 0.001f);
        } else {
            assertEquals("正常范围内的值应保持不变", inputValue, actualValue, 0.001f);
        }
    }
}