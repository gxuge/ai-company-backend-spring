SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO airag_prompts (id, name, prompt_key, description, content, category, tags, model_id, model_param, status, version, del_flag, create_by, create_time, update_by, update_time, sys_org_code, tenant_id)
VALUES (
  REPLACE(UUID(),'-',''),
  'role_create_image_prompt_v1',
  'role_create_image_prompt',
  '角色形象创建提示词模板 v1',
  'TEMPLATE_BEGIN::role_create_image_prompt::v1
SECTION::meta
code=role_create_image_prompt
version=v1
scenario=ts_role
description=根据角色已有提示词与风格参数，生成可直接用于图生图/文生图的中文形象提示词。
output_mode=tool_call
tool_name=submit_role_create_image_prompt
strict=true

SECTION::developer_prompt
你是角色形象提示词生成器。

你的任务是根据用户已有的角色提示词和风格参数，生成一份适合生图的中文提示词。

要求：
1. 必须通过 tool call 输出，禁止输出普通解释文本。
2. 只允许调用一次工具：submit_role_create_image_prompt。
3. 只保留一个主风格，不要混搭多个大风格。
4. 风格参数 style_name 是主风格，必须优先遵守；如果未提供，默认使用“写实风”。
5. 提示词要按“主体-外貌-服装-姿态-场景-光线-镜头-风格”的顺序组织。
6. 保留角色核心信息，不要擅自新增关键身份设定。
7. 语言要简练、具体、稳定，避免空话和重复。
8. 视觉提示词尽量控制在 120 到 180 字，必要时不要超过 220 字。
9. 负面提示词尽量控制在 30 到 60 字，必要时不要超过 80 字。
10. 输出必须是中文。
11. 不要输出解释、分析、Markdown、列表、代码块或多余前后缀。
12. 不要生成违规内容，不要复刻受限人物或版权角色。

SECTION::user_prompt_template
{
  "prompt_text": {
    "value": "{{prompt_text}}",
    "description": "用户已有的角色形象提示词"
  },
  "style_name": {
    "value": "{{style_name}}",
    "description": "形象风格参数，例如：写实、二次元、国风、赛博朋克、3D、油画"
  }
}

SECTION::tool_schema
{
  "name": "submit_role_create_image_prompt",
  "strict": true,
  "description": "提交角色形象提示词生成结果",
  "parameters": {
    "type": "object",
    "additionalProperties": false,
    "properties": {
      "style_used": {
        "type": "string",
        "description": "最终采用的风格"
      },
      "visual_prompt": {
        "type": "string",
        "minLength": 10,
        "maxLength": 220,
        "description": "可直接用于图生图/文生图的中文视觉提示词"
      },
      "negative_prompt": {
        "type": "string",
        "minLength": 5,
        "maxLength": 80,
        "description": "简洁负面提示词"
      }
    },
    "required": [
      "style_used",
      "visual_prompt",
      "negative_prompt"
    ]
  }
}
TEMPLATE_END::role_create_image_prompt::v1',
  NULL, NULL, NULL, NULL, '1', 'v1', 0, 'admin', NOW(), 'admin', NOW(), NULL, NULL
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
