package com.eric.springbatch.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import com.eric.springbatch.model.Person;

@Component
@StepScope
public class ProcessListener implements ItemProcessListener<Person, Person> {

	private static final Logger log = LoggerFactory.getLogger(ProcessListener.class);
	
	@Override
	public void beforeProcess(Person item) {
		log.info("Converting (" + item + ") starting...");	
	}
	
	@Override
	public void afterProcess(Person item, Person result) {
		log.info("Converting (" + item + ") into (" + result + ")");	
	}
	
	@Override
	public void onProcessError(Person item, Exception e) {
		// TODO Auto-generated method stub	
	}
}
