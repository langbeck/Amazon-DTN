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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import br.ufpa.adtn.core.ParsingException;
import br.ufpa.adtn.core.SerializableEntity;
import br.ufpa.adtn.util.Logger;
import br.ufpa.adtn.util.SDNV;

public class Message<T extends TLV> implements SerializableEntity {

	public static interface TLVParser<T extends TLV> {
		public T parse(ByteBuffer buffer, byte type, byte flags) throws TLVParsingException;
	}
	
	
	public static <T extends TLV> Message<T> create(
			byte result,
			byte code,
			short sender,
			short receiver,
			int identifier
	) {
		return new Message<T>(
				(byte) 0,
				(byte) 1,
				(byte) 0,
				result,
				code,
				sender,
				receiver,
				identifier,
				(short) 0
		);
	}
	
	public static <T extends TLV> Message<T> unpack(ByteBuffer buffer, TLVParser<T> parser) throws ParsingException {
		final byte protocol = buffer.get();
		
		final byte verfla = buffer.get();
		final byte version = (byte) (verfla >> 4);
		final byte flags = (byte) (verfla & 0x0F);
		
		final byte result = buffer.get();
		final byte code = buffer.get();
		
		final short receiver = buffer.getShort();
		final short sender = buffer.getShort();
		
		final int identifier = buffer.getInt();
		final short subMessage = buffer.getShort();
		
		int len = SDNV.decodeInt(buffer);
		len -= SDNV.length(len);
		len -= 14;
		
		if (buffer.remaining() != len) {
			System.err.println(buffer.remaining());
			System.err.println(buffer.capacity());
			System.err.println(buffer.limit());
			System.err.println(len);
			throw new ParsingException("Buffer offset problem");
		}
		
		final Message<T> msg = new Message<T>(
				protocol,
				version,
				flags,
				result,
				code,
				sender,
				receiver,
				identifier,
				subMessage
		);
		
		// Individual TLV parsing
		int bpos = buffer.position();
		int blen = buffer.limit();
		while (buffer.remaining() != 0) {
			final byte type = buffer.get();
			final byte tFlags = buffer.get();
			
			// Create a sub-buffer with respective TLV data
			buffer.limit(bpos += SDNV.decodeInt(buffer));
			msg.add(parser.parse(buffer.slice(), type, tFlags));
			
			// Define the new position for the end of current TLV
			buffer.position(bpos);
			
			// Restores original buffer limit
			buffer.limit(blen);
		}
		
		return msg;
	}
	
	
	
	private final List<T> tlvList;
	private final short subMessage;
	private final int identifier;
	
	private final byte protocolNumber;
	private final byte version;
	private final byte result;
	private final byte flags;
	private final byte code;
	
	private final short receiver;
	private final short sender;
	
	private Message(
			byte protocolNumber,
			byte version,
			byte flags,
			byte result,
			byte code,
			short sender,
			short receiver,
			int identifier,
			short subMessage
		) {
		
		if (subMessage < 0 || sender == 0)
			throw new IllegalArgumentException();
		
		if (flags < 0 || flags > 0x0F)
			throw new IllegalArgumentException();
		
		this.tlvList = new ArrayList<T>();
		
		this.subMessage = (subMessage != 0) ?
				(short) (subMessage | 0x8000) :
				(short) 0;
		
		this.protocolNumber = protocolNumber;
		this.identifier = identifier;
		this.receiver = receiver;
		this.version = version;
		this.result = result;
		this.sender = sender;
		this.flags = flags;
		this.code = code;
	}
	
	public short getReceiver() {
		return receiver;
	}
	
	public short getSender() {
		return sender;
	}

	public byte getProtocolNumber() {
		return protocolNumber;
	}
	
	public int getID() {
		return identifier;
	}

	public byte getVersion() {
		return version;
	}
	
	public byte getResult() {
		return result;
	}
	
	public byte getFlags() {
		return flags;
	}
	
	public void add(T tlv) {
		if (tlv == null)
			throw new IllegalArgumentException("TLV can not be null");
		
		synchronized (tlvList) {
			tlvList.add(tlv);
		}
	}
	
	public Collection<T> getTLVs() {
		return Collections.unmodifiableCollection(tlvList);
	}

	@Override
	public void serialize(ByteBuffer buffer) {
		buffer.put(protocolNumber);
		buffer.put((byte) ((version << 4) | (flags & 0x0F)));
		buffer.put(result);
		buffer.put(code);
		
		buffer.putShort(receiver);
		buffer.putShort(sender);
		buffer.putInt(identifier);
		buffer.putShort(subMessage);
		
		synchronized (tlvList) {
			//Fixed-length fields
			int tlen = 14;
			
			//TLVs in this Message
			for (TLV tlv : tlvList)
				tlen += tlv.getLength();
			
			int llen = SDNV.length(tlen);
			tlen += llen;
			if (SDNV.length(tlen + llen) != llen)
				tlen++;
			
			SDNV.encodeInt(buffer, tlen);
			
			/*
			 * Serialize all TLVs in this message with limited slices of the
			 * original buffer to prevent each TLV exceeds the size reported
			 */
			int spos = buffer.position();
			for (TLV tlv : tlvList) {
				final int len = tlv.getLength();
				Logger.e("TLV-Size", "In Message: " + tlv.getLength());
				buffer.limit(spos + len);
				tlv.serialize(buffer.slice());
				
				spos += len;
			}
			
			buffer.limit(spos);
		}
	}
}
