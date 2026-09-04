package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsUserFollow;
import org.jeecg.modules.system.po.tsuserfollow.TsUserFollowQueryPo;
import org.jeecg.modules.system.vo.tsuserfollow.TsUserFollowVo;

import java.util.Date;

/** 用户关注数据访问层。 */
public interface TsUserFollowMapper extends BaseMapper<TsUserFollow> {

    /** 分页查询当前用户关注的用户。 */
    Page<TsUserFollowVo> selectFollowingPage(
            Page<TsUserFollowVo> page, @Param("query") TsUserFollowQueryPo query);

    /** 分页查询当前用户的粉丝。 */
    Page<TsUserFollowVo> selectFollowerPage(
            Page<TsUserFollowVo> page, @Param("query") TsUserFollowQueryPo query);

    /** 查询当前用户是否已关注目标用户。 */
    int countActiveFollow(
            @Param("followerUserId") String followerUserId,
            @Param("followedUserId") String followedUserId);

    /** 查询目标用户是否正常可访问。 */
    int countAvailableUser(@Param("userId") String userId);

    /** 查询目标用户有效粉丝数量。 */
    long countFollowers(@Param("userId") String userId);

    /** 查询目标用户有效关注数量。 */
    long countFollowing(@Param("userId") String userId);

    /** 新增关注或恢复已取消的关注。 */
    int upsertFollow(
            @Param("followerUserId") String followerUserId,
            @Param("followedUserId") String followedUserId,
            @Param("now") Date now);

    /** 取消当前用户对目标用户的有效关注。 */
    int cancelFollow(
            @Param("followerUserId") String followerUserId,
            @Param("followedUserId") String followedUserId,
            @Param("now") Date now);
}
