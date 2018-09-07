package com.eric.springbatch.core;

import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemStreamException;

public class FileVerificationSkipper implements SkipPolicy {

	private static final Logger log = LoggerFactory.getLogger(FileVerificationSkipper.class);
	
	@Override
	public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
		if (t instanceof FileNotFoundException) {			
			log.info("File not found..."+ ((FileNotFoundException)t).getMessage());
			return false;
		} else if (t instanceof RuntimeException && skipCount <5){
			RuntimeException re = (RuntimeException)t;
			log.info("Bad data format..."+ re.getMessage());
			return true;
		} else {
			log.info("Somthing wrong..."+ t.getMessage());
			return false;
		}
	}
}
