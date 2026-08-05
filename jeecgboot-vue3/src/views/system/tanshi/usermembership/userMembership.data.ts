import { BasicColumn } from '/@/components/Table';

export const columns: BasicColumn[] = [
  { title: '用户账号', dataIndex: 'username', width: 140 },
  { title: '用户姓名', dataIndex: 'realname', width: 120 },
  { title: '用户ID', dataIndex: 'userId', width: 220 },
  { title: '会员等级', dataIndex: 'planName', width: 120 },
  { title: '套餐周期', dataIndex: 'cycleType', width: 100 },
  { title: '生效时间', dataIndex: 'startTime', width: 170 },
  { title: '到期时间', dataIndex: 'endTime', width: 170 },
  { title: '状态', dataIndex: 'status', width: 90, slots: { customRender: 'status' } },
  { title: '自动续费', dataIndex: 'autoRenew', width: 100, slots: { customRender: 'autoRenew' } },
];
