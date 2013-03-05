package eu.dm2e.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import eu.dm2e.task.worker.AbstractWorker;
import eu.dm2e.task.worker.XsltWorker;

public class WorkerMain {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		List<AbstractWorker> workerInstanceList = new ArrayList<AbstractWorker>();
		@SuppressWarnings("rawtypes")
		List<Future> workerThreadList = new ArrayList<Future>();
		workerInstanceList.add(new XsltWorker());
		workerInstanceList.add(new XsltWorker());
		workerInstanceList.add(new XsltWorker());
		ExecutorService executor = Executors.newCachedThreadPool();
		for (AbstractWorker workerInstance : workerInstanceList) {
			System.out.println("Starting new worker " + workerInstance);
			workerThreadList.add(executor.submit(workerInstance));
		}
		System.out.println("Press enter to stop all workers.");
		System.in.read();
		for (Future<?> thisThread : workerThreadList) {
			thisThread.cancel(true);
		}
		executor.shutdown();
		System.exit(0);
	}

}
