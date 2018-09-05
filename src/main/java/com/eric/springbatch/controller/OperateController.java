package com.eric.springbatch.controller;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eric.springbatch.config.BatchConfig;

@RestController
public class OperateController implements ApplicationContextAware {

	private ApplicationContext context;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}
	
	@GetMapping("/stop")
	public Object stop() throws Exception {
		BatchConfig batchConfig = context.getBean(BatchConfig.class);
		batchConfig.stop();
		return true;
	}
	
	@GetMapping("/start")
	public Object start() throws Exception {
		BatchConfig batchConfig = context.getBean(BatchConfig.class);
		batchConfig.start();
		return true;
	}
}
