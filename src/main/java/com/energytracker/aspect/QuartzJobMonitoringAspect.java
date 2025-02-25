package com.energytracker.aspect;

import com.energytracker.service.QuartzJobMonitoringService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
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

			// Führe die `execute`-Logik aus
			return pjp.proceed();

		} catch (InterruptedException e) {
			// Thread-Interrupt abfangen
			Thread.currentThread().interrupt();
			throw e; // Oder passende Fehlerbehandlung
		} finally {
			// Messe die Laufzeit
			long executionTime = System.currentTimeMillis() - startTime;

			// Nach Abschluss des Jobs Anzahl aktiver Jobs verringern
			activeJobCount.decrementAndGet();

			service.updateCurrentStats(activeJobCount.get(), queuedJobCount.get());
			service.updateMaxStats(maxActiveJobCount.get(), maxQueuedJobCount.get(), executionTime, className);
		}
	}
}
