package br.ufpa.adtn.core;

/**
 * This class is only used as a bridge between the components of Bundle
 * Protocol and the {@link BPAgent}. Only the low-level components (already
 * present in the architecture) will create connectors, and will do so only
 * during the initialization phase.
 * 
 * @author Dórian Langbeck
 *
 */
public abstract class BPAgentConnector {
	
	protected BPAgentConnector() throws IllegalAccessError {
		BPAgent.checkConnectorSyncAndState();
	}
}
