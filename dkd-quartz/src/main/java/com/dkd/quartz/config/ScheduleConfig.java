package com.dkd.quartz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * 定时任务配置（单机部署建议删除此类和qrtz数据库表，默认走内存会最高效）
 *
 * @author ruoyi
 */
@Configuration
public class ScheduleConfig
{
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource)
    {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        // 设置数据源
        factory.setDataSource(dataSource);

        // quartz属性参数
        Properties prop = new Properties();
        // 集群实例名称，在同一个集群内多个节点上该实例名称必须一致
        prop.put("org.quartz.scheduler.instanceName", "RuoyiScheduler");
        // 集群实例Id，在同一个集群内多个节点上该实例Id必须唯一
        // AUTO表示由schedulerFactoryBean自动生成（计算机名+时间戳）
        prop.put("org.quartz.scheduler.instanceId", "AUTO");
        // 线程池配置
        prop.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        prop.put("org.quartz.threadPool.threadCount", "20"); // 线程数
        prop.put("org.quartz.threadPool.threadPriority", "5");// 线程优先级（1~10）
        // JobStore配置：使用数据库来存储Job和Trigger信息
        prop.put("org.quartz.jobStore.class", "org.springframework.scheduling.quartz.LocalDataSourceJobStore");
        // 集群配置
        prop.put("org.quartz.jobStore.isClustered", "true"); // 开启集群
        prop.put("org.quartz.jobStore.clusterCheckinInterval", "15000");// 心跳间隔
        prop.put("org.quartz.jobStore.maxMisfiresToHandleAtATime", "10");// 最大允许处理的misfire数量
        prop.put("org.quartz.jobStore.txIsolationLevelSerializable", "true");// 隔离级别

        // sqlserver 启用
        // prop.put("org.quartz.jobStore.selectWithLockSQL", "SELECT * FROM {0}LOCKS UPDLOCK WHERE LOCK_NAME = ?");
        prop.put("org.quartz.jobStore.misfireThreshold", "12000");// 允许的misfire时间间隔
        prop.put("org.quartz.jobStore.tablePrefix", "QRTZ_");// 表前缀
        factory.setQuartzProperties(prop);

        factory.setSchedulerName("RuoyiScheduler");// 集群调度器名称
        // 延时启动
        factory.setStartupDelay(1);
        factory.setApplicationContextSchedulerContextKey("applicationContextKey");
        // 可选，QuartzScheduler
        // 启动时更新己存在的Job，这样就不用每次修改targetObject后删除qrtz_job_details表对应记录了
        factory.setOverwriteExistingJobs(true);
        // 设置自动启动，默认为true
        factory.setAutoStartup(true);

        return factory;
    }
}
