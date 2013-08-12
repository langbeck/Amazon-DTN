package br.ufpa.dtn;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

import br.ufpa.adtn.util.BufferSlicer;
import br.ufpa.adtn.util.ChainOfSegments;
import br.ufpa.adtn.util.DataBlock;

public class Segments {
	
	public static void main(String[] args) throws Exception {
		final ByteBuffer buf = ByteBuffer.allocate(0x10000);
		final ChainOfSegments chain = new ChainOfSegments();
		final BufferSlicer slicer = new BufferSlicer(buf);
		
		buf.put("Dorian".getBytes());
		ByteBuffer n1 = slicer.end();
		System.err.println(n1.limit());
		
		buf.put("Conde".getBytes());
		ByteBuffer n2 = slicer.end();
		System.err.println(n2.limit());
		
		buf.put("Langbeck".getBytes());
		ByteBuffer n3 = slicer.end();
		System.err.println(n3.limit());

		chain.append(n2);
		chain.append(n1);
		chain.append(" - ".getBytes(), 0, 1);
		chain.append(n3);
		chain.append(n1);
		chain.append(" + ".getBytes(), 1, 1);
		chain.append(n3);
		chain.append(n2);
		
		final DataBlock block = DataBlock.join(chain.getSegments());
		final BufferedReader reader = new BufferedReader(new InputStreamReader(block.open()));
		System.err.println("[" + reader.readLine() + "]");
		reader.close();
	}
}
