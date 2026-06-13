package com.dkd.framework.aspectj;

import com.dkd.common.annotation.DataScope;
import com.dkd.common.core.domain.BaseEntity;
import com.dkd.common.core.domain.entity.SysRole;
import com.dkd.common.core.domain.entity.SysUser;
import com.dkd.common.core.domain.model.LoginUser;
import com.dkd.common.core.text.Convert;
import com.dkd.common.utils.SecurityUtils;
import com.dkd.common.utils.StringUtils;
import com.dkd.framework.security.context.PermissionContextHolder;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据过滤处理
 *
 * @author ruoyi
 */
@Aspect
@Component
public class DataScopeAspect
{
    /**
     * 全部数据权限
     */
    public static final String DATA_SCOPE_ALL = "1";

    /**
     * 自定数据权限
     */
    public static final String DATA_SCOPE_CUSTOM = "2";

    /**
     * 部门数据权限
     */
    public static final String DATA_SCOPE_DEPT = "3";

    /**
     * 部门及以下数据权限
     */
    public static final String DATA_SCOPE_DEPT_AND_CHILD = "4";

    /**
     * 仅本人数据权限
     */
    public static final String DATA_SCOPE_SELF = "5";

    /**
     * 数据权限过滤关键字
     */
    public static final String DATA_SCOPE = "dataScope";

    @Before("@annotation(controllerDataScope)")
    public void doBefore(JoinPoint point, DataScope controllerDataScope) throws Throwable
    {
        // 清理数据范围（权限）过滤条件（params.dataScope）防止sql注入
        clearDataScope(point);
        // 设置数据范围（权限）过滤条件
        handleDataScope(point, controllerDataScope);
    }

    protected void handleDataScope(final JoinPoint joinPoint, DataScope controllerDataScope)
    {
        // 获取当前的登录用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNotNull(loginUser))
        {
            // 获取用户基本信息
            SysUser currentUser = loginUser.getUser();
            // 如果是超级管理员，则不过滤数据
            if (StringUtils.isNotNull(currentUser) && !currentUser.isAdmin())
            {
                // 获取目标方法的权限字符串  例如：用户列表（system:user:list）
                String permission = StringUtils.defaultIfEmpty(controllerDataScope.permission(), PermissionContextHolder.getContext());
                // 设置数据范围（权限）过滤条件，根据当前用户、部门别名、用户别名和权限标识对切点对象进行过滤处理
                dataScopeFilter(joinPoint, currentUser, controllerDataScope.deptAlias(),
                        controllerDataScope.userAlias(), permission);
            }
        }
    }

    /**
     * 数据范围过滤
     *
     * @param joinPoint 切点
     * @param user 用户
     * @param deptAlias 部门别名
     * @param userAlias 用户别名
     * @param permission 权限字符
     */
    public static void dataScopeFilter(JoinPoint joinPoint, SysUser user, String deptAlias, String userAlias, String permission)
    {
        // 构建SQL字符串，用于拼接数据范围条件
        StringBuilder sqlString = new StringBuilder();
        // 用于存储已经添加的数据范围类型，避免重复添加
        List<String> conditions = new ArrayList<String>();

        // 遍历用户的所有角色
        for (SysRole role : user.getRoles())
        {
            // 获取当前角色的数据范围条件类型 1~5
            String dataScope = role.getDataScope();
            // 如果数据范围类型是自定义类型且已添加，则跳过本次循环
            if (!DATA_SCOPE_CUSTOM.equals(dataScope) && conditions.contains(dataScope))
            {
                continue;
            }
            // 如果当前角色权限列表不包含目标方法的权限字符串（system:user:list），则跳过本次循环
            if (StringUtils.isNotEmpty(permission) && StringUtils.isNotEmpty(role.getPermissions())
                    && !StringUtils.containsAny(role.getPermissions(), Convert.toStrArray(permission)))
            {
                continue;
            }
            // 如果角色的数据范围类型是全部数据，则清空SQL字符串并添加数据范围类型，结束循环
            if (DATA_SCOPE_ALL.equals(dataScope))
            {
                sqlString = new StringBuilder();
                conditions.add(dataScope);
                break;
            }
            // 如果角色的数据范围类型是自定义数据
            else if (DATA_SCOPE_CUSTOM.equals(dataScope))
            {
                // 拼接SQL条件，限制部门ID在角色所关联的部门范围内
                sqlString.append(StringUtils.format(
                        " OR {}.dept_id IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = {} ) ", deptAlias,
                        role.getRoleId()));
            }
            // 如果角色的数据范围类型是本部门数据
            else if (DATA_SCOPE_DEPT.equals(dataScope))
            {
                // 拼接SQL条件，限制部门ID等于用户所在部门ID
                sqlString.append(StringUtils.format(" OR {}.dept_id = {} ", deptAlias, user.getDeptId()));
            }
            // 如果角色的数据范围类型是本部门及子部门数据
            else if (DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope))
            {
                // 拼接SQL条件，限制部门ID等于用户所在部门ID或在用户所在部门的子孙部门中
                sqlString.append(StringUtils.format(
                        " OR {}.dept_id IN ( SELECT dept_id FROM sys_dept WHERE dept_id = {} or find_in_set( {} , ancestors ) )",
                        deptAlias, user.getDeptId(), user.getDeptId()));
            }
            // 如果角色的数据范围类型是仅本人数据
            else if (DATA_SCOPE_SELF.equals(dataScope))
            {
                // 如果用户表别名不为空，拼接SQL条件限制用户ID等于当前用户ID
                if (StringUtils.isNotBlank(userAlias))
                {
                    sqlString.append(StringUtils.format(" OR {}.user_id = {} ", userAlias, user.getUserId()));
                }
                else
                { // 否则，拼接SQL条件限制部门ID为0，即不查询任何数据
                    sqlString.append(StringUtils.format(" OR {}.dept_id = 0 ", deptAlias));
                }
            }
            // 添加当前角色的数据范围类型条件
            conditions.add(dataScope);
        }

        // 如果数据范围类型集合（即多角色情况下，所有角色都不包含传递过来目标方法的权限字符），则添加一个条件使SQL查询不返回任何数据
        if (StringUtils.isEmpty(conditions))
        {
            sqlString.append(StringUtils.format(" OR {}.dept_id = 0 ", deptAlias));
        }

        // 如果SQL字符串不为空，则将构造好的数据范围条件添加到方法参数对象中，用于后续的SQL查询
        if (StringUtils.isNotBlank(sqlString.toString()))
        {
            // 获取切点方法的第一个参数
            Object params = joinPoint.getArgs()[0];
            // 检查参数是否非空且为BaseEntity类型
            if (StringUtils.isNotNull(params) && params instanceof BaseEntity)
            {
                // 将参数转换为BaseEntity类型
                BaseEntity baseEntity = (BaseEntity) params;
                // 向BaseEntity的params属性中添加数据范围条件
                baseEntity.getParams().put(DATA_SCOPE, " AND (" + sqlString.substring(4) + ")");
            }
        }
    }

    /**
     * 拼接权限sql前先清空params.dataScope参数防止注入
     */
    private void clearDataScope(final JoinPoint joinPoint)
    {
        // 获取切点方法的第一个参数
        Object params = joinPoint.getArgs()[0];
        // 检查参数不为null且是否为BaseEntity类型
        if (StringUtils.isNotNull(params) && params instanceof BaseEntity)
        {
            // 将参数转为BaseEntity类型
            BaseEntity baseEntity = (BaseEntity) params;
            // 将数据权限过滤条件设置为空
            baseEntity.getParams().put(DATA_SCOPE, "");
        }
    }
}
