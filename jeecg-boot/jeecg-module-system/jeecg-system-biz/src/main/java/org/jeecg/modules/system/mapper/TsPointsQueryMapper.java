package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.dto.tspoints.TsPointsAdminAccountQueryDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsAdminTransactionQueryDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsTransactionQueryDto;
import org.jeecg.modules.system.entity.TsPointsTransaction;
import org.jeecg.modules.system.entity.TsUserPointsAccount;
import org.jeecg.modules.system.vo.tspoints.TsPointsAdminAccountVo;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;

/** 积分账户与流水查询 Mapper。 */
public interface TsPointsQueryMapper {

    /** 幂等创建零余额账户。 */
    int insertAccountIgnore(@Param("userId") String userId);

    /** 查询账户。 */
    TsUserPointsAccount selectAccount(@Param("userId") String userId);

    /** 锁定账户作为用户记账串行点。 */
    TsUserPointsAccount selectAccountForUpdate(@Param("userId") String userId);

    /** 原子增加余额与累计收入。 */
    int increaseBalance(@Param("id") Long id, @Param("amount") Long amount);

    /** 原子扣减余额与累计支出。 */
    int deductBalance(@Param("id") Long id, @Param("amount") Long amount);

    /** 按用户与幂等 Key 查询流水。 */
    TsPointsTransaction selectByIdempotencyKey(
            @Param("userId") String userId,
            @Param("idempotencyKey") String idempotencyKey);

    /** 按流水号锁定原流水。 */
    TsPointsTransaction selectTransactionForUpdate(
            @Param("userId") String userId,
            @Param("transactionNo") String transactionNo);

    /** 汇总原消费流水已经成功返还的积分。 */
    Long sumSuccessfulRefunds(@Param("originalTransactionNo") String originalTransactionNo);

    /** 查询用户积分流水。 */
    Page<TsPointsTransactionVo> selectUserTransactionPage(
            Page<TsPointsTransactionVo> page,
            @Param("userId") String userId,
            @Param("query") TsPointsTransactionQueryDto query);

    /** 查询用户积分流水详情。 */
    TsPointsTransactionVo selectUserTransactionDetail(
            @Param("userId") String userId,
            @Param("id") Long id);

    /** 后台查询积分账户。 */
    Page<TsPointsAdminAccountVo> selectAdminAccountPage(
            Page<TsPointsAdminAccountVo> page,
            @Param("query") TsPointsAdminAccountQueryDto query);

    /** 后台查询积分流水。 */
    Page<TsPointsTransactionVo> selectAdminTransactionPage(
            Page<TsPointsTransactionVo> page,
            @Param("query") TsPointsAdminTransactionQueryDto query);
}
