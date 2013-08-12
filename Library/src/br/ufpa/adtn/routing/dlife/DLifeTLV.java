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
package br.ufpa.adtn.routing.dlife;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.core.SerializableObject;
import br.ufpa.adtn.routing.Message.TLVParser;
import br.ufpa.adtn.routing.TLV;
import br.ufpa.adtn.routing.TLVParsingException;
import br.ufpa.adtn.routing.dlife.DLifeUtil.BundleSpec;
import br.ufpa.adtn.routing.dlife.SocialInformation.NeighborWeight;
import br.ufpa.adtn.util.SDNV;

public abstract class DLifeTLV extends TLV {
	private static final byte HELLO			= (byte) 0x01;
	private static final byte ACK 			= (byte) 0x02;
	private static final byte SOCIAL 		= (byte) 0xA1;
	
	
	public static final TLVParser<DLifeTLV> PARSER = new TLVParser<DLifeTLV>() {
		
		@Override
		public DLifeTLV parse(ByteBuffer buffer, byte type, byte flags) throws TLVParsingException {
			switch (type) {
			case HELLO:
 				return new Hello(
 						HelloType.getType(flags),
 						EID.decode(buffer),
 						SDNV.decodeInt(buffer),
 						SDNV.decodeInt(buffer)
 				);
			
			case ACK:
				final AckType flag = AckType.getAckType(flags);
				if (flag == AckType.BREAK || flag == AckType.SOCIAL) {
					return new Ack(flag, null);
				} else {
					final byte[] data = new byte[2];
					buffer.get(data);
					
					return new Ack(flag, data);
				}
				
			case SOCIAL:
				return new Social(buffer);
				
			default:
				throw new TLVParsingException("Invalid type: " + type);
			}
		}
	};
	
	
	public static enum HelloType {
		HEL, SYN, ACK;
		
		public static HelloType getType(byte flags) {
			return HelloType.values()[flags - 1];
		}
	}
	
	
	
	public static final class Hello extends DLifeTLV {
		private final int storage;
		private final int timer;
		private final int tlen;
		private final EID eid;

		public Hello(HelloType type, EID eid, int timer, int storage) {
			super((byte) 0x01);
			
			if (type == null || eid == null)
				throw new IllegalArgumentException();
			
			this.storage = storage;
			this.timer = timer;
			this.eid = eid;
			
			int eidLength = eid.getDataLength();
			this.tlen = SDNV.length(storage) + SDNV.length(timer) +
						SDNV.length(eidLength) + eidLength;
			
			setFlags((byte) (type.ordinal() + 1));
		}
		
		public HelloType getHelloType() {
			return HelloType.getType(getFlags());
		}

		@Override
		protected void serializeTLV(ByteBuffer buffer) {
			eid.encode(buffer);
			SDNV.encodeInt(buffer, timer);
			SDNV.encodeInt(buffer, storage);
		}

		@Override
		protected int getDataLength() {
			return tlen;
		}
		
		public EID getEID() {
			return eid;
		}
		
		public int getStorage() {
			return storage;
		}
		
		public int getTimer() {
			return timer;
		}
	}
	
	
	public static enum AckType {
		BREAK, SOCIAL, EID_DISCREPANCY, EID_UNKNOW;
		
		public static AckType getAckType(byte flags) {
			switch (flags) {
			case (byte) 0x00:
				return BREAK;
			
			case (byte) 0x01:
				return SOCIAL;
			
			case (byte) 0x02:
				return EID_DISCREPANCY;
			
			case (byte) 0x03:
				return EID_UNKNOW;

			default:
				throw new RuntimeException("Undefined Ack Type at TLV Parse: " + flags);
			}
		}
	}
	
	public static final class Ack extends DLifeTLV {
		private final byte[] data;

		public Ack(AckType type, byte[] data) {
			super((byte) 0x02);
			
			if (type == null)
				throw new IllegalArgumentException("AckType can not be null");
			
			if (type == AckType.EID_DISCREPANCY || type == AckType.EID_UNKNOW) {
				if (data == null)
					throw new IllegalArgumentException("Data can not be null for this AckType");
				
			} else if (data != null) {
				throw new IllegalArgumentException("Data must be null for this AckType");
			}
			
			this.data = data;
			setFlags((byte) type.ordinal());
		}

		@Override
		protected void serializeTLV(ByteBuffer buffer) {
			if (data != null)
				buffer.put(data);
		}

		@Override
		protected int getDataLength() {
			return data == null ? 0 : data.length;
		}
	}
	
	
	public static final class Social extends DLifeTLV {
		private static class WeightEntry implements SerializableObject {
			private final float weight;
			private final int sid;
			
			public WeightEntry(int sid, float weight) {
				this.weight = weight;
				this.sid = sid;
			}

			@Override
			public void serialize(ByteBuffer buffer) {
				SDNV.encodeInt(buffer, sid);
				buffer.putFloat(weight);
			}
		}
		
		private static class BundleEntry implements SerializableObject {
			private final int bid;
			private final int sid;
			
			public BundleEntry(int bid, int sid) {
				this.bid = bid;
				this.sid = sid;
			}

			@Override
			public void serialize(ByteBuffer buffer) {
				SDNV.encodeInt(buffer, bid);
				SDNV.encodeInt(buffer, sid);
			}
		}
		
