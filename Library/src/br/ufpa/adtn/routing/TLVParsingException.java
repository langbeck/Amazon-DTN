package br.ufpa.adtn.routing;

import br.ufpa.adtn.core.ParsingException;

public class TLVParsingException extends ParsingException {
	private static final long serialVersionUID = -934846059442904846L;

	public TLVParsingException() {
		super();
	}

	public TLVParsingException(String message, Throwable cause) {
		super(message, cause);
	}

	public TLVParsingException(String message) {
		super(message);
	}

	public TLVParsingException(Throwable cause) {
		super(cause);
	}
}
