# ADR 0021：精简预设标签关联模型

## 状态
已接受，2026-08-30。

## 背景
现有生成预设通过 `ts_preset_tag` 关联大量 `ts_tag`，运行时再按标签类型分组并拼接 Prompt。角色标签、标签关系和用户标签表未形成稳定业务闭环，部分表为空，维护成本高于当前收益。

## 决策
1. 删除 `ts_role_tag`、`ts_tag_relation`、`ts_user_role_tag`、`ts_user_preference_tag`、`ts_preset_tag`。
2. 保留 `ts_preset`、`role_core_fill_preset`、`story_core_fill_preset` 及现有 preset 生成接口。
3. preset 生成只使用预设名称、预设描述和用户输入，不再加载、分组或拼接标签。
4. 生成快照保留预设身份信息，删除预设标签明细和标签分组字段。
5. 前端删除角色标签选择和管理端预设标签关联功能。

## 影响
- 接口地址保持兼容，调用方无需切换 URL。
- 预设质量由 `ts_preset.description` 承担，运营侧需要维护完整描述。
- 删除的数据无法通过迁移自动恢复，回滚时需要数据库备份。
