package com.energytracker.aspect;

import com.energytracker.service.QuartzJobMonitoringService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author André Heinen
 */
@Aspect
@Component
public class QuartzJobMonitoringAspect {

	private final AtomicInteger activeJobCount = new AtomicInteger(0);
	private final AtomicInteger queuedJobCount = new AtomicInteger(0);
	private final AtomicInteger maxActiveJobCount = new AtomicInteger(0);
	private final AtomicInteger maxQueuedJobCount = new AtomicInteger(0);

	private final QuartzJobMonitoringService service;

	@Autowired
	public QuartzJobMonitoringAspect(QuartzJobMonitoringService service) {
		this.service = service;
	}

	@Pointcut("execution(* org.quartz.Job.execute(..))")
	public void quartzJobExecution() {

	}

	@Before("quartzJobExecution()")
	public void beforeJobExecution() {
		int currentQueued = queuedJobCount.incrementAndGet();

		// Aktualisiere den Höchstwert für Warteschlange
		maxQueuedJobCount.updateAndGet(max -> Math.max(max, currentQueued));

		service.updateCurrentStats(activeJobCount.get(), queuedJobCount.get());
		service.updateMaxStats(maxActiveJobCount.get(), maxQueuedJobCount.get(), 0L, null);
	}

	@Around("quartzJobExecution()")
	public Object aroundJobExecution(ProceedingJoinPoint pjp) throws Throwable {
		String className = pjp.getTarget().getClass().getSimpleName();
		long startTime = System.currentTimeMillis();

		try {
			// Reduziere die Warteschlange und erhöhe aktive Jobs
			queuedJobCount.decrementAndGet();
			int currentActive = activeJobCount.incrementAndGet();
			maxActiveJobCount.updateAndGet(max -> Math.max(max, currentActive));

			service.updateCurrentStats(activeJobCount.get(), queuedJobCount.get());
			service.updateMaxStats(maxActiveJobCount.get(), maxQueuedJobCount.get(), 0L, null);

			Object object = pjp.proceed();

			long executionTime = System.currentTimeMillis() - startTime;
			service.updateMaxStats(maxActiveJobCount.get(), maxQueuedJobCount.get(), executionTime, className);

			// Führe die `execute`-Logik aus
			return object;

		} catch (Exception e) {
			throw new RuntimeException("Fehler im Around Aspect" + e.getMessage());
		}
	}

	@After("quartzJobExecution()")
	public void afterJobExecution() {
		activeJobCount.decrementAndGet();
		service.updateCurrentStats(activeJobCount.get(), queuedJobCount.get());
	}
}
