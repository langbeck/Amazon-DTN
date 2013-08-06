package br.ufpa.dtn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.Random;

import br.ufpa.adtn.util.CompressedInputStream;
import br.ufpa.adtn.util.CompressedOutputStream;
import br.ufpa.adtn.util.TrafficMeter;

public class Compression {
	static final short MAGIC = (short) 0xBEEF;
	static final short CLOSE = (short) 0xDEAD;
	static final short NEXT = (short) 0x1234;
	
	public static void main(String[] args) throws Exception {
		final String sdata = "Hello World / Hello World / Hello World / Hello World / Hello World / Hello World / Hello World / Hello World / Hello World / Hello World";
		
		final byte[] raw_data = new byte[0x1000];
		final Random r = new Random(0);
		for (int i = 0; i < raw_data.length; i++)
			raw_data[i] = (byte) r.nextInt(0xFF);
		
		final TrafficMeter meter0 = new TrafficMeter();
		final TrafficMeter meter1 = new TrafficMeter();
		
		final PipedOutputStream pout = new PipedOutputStream();
		final PipedInputStream pin = new PipedInputStream(pout);
		final BufferedOutputStream bout = new BufferedOutputStream(pout);
		final BufferedInputStream bin = new BufferedInputStream(pin);
		
		final CompressedOutputStream cout = new CompressedOutputStream(bout);
		cout.flush();
		
		final CompressedInputStream cin = new CompressedInputStream(bin);
		
		final int length = 0x10;
		final byte[] wbuf = new byte[length];
		final byte[] rbuf = new byte[length];
		for (int i = 0; i < 10; i++) {
			r.nextBytes(wbuf);
			cout.write(wbuf);
			cout.flush();
			
			int readed = cin.read(rbuf);
			if (readed != length)
				throw new IOException("Failure");
			
			System.err.println(Arrays.equals(wbuf, rbuf));
		}
	}
}
