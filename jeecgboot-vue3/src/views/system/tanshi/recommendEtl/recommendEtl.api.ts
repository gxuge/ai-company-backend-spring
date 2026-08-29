import { defHttp } from '/@/utils/http/axios';

export type EtlId = string | number;

export interface EtlPage<T> {
  records: T[];
  total: number;
  current?: number;
  size?: number;
  pages?: number;
}

export interface EtlTask {
  id?: EtlId;
  taskName?: string;
  recommendType?: 'ROLE' | 'STORY';
  timeRangeMode?: 'FIXED' | 'RECENT_DAYS';
  startTime?: string;
  endTime?: string;
  recentDays?: number;
  scriptPath?: string;
  outputDir?: string;
  storageType?: 'LOCAL' | 'OSS';
  trainRatio?: number;
  evalRatio?: number;
  runParamsJson?: string;
  cronExpression?: string;
  enabled?: number;
  timeoutSeconds?: number;
  runningExecutionId?: EtlId;
  lastRunAt?: string;
  createTime?: string;
  updateTime?: string;
}

export interface EtlExecution {
  id?: EtlId;
  taskId?: EtlId;
  taskName?: string;
  recommendType?: 'ROLE' | 'STORY';
  triggerType?: 'MANUAL' | 'SCHEDULED';
  status?: 'WAITING' | 'RUNNING' | 'SUCCESS' | 'FAILED';
  rangeStartTime?: string;
  rangeEndTime?: string;
  argumentsJson?: string;
  startedAt?: string;
  finishedAt?: string;
  durationMs?: number;
  processExitCode?: number;
  trainCount?: number;
  evalCount?: number;
  positiveCount?: number;
  negativeCount?: number;
  trainPath?: string;
  evalPath?: string;
  resultJson?: string;
  logPath?: string;
  logContent?: string;
  errorCode?: string;
  errorMessage?: string;
  createTime?: string;
}

export interface EtlTaskQuery {
  keyword?: string;
  recommendType?: string;
  enabled?: number;
  pageNo?: number;
  pageSize?: number;
}

export interface EtlExecutionQuery {
  taskId?: EtlId;
  keyword?: string;
  recommendType?: string;
  status?: string;
  triggerType?: string;
  pageNo?: number;
  pageSize?: number;
}

enum Api {
  taskPage = '/sys/ts-recommend-etl/task/page',
  taskDetail = '/sys/ts-recommend-etl/task/detail',
  taskCreate = '/sys/ts-recommend-etl/task/create',
  taskUpdate = '/sys/ts-recommend-etl/task/update',
  taskDelete = '/sys/ts-recommend-etl/task/delete',
  taskToggle = '/sys/ts-recommend-etl/task/toggle',
  taskExecute = '/sys/ts-recommend-etl/task/execute',
  executionPage = '/sys/ts-recommend-etl/execution/page',
  executionDetail = '/sys/ts-recommend-etl/execution/detail',
}

export const pageEtlTasks = (data: EtlTaskQuery) => defHttp.post<EtlPage<EtlTask>>({ url: Api.taskPage, data });

export const getEtlTask = (params: { id: EtlId }) => defHttp.get<EtlTask>({ url: Api.taskDetail, params });

export const createEtlTask = (data: EtlTask) => defHttp.post<EtlTask>({ url: Api.taskCreate, data });

export const updateEtlTask = (data: EtlTask) => defHttp.post<EtlTask>({ url: Api.taskUpdate, data });

export const deleteEtlTask = (data: { id: EtlId }) => defHttp.post({ url: Api.taskDelete, data });

export const toggleEtlTask = (data: { id: EtlId; enabled: number }) => defHttp.post<EtlTask>({ url: Api.taskToggle, data });

export const executeEtlTask = (data: { id: EtlId }) => defHttp.post<EtlExecution>({ url: Api.taskExecute, data });

export const pageEtlExecutions = (data: EtlExecutionQuery) => defHttp.post<EtlPage<EtlExecution>>({ url: Api.executionPage, data });

export const getEtlExecution = (params: { id: EtlId }) => defHttp.get<EtlExecution>({ url: Api.executionDetail, params });
