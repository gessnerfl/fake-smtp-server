package de.gessnerfl.fakesmtp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
public class SchedulingConfig implements SchedulingConfigurer {

    @Value("${fakesmtp.taskScheduler.poolSize:10}")
    Integer poolSize;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        var taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setThreadNamePrefix("task-pool");
        taskScheduler.setPoolSize(poolSize);
        taskScheduler.initialize();
        taskRegistrar.setTaskScheduler(taskScheduler);
    }

}
