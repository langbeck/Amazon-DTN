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
