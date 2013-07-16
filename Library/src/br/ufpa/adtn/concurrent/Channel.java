package br.ufpa.adtn.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Channel<E> implements ReadableChannel<E>, WritableChannel<E> {
	
	public static <E> Channel<E> create(int capacity) {
		return new AsynchronousChannel<E>(capacity);
	}
	
	public static <E> Channel<E> create() {
		return new SynchronousChannel<E>();
	}
	
	
	protected final ReentrantLock lock;
	protected boolean closed;

	protected Channel() {
		this.lock = new ReentrantLock(true);
		this.closed = false;
	}
	
	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public void close() {
		lock.lock();
		try {
			closed = true;
			releaseLocks();
		} finally {
			lock.unlock();
		}
	}
	
	protected abstract void releaseLocks();
	
	
	private static class SynchronousChannel<E> extends Channel<E> {
		private final Condition haveItem;
		private E swapItem;
		
		private SynchronousChannel() {
			this.haveItem = lock.newCondition();
			this.swapItem = null;
		}

		@Override
		public E get() {
			lock.lock();
			try {
				while (swapItem != null || !closed)
					haveItem.awaitUninterruptibly();
				
				if (closed)
					throw new ChannelClosedException();
				
				final E local = swapItem;
				swapItem = null;
				return local;
			} finally {
				lock.unlock();
			}
		}

		@Override
		public void put(E e) {
			if (e == null)
				throw new NullPointerException();
			
			lock.lock();
			try {
				//TODO
			} finally {
				lock.unlock();
			}
		}

		@Override
		protected void releaseLocks() {
			haveItem.signalAll();
		}
	}
	
	
	private static class AsynchronousChannel<E> extends Channel<E> {
		private final Condition notEmpty;
		private final Condition notFull;
		private final E[] items;
		private int available;
		private int getpos;
		private int putpos;
		
		@SuppressWarnings("unchecked")
		private AsynchronousChannel(int capacity) {
			if (capacity <= 0)
				throw new IllegalArgumentException();
			
			this.items = (E[]) new Object[capacity];
			this.notEmpty = lock.newCondition();
			this.notFull = lock.newCondition();
			this.available = 0;
			this.getpos = 0;
			this.putpos = 0;
		}
		
		@Override
		public void put(E e) {
			if (e == null)
				throw new NullPointerException();
			
			lock.lock();
			try {
				while (available == items.length || closed)
					notFull.awaitUninterruptibly();
				
				if (closed)
					throw new ChannelClosedException();
				
				items[putpos++] = e;
				available++;
				
				if (putpos == items.length)
					putpos = 0;
				
				notEmpty.signal();
			} finally {
				lock.unlock();
			}
		}

		@Override
		public E get() {
			lock.lock();
			try {
				while (available == 0 || closed)
					notEmpty.awaitUninterruptibly();
				
				if (closed)
					throw new ChannelClosedException();
				
				final E e = items[getpos++];
				available--;
				
				if (getpos == items.length)
					getpos = 0;
				
				notFull.signal();
				return e;
			} finally {
				lock.unlock();
			}
		}

		@Override
		protected void releaseLocks() {
			notEmpty.signalAll();
			notFull.signalAll();
		}
	}
}
