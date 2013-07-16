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
