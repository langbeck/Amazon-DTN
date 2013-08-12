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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;

public class CompressedOutputStream extends OutputStream implements CompressedStreamConstants {
	private final DataOutputStream dout;
	private final Deflater def;
	private final byte[] buf;
	private boolean closed;
	
	public CompressedOutputStream(OutputStream out) throws IOException {
		this.dout = !(out instanceof DataOutputStream) ?
				new DataOutputStream(out) :
				(DataOutputStream) out;

		this.def = new Deflater(Deflater.BEST_SPEED);
		this.buf = new byte[0x100];
		this.closed = false;
		
		dout.writeShort(MAGIC);
		dout.flush();
	}
	
	private void deflate() throws IOException {
        int len = def.deflate(buf, 0, buf.length);
        if (len > 0)
            dout.write(buf, 0, len);
    }
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (closed)
			throw new IOException("End of stream");
		
		if (def.finished()) {
			dout.writeShort(NEXT);
            def.reset();
		}
		
        if ((off | len | (off + len) | (b.length - (off + len))) < 0)
            throw new IndexOutOfBoundsException();
        
        else if (len == 0)
            return;
        
        final int stride = buf.length;
        for (int i = 0; i < len; i+= stride) {
            def.setInput(b, off + i, Math.min(stride, len - i));
            while (!def.needsInput())
                deflate();
        }
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[] { (byte) (b & 0xFF) }, 0, 1);
	}
	
	@Override
	public void flush() throws IOException {
        def.finish();
        while (!def.finished())
            deflate();
        
        dout.flush();
	}
	
	@Override
	public void close() throws IOException {
		if (closed)
			throw new IOException("Already closed");
		
		try {
			if (!def.finished()) {
	            def.finish();
	            while (!def.finished())
	                deflate();
	        }
		} catch (IOException e) { }
		
		def.end();
		try {
			dout.writeShort(CLOSE);
			dout.flush();
		} catch (IOException e) { }
		
		try {
			dout.close();
		} catch (IOException e) { }
		
		closed = true;
	}
}