		private final BundleEntry[] carried;
		private final BundleEntry[] acked;
		private final WeightEntry[] swni;
		private final EID[] eids;
		
		private final float importance;
		private final int tlen;

		public Social(ByteBuffer buffer) {
			super((byte) 0xA1);

			final int spos = buffer.position();
			this.importance = buffer.getFloat();
			
			final int eids_len = SDNV.decodeInt(buffer);
			this.eids = new EID[eids_len];
			for (int i = 0; i < eids_len; i++)
				eids[i] = EID.decode(buffer);
			
			final int swni_len = SDNV.decodeInt(buffer);
			this.swni = new WeightEntry[swni_len];
			for (int i = 0; i < swni_len; i++)
				swni[i] = new WeightEntry(
						SDNV.decodeInt(buffer),
						buffer.getFloat()
				);
			
			final int carried_len = SDNV.decodeInt(buffer);
			this.carried = new BundleEntry[carried_len];
			for (int i = 0; i < carried_len; i++)
				carried[i] = new BundleEntry(
						SDNV.decodeInt(buffer),
						SDNV.decodeInt(buffer)
				);

			final int acked_len = SDNV.decodeInt(buffer);
			this.acked = new BundleEntry[acked_len];
			for (int i = 0; i < acked_len; i++)
				acked[i] = new BundleEntry(
						SDNV.decodeInt(buffer),
						SDNV.decodeInt(buffer)
				);
			
			this.tlen = buffer.position() - spos;
		}
		
		public Social( 
				NeighborWeight[] swni,
				BundleSpec[] carried,
				BundleSpec[] acked,
				float importance
		) {
			super((byte) 0xA1);
			
			final List<EID> eids = new ArrayList<EID>();
			
			this.importance = importance;
			
			final int carried_len = carried.length;
			final int acked_len = acked.length;
			final int swni_len = swni.length;
			
			int len = SDNV.length(carried_len) +    // CarriedBundleCount
					  SDNV.length(acked_len)   +    // AckedBundleCount
					  SDNV.length(swni_len)    +    // SWNIStringCount
					  4;  						    // ImportanceValue

			this.swni = new WeightEntry[swni_len];
			for (int i = 0; i < swni_len; i++) {
				final NeighborWeight nWeight = swni[i];
				final EID eid = nWeight.getEID();
				int index = eids.indexOf(eid);
				if (index == -1) {
					index = eids.size();
					eids.add(eid);
				}
				
				len += SDNV.length(index) + 4;
				this.swni[i] = new WeightEntry(index, nWeight.getWeight());
			}

			this.carried = new BundleEntry[carried_len];
			for (int i = 0; i < carried_len; i++) {
				final BundleSpec bs = carried[i];
				final EID dst = bs.getDestination();
				int index = eids.indexOf(dst);
				if (index == -1) {
					index = eids.size();
					eids.add(dst);
				}
				
				len += SDNV.length(index) + SDNV.length(bs.getId());
				this.carried[i] = new BundleEntry(bs.getId(), index);
			}

			this.acked = new BundleEntry[acked_len];
			for (int i = 0; i < acked_len; i++) {
				final BundleSpec bs = acked[i];
				final EID dst = bs.getDestination();
				int index = eids.indexOf(dst);
				if (index == -1) {
					index = eids.size();
					eids.add(dst);
				}
				
				len += SDNV.length(index) + SDNV.length(bs.getId());
				this.acked[i] = new BundleEntry(index, bs.getId());
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
			buffer.putFloat(importance);

			final int carried_len = carried.length;
			final int acked_len = acked.length;
			final int swni_len = swni.length;
			final int eids_len = eids.length;
			
			SDNV.encodeInt(buffer, eids_len);
			for (int i = 0; i < eids_len; i++)
				eids[i].encode(buffer);

			SDNV.encodeInt(buffer, swni_len);
			for (int i = 0; i < swni_len; i++)
				swni[i].serialize(buffer);

			SDNV.encodeInt(buffer, carried_len);
			for (int i = 0; i < carried_len; i++)
				carried[i].serialize(buffer);

			SDNV.encodeInt(buffer, acked_len);
			for (int i = 0; i < carried_len; i++)
				acked[i].serialize(buffer);
		}

		@Override
		protected int getDataLength() {
			return tlen;
		}
		
		public BundleSpec[] getCarried() {
			final BundleSpec[] specs = new BundleSpec[carried.length];
			for (int i = 0; i < carried.length; i++) {
				final BundleEntry entry = carried[i];
				specs[i] = new BundleSpec(eids[entry.sid], entry.bid);
			}
			
			return specs;
		}

		public BundleSpec[] getAcked() {
			final BundleSpec[] specs = new BundleSpec[acked.length];
			for (int i = 0; i < acked.length; i++) {
				final BundleEntry entry = acked[i];
				specs[i] = new BundleSpec(eids[entry.sid], entry.bid);
			}
			
			return specs;
		}

		public NeighborWeight[] getSWNIs() {
			final NeighborWeight[] weights = new NeighborWeight[swni.length];
			for (int i = 0; i < swni.length; i++) {
				final WeightEntry entry = swni[i];
				weights[i] = new NeighborWeight(eids[entry.sid], entry.weight);
			}
			
			return weights;
		}

		public float getImportance() {
			return importance;
		}
	}


	private DLifeTLV(byte type) {
		super(type);
	}
}
