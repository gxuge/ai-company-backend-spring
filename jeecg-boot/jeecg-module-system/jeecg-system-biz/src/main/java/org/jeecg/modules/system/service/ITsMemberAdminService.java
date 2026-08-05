package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminConfigSaveDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminDeleteDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminIdDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminMembershipQueryDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminMembershipSaveDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminQuotaSaveDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsPaymentAdminQueryDto;
import org.jeecg.modules.system.vo.tsmemberadmin.TsMemberAdminConfigVo;
import org.jeecg.modules.system.vo.tsmemberadmin.TsMemberAdminMembershipDetailVo;
import org.jeecg.modules.system.vo.tsmemberadmin.TsMemberAdminMembershipVo;
import org.jeecg.modules.system.vo.tsmemberadmin.TsPaymentAdminDetailVo;
import org.jeecg.modules.system.vo.tsmemberadmin.TsPaymentAdminVo;

/** 会员后台管理服务。 */
public interface ITsMemberAdminService {
    /** 查询全部会员配置。 */
    TsMemberAdminConfigVo getConfig();
    /** 保存会员配置。 */
    void saveConfig(TsMemberAdminConfigSaveDto request);
    /** 删除会员配置。 */
    void deleteConfig(TsMemberAdminDeleteDto request);
    /** 分页查询用户会员。 */
    Page<TsMemberAdminMembershipVo> pageMemberships(TsMemberAdminMembershipQueryDto request);
    /** 保存用户会员。 */
    void saveMembership(TsMemberAdminMembershipSaveDto request);
    /** 删除用户会员。 */
    void deleteMembership(TsMemberAdminIdDto request);
    /** 查询用户会员详情。 */
    TsMemberAdminMembershipDetailVo getMembershipDetail(TsMemberAdminIdDto request);
    /** 保存用户权益额度。 */
    void saveQuota(TsMemberAdminQuotaSaveDto request);
    /** 删除用户权益额度。 */
    void deleteQuota(TsMemberAdminIdDto request);
    /** 分页查询支付流水。 */
    Page<TsPaymentAdminVo> pagePayments(TsPaymentAdminQueryDto request);
    /** 查询支付流水详情。 */
    TsPaymentAdminDetailVo getPaymentDetail(TsMemberAdminIdDto request);
}
