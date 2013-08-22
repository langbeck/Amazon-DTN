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

import java.io.IOException;
import java.nio.ByteBuffer;

import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.core.InformationHub;
import br.ufpa.adtn.core.SerializableSegmentedObject;
import br.ufpa.adtn.util.BufferSlicer;
import br.ufpa.adtn.util.ChainOfSegments;
import br.ufpa.adtn.util.DataBlock;
import br.ufpa.adtn.util.SDNV;

public final class Bundle implements SerializableSegmentedObject {
	private final DataBlock payload;
	private final BundleInfo info;
	
	public Bundle(ByteBuffer buffer) {
		this.info = BundleInfo.parse(buffer);
		info.attach(this);
		
		if (buffer.get() != (byte) 0x01)
			throw new RuntimeException("Wrong block type");
		
		if (buffer.get() != (byte) 0x08)
			throw new RuntimeException("Wrong block flags");
		
		final byte[] data = new byte[SDNV.decodeInt(buffer)];
		buffer.get(data);
		
		this.payload = DataBlock.wrap(data);
	}
	
	public Bundle(BundleInfo info, DataBlock payload) {
		if (info == null || payload == null)
			throw new NullPointerException();
		
		if (info.isAttached())
			throw new IllegalArgumentException("BundleInfo already attached");
		
		this.payload = payload;
		this.info = info;
		info.attach(this);
		
		InformationHub.onCreation(this);
	}
	
	/**
	 * Equivalent to: {@code getInfo().getDestination()}
	 */
	public EID getDestination() {
		return info.getDestination();
	}
	
	/**
	 * Equivalent to: {@code getInfo().getSource()}
	 */
	public EID getSource() {
		return info.getSource();
	}
	
	public DataBlock getPayload() {
		return payload;
	}
	
	public BundleInfo getInfo() {
		return info;
	}
	
	public long getUniqueID() {
		return info.getUniqueID();
	}
	
	public int getPayloadLength() {
		return payload.getLength();
	}

	public int getDataLength() {
		final int plen = payload.getLength();
		return	info.getBlockLength() +
				SDNV.length(plen) +
				plen + 2;
	}
	
	@Override
	public String toString() {
		return String.format("Bundle [ID: %016x]", info.getUniqueID());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((info == null) ? 0 : info.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Bundle other = (Bundle) obj;
		if (info == null) {
			if (other.info != null)
				return false;
		} else if (!info.equals(other.info))
			return false;
		return true;
	}

	@Override
	public void serialize(ChainOfSegments chain, ByteBuffer buffer) throws IOException {
		final ByteBuffer pBuffer = payload.read();
		
		info.serialize(chain, buffer);
		
		final BufferSlicer slicer = new BufferSlicer(buffer);
		buffer.put((byte) 0x01);
		buffer.put((byte) 0x08);
		SDNV.encodeInt(buffer, payload.getLength());
		chain.append(slicer.end());
		
		chain.append(pBuffer);
	}
}
