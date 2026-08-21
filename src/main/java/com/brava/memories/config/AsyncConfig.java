package com.brava.memories.config;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration @EnableAsync @EnableScheduling
public class AsyncConfig {
 @Bean(name="mediaProcessingExecutor")
 public Executor mediaProcessingExecutor(ProcessingProperties props){
   ThreadPoolTaskExecutor executor=new ThreadPoolTaskExecutor();
   executor.setCorePoolSize(Math.max(1,props.coreThreads()));
   executor.setMaxPoolSize(Math.max(props.coreThreads(),props.maxThreads()));
   executor.setQueueCapacity(Math.max(10,props.queueCapacity()));
   executor.setThreadNamePrefix("media-processing-");
   // Never block request threads on malware scanning. Rejected jobs stay UPLOADED and are recovered by the DB-backed recovery job.
   executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
   executor.setWaitForTasksToCompleteOnShutdown(true);
   executor.setAwaitTerminationSeconds(30);
   executor.initialize();
   return executor;
 }
}
