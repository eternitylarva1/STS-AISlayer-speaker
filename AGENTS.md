# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## 项目概述

这是一个《杀戮尖塔》(Slay the Spire)游戏的Mod，允许使用AI API控制游戏角色。项目使用Java开发，基于ModTheSpire框架。

## 构建和部署

- 使用Maven构建系统，Java 8兼容
- 构建命令: `mvn clean package`
- 构建后自动将JAR复制到Steam游戏目录的mods文件夹
- 依赖的JAR文件路径在pom.xml中通过Steam.path属性配置，默认为"C:\Program Files (x86)\Steam\steamapps"

## 核心架构

- **AISlayer.java**: 核心类，处理游戏状态信息收集和API配置
- **AIUtils.java**: AI工具类，处理API调用和游戏动作执行
- **actions/**: 游戏动作类，实现AI决策后的具体游戏操作
- **patchs/**: 使用SpirePatch框架修改游戏原版行为
- **subscribes/**: BaseMod事件订阅器，处理游戏生命周期事件
- **panels/**: Mod配置界面

## 关键代码模式

- 使用SpirePatch注解修改游戏原版方法，如@SpirePatch和@SpirePostfixPatch
- AI决策通过HTTP调用外部API实现，支持OpenAI兼容格式
- 游戏状态信息转换为JSON格式提供给AI
- 使用反射访问游戏私有字段，如pressProceedButton()方法
- 多语言支持通过JSON文件实现，位于aislayerResources/localization/

## API集成

- 支持两个AI平台配置，可在ConfigPanel中切换
- API URL自动处理逻辑在handleApiUrl()方法中实现
- 支持的工具函数: playCard, endTurn, usePotion, select, boolean
- API调用在独立线程中执行，避免阻塞游戏主线程

## 重要注意事项

- 游戏状态信息收集依赖AbstractDungeon全局对象
- AI决策通过GameActionManager.addToBottom()添加到动作队列
- 本地化文件路径使用File.separator构建跨平台兼容路径
- ModTheSpire.json定义了Mod的基本信息和依赖关系