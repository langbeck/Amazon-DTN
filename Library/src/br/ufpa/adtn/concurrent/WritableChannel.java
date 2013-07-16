package br.ufpa.adtn.concurrent;

public interface WritableChannel<E> extends AbstractChannel {
	public void put(E e);
}
