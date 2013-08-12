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
package br.ufpa.adtn.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

public class ChainOfSegments {
private final Collection<ByteBuffer> segments;
	
	public ChainOfSegments() {
		this.segments = new ArrayList<ByteBuffer>();
	}
	
	public void append(byte[] data, int off, int len) {
		segments.add(ByteBuffer.wrap(data, off, len));
	}
	
	public void append(byte[] data) {
		segments.add(ByteBuffer.wrap(data));
	}
	
	public void append(ByteBuffer data) {
		segments.add(data.duplicate());
	}
	
	public boolean isEmpty() {
		return segments.isEmpty();
	}
	
	public int length() {
		return segments.size();
	}
	
	public ByteBuffer[] getSegments() {
		final ByteBuffer[] data = new ByteBuffer[segments.size()];
		segments.toArray(data);
		return data;
	}
}
