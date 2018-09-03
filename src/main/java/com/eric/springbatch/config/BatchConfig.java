package com.eric.springbatch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.eric.springbatch.core.FileVerificationSkipper;
import com.eric.springbatch.core.JobCompletionNotificationListener;
import com.eric.springbatch.core.PersonItemProcessor;
import com.eric.springbatch.core.SimulateItemWriter;
import com.eric.springbatch.model.Person;

@Configuration
@EnableBatchProcessing
public class BatchConfig {
	
	@Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    
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
    public Job importUserJob(JobCompletionNotificationListener listener, Step step1) {
        return jobBuilderFactory.get("importUserJob")
            .incrementer(new RunIdIncrementer())
            .listener(listener)
            .flow(step1)
            .end()
            .build();
    }
	
	@Bean
	public SkipPolicy fileVerificationSkipper() {
	    return new FileVerificationSkipper();
	}

	
	@Bean
    public Step step1(ListItemWriter<Person> writer, SkipPolicy skipPolicy) {
        return stepBuilderFactory.get("step1")
            .<Person, Person> chunk(10)
            .reader(reader()) //指定讀取者
            .faultTolerant().skipPolicy(skipPolicy)
            .processor(processor()) //讀取後的處理者
            .writer(writer) //處理後的寫入者
            .build();
    }
}
