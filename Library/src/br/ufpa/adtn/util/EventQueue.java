/**
 * Amazon-DTN - Lightweight Delay Tolerant Networking Implementation
 * Copyright (C) 2013  Dórian C. Langbeck
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package br.ufpa.adtn.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import br.ufpa.adtn.core.BPAgent;

/**
 *
 *
 * @author Dórian Langbeck
 */
public class EventQueue {
	private static final AtomicInteger COUNT = new AtomicInteger();
    private final ScheduledThreadPoolExecutor executor;
    private final SingleThreadFactory singleFactory;
	private final Logger LOGGER;
    private final String name;
    

    public EventQueue(Logger parent, String name) {
    	this(name, parent, Thread.MIN_PRIORITY);
    }
    
    public EventQueue(String name) {
    	this(name, null, Thread.MIN_PRIORITY);
    }

    public EventQueue() {
        this(null, null, Thread.MIN_PRIORITY);
    }
    
    public EventQueue(int priority) {
        this(null, null, priority);
    }
    
    public EventQueue(boolean autostart) {
        this(null, null, Thread.MIN_PRIORITY, autostart);
    }
    
    public EventQueue(String name, Logger parent, final int prioriry) {
    	this(name, parent, prioriry, true);
    }
    
    public EventQueue(String name, Logger parent, final int prioriry, boolean autostart) {
		this.name = (name == null) ? "EventQueue#" + COUNT.getAndIncrement() : name;
		this.singleFactory = new SingleThreadFactory(!autostart);
		this.LOGGER = new Logger(parent, this.name);
		
        this.executor = new ScheduledThreadPoolExecutor(1, singleFactory);
    }
    
    public void start() {
    	singleFactory.startThread();
    }

    public boolean isOnInternalThread() {
        return Thread.currentThread() == singleFactory.getThread();
    }
    
    public void checkSync() {
    	if (Thread.currentThread() != singleFactory.getThread())
    		throw new IllegalAccessError(String.format(
    				"This method must run inside %s thread",
    				name
			));
    }
    
    public Future<?> schedule(Runnable r, long delay, TimeUnit unit) {
    	if (BPAgent.isSimulated()) {
    		final double scale = BPAgent.getSimulationConfig().getTimescale();
    		final long millis = unit.toMillis(delay);
    		delay = (long) (millis * scale);
    		unit = TimeUnit.MILLISECONDS;
    		
//    		LOGGER.v(String.format(
//    				"EventQueue in SIMULATED mode: %d ms --> %d ms",
//    				millis, delay
//			));
    	}
    	
    	if (!isOnInternalThread())
    		return executor.schedule(r, delay, unit);
    	
    	return UnblockingFuture.create(executor.schedule(r, delay, unit));
    }

    public Future<?> schedule(Event e, long delay, TimeUnit unit) {
    	return schedule(new EventRunner(e), delay, unit);
    }

    public Future<?> post(Event event) {
    	if (!isOnInternalThread())
    		return executor.submit(new EventRunner(event));
    	
    	return UnblockingFuture.create(executor.submit(new EventRunner(event)));
    }
    
    public void postAndWait(Runnable r) throws ExecutionException {
    	if (isOnInternalThread()) {
    		r.run();
    		return;
    	}
    	
        try {
        	executor.submit(r).get();
        } catch (InterruptedException e) {
            unhandledExceptionCacther(e);
        }
    }
        
    public <T> T submit(Callable<T> c) throws ExecutionException {
    	if (isOnInternalThread()) {
    		try {
				return c.call();
			} catch (Exception e) {
				throw new ExecutionException(e);
			}
    	}
    	
        try {
            return executor.submit(c).get();
        } catch (InterruptedException e) {
            throw new ExecutionException(e);
        }
    }
    
    public String getName() {
    	return name;
    }

    protected void unhandledExceptionCacther(Throwable t) {
    	LOGGER.e("Unhandled exception", t);
    }
    
    private class EventRunner implements Runnable {
    	private final Event event;
    	
    	public EventRunner(Event event) {
    		this.event = event;
    	}

		@Override
		public void run() {
			try {
				event.execute();
			} catch (Throwable t) {
				unhandledExceptionCacther(t);
			}
		}
    }
    

    private static class UnblockingFuture<T> implements Future<T> {
    	
    	public static <T> UnblockingFuture<T> create(Future<T> future) {
    		return new UnblockingFuture<T>(future);
    	}
    	
    	
    	private final Future<T> future;
    	
    	public UnblockingFuture(Future<T> future) {
    		this.future = future;
    	}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return future.cancel(mayInterruptIfRunning);
		}

		@Override
		public boolean isCancelled() {
			return future.isCancelled();
		}

		@Override
		public boolean isDone() {
			return future.isDone();
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			if (future.isDone())
				return future.get();
			
			throw new UnsupportedOperationException("Can not block when running inside the same EventQueue");
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			if (future.isDone())
				return future.get();
			
			throw new UnsupportedOperationException("Can not block when running inside the same EventQueue");
		}
    }
    
    
    public static abstract class Event {
    	
    	public abstract void execute() throws Throwable;
    }
}
