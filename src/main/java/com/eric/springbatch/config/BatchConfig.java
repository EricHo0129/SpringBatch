package com.eric.springbatch.config;

import java.util.Date;

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
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.eric.springbatch.core.FileVerificationSkipper;
import com.eric.springbatch.core.JobCompletionNotificationListener;
import com.eric.springbatch.core.PersonItemProcessor;
import com.eric.springbatch.core.SimulateItemWriter;
import com.eric.springbatch.model.Person;

@Configuration
@EnableBatchProcessing
@EnableScheduling
public class BatchConfig {
	
	private final Logger log = LoggerFactory.getLogger(BatchConfig.class);
	
	private final String apNo = "AP-9999";
	
	@Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;
    
    @Autowired
    public JobCompletionNotificationListener listener;
    
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

    
    //程序設定
    @Bean
    public FlatFileItemReader<Person> reader() {
        return new FlatFileItemReaderBuilder<Person>()
            .name("personItemReader")
            .resource(new ClassPathResource("sample-data.csv"))
            .delimited()
            .names(new String[]{"firstName", "lastName"})
            .fieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {{
                setTargetType(Person.class);
            }})
            .build();
    }
    
	@Bean
    public PersonItemProcessor processor() {
        return new PersonItemProcessor();
    }
	
	@Bean
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
    public Job importUserJob() {
        return jobBuilderFactory.get("importUserJob")
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
//				JobParameters params = (parameters == null) ? new JobParameters() : parameters;
//				long id = params.getLong(apNo, 0L) + 1;
//				return new JobParametersBuilder(params).addLong(apNo, id).toJobParameters();
				return new JobParametersBuilder().addDate(apNo, new Date()).toJobParameters();
			}
		};
	}
	
	@Bean
	public SkipPolicy fileVerificationSkipper() {
	    return new FileVerificationSkipper();
	}

	
	@Bean
    public Step step1() {
        return stepBuilderFactory.get("step1")
            .<Person, Person> chunk(5)
            .reader(reader()) //指定讀取者
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
	
	@Scheduled(fixedRate = 15000, initialDelay=10000)
    public void launchJob() throws Exception {
        Date date = new Date();
        log.info("scheduler starts at " + date);
        
            JobExecution jobExecution = jobLauncher().run(importUserJob(), incrementer().getNext(null));
            log.info("Batch job ends with status as " + jobExecution.getStatus());
        
        log.info("scheduler ends ");
    }
}
