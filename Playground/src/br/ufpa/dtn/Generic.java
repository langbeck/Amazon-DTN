package br.ufpa.dtn;

import java.nio.ByteBuffer;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.bundle.BundleInfo;
import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.routing.Message;
import br.ufpa.adtn.routing.dlife.DLifeTLV;
import br.ufpa.adtn.routing.dlife.DLifeTLV.Ack;
import br.ufpa.adtn.routing.dlife.DLifeTLV.AckType;
import br.ufpa.adtn.routing.dlife.DLifeTLV.Hello;
import br.ufpa.adtn.routing.dlife.DLifeTLV.HelloType;
import br.ufpa.adtn.util.ChainOfSegments;
import br.ufpa.adtn.util.DataBlock;

public class Generic {
	
	public static void main(String[] args) throws Exception {
		Message<DLifeTLV> message = Message.create(
				(byte) 0,
				(byte) 0,
				(short) 1,
				(short) 0,
				0
		);
		message.add(new Hello(
				HelloType.SYN,
				EID.NULL,
				0,
				0
		));
		message.add(new Ack(AckType.BREAK, null));
		
		
		final DataBlock pBlock = DataBlock.wrap(message);
		
		final Bundle bundle = new Bundle(BundleInfo.create(
				EID.get("dst://destination"),
				EID.get("src://source"),
				EID.get("rto://reportto"),
				EID.get("cus://custodian")
		), pBlock);
		final ChainOfSegments chain = new ChainOfSegments();
		bundle.serialize(chain, ByteBuffer.allocate(0x1000));
		
		final ByteBuffer[] segments = chain.getSegments();
		final DataBlock nBlock = DataBlock.join(segments);
		final ByteBuffer nBuffer = nBlock.read();
		final Bundle nBundle = new Bundle(nBuffer);
		final Message<DLifeTLV> msg = Message.unpack(
				nBundle.getPayload().read(),
				DLifeTLV.PARSER
		);

		System.err.println("Original bundle length: " + bundle.getDataLength());
		System.err.println("Bundle block length: " + nBlock.getLength());
		System.err.println("Bundle length: " + nBlock.getLength());
		System.err.println("Message Length: " + msg.getLength());
		System.err.println("Segments used: " + segments.length);
		System.err.printf("Overhead: %.1f%%\n", (nBlock.getLength() * 100.0 / msg.getLength()) - 100);
		System.err.println(nBundle.getInfo().getDestination());
		System.err.println(nBundle.getInfo().getSource());
		System.err.println(nBundle.getInfo().getReportTo());
		System.err.println(nBundle.getInfo().getCustodian());
		System.err.println(nBundle);
		System.err.println(msg);
		System.err.println();

		System.err.printf("BundleInfo equals: %s\n", nBundle.getInfo().equals(bundle.getInfo()));
		System.err.printf("BundleInfo equals: %s\n", bundle.getInfo().equals(nBundle.getInfo()));
		System.err.printf("BundleInfo: %s\n", bundle.equals(nBundle));
		System.err.printf("BundleInfo: %s\n", nBundle.equals(bundle));
		System.err.printf("Unique ID: [ID:%016x]\n", nBundle.getUniqueID());
		System.err.printf("Unique ID: [ID:%016x]\n", bundle.getUniqueID());
		System.err.printf("Hash Code: [%08x]\n", nBundle.hashCode());
		System.err.printf("Hash Code: [%08x]\n", bundle.hashCode());
	}
}
