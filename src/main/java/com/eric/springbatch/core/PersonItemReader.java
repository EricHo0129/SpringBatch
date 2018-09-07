package com.eric.springbatch.core;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import com.eric.springbatch.model.Person;

public class PersonItemReader implements ItemReader<Person> {

	private int index = 0;
	private List<Person> data = new ArrayList<>();
	
	private void loadData() throws Exception {
		data.add(new Person("Jill", "Doe"));
		data.add(new Person("Joe", "Doe"));
		data.add(new Person("Justin", "Doe"));
		data.add(new Person("Jane", "Doe"));
		data.add(new Person("John", "Doe"));
		data.add(new Person("Eric", "Doe"));
	}
	
	@Override
	public Person read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
		if (data.isEmpty()) {
			loadData();
		}
		if (index < data.size()) {
			return data.get(index++);
		}
		return null;
	}
}
