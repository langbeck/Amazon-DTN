package br.ufpa.adtn.core;

public class ParsingException extends InicializationException {
	private static final long serialVersionUID = 7058713529576476854L;

	public ParsingException() {
		super();
	}

	public ParsingException(String message, Throwable cause) {
		super(message, cause);
	}

	public ParsingException(String message) {
		super(message);
	}

	public ParsingException(Throwable cause) {
		super(cause);
	}
}
