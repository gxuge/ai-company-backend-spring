import { BasicColumn } from '/@/components/Table';
import { FormSchema } from '/@/components/Table';
import {duplicateCheckDelay} from "@/views/system/user/user.api";
export const DESFORM_NAME_MAX_LENGTH = 40;
import {pinyin} from "pinyin-pro";
//列表数据
export const columns: BasicColumn[] = [
  {
    title: '名称',
    align: 'center',
    dataIndex: 'name',
  },
  {
    title: '功能描述',
    align: 'center',
    dataIndex: 'description',
  },
  // {
  //   title: '状态',
  //   align: 'center',
  //   dataIndex: 'status',
  // },
  {
    title: '最近提交人',
    align: 'center',
    dataIndex: 'updateBy',
  },
  {
    title: '最近提交时间',
    align: 'center',
    dataIndex: 'updateTime',
  },
  {
    title: '创建人',
    align: 'center',
    dataIndex: 'createBy',
  },
  {
    title: '创建时间',
    align: 'center',
    dataIndex: 'createTime',
  }
];
//查询数据
export const searchFormSchema: FormSchema[] = [
    {
      label: '名称',
      field: 'name',
      component: 'Input',
    },
];
// 名称最大长度
export const NAME_MAX_LENGTH = 40;
// 编码最大长度
export const CODE_MAX_LENGTH = 50;
// 描述最大长度（后端字段 varchar(255)）
export const DESC_MAX_LENGTH = 255;
//表单数据
export const formSchema: FormSchema[] = [
  {
    label: '名称',
    field: 'name',
    component: 'Input',
    componentProps: ({ formModel }) => {
      return {
        placeholder: '例如：SQL转换',
        maxlength: DESFORM_NAME_MAX_LENGTH,
        showCount: true,
        onChange: (e: ChangeEvent) => {
          if(formModel.id){
            return
          }
          let code = pinyin(e.target.value, {
            toneType: 'none',
            type: 'array',
            nonZh: 'consecutive',
          }).join('_');
          code = code.replace(/[^a-zA-Z0-9_\-]/g, '');
          const versionMatch = code.match(/^(.*)_(v[\w.-]+)$/i);
          if (versionMatch && versionMatch[1]) {
            formModel.promptKey = versionMatch[1];
            if (!formModel.version) {
              formModel.version = versionMatch[2].toLowerCase();
            }
          } else {
            formModel.promptKey = code;
          }
        },
      };
    },
    dynamicRules() {
      return [
        {required: true, message: '请输入提示词名称'},
        {
          max: NAME_MAX_LENGTH,
          message: `名称长度不能超过${NAME_MAX_LENGTH}个字符`,
        },
      ];
    }
  },
  {
    label: '提示词编码',
    field: 'promptKey',
    component: 'Input',
    dynamicRules({ model }) {
      return [
        { required: true, message: '提示词编码' },
        {
          async validator(_, value) {
            if (value?.length > CODE_MAX_LENGTH) {
              throw `编码长度不能超过${CODE_MAX_LENGTH}个字符`;
            }
            const pattern = /^[a-z|A-Z][a-z|A-Z\d_-]*$/;
            if (!pattern.test(value)) {
              throw '编码必须以字母开头，可包含数字、下划线、横杠';
            } else if (/[A-Z]/.test(value)) {
              throw '不支持大写字母';
            } else {
              const res = await duplicateCheckDelay({
                tableName: 'airag_prompts',
                fieldName: 'prompt_key',
                fieldVal: value,
                dataId: model.id,
              }) as any;
              if (!res.success) {
                throw '表单编码已存在！';
              }
            }
          },
        },
      ];
    }
  },
  {
    label: '版本',
    field: 'version',
    component: 'Input',
    defaultValue: 'v1',
    dynamicRules() {
      return [
        { required: true, message: '请输入版本号，例如 v1' },
        {
          validator(_, value) {
            const pattern = /^v[\w.-]+$/i;
            if (!pattern.test(value || '')) {
              return Promise.reject('版本格式应为 v1 / v2 / v1.0');
            }
            return Promise.resolve();
          },
        },
      ];
    },
  },
  {
    label: '提示词功能描述',
    field: 'description',
    component: 'InputTextArea',
    componentProps: {
      maxlength: DESC_MAX_LENGTH,
      showCount: true,
      rows: 3,
      placeholder: '简短说明（不超过255字符）',
    },
    dynamicRules() {
      return [
        {
          max: DESC_MAX_LENGTH,
          message: `描述长度不能超过${DESC_MAX_LENGTH}个字符`,
        },
      ];
    },
  },
  {
    label: '模板内容',
    field: 'content',
    component: 'InputTextArea',
    componentProps: {
      rows: 10,
      placeholder: '请输入完整模板文本（含 TEMPLATE_BEGIN / SECTION / TEMPLATE_END）',
    },
    dynamicRules() {
      return [{ required: true, message: '请输入模板内容' }];
    },
  },
  {
    label: '',
    field: 'id',
    component: 'Input',
    show: false,
  },
];

/**
 * 流程表单调用这个方法获取formSchema
 * @param param
 */
export function getBpmFormSchema(_formData): FormSchema[] {
  // 默认和原始表单保持一致 如果流程中配置了权限数据，这里需要单独处理formSchema
  return formSchema;
}
