package br.ufpa.dtns.util;

import java.io.OutputStream;
import java.io.PrintStream;

public class PrintStreamHooker extends PrintStream {
	private final PrintStream copyTo;

	public PrintStreamHooker(PrintStream copyTo, OutputStream out) {
		super(out);
		this.copyTo = copyTo;
	}
	
	@Override
	public void write(byte[] buf, int off, int len) {
		synchronized (copyTo) {
			copyTo.write(buf, off, len);
		}
		
		super.write(buf, off, len);
	}
	
	@Override
	public void flush() {
		synchronized (copyTo) {
			copyTo.flush();
		}
		
		super.flush();
	}
}
