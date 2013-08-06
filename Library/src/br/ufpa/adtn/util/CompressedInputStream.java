package br.ufpa.adtn.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

public class CompressedInputStream extends InputStream implements CompressedStreamConstants {
	private final DataInputStream din;
	private final Inflater inf;
	private final byte[] buf;
	private boolean closed;
	private int blen;
	
	public CompressedInputStream(InputStream in) throws IOException {
		this.din = !(in instanceof DataInputStream) ?
				new DataInputStream(in) :
				(DataInputStream) in;

		this.buf = new byte[0x100];
		this.inf = new Inflater();
		this.closed = false;
		this.blen = 0;
		
		if (din.readShort() != MAGIC)
			throw new IOException("Wrong MAGIC");
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (closed)
			throw new IOException("Closed");
		
        if (off < 0 || len < 0 || len > b.length - off)
            throw new IndexOutOfBoundsException();
        
        else if (len == 0)
            return 0;
        
        try {
            int n;
            while ((n = inf.inflate(b, off, len)) == 0) {
            	if (inf.needsDictionary()) {
            		blen = -1;
            		throw new IOException("Stream error: Dictionary needed to Inflater");
            	}
            	
                if (inf.finished()) {
        			final int r = inf.getRemaining();
        			switch (r) {
        			case 0:
        				if (nextBlock(din.readShort()))
        					continue;
        				
        				return -1;
        				
        			case 1:
        				final int ch2 = din.read();
        				if (ch2 < 0) {
        					blen = -1;
        					throw new IOException("Unexpected EOS");
    					}
        				
        				if (nextBlock((short) ((buf[blen - 1] << 8) + (ch2 << 0))))
        					continue;
        				
        				return -1;
        				
    				default:
    					final int ich1 = buf[blen - r + 1] & 0xFF;
    					final int ich0 = buf[blen - r] & 0xFF;
        				switch ((short) ((ich0 << 8) + (ich1 << 0))) {
        				case NEXT:
	        				inf.reset();
	        				inf.setInput(buf, blen - r + 2, r - 2);
        	            	continue;
        	            	
        				case CLOSE:
        					close();
        					return -1;
        					
        				default:
        					throw new IOException("Wrong block marker");
        				}
        			}
                }
                    
                
                if (inf.needsInput()) {
                	blen = din.read(buf, 0, buf.length);
                	if (blen == -1)
                		return -1;
                	
                	inf.setInput(buf, 0, blen);
                }
            }
            return n;
        } catch (DataFormatException e) {
            String s = e.getMessage();
            throw new ZipException(s != null ? s : "Invalid ZLIB data format");
        }
	}
	
	@Override
	public int available() throws IOException {
		return blen == -1 ? 0 : 1;
	}
	
	private boolean nextBlock(short header) throws IOException {
		switch (header) {
		case NEXT:
			inf.reset();
			blen = din.read(buf, 0, buf.length);
			if (blen == -1) {
				System.err.println("-----------");
        		return false;
			}
        	
        	inf.setInput(buf, 0, blen);
        	return true;
        	
		case CLOSE:
			close();
			return false;
			
		default:
			throw new IOException("Wrong block marker");
		}
	}
	
	@Override
	public int read() throws IOException {
		final byte[] buf = new byte[1];
		return read(buf, 0, 1) == -1 ? -1 : buf[0] & 0xFF;
	}
	
	@Override
	public boolean markSupported() {
		return false;
	}
	
	@Override
	public void close() throws IOException {
		closed = true;
		blen = -1;
		inf.end();
		
		din.close();
	}
}
