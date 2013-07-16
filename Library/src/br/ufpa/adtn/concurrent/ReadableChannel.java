package br.ufpa.adtn.concurrent;

public interface ReadableChannel<E> extends AbstractChannel {
	public E get();
}
