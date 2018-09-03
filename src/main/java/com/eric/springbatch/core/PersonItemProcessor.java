package com.eric.springbatch.core;

import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;

import com.eric.springbatch.model.Person;
public class PersonItemProcessor implements ItemProcessor<Person, Person> {

    private static final Logger log = LoggerFactory.getLogger(PersonItemProcessor.class);

    @Override
    @Retryable(value= {TimeoutException.class}, maxAttempts=3, stateful=false, backoff=@Backoff(delay=2000))
    public Person process(final Person person) throws Exception {
        final String firstName = person.getFirstName().toUpperCase();
        final String lastName = person.getLastName().toUpperCase();

        final Person transformedPerson = new Person(firstName, lastName);
        log.info("Converting (" + person + ") starting...");
        
        if (person.getFirstName().equals("Joe")) {
        	throw new TimeoutException("Too many seconds..."+person);
        }

        log.info("Converting (" + person + ") into (" + transformedPerson + ")");

        return transformedPerson;
    }
    
    /**
     * 特別注意:當retry到達上限的時候會呼叫此method,且第一個參數一定要是拋出的Exception,
     * 第二個以後的參數要和標示Retryable的方法相同,且回傳型別也要相同,才會有作用
     * @param re
     * @param person
     * @return
     */
    @Recover
    public Person recover(TimeoutException te, final Person person) {
    	log.info("Give up converting ... ("+person+") when "+te.getClass().getName()+" occured.");
    	return person;
    }

}
