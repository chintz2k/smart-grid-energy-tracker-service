package com.energytracker.quartz.jobs.producer;

import com.energytracker.entity.BaseProducer;
import org.quartz.Job;

/**
 * @author André Heinen
 */
public abstract class AbstractProducerLoggerJob<T extends BaseProducer> implements Job {

}
