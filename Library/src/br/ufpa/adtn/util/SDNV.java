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

public final class SDNV {
	private SDNV() { }

	public static void encodeShort(ByteBuffer buf, short value) {
		if (value < 0 || value > 0x3FFF) {
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0x7F) {
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0x3FFF) {
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else {
			throw new InternalError();
		}
	}
	
	public static int writeShort(ByteBuffer buf, short value) {
		if (value < 0 || value > 0x3FFF) {
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 3;
		} else if (value <= 0x7F) {
			buf.put((byte) (value & 0x7F));
			return 1;
		} else if (value <= 0x3FFF) {
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 2;
		} else {
			throw new InternalError();
		}
	}
	
	public static void encodeInt(ByteBuffer buf, int value) {
		if (value < 0 || value > 0xFFFFFFF)	{
			buf.put((byte) ((value >> 0x1C) | 0x80));
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0x7F) {
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0x3FFF) {
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0x1FFFFF) {
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0xFFFFFFF) {
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else {
			throw new InternalError();
		}
	}

	public static int writeInt(ByteBuffer buf, int value) {
		if (value < 0 || value > 0xFFFFFFF)	{
			buf.put((byte) ((value >> 0x1C) | 0x80));
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 5;
		} else if (value <= 0x7F) {
			buf.put((byte) (value & 0x7F));
			return 1;
		} else if (value <= 0x3FFF) {
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 2;
		} else if (value <= 0x1FFFFF) {
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 3;
		} else if (value <= 0xFFFFFFF) {
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 4;
		} else {
			throw new InternalError();
		}
	}

	public static void encodeLong(ByteBuffer buf, long value) {
		if (value < 0L) {
			buf.put((byte) ((value >> 0x3F) | 0x80));
			buf.put((byte) ((value >> 0x38) | 0x80));
			buf.put((byte) ((value >> 0x31) | 0x80));
			buf.put((byte) ((value >> 0x2A) | 0x80));
			buf.put((byte) ((value >> 0x23) | 0x80));
			buf.put((byte) ((value >> 0x1C) | 0x80));
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0x7FL) {
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0x3FFFL) {
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0x1FFFFFL) {
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0xFFFFFFFL) {
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0x7FFFFFFFFL) {
			buf.put((byte) ((value >> 0x1C) | 0x80));
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0x3FFFFFFFFFFL) {
			buf.put((byte) ((value >> 0x23) | 0x80));
			buf.put((byte) ((value >> 0x1C) | 0x80));
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0x1FFFFFFFFFFFFL) {
			buf.put((byte) ((value >> 0x2A) | 0x80));
			buf.put((byte) ((value >> 0x23) | 0x80));
			buf.put((byte) ((value >> 0x1C) | 0x80));
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0xFFFFFFFFFFFFFFL) {
			buf.put((byte) ((value >> 0x31) | 0x80));
			buf.put((byte) ((value >> 0x2A) | 0x80));
			buf.put((byte) ((value >> 0x23) | 0x80));
			buf.put((byte) ((value >> 0x1C) | 0x80));
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value <= 0x7FFFFFFFFFFFFFFFL) {
			buf.put((byte) ((value >> 0x38) | 0x80));
			buf.put((byte) ((value >> 0x31) | 0x80));
			buf.put((byte) ((value >> 0x2A) | 0x80));
			buf.put((byte) ((value >> 0x23) | 0x80));
			buf.put((byte) ((value >> 0x1C) | 0x80));
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else {
			throw new InternalError();
		}
	}

	public static int writeLong(ByteBuffer buf, long value) {
		if (value < 0L) {
			buf.put((byte) ((value >> 0x3F) | 0x80));
			buf.put((byte) ((value >> 0x38) | 0x80));
			buf.put((byte) ((value >> 0x31) | 0x80));
			buf.put((byte) ((value >> 0x2A) | 0x80));
			buf.put((byte) ((value >> 0x23) | 0x80));
			buf.put((byte) ((value >> 0x1C) | 0x80));
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 10;
		} else if (value <= 0x7FL) {
			buf.put((byte) (value & 0x7F));
			return 1;
		} else if (value <= 0x3FFFL) {
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 2;
		} else if (value <= 0x1FFFFFL) {
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 3;
		} else if (value <= 0xFFFFFFFL) {
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 4;
		} else if (value <= 0x7FFFFFFFFL) {
			buf.put((byte) ((value >> 0x1C) | 0x80));
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 5;
		} else if (value <= 0x3FFFFFFFFFFL) {
			buf.put((byte) ((value >> 0x23) | 0x80));
			buf.put((byte) ((value >> 0x1C) | 0x80));
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 6;
		} else if (value <= 0x1FFFFFFFFFFFFL) {
			buf.put((byte) ((value >> 0x2A) | 0x80));
			buf.put((byte) ((value >> 0x23) | 0x80));
			buf.put((byte) ((value >> 0x1C) | 0x80));
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 7;
		} else if (value <= 0xFFFFFFFFFFFFFFL) {
			buf.put((byte) ((value >> 0x31) | 0x80));
			buf.put((byte) ((value >> 0x2A) | 0x80));
			buf.put((byte) ((value >> 0x23) | 0x80));
			buf.put((byte) ((value >> 0x1C) | 0x80));
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 8;
		} else if (value <= 0x7FFFFFFFFFFFFFFFL) {
			buf.put((byte) ((value >> 0x38) | 0x80));
			buf.put((byte) ((value >> 0x31) | 0x80));
			buf.put((byte) ((value >> 0x2A) | 0x80));
			buf.put((byte) ((value >> 0x23) | 0x80));
			buf.put((byte) ((value >> 0x1C) | 0x80));
			buf.put((byte) ((value >> 0x15) | 0x80));
			buf.put((byte) ((value >> 0x0E) | 0x80));
			buf.put((byte) ((value >> 0x07) | 0x80));
			buf.put((byte) (value & 0x7F));
			return 9;
		} else {
			throw new InternalError();
		}
	}

	public static short decodeShort(ByteBuffer buf) {
		byte b;
		if ((b = buf.get()) >= 0)
			return b;

		int value = b & 0x7F;
		if ((b = buf.get()) >= 0)
			return (short) (value << 7 | b);

		if ((b = buf.get()) >= 0 && value <= 0x1FF)
			return (short) (value << 7 | b);

		throw new RuntimeException();
	}

	public static int decodeInt(ByteBuffer buf) {
		byte b;
		if ((b = buf.get()) >= 0)
			return b;
		
		int value = b & 0x7F;
		if ((b = buf.get()) >= 0)
			return value << 7 | b;

		value = (value << 7) | (b & 0x7F);
		if ((b = buf.get()) >= 0)
			return value << 7 | b;

		value = (value << 7) | (b & 0x7F);
		if ((b = buf.get()) >= 0)
			return value << 7 | b;

		value = (value << 7) | (b & 0x7F);
		if ((b = buf.get()) >= 0 && value <= 0x1FFFFFF)
			return value << 7 | b;
		
		throw new RuntimeException();
	}
	
	public static long decodeLong(ByteBuffer buf) {
		byte b;
		if ((b = buf.get()) >= 0)
			return b;
		
		long value = b & 0x7F;
		if ((b = buf.get()) >= 0)
			return value << 7 | b;

		value = (value << 7) | (b & 0x7F);
		if ((b = buf.get()) >= 0)
			return value << 7 | b;

		value = (value << 7) | (b & 0x7F);
		if ((b = buf.get()) >= 0)
			return value << 7 | b;

		value = (value << 7) | (b & 0x7F);
		if ((b = buf.get()) >= 0)
			return value << 7 | b;

		value = (value << 7) | (b & 0x7F);
		if ((b = buf.get()) >= 0)
			return value << 7 | b;

		value = (value << 7) | (b & 0x7F);
		if ((b = buf.get()) >= 0)
			return value << 7 | b;

		value = (value << 7) | (b & 0x7F);
		if ((b = buf.get()) >= 0)
			return value << 7 | b;

		value = (value << 7) | (b & 0x7F);
		if ((b = buf.get()) >= 0)
			return value << 7 | b;

		value = (value << 7) | (b & 0x7F);
		if ((b = buf.get()) >= 0 && value <= 0x1FFFFFFFFFFFFFFL)
			return value << 7 | b;
		
		throw new RuntimeException();
	}

	public static int length(int value) {
		if (value < 0 || value > 0xFFFFFFF)	return 5;
		if (value <= 0x7F)					return 1;
		if (value <= 0x3FFF)				return 2;
		if (value <= 0x1FFFFF)				return 3;
		if (value <= 0xFFFFFFF)				return 4;
		
		throw new InternalError();
	}
	
	public static int length(long value) {
		if (value < 0)						return 10;
		if (value <= 0x7FL)					return 1;
		if (value <= 0x3FFFL)				return 2;
		if (value <= 0x1FFFFFL)				return 3;
		if (value <= 0xFFFFFFFL)			return 4;
		if (value <= 0x7FFFFFFFFL)			return 5;
		if (value <= 0x3FFFFFFFFFFL)		return 6;
		if (value <= 0x1FFFFFFFFFFFFL)		return 7;
		if (value <= 0xFFFFFFFFFFFFFFL)		return 8;
		if (value <= 0x7FFFFFFFFFFFFFFFL)	return 9;
		
		throw new InternalError();
	}
}
