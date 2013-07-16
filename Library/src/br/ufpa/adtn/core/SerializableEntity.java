package br.ufpa.adtn.core;

import java.nio.ByteBuffer;

public interface SerializableEntity {
	
	public void serialize(ByteBuffer buffer);
}
