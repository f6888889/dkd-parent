package com.dkd.quartz.service.impl;

import com.dkd.common.constant.ScheduleConstants;
import com.dkd.common.exception.job.TaskException;
import com.dkd.quartz.domain.SysJob;
import com.dkd.quartz.mapper.SysJobMapper;
import com.dkd.quartz.service.ISysJobService;
import com.dkd.quartz.util.CronUtils;
import com.dkd.quartz.util.ScheduleUtils;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * 定时任务调度信息 服务层
 *
 * @author ruoyi
 */
@Service
public class SysJobServiceImpl implements ISysJobService
{
    @Autowired
    private Scheduler scheduler;

    @Autowired
    private SysJobMapper jobMapper;

    /**
     * 项目启动时，初始化定时器 主要是防止手动修改数据库导致未同步到定时任务处理（注：不能手动修改数据库ID和任务组名，否则会导致脏数据）
     */
    @PostConstruct
    public void init() throws SchedulerException, TaskException {
        // 清空调度器中的所有任务，以确保从数据库加载最新的任务配置
        scheduler.clear();

        // 从数据库查询所有定时任务配置
        List<SysJob> jobList = jobMapper.selectJobAll();

        // 遍历任务配置列表，创建定时任务
        for (SysJob job : jobList) {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
    }

    /**
     * 获取quartz调度器的计划任务列表
     *
     * @param job 调度信息
     * @return
     */
    @Override
    public List<SysJob> selectJobList(SysJob job)
    {
        return jobMapper.selectJobList(job);
    }

    /**
     * 通过调度任务ID查询调度信息
     *
     * @param jobId 调度任务ID
     * @return 调度任务对象信息
     */
    @Override
    public SysJob selectJobById(Long jobId)
    {
        return jobMapper.selectJobById(jobId);
    }

    /**
     * 暂停任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int pauseJob(SysJob job) throws SchedulerException
    {
        // 获取任务ID
        Long jobId = job.getJobId();
        // 获取任务组
        String jobGroup = job.getJobGroup();
        // 将任务状态设置为暂停
        job.setStatus(ScheduleConstants.Status.PAUSE.getValue());
        // 更新数据库中的任务配置信息
        int rows = jobMapper.updateJob(job);
        // 如果数据库更新成功
        if (rows > 0)
        {
            // 暂停调度器中的任务
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        // 返回更新的行数
        return rows;
    }

    /**
     * 恢复任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resumeJob(SysJob job) throws SchedulerException
    {
        // 获取任务ID
        Long jobId = job.getJobId();
        // 获取任务组
        String jobGroup = job.getJobGroup();
        // 将任务状态设置为正常
        job.setStatus(ScheduleConstants.Status.NORMAL.getValue());
        // 更新数据库中的任务配置信息
        int rows = jobMapper.updateJob(job);
        // 如果数据库更新成功
        if (rows > 0)
        {
            // 恢复任务的调度
            scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        // 返回更新的行数
        return rows;
    }

    /**
     * 删除任务后，所对应的trigger也将被删除
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteJob(SysJob job) throws SchedulerException
    {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        int rows = jobMapper.deleteJobById(jobId);
        if (rows > 0)
        {
            scheduler.deleteJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    /**
     * 批量删除调度信息
     *
     * @param jobIds 需要删除的任务ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJobByIds(Long[] jobIds) throws SchedulerException
    {
        for (Long jobId : jobIds)
        {
            SysJob job = jobMapper.selectJobById(jobId);
            deleteJob(job);
        }
    }

    /**
     * 任务调度状态修改
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeStatus(SysJob job) throws SchedulerException
    {
        // 初始化受影响的行数为0
        int rows = 0;
        // 获取任务的当前状态
        String status = job.getStatus();
        // 如果任务状态为正常，则尝试恢复任务
        if (ScheduleConstants.Status.NORMAL.getValue().equals(status))
        {
            // 执行恢复任务操作，返回受影响的行数
            rows = resumeJob(job);
        }
        // 如果任务状态为暂停，则尝试暂停任务
        else if (ScheduleConstants.Status.PAUSE.getValue().equals(status))
        {
            // 执行暂停任务操作，返回受影响的行数
            rows = pauseJob(job);
        }
        // 返回受影响的行数
        return rows;
    }

    /**
     * 立即运行任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean run(SysJob job) throws SchedulerException
    {
        boolean result = false;
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        SysJob properties = selectJobById(job.getJobId());
        // 参数
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduleConstants.TASK_PROPERTIES, properties);
        JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
        if (scheduler.checkExists(jobKey))
        {
            result = true;
            scheduler.triggerJob(jobKey, dataMap);
        }
        return result;
    }

    /**
     * 新增任务
     *
     * @param job 调度信息 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertJob(SysJob job) throws SchedulerException, TaskException
    {
        // 初始化任务状态为暂停
        job.setStatus(ScheduleConstants.Status.PAUSE.getValue());

        // 执行插入操作并获取影响的行数
        int rows = jobMapper.insertJob(job);

        // 如果插入成功，则创建定时任务
        if (rows > 0)
        {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }

        // 返回影响的数据库行数
        return rows;
    }

    /**
     * 更新任务的时间表达式
     *
     * @param job 调度信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateJob(SysJob job) throws SchedulerException, TaskException
    {
        SysJob properties = selectJobById(job.getJobId());
        int rows = jobMapper.updateJob(job);
        if (rows > 0)
        {
            updateSchedulerJob(job, properties.getJobGroup());
        }
        return rows;
    }

    /**
     * 更新任务
     *
     * @param job 任务对象
     * @param jobGroup 任务组名
     */
    public void updateSchedulerJob(SysJob job, String jobGroup) throws SchedulerException, TaskException
    {
        Long jobId = job.getJobId();
        // 判断是否存在
        JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
        if (scheduler.checkExists(jobKey))
        {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(jobKey);
        }
        ScheduleUtils.createScheduleJob(scheduler, job);
    }

    /**
     * 校验cron表达式是否有效
     *
     * @param cronExpression 表达式
     * @return 结果
     */
    @Override
    public boolean checkCronExpressionIsValid(String cronExpression)
    {
        return CronUtils.isValid(cronExpression);
    }
}
