package com.dkd.quartz.util;

import com.dkd.common.constant.Constants;
import com.dkd.common.constant.ScheduleConstants;
import com.dkd.common.utils.ExceptionUtil;
import com.dkd.common.utils.StringUtils;
import com.dkd.common.utils.bean.BeanUtils;
import com.dkd.common.utils.spring.SpringUtils;
import com.dkd.quartz.domain.SysJob;
import com.dkd.quartz.domain.SysJobLog;
import com.dkd.quartz.service.ISysJobLogService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * 抽象quartz调用
 *
 * @author ruoyi
 */
public abstract class AbstractQuartzJob implements Job
{
    private static final Logger log = LoggerFactory.getLogger(AbstractQuartzJob.class);

    /**
     * 线程本地变量
     */
    private static ThreadLocal<Date> threadLocal = new ThreadLocal<>();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        // 初始化任务配置对象
        SysJob sysJob = new SysJob();
        // 从任务上下文中复制属性到任务配置对象
        BeanUtils.copyBeanProp(sysJob, context.getMergedJobDataMap().get(ScheduleConstants.TASK_PROPERTIES));
        try
        {
            // 执行任务前的处理
            before(context, sysJob);
            // 如果任务不为空，就执行此任务
            if (sysJob != null)
            {
                // 任务有 允许并发执行/不允许并发执行 两种
                doExecute(context, sysJob);
            }
            // 执行任务后的处理
            after(context, sysJob, null);
        }
        catch (Exception e)
        {
            log.error("任务执行异常  - ：", e);
            after(context, sysJob, e);
        }
    }

    /**
     * 执行前
     *
     * @param context 工作执行上下文对象
     * @param sysJob 系统计划任务
     */
    protected void before(JobExecutionContext context, SysJob sysJob)
    {
        // 线程局部变量存入当前时间
        threadLocal.set(new Date());
    }

    /**
     * 执行后
     *
     * @param context 工作执行上下文对象
     * @param sysJob 系统计划任务
     */
    protected void after(JobExecutionContext context, SysJob sysJob, Exception e)
    {
        // 从线程局部变量中获取当前时间
        Date startTime = threadLocal.get();
        // 清空线程局部变量
        threadLocal.remove();

        // 记录任务日志
        final SysJobLog sysJobLog = new SysJobLog();
        sysJobLog.setJobName(sysJob.getJobName());
        sysJobLog.setJobGroup(sysJob.getJobGroup());
        sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
        sysJobLog.setStartTime(startTime);
        sysJobLog.setStopTime(new Date());
        // 计算运行时间
        long runMs = sysJobLog.getStopTime().getTime() - sysJobLog.getStartTime().getTime();
        sysJobLog.setJobMessage(sysJobLog.getJobName() + " 总共耗时：" + runMs + "毫秒");
        // 存在异常，记录异常信息
        if (e != null)
        {   // 设置任务失败标志
            sysJobLog.setStatus(Constants.FAIL);
            // 设置异常信息
            String errorMsg = StringUtils.substring(ExceptionUtil.getExceptionMessage(e), 0, 2000);
            sysJobLog.setExceptionInfo(errorMsg);
        }
        else
        {
            // 设置任务成功标志
            sysJobLog.setStatus(Constants.SUCCESS);
        }

        // 写入数据库当中
        SpringUtils.getBean(ISysJobLogService.class).addJobLog(sysJobLog);
    }

    /**
     * 执行方法，由子类重载
     *
     * @param context 工作执行上下文对象
     * @param sysJob 系统计划任务
     * @throws Exception 执行过程中的异常
     */
    protected abstract void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception;
}
