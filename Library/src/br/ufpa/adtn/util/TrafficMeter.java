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

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class TrafficMeter {
	private int bRecv;
	private int bSent;
	
	public TrafficMeter() {
		bRecv = 0;
		bSent = 0;
	}

	public OutputStream wrap(OutputStream out) {
		return new OStream(out);
	}
	
	public InputStream wrap(InputStream in) {
		return new IStream(in);
	}
	
	private synchronized void incReceived(int count) {
		bRecv += count;
	}
	
	private synchronized void incSent(int count) {
		bSent += count;
	}
	
	public int getTotalReceived() {
		return bRecv;
	}
	
	public int getTotalSent() {
		return bSent;
	}
	
	public int getTotal() {
		return bSent + bRecv;
	}
	
	
	private class IStream extends FilterInputStream {
		
		private IStream(InputStream in) {
			super(in);
		}
		
		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			final int readed = in.read(b, off, len);
			incReceived(readed);
			return readed;
		}

		@Override
		public int read() throws IOException {
			final int i = in.read();
			incReceived(1);
			return i;
		}
	}
	
	
	private class OStream extends FilterOutputStream {
		
		private OStream(OutputStream out) {
			super(out);
		}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			out.write(b, off, len);
			incSent(len);
		}

		@Override
		public void write(int b) throws IOException {
			out.write(b);
			incSent(1);
		}
	}
}
