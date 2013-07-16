package br.ufpa.adtn.routing;

import java.nio.ByteBuffer;

import br.ufpa.adtn.core.SerializableEntity;
import br.ufpa.adtn.util.Logger;
import br.ufpa.adtn.util.SDNV;

public abstract class TLV implements SerializableEntity {
	public static final Logger LOGGER = new Logger("TLV");
	
	private final byte type;
	private byte flags;
	
	protected TLV(byte type) {
		this.type = type;
		this.flags = 0;
	}
	
	protected final void setFlags(byte flags) {
		this.flags = flags;
	}
	
	public final byte getFlags() {
		return flags;
	}
	
	public final byte getType() {
		return type;
	}
	
	public final int getLength() {
		int tlen = getDataLength() + 3;
		final int llen = SDNV.length(tlen);
		if (SDNV.length(tlen + llen) != llen)
			tlen++;
		
		return tlen;
	}
	
	@Override
	public synchronized final void serialize(ByteBuffer buffer) {
		buffer.put(type);
		buffer.put(flags);
		SDNV.encodeInt(buffer, getLength());
		
		//Define a limit to new sub-buffer created by ByteBuffer.slice()
		buffer.limit(buffer.position() + getDataLength());
		serializeTLV(buffer.slice());
	}
	
	protected abstract void serializeTLV(ByteBuffer buffer);
	protected abstract int getDataLength();
}
