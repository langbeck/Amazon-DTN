package br.ufpa.adtn.core;

public class InicializationException extends RuntimeException {
	private static final long serialVersionUID = -6714999894660889089L;

	public InicializationException() {
		super();
	}

	public InicializationException(String message, Throwable cause) {
		super(message, cause);
	}

	public InicializationException(String message) {
		super(message);
	}

	public InicializationException(Throwable cause) {
		super(cause);
	}
}
