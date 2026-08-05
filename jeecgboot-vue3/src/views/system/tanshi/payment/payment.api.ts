import { defHttp } from '/@/utils/http/axios';

enum Api {
  page = '/sys/ts-member-admin/payment/page',
  detail = '/sys/ts-member-admin/payment/detail',
}

export const pagePayments = (data) => defHttp.post({ url: Api.page, data });
export const getPaymentDetail = (data) => defHttp.post({ url: Api.detail, data });
