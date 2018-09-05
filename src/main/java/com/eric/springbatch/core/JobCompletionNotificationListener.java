package com.eric.springbatch.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

	private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Override
	public void afterJob(JobExecution jobExecution) {
		if(jobExecution.getStatus() == BatchStatus.COMPLETED) {
			log.info("!!! JOB FINISHED! Time to verify the results");
		}
	}
	
	@Override
	public void beforeJob(JobExecution jobExecution) {
		// 清除舊資料
		int remain = 4; //留4筆與將要跑的1筆=5筆
		jdbcTemplate.update("delete from batch_step_execution_context");
		jdbcTemplate.update("delete from batch_step_execution where JOB_EXECUTION_ID < (select ID from batch_job_seq) - ?", remain);
		jdbcTemplate.update("delete from batch_job_execution_params where JOB_EXECUTION_ID < (select ID from batch_job_seq) - ?", remain);
		jdbcTemplate.update("delete from batch_job_execution_context where JOB_EXECUTION_ID < (select ID from batch_job_seq) - ?", remain);
		jdbcTemplate.update("delete from batch_job_execution where JOB_EXECUTION_ID < (select ID from batch_job_seq) - ?", remain);
		int result = jdbcTemplate.update("delete from batch_job_instance where JOB_INSTANCE_ID < (select ID from batch_job_seq) - ?", remain);
		log.info("!!! OLD JOB HAS BEEN DELETED! ("+result+")");
	}
}
