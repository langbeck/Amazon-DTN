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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import br.ufpa.adtn.core.SystemClock;

/**
 * 
 * @author Dórian Langbeck
 */
public class TimeoutHelper<T> {
	
	public static <T> TimeoutHelper<T> create(
			EventQueue eventQueue,
			Handler<T> handler,
			long timeout
	) {
		return new TimeoutHelper<T>(eventQueue, handler, timeout);
	}
	
	
	private final Map<T, Entry> reference;
	private final EventQueue eventQueue;
	private final Handler<T> handler;
	private final long timeout;
	
	public TimeoutHelper(Handler<T> handler, long timeout) {
		this(new EventQueue(), handler, timeout);
	}
	
	public TimeoutHelper(EventQueue eQueue, Handler<T> handler, long timeout) {
		if (handler == null || eQueue == null)
			throw new NullPointerException();
		
		this.reference = new HashMap<T, Entry>();
		this.eventQueue = eQueue;
		this.handler = handler;
		this.timeout = timeout;
	}
	
	public long reset(T o) {
		try {
			return eventQueue.submit(new ResetRequest(o));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	public long purge(T o) {
		try {
			return eventQueue.submit(new PurgeRequest(o));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void refresh(T o) {
		try {
			eventQueue.postAndWait(new RefreshRequest(o));
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
	public Collection<T> getContents() {
		return Collections.unmodifiableCollection(reference.keySet());
	}
	
	public boolean contains(T o) {
		synchronized (reference) {
			return reference.containsKey(o);
		}
	}
	
	
	private class RefreshRequest implements Runnable {
		private final T obj;

		private RefreshRequest(T obj) {
			this.obj = obj;
		}
		
		@Override
		public void run() {
			synchronized (reference) {
				final Entry entry = reference.get(obj);
				if (entry == null) {
					reference.put(obj, new Entry(createRemoveEvent()));
					handler.onInserted(obj);
				} else if (entry.delEvent != null) {
					if (!entry.delEvent.cancel(false))
						throw new InternalError("Event cancelation failure");
					
					entry.delEvent = createRemoveEvent();
				} else {
					throw new InternalError("No remove event scheduled");
				}	
			}
		}
		
		private Future<?> createRemoveEvent() {
			return eventQueue.schedule(
					new DeleteRequest(obj),
					timeout,
					TimeUnit.MILLISECONDS
			);
		}
	}


	private class DeleteRequest implements Runnable {
		private final T obj;
		
		private DeleteRequest(T obj) {
			this.obj = obj;
		}

		@Override
		public void run() {
			synchronized (reference) {
				final Entry entry = reference.remove(obj);
				if (entry != null) {
					handler.onTimeout(
							obj,
							SystemClock.millis() - entry.creation
					);
				} else {
					throw new InternalError("Removing an inexistent entry");
				}
			}
		}
	}
	

	private class PurgeRequest implements Callable<Long> {
		private final T obj;
		
		private PurgeRequest(T obj) {
			this.obj = obj;
		}

		@Override
		public Long call() throws Exception {
			synchronized (reference) {
				final Entry entry = reference.remove(obj);
				if (entry != null) {
					if (entry.delEvent == null)
						throw new InternalError("No remove event scheduled");
					
					entry.delEvent.cancel(true);
					return SystemClock.millis() - entry.creation;
				} else {
					return -1L;
				}
			}
		}
	}
	

	private class ResetRequest implements Callable<Long> {
		private final T obj;
		
		private ResetRequest(T obj) {
			this.obj = obj;
		}

		@Override
		public Long call() throws Exception {
			synchronized (reference) {
				final Entry entry = reference.get(obj);
				if (entry != null) {
					final long now = SystemClock.millis();
					final long dur = now - entry.creation;
					entry.creation = now;
					return dur;
				} else {
					return -1L;
				}
			}
		}
	}
	
	
	private class Entry {
		private Future<?> delEvent;
		private long creation;
		
		public Entry(Future<?> f) {
			this.creation = SystemClock.millis();
			this.delEvent = f;
		}
	}
	
	
	public static interface Handler<T> {
		public void onTimeout(T o, long t);
		public void onInserted(T o);
	}
	
	
	public static abstract class AbstractHandler<T> implements Handler<T> {

		@Override
		public void onTimeout(T o, long t) { }

		@Override
		public void onInserted(T o) { }
	}
}
