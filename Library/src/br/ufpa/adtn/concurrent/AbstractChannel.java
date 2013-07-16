package br.ufpa.adtn.concurrent;

public interface AbstractChannel {
	public boolean isClosed();
	public void close();
}
