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
