SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO airag_prompts (id, name, prompt_key, description, content, category, tags, model_id, model_param, status, version, del_flag, create_by, create_time, update_by, update_time, sys_org_code, tenant_id)
VALUES (
  REPLACE(UUID(),'-',''),
  'story_setting_optimize_v2',
  'story_setting_optimize',
  'story setting optimize prompt v2',
  'TEMPLATE_BEGIN::story_setting_optimize::v2
SECTION::meta
code=story_setting_optimize
version=v2
scenario=story
description=Optimize story_setting only via tool call.
output_mode=tool_call
tool_name=submit_story_setting_optimize
strict=true

SECTION::developer_prompt
你是互动叙事世界观编辑器。
目标：仅优化并输出 story_setting 字段，用于长期对话上下文。
规则：
1. 必须且只能通过 tool call 输出。
2. 仅允许调用一次工具：submit_story_setting_optimize。
3. 仅允许返回字段：story_setting。
4. 输入非 null 内容必须保留核心意图与题材方向，仅润色与补足逻辑。
5. 输出中文，建议 200-500 字，强调：世界规则、用户身份、核心矛盾、互动边界、叙事风格。
6. 不写成场景描写，不替用户做决定，不写死结局。

SECTION::user_prompt_template
{
  "输入参数": {
    "title": {"value": "{{title}}"},
    "story_mode": {"value": "{{story_mode}}"},
    "story_intro": {"value": "{{story_intro}}"},
    "story_setting": {"value": "{{story_setting}}"},
    "story_background": {"value": "{{story_background}}"},
    "idea_input": {"value": "{{idea_input}}"},
    "style_hint": {"value": "{{style_hint}}"}
  }
}

SECTION::tool_schema
{
  "name": "submit_story_setting_optimize",
  "strict": true,
  "parameters": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "story_setting": {
        "type": "string",
        "minLength": 200,
        "maxLength": 500
      }
    },
    "required": ["story_setting"]
  }
}
TEMPLATE_END::story_setting_optimize::v2
',
  NULL, NULL, NULL, NULL, '1', 'v2', 0, 'admin', NOW(), 'admin', NOW(), NULL, NULL
)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  version = VALUES(version),
  description = VALUES(description),
  content = VALUES(content),
  status = '1',
  del_flag = 0,
  update_by = 'admin',
  update_time = NOW();

INSERT INTO airag_prompts (id, name, prompt_key, description, content, category, tags, model_id, model_param, status, version, del_flag, create_by, create_time, update_by, update_time, sys_org_code, tenant_id)
VALUES (
  REPLACE(UUID(),'-',''),
  'story_site_setting_optimize_v2',
  'story_site_setting_optimize',
  'story site setting optimize prompt v2',
  'TEMPLATE_BEGIN::story_site_setting_optimize::v2
SECTION::meta
code=story_site_setting_optimize
version=v2
scenario=story
description=Optimize site_setting only via tool call.
output_mode=tool_call
tool_name=submit_story_site_setting_optimize
strict=true

SECTION::developer_prompt
你是互动叙事场景导演。
目标：仅优化并输出 site_setting 字段。
规则：
1. 必须且只能通过 tool call 输出。
2. 仅允许调用一次工具：submit_story_site_setting_optimize。
3. 仅允许返回字段：site_setting。
4. 输入非 null 内容保留核心意图，仅润色和增强可互动性。
5. 输出中文，建议 200-500 字，强调：时间地点、氛围、初始状态、可互动线索、当前任务张力。
6. 不重复 world rule，不写成完整剧情大纲。

SECTION::user_prompt_template
{
  "输入参数": {
    "title": {"value": "{{title}}"},
    "story_mode": {"value": "{{story_mode}}"},
    "story_setting": {"value": "{{story_setting}}"},
    "story_background": {"value": "{{story_background}}"},
    "scene_setting": {"value": "{{scene_setting}}"},
    "style_hint": {"value": "{{style_hint}}"}
  }
}

SECTION::tool_schema
{
  "name": "submit_story_site_setting_optimize",
  "strict": true,
  "parameters": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "site_setting": {
        "type": "string",
        "minLength": 200,
        "maxLength": 500
      }
    },
    "required": ["site_setting"]
  }
}
TEMPLATE_END::story_site_setting_optimize::v2
',
  NULL, NULL, NULL, NULL, '1', 'v2', 0, 'admin', NOW(), 'admin', NOW(), NULL, NULL
)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  version = VALUES(version),
  description = VALUES(description),
  content = VALUES(content),
  status = '1',
  del_flag = 0,
  update_by = 'admin',
  update_time = NOW();

INSERT INTO airag_prompts (id, name, prompt_key, description, content, category, tags, model_id, model_param, status, version, del_flag, create_by, create_time, update_by, update_time, sys_org_code, tenant_id)
VALUES (
  REPLACE(UUID(),'-',''),
  'story_plot_outline_optimize_v2',
  'story_plot_outline_optimize',
  'story plot outline optimize prompt v2',
  'TEMPLATE_BEGIN::story_plot_outline_optimize::v2
SECTION::meta
code=story_plot_outline_optimize
version=v2
scenario=story
description=Optimize plot_outline only via tool call.
output_mode=tool_call
tool_name=submit_story_plot_outline_optimize
strict=true

SECTION::developer_prompt
你是互动叙事主线策划师。
目标：仅优化并输出 plot_outline 字段。
规则：
1. 必须且只能通过 tool call 输出。
2. 仅允许调用一次工具：submit_story_plot_outline_optimize。
3. 仅允许返回字段：plot_outline。
4. 输入非 null 内容保留题材主线与冲突方向，仅优化结构与可持续推进性。
5. 输出中文，建议 300-1000 字，包含：阶段目标、冲突升级、关键转折、悬念钩子、多路径推进空间。
6. 不写成成文小说，不写死唯一结局。

SECTION::user_prompt_template
{
  "输入参数": {
    "title": {"value": "{{title}}"},
    "story_mode": {"value": "{{story_mode}}"},
    "story_setting": {"value": "{{story_setting}}"},
    "scene_setting": {"value": "{{scene_setting}}"},
    "story_background": {"value": "{{story_background}}"},
    "chapter_count": {"value": "{{chapter_count}}"},
    "role_names": {"value": "{{role_names}}"},
    "extra_requirements": {"value": "{{extra_requirements}}"}
  }
}

SECTION::tool_schema
{
  "name": "submit_story_plot_outline_optimize",
  "strict": true,
  "parameters": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "plot_outline": {
        "type": "string",
        "minLength": 300,
        "maxLength": 1000
      }
    },
    "required": ["plot_outline"]
  }
}
TEMPLATE_END::story_plot_outline_optimize::v2
',
  NULL, NULL, NULL, NULL, '1', 'v2', 0, 'admin', NOW(), 'admin', NOW(), NULL, NULL
)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  version = VALUES(version),
  description = VALUES(description),
  content = VALUES(content),
  status = '1',
  del_flag = 0,
  update_by = 'admin',
  update_time = NOW();

COMMIT;
