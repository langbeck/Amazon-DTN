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
package br.ufpa.adtn.routing.prophet;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.List;

import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.core.SerializableEntity;
import br.ufpa.adtn.routing.Message.TLVParser;
import br.ufpa.adtn.routing.prophet.ProphetUtil.BundleSpec;
import br.ufpa.adtn.routing.prophet.ProphetDataRouting.NeighborPredict;
import br.ufpa.adtn.routing.TLV;
import br.ufpa.adtn.routing.TLVParsingException;
import br.ufpa.adtn.util.SDNV;

public abstract class ProphetTLV extends TLV {
	private static final byte HELLO 			= 0x01;
	private static final byte ERROR 			= 0x02;
	private static final byte ROUTING_INFO 		= (byte) 0xA1;
	private static final byte BUNDLE_OFFER 		= (byte) 0xA4;
	private static final byte BUNDLE_RESPONSE 	= (byte) 0xA5;
	

	public static final TLVParser<ProphetTLV> PARSER = new TLVParser<ProphetTLV>() {

		@Override
		public ProphetTLV parse(ByteBuffer buffer, byte type, byte flags) throws TLVParsingException {
			switch (type) {
			case HELLO:
				return new Hello(
						HelloType.getType(flags),
						EID.decode(buffer),
						SDNV.decodeInt(buffer)
				);
			
			case ERROR:
				final ErrorType flag = ErrorType.getErrorType(flags);
				final byte[] data = new byte[2];
				buffer.get(data);
				
				return new Error(flag, data);
				
			case ROUTING_INFO:
				return new RoutingInformation(buffer);
				
			case BUNDLE_OFFER:
				return new BundleOffer(buffer);
				
			case BUNDLE_RESPONSE:
				return new BundleResponse(buffer);

			default:
				throw new TLVParsingException("Invalid type: " + type);
			}
		}
	};
	
	public static enum HelloType {
		SYN, SYNACK, ACK, RSTACK;
		
		public static HelloType getType(byte flags) {
			return HelloType.values()[flags - 1];
		}
	}
	
	public static final class Hello extends ProphetTLV {
		private final int timer;
		private final EID eid;
		private final int tlen;
		
		public Hello(HelloType type, EID eid, int timer) {
			super((byte) 0x01);
			
			if (type == null || eid == null)
				throw new IllegalArgumentException();
			
			this.timer = timer;
			this.eid = eid;
			
			int eidLength = eid.getDataLength();
			this.tlen = SDNV.length(timer) + SDNV.length(eidLength) 
						+ eidLength;
			
			setFlags((byte) (type.ordinal() + 1));
		}
		
		public HelloType getHelloType() {
			return HelloType.getType(getFlags());
		}

		@Override
		protected void serializeTLV(ByteBuffer buffer) {
			SDNV.encodeInt(buffer, timer);
			eid.encode(buffer);
		}

		@Override
		protected int getDataLength() {
			return tlen;
		}
		
		protected EID getEID() {
			return eid;
		}
		
		protected int getTimer() {
			return timer;
		}
	}
	
	public static enum ErrorType {
		DICT_CONFLICT, BAD_STRING_ID;
		
		public static ErrorType getErrorType(byte flags) {
			switch (flags) {
			case (byte) 0x00:
				return DICT_CONFLICT;
			
			case (byte) 0x01:
				return BAD_STRING_ID;

			default:
				throw new RuntimeException("Undefined Erro Type at TLV Parse: " + flags);
			}
		}
	}
	
	public static final class Error extends ProphetTLV {
		private final byte[] data;
		
		public Error(ErrorType type, byte[] data) {
			super((byte) 0x02);
			
			if (type == null)
				throw new IllegalArgumentException("ErroType can not be null");
			
			if (type == ErrorType.DICT_CONFLICT || type == ErrorType.BAD_STRING_ID) {
				if (data == null)
					throw new IllegalArgumentException("Data can not be null for this ErroType");
			}
			
			this.data = data;
			setFlags((byte) type.ordinal());
		}

		@Override
		protected void serializeTLV(ByteBuffer buffer) {
			buffer.put(data);
		}

		@Override
		protected int getDataLength() {
			return data.length;
		}
	}
	
	public static final class RoutingInformation extends ProphetTLV {
		private static class PredictEntry implements SerializableEntity {
			private final float predict;
			private final int sid;
			
			public PredictEntry(int sid, float weight) {
				this.predict = weight;
				this.sid = sid;
			}

			@Override
			public void serialize(ByteBuffer buffer) {
				SDNV.encodeInt(buffer, sid);
				buffer.putFloat(predict);
			}
		}
		
		private final PredictEntry[] rib;
		private final EID[] eids;
		private final int tlen;
		
