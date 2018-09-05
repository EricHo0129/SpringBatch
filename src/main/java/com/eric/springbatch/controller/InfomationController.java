package com.eric.springbatch.controller;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eric.springbatch.config.BatchConfig;

@RestController
public class InfomationController {
	
	@Autowired
	private JobExplorer jobExplorer;
	
	@GetMapping("/last")
	public Map<String, Object> getLastInfo() throws Exception {
		Map<String, Object> result = new HashMap<>();
		result.put("job-count", jobExplorer.getJobInstanceCount(BatchConfig.AP_JOB_NAME));
		Set<JobExecution> jobExecSet = jobExplorer.findRunningJobExecutions(BatchConfig.AP_JOB_NAME);
		result.put("running-job-count", jobExecSet.size());
		JobExecution last = jobExecSet.stream().sorted(Comparator.comparingLong(JobExecution::getId).reversed()).findFirst().get();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		result.put("running-last-job", sdf.format(last.getCreateTime()));
		
		return result;
	}
}
