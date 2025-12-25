package aislayer.utils;

/**
 * 解说模式枚举
 * 定义不同的解说触发模式
 */
public enum CommentaryMode {
    /**
     * 按牌数解说模式
     * 每打N张牌触发一次解说
     */
    BY_CARDS(0, "按牌数解说"),
    
    /**
     * 回合结束解说模式
     * 只在回合结束时触发解说
     */
    BY_TURN_END(1, "回合结束解说");
    
    private final int value;
    private final String description;
    
    CommentaryMode(int value, String description) {
        this.value = value;
        this.description = description;
    }
    
    /**
     * 获取枚举对应的数值
     * @return 数值
     */
    public int getValue() {
        return value;
    }
    
    /**
     * 获取枚举的描述
     * @return 描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据数值获取枚举
     * @param value 数值
     * @return 对应的枚举，如果无效则返回BY_CARDS
     */
    public static CommentaryMode fromValue(int value) {
        for (CommentaryMode mode : values()) {
            if (mode.value == value) {
                return mode;
            }
        }
        return BY_CARDS; // 默认值
    }
    
    /**
     * 检查是否为按牌数解说模式
     * @return 是否为按牌数解说模式
     */
    public boolean isByCards() {
        return this == BY_CARDS;
    }
    
    /**
     * 检查是否为回合结束解说模式
     * @return 是否为回合结束解说模式
     */
    public boolean isByTurnEnd() {
        return this == BY_TURN_END;
    }
}