		public RoutingInformation(ByteBuffer buffer) {
			super((byte) 0xA1);
			
			final int spos = buffer.position();
			
			final int eids_len = SDNV.decodeInt(buffer);
			this.eids = new EID[eids_len];
			for (int i = 0; i < eids_len; i++)
				eids[i] = EID.decode(buffer);
			
			final int rib_len = SDNV.decodeInt(buffer);
			this.rib = new PredictEntry[rib_len];
			for (int i = 0; i < rib_len; i++)
				rib[i] = new PredictEntry(
						SDNV.decodeInt(buffer),
						buffer.getFloat()
				);
			
			this.tlen = buffer.position() - spos;
		}
		
		public RoutingInformation(NeighborPredict[] rib) {
			super((byte) 0xA1);
			
			final List<EID> eids = new ArrayList<EID>();
			
			final int rib_len = rib.length;
			
			int len = SDNV.length(rib_len);	 // RIBStringCount
			this.rib = new PredictEntry[rib_len];
			for (int i = 0; i < rib_len; i++) {
				final NeighborPredict nWeight = rib[i];
				final EID eid = nWeight.getEID();
				int index = eids.indexOf(eid);
				if (index == -1) {
					index = eids.size();
					eids.add(eid);
				}
				
				final int dlen = eid.getDataLength();
				len += dlen + SDNV.length(dlen) + SDNV.length(index);
				this.rib[i] = new PredictEntry(index, nWeight.getPredict());
			}
			
			final int eids_len = eids.size();
			this.eids = new EID[eids_len];
			eids.toArray(this.eids);

			for (int i = 0; i < eids_len; i++) {
				final int dlen = this.eids[i].getDataLength();
				len += dlen + SDNV.length(dlen);
			}
			
			this.tlen = len + SDNV.length(eids_len);
		}
		
		@Override
		protected void serializeTLV(ByteBuffer buffer) {
			final int swni_len = rib.length;
			final int eids_len = eids.length;
			
			SDNV.encodeInt(buffer, eids_len);
			for (int i = 0; i < eids_len; i++)
				eids[i].encode(buffer);

			SDNV.encodeInt(buffer, swni_len);
			for (int i = 0; i < swni_len; i++)
				rib[i].serialize(buffer);
			
		}

		@Override
		protected int getDataLength() {
			return tlen;
		}
		
		public NeighborPredict[] getPredicts() {
			final NeighborPredict[] predicts = new NeighborPredict[rib.length];
			for (int i = 0; i < rib.length; i++) {
				final PredictEntry entry = rib[i];
				predicts[i] = new NeighborPredict(eids[entry.sid], entry.predict);
			}
			
			return predicts;
		}
	}
	
	public static final class BundleOffer extends ProphetTLV {
		private static class BundleEntry implements SerializableEntity {
			private final byte b_flag;
			private final int src;
			private final int dest;
			private final int cts;
			private final int cts_seqn;
			
			public BundleEntry(byte b_flag, int src, int dest, int cts, int cts_seqn) {
				this.b_flag = b_flag;
				this.src = src;
				this.dest = dest;
				this.cts = cts;
				this.cts_seqn = cts_seqn;
			}

			@Override
			public void serialize(ByteBuffer buffer) {
				buffer.put(b_flag);
				SDNV.encodeInt(buffer, src);
				SDNV.encodeInt(buffer, dest);
				SDNV.encodeInt(buffer, cts);
				SDNV.encodeInt(buffer, cts_seqn);
			}
		}
		
		private final BundleEntry[] offers;
		
		final List<EID> eids = new ArrayList<EID>();
		private final int tlen;
		
		public BundleOffer(ByteBuffer buffer) {
			super((byte) 0xA4);
			
			final int spos = buffer.position();
			final int offer_count = SDNV.decodeInt(buffer);
			this.offers = new BundleEntry[offer_count];
			for (int i = 0; i < offer_count; i++)
				offers[i] = new BundleEntry(
						buffer.get(),
						SDNV.decodeInt(buffer), 
						SDNV.decodeInt(buffer), 
						SDNV.decodeInt(buffer), 
						SDNV.decodeInt(buffer)
				);
			
			this.tlen = buffer.position() - spos;
		}
		
