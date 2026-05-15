# Prompt ToolCall 閫氱敤瑙勮寖 v2锛?-6锛?
## 1. 鐩爣涓庤寖鍥?- 鐩爣锛氭妸鐜版湁鎵€鏈?`JSON鐩村嚭` 妯℃澘缁熶竴鍗囩骇涓?`ToolCall鐩村嚭`銆?- 鍚庣鑱岃矗鍥哄畾涓猴細`宸ュ叿鍙傛暟瑙ｆ瀽 + 涓氬姟瀛楁鐧藉悕鍗曟彁鍙?+ 鍓嶇杩斿洖`銆?- 鐗堟湰绛栫暐锛氫笉瑕嗙洊鏃х増锛屼繚鐣?`v1(JSON)`锛屾柊澧?`v2(tool_call)`銆?
## 2. 妯℃澘灞傜粺涓€瑙勮寖锛堝鎵€鏈?prompt 鐢熸晥锛?- 妯℃澘蹇呴』鍖呭惈骞舵爣鍑嗗寲浠ヤ笅 section锛?- `SECTION::meta`
- `SECTION::developer_prompt`
- `SECTION::user_prompt_template`
- `SECTION::tool_schema`锛堟柊澧烇紝鏇夸唬/琛ュ厖 `output_schema_hint`锛?
- `SECTION::output_extract` 涓嶅啀鍗曠嫭瀛樺湪锛屽繀椤诲苟鍏?`SECTION::developer_prompt`銆?- 骞跺叆鏂瑰紡瑕佹眰锛氬湪 `developer_prompt` 涓樉寮忓啓鍑衡€滃悗绔櫧鍚嶅崟鎻愬彇瀛楁鈥濓紝涓旀瘡涓瓧娈甸兘瑕佺敤涓枃璇存槑鍏朵笟鍔″惈涔変笌鐢ㄩ€斻€?
## 3. meta 瑙勮寖锛堝浐瀹氬瓧娈碉級
- 蹇呭～锛歚code`銆乣version`銆乣scenario`
- 蹇呭～锛歚output_mode=tool_call`
- 蹇呭～锛歚tool_name=submit_{code}`
- 蹇呭～锛歚strict=true|false`锛堟槸鍚︿弗鏍兼牎楠岋級

## 4. tool_schema 瑙勮寖锛堝繀椤诲畬鏁达級
- 蹇呴』澹版槑锛歚name`銆乣description`銆乣parameters(JSON Schema)`銆乣required`
- 鏋氫妇蹇呴』鏈夎涔夎鏄庯紙鍙斁 `x-enum-descriptions`锛屾垨浣跨敤 `oneOf + description`锛?- `parameters.properties` 涓嬫瘡涓瓧娈甸兘蹇呴』鏈変腑鏂?`description`锛堝己鍒讹級
- 鎺ㄨ崘绾︽潫锛?- `additionalProperties=false`
- 鏁扮粍瀛楁澹版槑 `minItems/maxItems`
- 瀛楃涓插瓧娈靛０鏄?`minLength/maxLength`

- 严格 JSON 输出纪律应写在 `tool_schema.description`：仅允许 tool call 返回结构化 JSON，不得输出解释/Markdown/额外文本。
## 5. 璋冪敤鍗忚瑙勮寖
- 璋冪敤閾剧粺涓€鎶借薄鎴愶細
- `messages`锛坰ystem + user 娓叉煋缁撴灉锛?- `tools`锛坱ool_schema 杞瘧缁撴灉锛?- `tool_choice`锛坮equired + 鎸囧畾 tool_name锛?
## 6. Tool 鍏ュ弬閫氱敤绾︽潫
- 姣忎釜 Prompt 鍙厑璁镐竴涓笟鍔″伐鍏凤紙鍗曟璋冪敤鍗曞伐鍏凤級銆?- 宸ュ叿鍙傛暟蹇呴』鏄粨鏋勫寲 JSON锛岀姝㈣嚜鐒惰瑷€娣锋潅銆?- 鏋氫妇蹇呴』鏄惧紡瀹氫箟涓庤В閲婏紝渚嬪锛?- `emotional`锛氭儏缁洖搴?- `probing`锛氭帰璇㈡帹杩?- `actionable`锛氳交琛屽姩寤鸿

- `SECTION::user_prompt_template` 蹇呴』浣跨敤 JSON 鏍煎紡缁勭粐鍙傛暟銆?- `user_prompt_template` 涓瘡涓弬鏁伴兘蹇呴』闄勫甫涓枃閲婁箟瀛楁锛堜緥濡?`desc`/`鍚箟`锛夛紝涓嶈兘鍙粰瑁稿彉閲忓€笺€?


- 新增或迁移模板文件时，必须沿用原文件编码（含 BOM 与换行风格）创建/保存新文件，防止中文乱码。
