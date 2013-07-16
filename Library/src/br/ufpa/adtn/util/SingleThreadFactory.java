package br.ufpa.adtn.util;

import java.util.concurrent.ThreadFactory;

public class SingleThreadFactory implements ThreadFactory {
	private Thread thread;
	
	public SingleThreadFactory() {
		this.thread = null;
	}

	@Override
	public synchronized Thread newThread(Runnable r) {
		if (thread != null)
			throw new IllegalStateException("One thread has already been created");
		
		thread = new Thread(r);
		return thread;
	}
	
	public synchronized Thread getThread() {
		return thread;
	}
}
