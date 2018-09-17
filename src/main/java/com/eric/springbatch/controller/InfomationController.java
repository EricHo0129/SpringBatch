package com.eric.springbatch.controller;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.server.PathParam;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.eric.springbatch.config.BatchConfig;

@RestController
public class InfomationController {
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private JobExplorer jobExplorer;
	
	@GetMapping("/last")
	public Map<String, Object> getLastInfo() throws Exception {
		BatchConfig batchConfig = context.getBean(BatchConfig.class);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Map<String, Object> result = new HashMap<>();
		result.put("current-status", batchConfig.currentStatus()?"Started":"Stoped");
		result.put("job-count", jobExplorer.getJobInstanceCount(BatchConfig.AP_JOB_NAME));
		List<JobInstance> jobInsList = jobExplorer.findJobInstancesByJobName(BatchConfig.AP_JOB_NAME, 0, 1);
		if (jobInsList!=null && jobInsList.size()>0) {
			List<JobExecution> jobExecList = jobExplorer.getJobExecutions(jobInsList.get(0));
			result.put("total-last-job", sdf.format(jobExecList.get(0).getCreateTime()));
		}
		Set<JobExecution> jobExecSet = jobExplorer.findRunningJobExecutions(BatchConfig.AP_JOB_NAME);
		result.put("running-job-count", jobExecSet.size());
		result.put("launched-job-count", batchConfig.currentLaunchJobCount());
		Optional<JobExecution> lastJobExec = jobExecSet.stream().sorted(Comparator.comparingLong(JobExecution::getId).reversed()).findFirst();
		if (lastJobExec.isPresent()) {			
			JobExecution last = lastJobExec.get();
			result.put("running-last-job", sdf.format(last.getCreateTime()));
		}
		
		return result;
	}
	
	@GetMapping("/test/{pid}")
	public void test(HttpServletRequest request ,@PathVariable("pid") Long pid) throws Exception {
		System.out.println("ok..."+pid);
	}
}
