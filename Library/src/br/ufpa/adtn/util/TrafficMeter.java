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
			final int readed = super.read(b, off, len);
			incReceived(readed);
			return readed;
		}

		@Override
		public int read() throws IOException {
			final int i = super.read();
			incReceived(1);
			return i;
		}
	}
	
	
	private class OStream extends FilterOutputStream {
		
		private OStream(OutputStream out) {
			super(out);
			this.out = out;
		}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			super.write(b, off, len);
			incSent(len);
		}

		@Override
		public void write(int b) throws IOException {
			super.write(b);
			incSent(1);
		}
	}
}
