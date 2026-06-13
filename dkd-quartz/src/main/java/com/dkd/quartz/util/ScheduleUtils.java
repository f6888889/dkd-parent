package com.dkd.quartz.util;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import com.dkd.common.constant.Constants;
import com.dkd.common.constant.ScheduleConstants;
import com.dkd.common.exception.job.TaskException;
import com.dkd.common.exception.job.TaskException.Code;
import com.dkd.common.utils.StringUtils;
import com.dkd.common.utils.spring.SpringUtils;
import com.dkd.quartz.domain.SysJob;

/**
 * 定时任务工具类
 *
 * @author ruoyi
 */
public class ScheduleUtils {
    /**
     * 得到quartz任务类
     *
     * @param sysJob 执行计划
     * @return 具体执行任务类
     */
    private static Class<? extends Job> getQuartzJobClass(SysJob sysJob) {
        // 判断任务是否允许并发执行，"0"表示允许并发
        boolean isConcurrent = "0".equals(sysJob.getConcurrent());
        // 根据任务的并发执行设置，返回相应的Job执行类
        return isConcurrent ? QuartzJobExecution.class : QuartzDisallowConcurrentExecution.class;
    }

    /**
     * 构建任务触发对象
     */
    public static TriggerKey getTriggerKey(Long jobId, String jobGroup) {
        return TriggerKey.triggerKey(ScheduleConstants.TASK_CLASS_NAME + jobId, jobGroup);
    }

    /**
     * 构建任务键对象
     */
    public static JobKey getJobKey(Long jobId, String jobGroup) {
        return JobKey.jobKey(ScheduleConstants.TASK_CLASS_NAME + jobId, jobGroup);
    }

    /**
     * 创建定时任务
     */
    public static void createScheduleJob(Scheduler scheduler, SysJob job) throws SchedulerException, TaskException {
        // 得到quartz任务类的Class
        // quartz任务类继承了AbstractQuartzJob类，而该类实现了Job接口
        Class<? extends Job> jobClass = getQuartzJobClass(job);
        // 构建job信息
        Long jobId = job.getJobId();// 任务id
        String jobGroup = job.getJobGroup();// 任务组名
        // 使用JobBuilder根据quartz任务类的Class使用静态方法newJob去build一个JobDetail的实例
        // 这个实例就可以去执行quartz任务类的相关方法
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(getJobKey(jobId, jobGroup)).build();

        // 表达式调度构建器，使用cron表达式
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        // 设置定时任务策略(cron计划策略)
        cronScheduleBuilder = handleCronScheduleMisfirePolicy(job, cronScheduleBuilder);

        // 按新的cronExpression表达式构建一个新的trigger
        CronTrigger trigger = TriggerBuilder.newTrigger()
                // 与上方的Job进行绑定
                .withIdentity(getTriggerKey(jobId, jobGroup))
                // 与Schedule绑定
                .withSchedule(cronScheduleBuilder).build();

        // 放入参数，运行时的方法可以获取
        jobDetail.getJobDataMap().put(ScheduleConstants.TASK_PROPERTIES, job);

        // 判断是否存在
        if (scheduler.checkExists(getJobKey(jobId, jobGroup))) {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(getJobKey(jobId, jobGroup));
        }

        // 判断任务是否过期
        if (StringUtils.isNotNull(CronUtils.getNextExecution(job.getCronExpression()))) {
            // 执行调度任务
            scheduler.scheduleJob(jobDetail, trigger);
        }

        // 暂停任务
        if (job.getStatus().equals(ScheduleConstants.Status.PAUSE.getValue())) {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
    }

    /**
     * 设置定时任务策略
     */
    public static CronScheduleBuilder handleCronScheduleMisfirePolicy(SysJob job, CronScheduleBuilder cb)
            throws TaskException {
        // 判断任务的执行策略
        switch (job.getMisfirePolicy()) {
            // 默认策略（放弃执行）
            case ScheduleConstants.MISFIRE_DEFAULT:
                return cb;
            // 立即执行
            case ScheduleConstants.MISFIRE_IGNORE_MISFIRES:
                return cb.withMisfireHandlingInstructionIgnoreMisfires();
            // 执行一次
            case ScheduleConstants.MISFIRE_FIRE_AND_PROCEED:
                return cb.withMisfireHandlingInstructionFireAndProceed();
            // 放弃执行
            case ScheduleConstants.MISFIRE_DO_NOTHING:
                return cb.withMisfireHandlingInstructionDoNothing();
            default:
                throw new TaskException("The task misfire policy '" + job.getMisfirePolicy()
                        + "' cannot be used in cron schedule tasks", Code.CONFIG_ERROR);
        }
    }

    /**
     * 检查包名是否为白名单配置
     *
     * @param invokeTarget 目标字符串
     * @return 结果
     */
    public static boolean whiteList(String invokeTarget) {
        // 提取调用目标的包名或类名部分，去除方法调用部分
        String packageName = StringUtils.substringBefore(invokeTarget, "(");
        // 计算包名中"."的数量，用于判断是否为完整包名
        int count = StringUtils.countMatches(packageName, ".");
        // 如果包名中至少有两个"."，则认为是完整包名，直接检查是否包含白名单内的字符串
        if (count > 1) {
            return StringUtils.containsAnyIgnoreCase(invokeTarget, Constants.JOB_WHITELIST_STR);
        }
        // 尝试通过Spring上下文获取调用目标对应的Bean，用于检查其所在包名
        Object obj = SpringUtils.getBean(StringUtils.split(invokeTarget, ".")[0]);
        // 获取Bean所在包的全名
        String beanPackageName = obj.getClass().getPackage().getName();
        // 检查Bean的包名是否在白名单内，同时确保包名不在违规列表内
        return StringUtils.containsAnyIgnoreCase(beanPackageName, Constants.JOB_WHITELIST_STR)
                && !StringUtils.containsAnyIgnoreCase(beanPackageName, Constants.JOB_ERROR_STR);
    }
}
