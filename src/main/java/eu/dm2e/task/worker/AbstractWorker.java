package eu.dm2e.task.worker;

import java.io.IOException;

import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.ShutdownSignalException;

import eu.dm2e.task.util.AbstractConnector;

public abstract class AbstractWorker extends AbstractConnector {

	public abstract void doWork() throws IOException, ShutdownSignalException,
			ConsumerCancelledException, InterruptedException;

}
