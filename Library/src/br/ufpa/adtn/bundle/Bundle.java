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
package br.ufpa.adtn.bundle;

import java.io.Serializable;
import java.nio.ByteBuffer;

import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.core.SerializableEntity;

public class Bundle implements SerializableEntity, Serializable {
	private static final long serialVersionUID = 1422146061796800329L;
	private final EID destination;
	private final EID source;
	
	private final byte[] data;
	
	public Bundle(EID source, EID destination, ByteBuffer data) {
		this.destination = destination;
		this.source = source;
		
		this.data = new byte[data.rewind().limit()];
		data.get(this.data);
		data.rewind();
	}
	
	public EID getDestination() {
		return destination;
	}
	
	public EID getSource() {
		return source;
	}

	public ByteBuffer getPayload() {
		/*
		 * TODO Each time getPayload() is invoked a independent ByteBuffer must
		 * be returned.
		 */
		return ByteBuffer.wrap(data).asReadOnlyBuffer();
	}
	
	@Override
	public int hashCode() {
		//TODO Implement the real hashCode method
		return super.hashCode();
	}

	@Override
	public void serialize(ByteBuffer buffer) {
		//TODO Implement
		throw new UnsupportedOperationException("Not implemented");
	}
	
	public int length() {
		return 0;
	}
}
