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

import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.Set;

import br.ufpa.adtn.core.BPAgent;
import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.core.ParsingException;
import br.ufpa.adtn.core.SerializableSegmentedObject;
import br.ufpa.adtn.core.SystemClock;
import br.ufpa.adtn.util.BufferSlicer;
import br.ufpa.adtn.util.ChainOfSegments;
import br.ufpa.adtn.util.SDNV;


public final class BundleInfo implements SerializableSegmentedObject {
	public final static int BUNDLE_IS_A_FRAGMENT_FLAG	= 0x01;
	public final static int ADMINISTRATIVE_RECORD_FLAG	= 0x02;
	public final static int MUST_NOT_FRAGMENT_FLAG		= 0x04;
	public final static int CUSTODY_TRANSFER_FLAG		= 0x08;
	public final static int SINGLETON_DESTINATION_FLAG	= 0x10;
	public final static int ACK_REQUESTED_FLAG			= 0x20;
	
	private static int DEFAULT_LIFETIME;
	private static int CREATION_TIME;
	private static int CREATION_SEQ;
	
	static {
		CREATION_TIME = (int) (SystemClock.millis() / 1000);
		DEFAULT_LIFETIME = 3600;
		CREATION_SEQ = 0;
	}
	
	public static void setDefaultLifetime(int new_lifetime) {
		DEFAULT_LIFETIME = new_lifetime;
	}
	
	public static BundleInfo parse(ByteBuffer buffer) throws ParsingException {
		return new BundleInfo(buffer);
	}

	public static BundleInfo create(EID destination, EID source, int size) {
		return create(
				destination,
				EID.NULL,
				EID.NULL,
				source,
				0,
				size,
				0,
				0
		);
	}
	
	public static BundleInfo create(EID destination, EID custodian,
			EID reportTo, EID source, int fragment_offset, int data_len,
			int total_data_len, int flags) {
		
		return create(destination,
				custodian,
				reportTo,
				source,
				fragment_offset,
				data_len,
				total_data_len,
				DEFAULT_LIFETIME,
				flags
		);
	}
	
	public synchronized static BundleInfo create(EID destination, EID custodian,
			EID reportTo, EID source, int fragment_offset, int data_len,
			int total_data_len, int lifetime, int flags) {
		
		final int now = (int) (SystemClock.millis() / 1000);
		final int creation_seq;
		if (now != CREATION_TIME) {
			CREATION_SEQ = creation_seq = 0;
			CREATION_TIME = now;
		} else {
			creation_seq = ++CREATION_SEQ;
		}
		
		return new BundleInfo(
				destination,
				custodian,
				reportTo,
				source,
				now,
				creation_seq,
				fragment_offset,
				data_len,
				total_data_len,
				lifetime,
				flags
		);
	}
	
	
	private final EID destination;
	private final EID custodian;
	private final EID reportTo;
	private final EID source;

	private final int creation_time;
	private final int creation_seq;

	private final int fragment_offset;
	private final int total_data_len;
	private final int data_len;
	
	private final int lifetime;
	private final int flags;
	
	private BundleInfo(ByteBuffer buffer) throws ParsingException {
		if (buffer.get() != BPAgent.VERSION)
			throw new ParsingException("Version not supported");

		flags = SDNV.decodeInt(buffer);

		final int length = SDNV.decodeInt(buffer);
		final int start = buffer.position();
		if (buffer.remaining() < length)
			throw new ParsingException("Buffer offset problem");

		final int[] offsets = new int[] {
				SDNV.decodeInt(buffer),
				SDNV.decodeInt(buffer),
				SDNV.decodeInt(buffer),
				SDNV.decodeInt(buffer),
				SDNV.decodeInt(buffer),
				SDNV.decodeInt(buffer),
				SDNV.decodeInt(buffer),
				SDNV.decodeInt(buffer)
		};

		creation_time = SDNV.decodeInt(buffer);
		creation_seq = SDNV.decodeInt(buffer);
		lifetime = SDNV.decodeInt(buffer);

		final byte[] dictData = new byte[SDNV.decodeInt(buffer)];
		buffer.get(dictData);
		
		String scheme;
		String ssp;
		int offset;
		int pos;

		for (offset = offsets[0], pos = offset; dictData[pos] != (byte) 0; pos++);
		scheme = new String(dictData, offset, pos - offset);
		for (offset = offsets[1], pos = offset; dictData[pos] != (byte) 0; pos++);
		ssp = new String(dictData, offset, pos - offset);
		destination = EID.get(scheme, ssp);

		for (offset = offsets[2], pos = offset; dictData[pos] != (byte) 0; pos++);
		scheme = new String(dictData, offset, pos - offset);
		for (offset = offsets[3], pos = offset; dictData[pos] != (byte) 0; pos++);
		ssp = new String(dictData, offset, pos - offset);
		source = EID.get(scheme, ssp);

		for (offset = offsets[4], pos = offset; dictData[pos] != (byte) 0; pos++);
		scheme = new String(dictData, offset, pos - offset);
		for (offset = offsets[5], pos = offset; dictData[pos] != (byte) 0; pos++);
		ssp = new String(dictData, offset, pos - offset);
		reportTo = EID.get(scheme, ssp);
		
		for (offset = offsets[6], pos = offset; dictData[pos] != (byte) 0; pos++);
		scheme = new String(dictData, offset, pos - offset);
		for (offset = offsets[7], pos = offset; dictData[pos] != (byte) 0; pos++);
		ssp = new String(dictData, offset, pos - offset);
		custodian = EID.get(scheme, ssp);
		
		if (isFragment()) {
			fragment_offset = SDNV.decodeInt(buffer);
			total_data_len = SDNV.decodeInt(buffer);
		} else {
			fragment_offset = 0;
			total_data_len = 0;
		}
		
		if (length != (buffer.position() - start))
			throw new ParsingException("Buffer offset problem");
		
		data_len = -1;
	}
	
