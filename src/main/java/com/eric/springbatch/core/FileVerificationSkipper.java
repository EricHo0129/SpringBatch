package com.eric.springbatch.core;

import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;

public class FileVerificationSkipper implements SkipPolicy {

	private static final Logger log = LoggerFactory.getLogger(FileVerificationSkipper.class);
	
	@Override
	public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
		if (t instanceof FileNotFoundException) {			
			return false;
		} else if (t instanceof RuntimeException && skipCount <5){
			RuntimeException re = (RuntimeException)t;
			log.info("Bad data format..."+ re.getMessage());
			return true;
		} else {
			return false;
		}
	}
}
