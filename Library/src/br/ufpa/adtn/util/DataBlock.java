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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import br.ufpa.adtn.core.SerializableFixedObject;

public abstract class DataBlock implements Closeable {
	
	public static DataBlock join(ByteBuffer[] segments) {
		return new MultiSegmentBlock(segments);
	}
	
	public static DataBlock wrap(byte[] data, int offset, int length) {
		return new ByteBufferBlock(ByteBuffer.wrap(data, offset, length));
	}
	
	public static DataBlock wrap(byte[] data) {
		return new ByteBufferBlock(ByteBuffer.wrap(data));
	}
	
	public static DataBlock wrap(SerializableFixedObject data) {
		return new FixedObjectBlock(data);
	}

	public static DataBlock wrap(ByteBuffer data) {
		return new ByteBufferBlock(data.duplicate());
	}
	
	public static DataBlock open(File file) throws IOException {
		return new FileBlock(file);
	}
	
	
	private DataBlock() { }
	

	public void copy(OutputStream out) throws IOException {
		final byte[] data = new byte[0x1000];
		final InputStream in = open();
		try {
			for (int readed = 0; (readed = in.read(data, 0, data.length)) != -1; )
				out.write(data, 0, readed);
		} finally {
			in.close();
		}
	}
	
	public void copy(ByteBuffer buffer) throws IOException {
		if (getLength() > buffer.remaining())
			throw new BufferOverflowException();
		
		if (buffer.hasArray()) {
			final InputStream in = open();
			try {
				final byte[] data = buffer.array();
				int pos = buffer.arrayOffset();
				int remaining = getLength();
				int readed = 0;
				
				while (readed > 0 && (readed = in.read(data, pos, remaining)) != -1) {
					buffer.put(data, 0, readed);
					remaining -= readed;
					pos += readed;
				}
			} finally {
				in.close();
			}
		} else {
			final byte[] data = new byte[0x1000];
			final InputStream in = open();
			try {
				for (int readed = 0; (readed = in.read(data, 0, data.length)) != -1; )
					buffer.put(data, 0, readed);
			} finally {
				in.close();
			}
		}
	}
	
	public ByteBuffer read() throws IOException {
		final byte[] data = new byte[getLength()];
		final InputStream in = open();
		try {
			for (int readed = 0, pos = 0; (readed = in.read(data, pos, data.length - pos)) != -1; pos += readed);
			return ByteBuffer.wrap(data);
		} finally {
			in.close();
		}
	}
	
	@Override
	public void close() throws IOException { }
	
	public abstract InputStream open() throws IOException;
	public abstract int getLength();
	
	
	private static class FileBlock extends DataBlock {
		private final RandomAccessFile rFile;
		private final FileChannel channel;
		private final FileLock lock;
		private final int length;
		
		private FileBlock(File file) throws IOException {
			this.rFile = new RandomAccessFile(file, "r");
			this.channel = rFile.getChannel();
			this.lock = channel.lock();
			
			try {
				final long length = rFile.length();
				if (length > Integer.MAX_VALUE)
					throw new IOException("File is too big");
				
				this.length = (int) length;
			} finally {
				lock.release();
			}
		}

		@Override
		public InputStream open() throws IOException {
			return new FileChannelInputStream(channel);
		}

		@Override
		public int getLength() {
			return length;
		}

		@Override
		public void close() throws IOException {
			try {
				rFile.close();
			} finally {
				if (lock.isValid())
					lock.release();
			}
		}
	}
	
	
	private static class FileChannelInputStream extends InputStream {
		private final FileChannel channel;
		private long position;
		
		private FileChannelInputStream(FileChannel channel) {
			this.channel = channel;
			this.position = 0;
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			final int readed = channel.read(ByteBuffer.wrap(b, off, len), position);
			return readed;
		}

		@Override
		public int read() throws IOException {
			final byte[] buf = new byte[1];
			return read(buf, 0, 1) == -1 ? -1 : buf[0] & 0xFF;
		}
	}

	
	private static class ByteBufferBlock extends DataBlock {
		private final ByteBuffer data;
		
		private ByteBufferBlock(ByteBuffer data) {
			this.data = data;
		}
		
		@Override
		public void copy(OutputStream out) throws IOException {
			if (data.hasArray()) {
				out.write(
						data.array(),
						data.arrayOffset(),
						data.limit()
				);
			} else {
				super.copy(out);
			}
		}
		
		@Override
		public void copy(ByteBuffer buffer) throws IOException {
			buffer.put(data);
		}

		@Override
		public int getLength() {
			return data.limit();
		}
		
		@Override
		public ByteBuffer read() throws IOException {
			return data.asReadOnlyBuffer();
		}

		@Override
		public InputStream open() {
			return new ByteBufferInputStream(data.duplicate());
		}
	}
	
	
	private static class ByteBufferInputStream extends InputStream {
		private final ByteBuffer data;
		
		private ByteBufferInputStream(ByteBuffer data) {
			this.data = data;
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			final int min = Math.min(data.remaining(), len);
			data.get(b, off, min);
			return min;
		}

		@Override
		public int read() throws IOException { 
			return data.remaining() > 0 ? data.get() : -1;
		}
	}
	

	private static class MultiSegmentBlock extends DataBlock {
		private final ByteBuffer[] segments;
		private final int length;
		
		private MultiSegmentBlock(ByteBuffer[] segments) {
			if (segments == null)
				throw new NullPointerException();
			
			final int slen = segments.length;
			if (slen == 0)
				throw new IllegalArgumentException("No segments");
			
			long length = 0;
			for (int i = 0; i < slen; i++)
				length += segments[i].limit();

			if (length > Integer.MAX_VALUE)
				throw new IllegalArgumentException("Segments are too big");

			this.segments = segments;
			this.length = (int) length;
		}

		@Override
		public InputStream open() throws IOException {
			return new MultiSegmentInputStream(segments);
		}

		@Override
		public int getLength() {
			return length;
		}
	}
	
	
	private static class MultiSegmentInputStream extends InputStream {
		private ByteBuffer[] segments;
		private boolean finished;
		private int segment;
		
		private MultiSegmentInputStream(ByteBuffer[] segments) {
			this.segments = segments;
			this.finished = false;
			this.segment = 0;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (finished)
				return -1;
			
			final ByteBuffer seg = segments[segment];
			final int remaining = seg.remaining();
			if (len < seg.remaining()) {
				seg.get(b, off, len);
				return len;
			} else {
				seg.get(b, off, remaining);
				
				if (++segment == segments.length) {
					finished = true;
					segments = null;
					return remaining;
				}
				
				return remaining + read(b, off + remaining, len - remaining);
			}
		}
		
		@Override
		public int read() throws IOException {
			final byte[] buf = new byte[1];
			return read(buf, 0, 1) == -1 ? -1 : buf[0] & 0xFF;
		}
		
		@Override
		public void close() throws IOException {
			segments = null;
			finished = true;
		}
	}
	
	
	private static class FixedObjectBlock extends DataBlock {
		private final SerializableFixedObject data;
		
		private FixedObjectBlock(SerializableFixedObject data) {
			this.data = data;
		}
		
		@Override
		public void copy(ByteBuffer buffer) throws IOException {
			data.serialize(buffer);
		}

		@Override
		public InputStream open() throws IOException {
			final byte[] data = new byte[getLength()];
			copy(ByteBuffer.wrap(data));
			
			return new ByteArrayInputStream(data);
		}

		@Override
		public int getLength() {
			return data.getLength();
		}
	}
}
