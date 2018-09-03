package com.eric.springbatch.core;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.adapter.ItemWriterAdapter;
import org.springframework.batch.item.support.ListItemWriter;

import com.eric.springbatch.model.Person;

public class SimulateItemWriter extends ListItemWriter<Person> {
	
	private static final Logger log = LoggerFactory.getLogger(SimulateItemWriter.class);

	@Override
	public void write(List<? extends Person> items) throws Exception {
		items.stream().forEach( p -> log.info("!!! JOB WROTED! Check..."+p));
	}
}
