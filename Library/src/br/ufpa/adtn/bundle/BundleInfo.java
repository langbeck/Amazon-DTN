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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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
	private final static long SECONDS_AT_2K = 946684800L;
	
	public final static int BUNDLE_IS_A_FRAGMENT_FLAG	= 0x01;
	public final static int ADMINISTRATIVE_RECORD_FLAG	= 0x02;
	public final static int MUST_NOT_FRAGMENT_FLAG		= 0x04;
	public final static int CUSTODY_TRANSFER_FLAG		= 0x08;
	public final static int SINGLETON_DESTINATION_FLAG	= 0x10;
	public final static int ACK_REQUESTED_FLAG			= 0x20;
	
	/**
	 * Custom flag used to mark (bit 5)
	 */
	public final static int IS_META_BUNDLE_FLAG			= 0x40;
	
	private static long DEFAULT_LIFETIME;
	private static long CREATION_TIME;
	private static int CREATION_SEQ;
	
	static {
		CREATION_TIME = SystemClock.secs() - SECONDS_AT_2K;
		DEFAULT_LIFETIME = 3600L;
		CREATION_SEQ = 0;
	}
	
	public static void setDefaultLifetime(int new_lifetime) {
		DEFAULT_LIFETIME = new_lifetime;
	}
	
	public static BundleInfo parse(ByteBuffer buffer) throws ParsingException {
		return new BundleInfo(buffer);
	}

	public static BundleInfo create(EID destination, EID source) {
		return create(
				destination,
				source,
				EID.NULL,
				EID.NULL
		);
	}
	
	public static BundleInfo create(EID destination, EID source, int flags) {
		return create(
				destination,
				source,
				EID.NULL,
				EID.NULL,
				0,
				0,
				flags
		);
	}
	
	public static BundleInfo create(EID destination, EID source, EID reportTo, EID custodian) {
		return create(
				destination,
				source,
				reportTo,
				custodian,
				0,
				0,
				0
		);
	}
	
	public static BundleInfo create(EID destination, EID source,
			EID reportTo, EID custodian, int fragment_offset,
			int total_data_len, int flags) {
		
		return create(destination,
				custodian,
				reportTo,
				source,
				fragment_offset,
				total_data_len,
				DEFAULT_LIFETIME,
				flags
		);
	}
	
	public synchronized static BundleInfo create(EID destination, EID custodian,
			EID reportTo, EID source, int fragment_offset,
			int total_data_len, long lifetime, int flags) {
		
		final long now = SystemClock.secs() - SECONDS_AT_2K;
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
				total_data_len,
				lifetime,
				flags
		);
	}
	
	
	private final EID destination;
	private final EID custodian;
	private final EID reportTo;
	private final EID source;

	private final long creation_time;
	private final int creation_seq;

	private final int fragment_offset;
	private final int total_data_len;
	
	private final long lifetime;
	private final int flags;

	private Bundle bundle;
	private int block_len;
	
	private BundleInfo(ByteBuffer buffer) throws ParsingException {
		if (buffer.get() != BPAgent.VERSION)
			throw new ParsingException("Version not supported");

		final int p0 = buffer.position();
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

		creation_time = SDNV.decodeLong(buffer);
		creation_seq = SDNV.decodeInt(buffer);
		lifetime = SDNV.decodeInt(buffer);

		final byte[] data = new byte[SDNV.decodeInt(buffer)];
		buffer.get(data);
		
		String scheme;
		String ssp;
		int offset;
		int pos;

		for (offset = offsets[0], pos = offset; data[pos] != (byte) 0; pos++);
		scheme = new String(data, offset, pos - offset);
		for (offset = offsets[1], pos = offset; data[pos] != (byte) 0; pos++);
		ssp = new String(data, offset, pos - offset);
		destination = EID.get(scheme, ssp);

		for (offset = offsets[2], pos = offset; data[pos] != (byte) 0; pos++);
		scheme = new String(data, offset, pos - offset);
		for (offset = offsets[3], pos = offset; data[pos] != (byte) 0; pos++);
		ssp = new String(data, offset, pos - offset);
		source = EID.get(scheme, ssp);

		for (offset = offsets[4], pos = offset; data[pos] != (byte) 0; pos++);
		scheme = new String(data, offset, pos - offset);
		for (offset = offsets[5], pos = offset; data[pos] != (byte) 0; pos++);
		ssp = new String(data, offset, pos - offset);
		reportTo = EID.get(scheme, ssp);
		
		for (offset = offsets[6], pos = offset; data[pos] != (byte) 0; pos++);
		scheme = new String(data, offset, pos - offset);
		for (offset = offsets[7], pos = offset; data[pos] != (byte) 0; pos++);
		ssp = new String(data, offset, pos - offset);
		custodian = EID.get(scheme, ssp);
		
		if (isFragment()) {
			fragment_offset = SDNV.decodeInt(buffer);
			total_data_len = SDNV.decodeInt(buffer);
		} else {
			fragment_offset = 0;
			total_data_len = 0;
		}
		
		final int pf = buffer.position();
		if (length != (pf - start))
			throw new ParsingException("Buffer offset problem");
		
		block_len = pf - p0 + 1;
		bundle = null;
	}
	
	private BundleInfo(EID destination, EID custodian, EID reportTo, EID source,
			long creation_time, int creation_seq, int fragment_offset,
			int total_data_len, long lifetime, int flags) {
		
		this.destination = destination;
		this.custodian = custodian;
		this.reportTo = reportTo;
		this.source = source;
		this.creation_time = creation_time;
		this.creation_seq = creation_seq;
		this.fragment_offset = fragment_offset;
		this.total_data_len = total_data_len;
		this.lifetime = lifetime;
		this.flags = flags;
		
		this.block_len = -1;
		this.bundle = null;
	}
	
	public boolean isExpired() {
		return getSecondsToExpiration() > 0;
	}
	
	public long getSecondsToExpiration() {
		return SECONDS_AT_2K + creation_time + lifetime - SystemClock.secs();
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

	public long getCreationTime() {
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
		if (bundle == null)
			throw new IllegalStateException("Not attached");
		
		return bundle.getPayload().getLength();
	}

	public long getLifetime() {
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
	
	public boolean isMeta() {
		return (flags & IS_META_BUNDLE_FLAG) != 0;
	}
	
	public long getUniqueID() {
		if (bundle == null)
			throw new IllegalStateException("Not attached");
		
		long hi_result = 1;
		long lo_result = 1;
		
		final int plen = bundle.getPayload().getLength();
		final int src_hash = source.hashCode();
		
		hi_result = 11 * hi_result + creation_time;
		hi_result = 11 * hi_result + creation_seq;
		hi_result = 11 * hi_result + src_hash;
		hi_result = 11 * hi_result + plen;

		lo_result = 23 * lo_result + creation_time;
		lo_result = 23 * lo_result + creation_seq;
		lo_result = 23 * lo_result + src_hash;
		lo_result = 23 * lo_result + plen;

		if (isFragment()) {
			hi_result = 11 * hi_result + fragment_offset;
			hi_result = 11 * hi_result + total_data_len;

			lo_result = 23 * lo_result + fragment_offset;
			lo_result = 23 * lo_result + total_data_len;
		}
		
		return ((hi_result & 0xFFFFFFFFL) << 32) | (lo_result & 0xFFFFFFFFL);
	}
	
	@Override
	public int hashCode() {
		if (bundle == null)
			throw new IllegalStateException("Not attached");
		
		final int prime = 31;
		int result = 1;
		
		result = prime * result + bundle.getPayload().getLength();
		result = prime * result + source.hashCode();
		result = prime * result + creation_seq;
		result = (int) (prime * result + creation_time);
		
		if (isFragment()) {
			result = prime * result + fragment_offset;
			result = prime * result + total_data_len;
		}
		
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)					return true;
		if (obj == null)					return false;
		if (getClass() != obj.getClass())	return false;
		
		final BundleInfo other = (BundleInfo) obj;
		if (bundle == null || other.bundle == null)	return false;
		if (creation_time != other.creation_time)	return false;
		if (creation_seq != other.creation_seq)		return false;
		
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;

		final boolean f0 = other.isFragment();
		final boolean f1 = isFragment();
		if (f0 ^ f1)
			return false;
		
		if (f0 && f1) {
			if (fragment_offset != other.fragment_offset)
				return false;
			if (total_data_len != other.total_data_len)
				return false;	
		}
		
		return true;
	}

	boolean isAttached() {
		return bundle != null;
	}
	
	void attach(Bundle bundle) {
		if (bundle.getInfo() != this)
			throw new IllegalArgumentException("Attachment must be reciprocal");
		
		if (this.bundle != null)
			throw new IllegalStateException("Already attached");
		
		this.bundle = bundle;
	}
	
	public int getBlockLength() {
		if (block_len < 0)
			block_len = getBlockLength0();
		
		return block_len;
	}
	
	private int getBlockLength0() {
		final Map<String, Integer> offsets = new HashMap<String, Integer>();
		int glen =	SDNV.length(creation_time)	+
					SDNV.length(creation_seq)	+
					SDNV.length(lifetime);
		
		Integer offset;
		int dlen = 0;
		String str;

		if ((offset = offsets.get(str = destination.getScheme())) == null) {
			final int length = str.getBytes().length;
			glen += SDNV.length(dlen);
			dlen += length + 1;
		} else {
			glen += SDNV.length(offset);
		}

		if ((offset = offsets.get(str = destination.getSSP())) == null) {
			final int length = str.getBytes().length;
			glen += SDNV.length(dlen);
			dlen += length + 1;
		} else {
			glen += SDNV.length(offset);
		}
		

		if ((offset = offsets.get(str = source.getScheme())) == null) {
			final int length = str.getBytes().length;
			glen += SDNV.length(dlen);
			dlen += length + 1;
		} else {
			glen += SDNV.length(offset);
		}

		if ((offset = offsets.get(str = source.getSSP())) == null) {
			final int length = str.getBytes().length;
			glen += SDNV.length(dlen);
			dlen += length + 1;
		} else {
			glen += SDNV.length(offset);
		}
		

		if ((offset = offsets.get(str = reportTo.getScheme())) == null) {
			final int length = str.getBytes().length;
			glen += SDNV.length(dlen);
			dlen += length + 1;
		} else {
			glen += SDNV.length(offset);
		}

		if ((offset = offsets.get(str = reportTo.getSSP())) == null) {
			final int length = str.getBytes().length;
			glen += SDNV.length(dlen);
			dlen += length + 1;
		} else {
			glen += SDNV.length(offset);
		}
		

		if ((offset = offsets.get(str = custodian.getScheme())) == null) {
			final int length = str.getBytes().length;
			glen += SDNV.length(dlen);
			dlen += length + 1;
		} else {
			glen += SDNV.length(offset);
		}

		if ((offset = offsets.get(str = custodian.getSSP())) == null) {
			final int length = str.getBytes().length;
			glen += SDNV.length(dlen);
			dlen += length + 1;
		} else {
			glen += SDNV.length(offset);
		}
		
		glen += dlen + SDNV.length(dlen);
		if (isFragment())
			glen += SDNV.length(fragment_offset) + SDNV.length(total_data_len);
		
		return 1 + SDNV.length(flags) + SDNV.length(glen) + glen;
	}
	
	private int[] serializeDictionary(ByteBuffer buffer) {
		final Set<String> strings = new LinkedHashSet<String>();
		final int[] offsets = new int[8];
		byte[] data = null;
		int offset = 0;
		
		final String str0 = destination.getScheme();
		offsets[0] = offset;
		
		data = str0.getBytes();
		buffer.put(data).put((byte) 0);
		offset += data.length + 1;
		
		final String str1 = destination.getSSP();
		if (strings.add(str1)) {
			offsets[1] = offset;

			data = str1.getBytes();
			buffer.put(data).put((byte) 0);
			offset += data.length + 1;
		} else {
			if (str1.equals(str0))
				offsets[1] = offsets[0];
		}
		
		final String str2 = source.getScheme();
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
		
		final String str3 = source.getSSP();
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
		
		final String str4 = reportTo.getScheme();
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
		
		final String str5 = reportTo.getSSP();
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
		
		final String str6 = custodian.getScheme();
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
		
		final String str7 = custodian.getSSP();
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
		
		SDNV.encodeLong(buffer, creation_time);
		SDNV.encodeInt(buffer, creation_seq);
		
		SDNV.encodeLong(buffer, lifetime);
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
