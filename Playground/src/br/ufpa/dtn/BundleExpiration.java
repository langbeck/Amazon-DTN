package br.ufpa.dtn;

import java.io.FileInputStream;
import java.io.InputStreamReader;

import br.ufpa.adtn.bundle.BundleInfo;
import br.ufpa.adtn.core.BPAgent;
import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.core.configuration.SimulationConfiguration;

public class BundleExpiration {
	
	public static void main(String[] args) throws Exception {
		BPAgent.init(new SimulationConfiguration(new InputStreamReader(new FileInputStream("contact.conf"))));
		BundleInfo info = BundleInfo.create(EID.NULL, EID.NULL);
		
		System.err.println(info.getSecondsToExpiration());
		Thread.sleep(1000L);
		System.err.println(info.getSecondsToExpiration());
		Thread.sleep(1000L);
		System.err.println(info.getSecondsToExpiration());
		Thread.sleep(1000L);
		System.err.println(info.getSecondsToExpiration());
		Thread.sleep(1000L);
		System.err.println(info.getSecondsToExpiration());
		Thread.sleep(1000L);
		System.err.println(info.getSecondsToExpiration());
	}
}
