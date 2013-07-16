package br.ufpa.dtn;

import br.ufpa.adtn.util.ContactRaceConditionHelper;

public class Sync {
	
	public static void main(String[] args) {
		final ContactRaceConditionHelper crc1 = new ContactRaceConditionHelper("dlife://dorian.node", 1000);
		final ContactRaceConditionHelper crc2 = new ContactRaceConditionHelper("dlife://langbeck.node", 1000);
		System.err.println(crc1.update("dtn://langbeck.node"));
		System.err.println(crc2.update("dtn://dorian.node"));
		
		System.exit(0);
	}
}
