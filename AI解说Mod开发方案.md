# AI爬塔解说Mod开发方案

## 项目概述

### 目标
将现有的AI自动操作Mod改造为玩家行动解说Mod，在玩家做出游戏行动时提供AI解说。

### 改造原则
- 保持现有架构稳定性，最小化代码改动
- 保留现有的游戏状态收集机制
- 重构AI调用逻辑，从决策模式改为解说模式
- 确保用户体验流畅，不影响游戏正常进行

## 技术架构设计

### 现有架构分析
- **AISlayer.java**: 核心类，处理游戏状态信息收集
- **AIUtils.java**: AI工具类，处理API调用和游戏动作执行
- **patchs/**: 使用SpirePatch框架修改游戏原版行为
- **actions/**: 游戏动作类，实现AI决策后的具体游戏操作
- **panels/**: Mod配置界面

### 新架构设计
```
解说Mod架构
├── 游戏状态收集层 (保留现有AISlayer.java)
├── 玩家行动监听层 (新增SpirePatch)
├── AI解说处理层 (重构AIUtils.java)
├── 解说显示层 (新增UI组件)
└── 配置管理层 (更新ConfigPanel.java)
```

## 详细实现计划

### 第一阶段：核心架构调整

#### 任务1.1：修改AISlayer.java
- **目标**: 增强游戏状态收集，支持解说模式
- **具体工作**:
  - 添加`getActionInfo(String actionType, Object... params)`方法
  - 修改`getInfo(String todo)`方法，增加行动类型参数
  - 实现状态变化检测功能
- **交付物**: 更新的AISlayer.java

#### 任务1.2：重构AIUtils.java
- **目标**: 分离决策逻辑和解说逻辑
- **具体工作**:
  - 保留现有API配置和错误处理机制
  - 新增`getCommentary(JSONObject actionInfo)`方法
  - 重构`action()`方法，分离为`executeAction()`和`showCommentary()`
- **交付物**: 重构的AIUtils.java

#### 任务1.3：创建CommentaryUtils.java
- **目标**: 专门处理解说功能
- **具体工作**:
  - 实现解说提示词模板
  - 实现解说缓存机制
  - 实现解说冷却和队列管理
- **交付物**: 新的CommentaryUtils.java

### 第二阶段：玩家行动监听

#### 任务2.1：实现PlayerActionPatch.java
- **目标**: 监听玩家打牌行动
- **技术要点**:
  ```java
  @SpirePatch(clz = AbstractPlayer.class, method = "useCard", 
              paramtypez = {AbstractCard.class, AbstractMonster.class, int.class})
  public static class PlayerActionPatch {
      @SpirePostfixPatch
      public static void Postfix(AbstractPlayer __instance, AbstractCard card, 
                                AbstractMonster target, int energyOnUse) {
          // 收集打牌行动信息并触发解说
      }
  }
  ```
- **交付物**: PlayerActionPatch.java

#### 任务2.2：实现PlayerPotionPatch.java
- **目标**: 监听玩家用药水行动
- **技术要点**: 监听AbstractPlayer.usePotion方法
- **交付物**: PlayerPotionPatch.java

#### 任务2.3：实现EndTurnPatch.java
- **目标**: 监听玩家结束回合行动
- **技术要点**: 监听GameActionManager.endTurn方法
- **交付物**: EndTurnPatch.java

#### 任务2.4：实现SelectionPatch.java
- **目标**: 监听各种选择操作（遗物、卡牌、奖励等）
- **技术要点**: 监听AbstractDungeon.screen相关方法
- **交付物**: SelectionPatch.java

### 第三阶段：解说系统集成

#### 任务3.1：实现AI解说API调用
- **目标**: 调用AI API获取解说内容
- **具体工作**:
  - 设计解说提示词模板
  - 实现API调用逻辑
  - 处理解说内容格式化
- **交付物**: 完整的getCommentary方法

#### 任务3.2：实现解说显示系统
- **目标**: 在游戏中显示解说内容
- **具体工作**:
  - 使用TalkAction显示简短解说
  - 创建CommentaryDisplay.java管理解说显示
  - 实现解说历史记录功能
- **交付物**: CommentaryDisplay.java和相关UI组件

#### 任务3.3：集成测试
- **目标**: 测试解说功能完整性
- **测试场景**:
  - 打牌行动解说
  - 用药水行动解说
  - 结束回合解说
  - 选择操作解说
- **交付物**: 测试报告和问题修复

### 第四阶段：配置和优化

#### 任务4.1：更新ConfigPanel.java
- **目标**: 添加解说相关配置选项
- **配置项**:
  - 解说开关
  - 解说频率控制
  - API配置（复用现有）
  - 解说风格选择
- **交付物**: 更新的ConfigPanel.java

#### 任务4.2：性能优化
- **目标**: 优化解说系统性能
- **优化措施**:
  - 实现解说冷却时间
  - 实现解说队列机制
  - 添加本地缓存
  - 优化API调用频率
- **交付物**: 性能优化代码

#### 任务4.3：错误处理和重试机制
- **目标**: 提高系统稳定性
- **具体工作**:
  - 完善API调用错误处理
  - 实现自动重试机制
  - 添加降级处理（API失败时的备选方案）
- **交付物**: 错误处理代码

## 代码结构变更

### 新增文件
```
src/main/java/aislayer/
├── patchs/
│   ├── PlayerActionPatch.java
│   ├── PlayerPotionPatch.java
│   ├── EndTurnPatch.java
│   └── SelectionPatch.java
├── utils/
│   └── CommentaryUtils.java
└── ui/
    └── CommentaryDisplay.java
```

### 修改文件
```
src/main/java/aislayer/
├── AISlayer.java (增强状态收集)
├── AIUtils.java (重构解说逻辑)
├── panels/ConfigPanel.java (添加解说配置)
└── subscribes/Subscribe.java (可能需要调整)
```

### 本地化文件更新
```
src/main/resources/aislayerResources/localization/
├── zhs/
│   ├── text.json (添加解说相关文本)
│   └── ui.json (添加解说配置文本)
└── eng/
    ├── text.json (添加解说相关文本)
    └── ui.json (添加解说配置文本)
```

## 风险评估和缓解措施

### 技术风险
1. **SpirePatch兼容性问题**
   - 风险：游戏更新导致Patch失效
   - 缓解：使用稳定的Hook点，添加optional参数

2. **API调用性能问题**
   - 风险：频繁API调用影响游戏性能
   - 缓解：实现冷却时间和队列机制

3. **游戏状态同步问题**
   - 风险：状态收集时机不当
   - 缓解：使用PostfixPatch确保行动完成后再收集

### 用户体验风险
1. **解说频率过高**
   - 风险：影响游戏体验
   - 缓解：提供频率控制和开关选项

2. **解说内容质量**
   - 风险：AI解说质量不稳定
   - 缓解：优化提示词，添加内容过滤

## 测试和验证计划

### 单元测试
- 测试游戏状态收集功能
- 测试API调用逻辑
- 测试解说显示功能

### 集成测试
- 测试完整的解说流程
- 测试各种游戏场景下的解说
- 测试配置系统功能

### 用户验收测试
- 邀请玩家测试解说体验
- 收集反馈并优化
- 性能压力测试

## 部署和维护计划

### 构建和部署
- 使用现有Maven构建系统
- 保持与原版Mod相同的部署流程
- 确保向后兼容性

### 后续维护
- 定期更新SpirePatch以适配游戏版本
- 根据用户反馈优化解说质量
- 持续优化性能和用户体验

## 总结

本开发方案提供了一个系统性的改造路径，将AI自动操作Mod转换为玩家行动解说Mod。通过分阶段实施，可以确保改造过程的稳定性和可控性。方案充分考虑了技术实现、用户体验和性能优化等多个方面，为项目的成功实施提供了详细的指导。