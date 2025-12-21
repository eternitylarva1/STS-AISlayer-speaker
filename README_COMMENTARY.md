# AI爬塔解说Mod

## 概述

这是一个《杀戮尖塔》游戏的Mod，将原有的AI自动操作功能改造为玩家行动解说功能。当玩家在游戏中进行各种操作时，AI会提供实时的解说评论。

## 功能特性

### 核心功能
- **实时解说**: 监听玩家的打牌、用药水、结束回合等行动并提供AI解说
- **智能监听**: 自动识别游戏中的各种选择操作（卡牌选择、遗物选择、火堆选择等）
- **多语言支持**: 支持中文和英文解说
- **可配置性**: 提供丰富的配置选项

### 解说类型
- 打牌行动解说
- 药水使用解说
- 回合结束解说
- 选择操作解说（卡牌、遗物、火堆、地图等）

## 安装和使用

### 安装步骤
1. 确保已安装ModTheSpire框架
2. 将编译好的JAR文件放入Steam游戏目录的mods文件夹
3. 启动游戏

### 配置设置
在游戏Mod配置界面中可以设置：
- **启用AI解说**: 开关解说功能
- **解说频率**: 控制解说的频繁程度
- **解说风格**: 选择解说的风格（幽默、专业等）
- **显示解说历史**: 是否显示解说历史记录

### API配置
需要配置AI API以获得解说内容：
- 支持多个AI平台（如DeepSeek、OpenAI等）
- 可配置API密钥和请求地址
- 可选择不同的AI模型

## 快捷键

- **F1**: 切换解说显示/隐藏
- **F2**: 清空解说历史
- **F3**: 重置解说系统
- **F4**: 显示统计信息（调试模式）

## 技术架构

### 核心组件
- **AISlayer.java**: 游戏状态信息收集和增强
- **AIUtils.java**: AI API调用和逻辑处理
- **CommentaryUtils.java**: 解说功能专门处理
- **CommentaryDisplay.java**: 解说显示系统

### 监听组件
- **PlayerActionPatch.java**: 监听玩家打牌行动
- **PlayerPotionPatch.java**: 监听玩家用药水行动
- **EndTurnPatch.java**: 监听玩家结束回合行动
- **SelectionPatch.java**: 监听各种选择操作

### 集成组件
- **CommentarySubscribe.java**: 解说系统集成和管理
- **ConfigPanel.java**: 配置界面
- **本地化文件**: 多语言支持

## 使用说明

### 基本使用
1. 在Mod配置中启用AI解说功能
2. 配置有效的AI API密钥
3. 开始游戏，AI会自动为你的行动提供解说

### 解说示例
- 打出攻击牌时："精彩的出牌！对敌人造成了有效伤害！"
- 使用药水时："明智的药水使用！在关键时刻恢复了生命值！"
- 结束回合时："回合结束，期待下一轮的精彩表现！"

## 故障排除

### 常见问题
1. **解说不显示**: 检查API配置是否正确，确保解说功能已启用
2. **解说延迟**: 检查网络连接和API响应速度
3. **解说频率过高**: 调整解说频率设置

### 调试信息
在调试模式下，可以使用F4键查看详细的系统状态信息。

## 开发说明

### 代码结构
```
src/main/java/aislayer/
├── AISlayer.java              # 核心状态收集
├── utils/
│   ├── AIUtils.java          # AI API调用
│   └── CommentaryUtils.java  # 解说功能
├── patchs/                   # 游戏Hook
│   ├── PlayerActionPatch.java
│   ├── PlayerPotionPatch.java
│   ├── EndTurnPatch.java
│   └── SelectionPatch.java
├── ui/
│   └── CommentaryDisplay.java # 解说显示
├── panels/
│   └── ConfigPanel.java      # 配置界面
└── subscribes/
    ├── Subscribe.java        # 主订阅器
    └── CommentarySubscribe.java # 解说系统集成
```

### 扩展开发
如需添加新的解说类型，可以：
1. 在相应的Patch类中添加监听逻辑
2. 在CommentaryUtils中添加新的解说处理
3. 更新本地化文件添加相关文本

## 版本历史

### v1.0.0
- 初始版本发布
- 基础解说功能实现
- 支持打牌、用药水、结束回合解说
- 基础配置界面

## 许可证

本项目基于原AI爬塔Mod进行改造开发，遵循相应的开源许可证。

## 贡献

欢迎提交Issue和Pull Request来改进这个Mod。

## 联系方式

如有问题或建议，请通过GitHub Issues联系。