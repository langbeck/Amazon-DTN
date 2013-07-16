package br.ufpa.dtn.tests;

import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.routing.dlife.SocialInformation;
import br.ufpa.adtn.routing.dlife.SocialInformation.NeighborWeight;
import br.ufpa.adtn.util.Logger;


public class DLifeTest {
	
	public static void main(String[] args) throws Exception {
		Logger.setLogHandler(null);
		final int contact = 60 * 30;
		final int dslen = 24;

		final Social s1 = new Social(dslen, 3600, 0.8f);
		final Social s2 = new Social(dslen, 3600, 0.8f);

		final EID e1 = EID.get("dtn://douglas.dtn");
		final EID e2 = EID.get("dtn://dorian.dtn");
		
		final int silence_start = 40;
		for (int i = 1; i < dslen * 20; i++) {
			boolean inSilence = i / dslen >= silence_start;
			System.err.println("In silence: " + inSilence);
			
			if (!inSilence)
				s1.updateTCT(e2, contact);
			
			s1.setNeighborTECDi(e2, s2.getTECDi());

			if (!inSilence)
				s2.updateTCT(e1, contact);
			
			s2.setNeighborTECDi(e1, s1.getTECDi());

			s1.setCurrentDS(i);
			s2.setCurrentDS(i);
			
			s1.update();
			s2.update();
			
			
			System.err.printf("[ Cycle %2d / %2d ]\n", (i / dslen) + 1, (i % dslen));
			System.err.printf("TECDi: %.3f\n", s1.getTECDi());
			
			final NeighborWeight[] weights = s1.getSampleWeights();
			if (weights.length != 0)
				System.err.printf("TECD:  %.3f\n", weights[0].getWeight());
			
			System.err.println();
		}
	}
}

class Social extends SocialInformation {
	private int currentDS;

	public Social(int dslen, int dsdur, float dumpingFactor) {
		super(dslen, dsdur, dumpingFactor);
		currentDS = 0;
	}
	
	public void setCurrentDS(int ds) {
		currentDS = ds;
	}
	
	@Override
	public int getCurrentDS() {
		return currentDS;
	}
}