		public BundleOffer(BundleSpec[] offers) {
			super((byte) 0xA4);
			
			
			final int offer_count = offers.length;
			
			int len = SDNV.length(offer_count);
			this.offers = new BundleEntry[offer_count];
			for (int i = 0; i < offer_count; i++) {
				final BundleSpec bs = offers[i];
				final EID dst = bs.getDestination();
				// TODO Obtaining the index is correct this way?
				int index_dest = eids.indexOf(dst);
				if (index_dest == -1) {
					index_dest = eids.size();
					eids.add(dst);
				}
				final EID src = bs.getSource();
				// TODO Obtaining the index is correct this way?
				int index_src = eids.indexOf(src);
				if (index_src == -1) {
					index_src = eids.size();
					eids.add(src);
				}
				final int cts = bs.timeStamp();
				final int cts_seqn = bs.timeStampSeqNumber();
				len += SDNV.length(index_src) + SDNV.length(index_dest) +
					   SDNV.length(cts) + SDNV.length(cts_seqn) + 1;
				this.offers[i] = new BundleEntry(
							(byte)0x00, 
							index_src, 
							index_src,
							cts, 
							cts_seqn
				);
			}
			
			this.tlen = len;
		}

		@Override
		protected void serializeTLV(ByteBuffer buffer) {
			final int offer_count = offers.length;
			
			SDNV.encodeInt(buffer, offer_count);
			for (int i = 0; i < offer_count; i++)
				offers[i].serialize(buffer);
		}

		@Override
		protected int getDataLength() {
			return tlen;
		}
		
		public BundleSpec[] getOffers() {
			final int len = offers.length;
			final BundleSpec[] ofs = new BundleSpec[len];
			for (int i = 0; i < len; i++)
				ofs[i] = new BundleSpec(
						eids.get(offers[i].src),
						eids.get(offers[i].dest),
						offers[i].b_flag
				);
				
			return ofs;
		}
	}
	
	public static final class BundleResponse extends ProphetTLV {
		
		private static class BundleEntry implements SerializableEntity {
			private final byte b_flag;
			private final int src;
			private final int dest;
			private final int cts;
			private final int cts_seqn;
			
			public BundleEntry(byte b_flag, int src, int dest, int cts, int cts_seqn) {
				this.b_flag = b_flag;
				this.src = src;
				this.dest = dest;
				this.cts = cts;
				this.cts_seqn = cts_seqn;
			}

			@Override
			public void serialize(ByteBuffer buffer) {
				buffer.put(b_flag);
				SDNV.encodeInt(buffer, src);
				SDNV.encodeInt(buffer, dest);
				SDNV.encodeInt(buffer, cts);
				SDNV.encodeInt(buffer, cts_seqn);
			}
		}
		
		private final BundleEntry[] responses;
		
		final List<EID> eids = new ArrayList<EID>();
		private final int tlen;
		
		public BundleResponse(ByteBuffer buffer) {
			super((byte) 0xA5);
			
			final int spos = buffer.position();
			final int respos_count = SDNV.decodeInt(buffer);
			this.responses = new BundleEntry[respos_count];
			for (int i = 0; i < respos_count; i++)
				responses[i] = new BundleEntry(
						buffer.get(),
						SDNV.decodeInt(buffer), 
						SDNV.decodeInt(buffer), 
						SDNV.decodeInt(buffer), 
						SDNV.decodeInt(buffer)
				);
			
			this.tlen = buffer.position() - spos;
		}
		
		public BundleResponse(BundleSpec[] responses) {
			super((byte) 0xA4);
			
			final int respos_count = responses.length;
			
			int len = SDNV.length(respos_count);
			this.responses = new BundleEntry[respos_count];
			for (int i = 0; i < respos_count; i++) {
				final BundleSpec bs = responses[i];
				final EID dst = bs.getDestination();
				int index_dest = eids.indexOf(dst);
				if (index_dest == -1) {
					index_dest = eids.size();
					eids.add(dst);
				}
				final EID src = bs.getSource();
				int index_src = eids.indexOf(src);
				if (index_src == -1) {
					index_src = eids.size();
					eids.add(src);
				}
				final int cts = bs.timeStamp();
				final int cts_seqn = bs.timeStampSeqNumber();
				len += SDNV.length(index_src) + SDNV.length(index_dest) +
					   SDNV.length(cts) + SDNV.length(cts_seqn) + 1;
				this.responses[i] = new BundleEntry(
							(byte)0x00, 
							index_src, 
							index_src,
							cts, 
							cts_seqn
				);
			}
			
			this.tlen = len;
		}

		@Override
		protected void serializeTLV(ByteBuffer buffer) {
			final int respos_count = responses.length;
			
			SDNV.encodeInt(buffer, respos_count);
			for (int i = 0; i < respos_count; i++)
				responses[i].serialize(buffer);
		}

		@Override
		protected int getDataLength() {
			return tlen;
		}
		
		public BundleSpec[] getResponses() {
			final int len = responses.length;
			final BundleSpec[] resp = new BundleSpec[len];
			for (int i = 0; i < len; i++)
				resp[i] = new BundleSpec(
						eids.get(responses[i].src),
						eids.get(responses[i].dest),
						responses[i].b_flag
				);
				
			return resp;
		}
	}
	
	private ProphetTLV(byte type) {
		super(type);
	}
}