	private BundleInfo(EID destination, EID custodian, EID reportTo, EID source,
			int creation_time, int creation_seq, int fragment_offset,
			int data_len, int total_data_len, int lifetime, int flags) {
		
		this.destination = destination;
		this.custodian = custodian;
		this.reportTo = reportTo;
		this.source = source;
		this.creation_time = creation_time;
		this.creation_seq = creation_seq;
		this.fragment_offset = fragment_offset;
		this.data_len = data_len;
		this.total_data_len = total_data_len;
		this.lifetime = lifetime;
		this.flags = flags;
	}
	
	public BundleInfo createFragment(int offset, int length) {
		if (isFragment())
			throw new IllegalStateException("Bundle is already a fragment");
		
		if (!canFragment())
			throw new IllegalStateException("Bundle can not be fragmented");
		
		return new BundleInfo(
				destination,
				custodian,
				reportTo,
				source,
				creation_time,
				creation_seq,
				offset,
				length,
				total_data_len,
				lifetime,
				flags | BUNDLE_IS_A_FRAGMENT_FLAG
		);
	}

	public EID getDestination() {
		return destination;
	}

	public EID getCustodian() {
		return custodian;
	}

	public EID getReportTo() {
		return reportTo;
	}

	public EID getSource() {
		return source;
	}

	public int getCreationTime() {
		return creation_time;
	}

	public int getCreationSequence() {
		return creation_seq;
	}

	public int getFragmentOffset() {
		return fragment_offset;
	}
	
	public int getTotalDataLength() {
		return total_data_len;
	}

	public int getDataLength() {
		return data_len;
	}

	public int getLifetime() {
		return lifetime;
	}

	public int getFlags() {
		return flags;
	}
	
	public boolean isAdministrativeRecord() {
		return (flags & ADMINISTRATIVE_RECORD_FLAG) != 0;
	}
	
	public boolean canFragment() {
		return (flags & MUST_NOT_FRAGMENT_FLAG) == 0;
	}
	
	public boolean isFragment() {
		return (flags & BUNDLE_IS_A_FRAGMENT_FLAG) != 0;
	}
	
