package com.eric.springbatch.logger;

import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Aspect
public class LogAspect {
	
	private Logger log = LoggerFactory.getLogger(LogAspect.class);

	@Pointcut("execution(* com.eric.springbatch.controller.*.*(..))")
	public void controller() {}
	
	@Around("controller()")
	public Object execute(ProceedingJoinPoint pjp) throws Throwable {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> logMap = new HashMap<>();
		
		logMap.put("class",pjp.getTarget().getClass().getName());
		String methodName = pjp.getSignature().getName();
		logMap.put("methodName",methodName);
		String[] argNames = ((MethodSignature) pjp.getSignature()).getParameterNames();
        Object[] values = pjp.getArgs();
        StringBuffer sb = new StringBuffer();
        if (argNames.length>0) {
        	for (int i=0; i<argNames.length; i++) {
        		sb.append(argNames[i]).append("=").append(values[i]).append(",");
        	}
        	logMap.put("param", sb.deleteCharAt(sb.lastIndexOf(",")));
        }
        log.info(mapper.writeValueAsString(logMap));
		return pjp.proceed();
	}
}
