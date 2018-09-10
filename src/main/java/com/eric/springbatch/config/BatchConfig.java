package com.eric.springbatch.config;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

import com.eric.springbatch.core.FileVerificationSkipper;
import com.eric.springbatch.core.JobCompletionNotificationListener;
import com.eric.springbatch.core.PersonItemProcessor;
import com.eric.springbatch.core.PersonItemReader;
import com.eric.springbatch.core.ProcessListener;
import com.eric.springbatch.core.SimulateItemWriter;
import com.eric.springbatch.model.Person;

@Configuration
@EnableBatchProcessing
@EnableScheduling
public class BatchConfig {
	
	private final Logger log = LoggerFactory.getLogger(BatchConfig.class);
	//AP主要識別
	public static final String AP_JOB_KEY = "AP-9999";
	//AP工作名稱
	public static final String AP_JOB_NAME = AP_JOB_KEY+"-CONVERT_USER";
	//AP階段名稱
	public static final String AP_JOB_STEP = AP_JOB_NAME+"-STEP";
	//啟動標記
	private boolean enabled = true;
	//任務計數器
	private AtomicInteger batchRunCounter = new AtomicInteger(0);
	//目前正在執行的排程
	private final Map<Object, ScheduledFuture<?>> scheduledTasks = new IdentityHashMap<>();
	
	@Autowired
	private ApplicationContext appContext;
	
	@Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;
    
    @Autowired
    public JobCompletionNotificationListener listener;
    
    @Autowired
    public ProcessListener processListener;
    
    //資料庫設定
    @Bean
    public DriverManagerDataSource dataSource() {
    	DriverManagerDataSource dataSource = new DriverManagerDataSource();
    	dataSource.setDriverClassName("com.mysql.jdbc.Driver");
    	dataSource.setUrl("jdbc:mysql://127.0.0.1:3306/test");
    	dataSource.setUsername("root");
    	dataSource.setPassword("ec2user");
    	return dataSource;
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate(DriverManagerDataSource dataSource) {
    	return new JdbcTemplate(dataSource);
    }

    
    //程序設定
    @Bean
    @StepScope
    public ItemReader<Person> personReader() {
    	return new PersonItemReader();
    }
    
	@Bean
	@StepScope
    public PersonItemProcessor processor() {
        return new PersonItemProcessor();
    }
	
	@Bean
	@StepScope
    public ListItemWriter<Person> writer() {
        return new SimulateItemWriter();
    }
	
	
	
	//工作階段設定
	@Bean
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(3);
		taskExecutor.setMaxPoolSize(6);
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		return taskExecutor;
	}
	
	@Bean
    public TaskScheduler poolScheduler() {
        return new CustomTaskScheduler();
    }

	/**
	 * 自訂的排程器 
	 */
    private class CustomTaskScheduler extends ThreadPoolTaskScheduler {

        private static final long serialVersionUID = -5564662992816645752L;

		@Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
            ScheduledFuture<?> future = super.scheduleAtFixedRate(task, period);

            ScheduledMethodRunnable runnable = (ScheduledMethodRunnable) task;
            //放入類別層級的暫存空間,以便做控制(getTarget出來是指BatchConfig這個類)
            scheduledTasks.put(runnable.getTarget(), future);

            return future;
        }

    }
	
	@Bean
    public Job importUserJob() {
        return jobBuilderFactory.get(AP_JOB_NAME)
            .incrementer(incrementer())
            .listener(listener)
            .flow(step1())
            .end()
            .build();
    }
    
	
	private JobParametersIncrementer incrementer() {
		return new JobParametersIncrementer() {
			@Override
			public JobParameters getNext(JobParameters parameters) {
				return new JobParametersBuilder().addDate(AP_JOB_NAME, new Date()).toJobParameters();
			}
		};
	}
	
	@Bean
	public SkipPolicy fileVerificationSkipper() {
	    return new FileVerificationSkipper();
	}

	
	@Bean
    public Step step1() {
        return stepBuilderFactory.get(AP_JOB_STEP)
            .<Person, Person> chunk(3)
            .listener(processListener)
            .reader(personReader()) //指定讀取者
            .faultTolerant().skipPolicy(fileVerificationSkipper())
            .processor(processor()) //讀取後的處理者
            .writer(writer()) //處理後的寫入者
            .taskExecutor(taskExecutor())
            .build();
    }
	
	 @Bean
    public JobLauncher jobLauncher() throws Exception {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(jobRepository());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    @Bean
    public JobRepository jobRepository() throws Exception {
        //MapJobRepositoryFactoryBean factory = new MapJobRepositoryFactoryBean();
        //factory.setTransactionManager(new ResourcelessTransactionManager());
        //return (JobRepository) factory.getObject();
    	JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
    	factory.setDatabaseType("mysql");
    	factory.setDataSource(dataSource());
    	factory.setTransactionManager(new ResourcelessTransactionManager());
    	factory.afterPropertiesSet();
    	return factory.getObject();
    }
    
    @Bean
    public JobExplorer jobExplorer() throws Exception {
    	JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
    	factory.setDataSource(dataSource());
    	factory.afterPropertiesSet();
    	return factory.getObject();
    }
	
	@Scheduled(fixedRate = 10000)
    public void launchJob() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		if (enabled) {			
			log.info("scheduler starts at " + sdf.format(date));
			Job job = importUserJob();
			batchRunCounter.incrementAndGet();
			JobExecution jobExecution = jobLauncher().run(job, job.getJobParametersIncrementer().getNext(null));
			log.info("Batch job ends with status as " + jobExecution.getStatus());
			log.info("----- scheduler ends -----");
			batchRunCounter.decrementAndGet();
		} else {
			log.info("[enabled=false] scheduler couldn't start at " + sdf.format(date));
		}
    }
	
	/**
	 * 目前Job執行數量
	 * @return
	 */
	public int currentLaunchJobCount() {
		return batchRunCounter.get()>0?batchRunCounter.get():0;
	}
	
	/**
	 * 目前的啟動狀態
	 * @return
	 */
	public boolean currentStatus() {
		return this.enabled;
	}
 	
	/*
	 * 允許排程器執行
	 */
	public void start() throws Exception {
		this.enabled = true;
		BatchConfig config = appContext.getBean(BatchConfig.class);
		poolScheduler().scheduleAtFixedRate(new ScheduledMethodRunnable(config, "launchJob"), 10000);
		log.info("scheduledTasks has been started.");
	}
	
	/**
	 * 停止排程器執行
	 */
	public void stop() {
		scheduledTasks.forEach((key, val)->{
			if (key instanceof BatchConfig) {
				val.cancel(false); //不強制中斷
			}
		});
		this.enabled = false;
		log.info("scheduledTasks has been stoped.");
	}
}