	private int[] serializeDictionary(ByteBuffer buffer) {
		int[] offsets = new int[8];

		Set<String> strings = new LinkedHashSet<String>();
		byte[] data = null;
		int offset = 0;
		
		String str0 = destination.getScheme();
		if (strings.add(str0)) {
			offsets[0] = offset;
			
			data = str0.getBytes();
			buffer.put(data).put((byte) 0);
			offset += data.length + 1;
		}
		
		String str1 = destination.getSSP();
		if (strings.add(str1)) {
			offsets[1] = offset;

			data = str1.getBytes();
			buffer.put(data).put((byte) 0);
			offset += data.length + 1;
		} else {
			if (str1.equals(str0))
				offsets[1] = offsets[0];
		}
		
		String str2 = source.getScheme();
		if (strings.add(str2)) {
			offsets[2] = offset;

			data = str2.getBytes();
			buffer.put(data).put((byte) 0);
			offset += data.length + 1;
		} else {
			if (str2.equals(str0))
				offsets[2] = offsets[0];
			else if (str2.equals(str1))
				offsets[2] = offsets[1];
		}
		
		String str3 = source.getSSP();
		if (strings.add(str3)) {
			offsets[3] = offset;

			data = str3.getBytes();
			buffer.put(data).put((byte) 0);
			offset += data.length + 1;
		} else {
			if (str3.equals(str0))
				offsets[3] = offsets[0];
			else if (str3.equals(str1))
				offsets[3] = offsets[1];
			else if (str3.equals(str2))
				offsets[3] = offsets[2];
		}
		
		String str4 = reportTo.getScheme();
		if (strings.add(str4)) {
			offsets[4] = offset;

			data = str4.getBytes();
			buffer.put(data).put((byte) 0);
			offset += data.length + 1;
		} else {
			if (str4.equals(str0))
				offsets[4] = offsets[0];
			else if (str4.equals(str1))
				offsets[4] = offsets[1];
			else if (str4.equals(str2))
				offsets[4] = offsets[2];
			else if (str4.equals(str3))
				offsets[4] = offsets[3];
		}
		
		String str5 = reportTo.getSSP();
		if (strings.add(str5)) {
			offsets[5] = offset;

			data = str5.getBytes();
			buffer.put(data).put((byte) 0);
			offset += data.length + 1;
		} else {
			if (str5.equals(str0))
				offsets[5] = offsets[0];
			else if (str5.equals(str1))
				offsets[5] = offsets[1];
			else if (str5.equals(str2))
				offsets[5] = offsets[2];
			else if (str5.equals(str3))
				offsets[5] = offsets[3];
			else if (str5.equals(str4))
				offsets[5] = offsets[4];
		}
		
		String str6 = custodian.getScheme();
		if (strings.add(str6)) {
			offsets[6] = offset;

			data = str6.getBytes();
			buffer.put(data).put((byte) 0);
			offset += data.length + 1;
		} else {
			if (str6.equals(str0))
				offsets[6] = offsets[0];
			else if (str6.equals(str1))
				offsets[6] = offsets[1];
			else if (str6.equals(str2))
				offsets[6] = offsets[2];
			else if (str6.equals(str3))
				offsets[6] = offsets[3];
			else if (str6.equals(str4))
				offsets[6] = offsets[4];
			else if (str6.equals(str5))
				offsets[6] = offsets[5];
		}
		
		String str7 = reportTo.getSSP();
		if (strings.add(str7)) {
			offsets[7] = offset;

			data = str7.getBytes();
			buffer.put(data).put((byte) 0);
			offset += data.length + 1;
		} else {
			if (str7.equals(str0))
				offsets[7] = offsets[0];
			else if (str7.equals(str1))
				offsets[7] = offsets[1];
			else if (str7.equals(str2))
				offsets[7] = offsets[2];
			else if (str7.equals(str3))
				offsets[7] = offsets[3];
			else if (str7.equals(str4))
				offsets[7] = offsets[4];
			else if (str7.equals(str5))
				offsets[7] = offsets[5];
			else if (str7.equals(str6))
				offsets[7] = offsets[6];
		}
		
		
		return offsets;
	}

	@Override
	public void serialize(ChainOfSegments chain, ByteBuffer buffer) {
		final BufferSlicer slicer = new BufferSlicer(buffer);
		
		buffer.put(BPAgent.VERSION);
		SDNV.encodeInt(buffer, flags);
		final ByteBuffer bh = slicer.end();
		
		final int blockStart = buffer.position();
		
		int[] offsets = serializeDictionary(buffer);
		final ByteBuffer dict = slicer.end();

		SDNV.encodeInt(buffer, offsets[0]);
		SDNV.encodeInt(buffer, offsets[1]);
		SDNV.encodeInt(buffer, offsets[2]);
		SDNV.encodeInt(buffer, offsets[3]);
		SDNV.encodeInt(buffer, offsets[4]);
		SDNV.encodeInt(buffer, offsets[5]);
		SDNV.encodeInt(buffer, offsets[6]);
		SDNV.encodeInt(buffer, offsets[7]);
		
		SDNV.encodeInt(buffer, creation_time);
		SDNV.encodeInt(buffer, creation_seq);
		SDNV.encodeInt(buffer, lifetime);
		SDNV.encodeInt(buffer, dict.limit());
		final ByteBuffer middle = slicer.end();

		ByteBuffer fragment = null;
		if (isFragment()) {
			SDNV.encodeInt(buffer, fragment_offset);
			SDNV.encodeInt(buffer, total_data_len);
			fragment = slicer.end();
		}
		
		SDNV.encodeInt(buffer, buffer.position() - blockStart);
		final ByteBuffer blocklen = slicer.end();

		
		chain.append(bh);
		chain.append(blocklen);
		chain.append(middle);
		chain.append(dict);
		
		if (fragment != null)
			chain.append(fragment);
	}
}
