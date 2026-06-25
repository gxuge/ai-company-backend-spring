package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.mapper.SysUserMapper;
import org.jeecg.modules.system.vo.tspublicchannel.TsPublicManageUserOptionVo;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 公开管理通用下拉选项接口。
 */
@Tag(name = "TsPublicManageOptions 公开管理下拉")
@RestController
@RequiresAuthentication
@RequestMapping("/sys/ts-public-manage")
public class TsPublicManageOptionsController {

    private final SysUserMapper sysUserMapper;

    public TsPublicManageOptionsController(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    @Operation(summary = "公开管理用户下拉")
    @GetMapping("/user-options")
    public Result<Page<TsPublicManageUserOptionVo>> pageUserOptions(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "pageNo", required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        Page<SysUser> page = new Page<>(normalizePageNo(pageNo), normalizePageSize(pageSize));
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        String normalizedKeyword = trimToNull(keyword);
        wrapper.select("id", "username", "realname", "update_time", "create_time");
        wrapper.eq("del_flag", 0);
        if (StringUtils.hasText(normalizedKeyword)) {
            wrapper.and(q -> q.like("username", normalizedKeyword).or().like("realname", normalizedKeyword));
        }
        wrapper.orderByDesc("update_time");
        wrapper.orderByDesc("create_time");
        wrapper.orderByDesc("id");
        Page<SysUser> pageData = sysUserMapper.selectPage(page, wrapper);

        Page<TsPublicManageUserOptionVo> resultPage = new Page<>(pageData.getCurrent(), pageData.getSize(), pageData.getTotal());
        List<TsPublicManageUserOptionVo> records = new ArrayList<>();
        if (pageData.getRecords() != null) {
            for (SysUser user : pageData.getRecords()) {
                TsPublicManageUserOptionVo option = new TsPublicManageUserOptionVo();
                option.setId(user.getId());
                option.setUsername(user.getUsername());
                option.setRealname(user.getRealname());
                option.setDisplayName(buildDisplayName(user.getRealname(), user.getUsername()));
                records.add(option);
            }
        }
        resultPage.setRecords(records);
        return Result.OK(resultPage);
    }

    private long normalizePageNo(Integer value) {
        return value == null || value < 1 ? 1L : value;
    }

    private long normalizePageSize(Integer value) {
        if (value == null || value < 1) {
            return 20L;
        }
        return Math.min(value, 100);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String buildDisplayName(String realname, String username) {
        String normalizedRealname = trimToNull(realname);
        String normalizedUsername = trimToNull(username);
        if (StringUtils.hasText(normalizedRealname) && StringUtils.hasText(normalizedUsername)) {
            return normalizedRealname + "（" + normalizedUsername + "）";
        }
        return StringUtils.hasText(normalizedRealname) ? normalizedRealname : normalizedUsername;
    }
}
