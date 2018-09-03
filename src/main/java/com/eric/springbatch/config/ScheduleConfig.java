package com.eric.springbatch.config;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.eric.springbatch.core.PersonItemProcessor;
import com.eric.springbatch.core.SimulateItemWriter;
import com.eric.springbatch.model.Person;

//@Configuration
//@EnableBatchProcessing
//@EnableScheduling
public class ScheduleConfig {

	private final Logger log = LoggerFactory.getLogger(ScheduleConfig.class);

    private AtomicBoolean enabled = new AtomicBoolean(true);

    private AtomicInteger batchRunCounter = new AtomicInteger(0);
    
    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;
    
    @Scheduled(fixedRate = 15000, initialDelay=10000)
    public void launchJob() throws Exception {
        Date date = new Date();
        log.info("scheduler starts at " + date);
        if (enabled.get()) {
            JobExecution jobExecution = jobLauncher().run(job(), new JobParametersBuilder().addDate("launchDate", date)
                .toJobParameters());
            batchRunCounter.incrementAndGet();
            log.info("Batch job ends with status as " + jobExecution.getStatus());
        }
        log.info("scheduler ends ");
    }
    
    @Bean
    public Job job() {
        return jobBuilderFactory.get("job")
            .start(readPerson())
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
        MapJobRepositoryFactoryBean factory = new MapJobRepositoryFactoryBean();
        //factory.setTransactionManager(new ResourcelessTransactionManager());
        return (JobRepository) factory.getObject();
    }

    @Bean
    protected Step readPerson() {
        return stepBuilderFactory.get("readPerson")
            .<Person, Person> chunk(10)
            .reader(reader2())
            .processor(processor2())
            .writer(writer2())
            .build();
    }
    
    @Bean
    public FlatFileItemReader<Person> reader2() {
        return new FlatFileItemReaderBuilder<Person>()
            .name("personItemReader2")
            .resource(new ClassPathResource("sample-data2.csv"))
            .delimited()
            .names(new String[]{"firstName", "lastName"})
            .fieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {{
                setTargetType(Person.class);
            }})
            .build();
    }
    
	@Bean
    public PersonItemProcessor processor2() {
        return new PersonItemProcessor();
    }
	
	@Bean
    public ListItemWriter<Person> writer2() {
        return new SimulateItemWriter();
    }
}
