# AI爬塔Mod更新日志

## v2.0.0 - 增强版发布 (2025-12-22)

### 🎉 重大更新

#### 🎙️ AI解说系统全面重构
- **怪物介绍功能**：战斗开始时自动介绍怪物信息，替代原来的"让俺寻思寻思"
- **解说模式切换**：支持按牌数解说或回合结束解说两种模式
- **解说频率控制**：可自定义每几张牌触发一次解说
- **战斗状态跟踪**：实时跟踪战斗状态，提供更丰富的解说内容

#### ⚙️ 新增配置选项
- `commentaryMode`: 解说模式（0=按牌数解说，1=回合结束解说）
- `cardsPerCommentary`: 每几张牌解说一次
- `introduceMonsters`: 是否在战斗开始时介绍怪物
- `monsterIntroDetail`: 怪物介绍详细程度（0=简单，1=详细）

#### 🌍 多语言支持完善
- 完整的中文和英文界面支持
- 新增怪物介绍和解说模板的本地化
- 支持动态语言切换

#### 🔧 性能优化
- 异步API调用，避免阻塞游戏主线程
- 智能缓存机制，减少重复API调用
- 可配置的超时和重试机制
- 内存使用优化

### 📋 新增文件

#### 核心功能类
- `src/main/java/aislayer/utils/TurnData.java` - 回合数据跟踪类
- `src/main/java/aislayer/utils/BattleStateTracker.java` - 战斗状态跟踪器
- `src/main/java/aislayer/patchs/StartTurnPatch.java` - 回合开始事件监听

#### 配置和文档
- `performance.properties` - 性能优化配置文件
- `测试指南.md` - 详细的功能测试指南
- `AI解说Mod增强开发方案.md` - 完整的开发方案文档

### 🔄 重构文件

#### 解说系统重构
- `src/main/java/aislayer/utils/CommentaryUtils.java`
  - 新增怪物介绍功能
  - 新增解说模式检查逻辑
  - 优化解说队列管理
  - 增强错误处理机制

- `src/main/java/aislayer/utils/AIUtils.java`
  - 重构解说提示词构建逻辑
  - 增加战斗状态信息集成
  - 优化API调用处理

#### Patch系统更新
- `src/main/java/aislayer/patchs/PlayFirstCardPatch.java`
  - 集成怪物介绍功能
  - 添加战斗状态跟踪初始化

- `src/main/java/aislayer/patchs/EndTurnPatch.java`
  - 集成解说模式检查
  - 添加回合结束状态跟踪

- `src/main/java/aislayer/patchs/PlayerActionPatch.java`
  - 集成按牌数解说逻辑
  - 添加出牌状态记录

#### 配置系统扩展
- `src/main/java/aislayer/panels/ConfigPanel.java`
  - 新增4个配置选项
  - 优化配置界面布局

#### 本地化文件更新
- `src/main/resources/aislayerResources/localization/zhs/ui.json`
- `src/main/resources/aislayerResources/localization/eng/ui.json`
- `src/main/resources/aislayerResources/localization/zhs/text.json`
- `src/main/resources/aislayerResources/localization/eng/text.json`

### 🐛 修复问题
- 修复解说队列处理的并发问题
- 优化内存使用，防止内存泄漏
- 改进错误处理，提高系统稳定性
- 修复多语言显示问题

### 📈 性能改进
- API调用响应时间优化
- 减少不必要的游戏状态检查
- 优化解说文本处理逻辑
- 改进缓存机制

### 🔒 安全性增强
- 增强API调用的错误处理
- 添加超时保护机制
- 改进异常恢复逻辑

### 📚 文档完善
- 全新的README.md文档
- 详细的API使用说明
- 完整的配置指南
- 故障排除指南

## v1.0.0 - 原始版本

### 🎯 基础功能
- AI控制游戏角色
- 基础解说系统
- 简单的配置界面
- API集成功能

### 📋 初始文件结构
- 基础的AI控制逻辑
- 简单的解说功能
- 基本的配置系统

---

## 升级指南

### 从v1.0.0升级到v2.0.0

1. **备份现有配置**
   - 备份当前的配置文件
   - 记录自定义的API设置

2. **安装新版本**
   - 替换JAR文件
   - 确保所有依赖文件完整

3. **配置迁移**
   - 重新配置API设置
   - 调整新的解说选项
   - 根据需要设置性能参数

4. **功能测试**
   - 按照测试指南验证新功能
   - 检查解说模式是否正常工作
   - 验证怪物介绍功能

### 配置迁移说明

#### 新增配置项
```properties
# 解说模式（0=按牌数解说，1=回合结束解说）
commentaryMode=0

# 每几张牌解说一次
cardsPerCommentary=3

# 是否在战斗开始时介绍怪物
introduceMonsters=true

# 怪物介绍详细程度（0=简单，1=详细）
monsterIntroDetail=1
```

#### 性能配置（可选）
```properties
# 解说API调用超时时间（秒）
commentary.timeout=10

# 解说冷却时间（毫秒）
commentary.cooldown=3000

# 战斗状态历史记录最大数量
battle.history.max_turns=50
```

## 已知问题

### v2.0.0已知问题
1. 在极低网络环境下，解说可能出现延迟
2. 某些特殊怪物状态可能影响介绍准确性
3. 多语言切换需要重启游戏生效

### 计划修复
- 优化网络异常处理
- 改进怪物状态检测
- 支持热切换语言

## 反馈和支持

如果您在使用过程中遇到问题或有改进建议，请：
1. 查看故障排除指南
2. 检查已知问题列表
3. 提交详细的Issue报告

---

**感谢您使用AI爬塔Mod增强版！** 🎮✨