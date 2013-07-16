package br.ufpa.adtn.routing;

import java.nio.ByteBuffer;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.BundleRouter;
import br.ufpa.adtn.core.LinkConnection;
import br.ufpa.adtn.core.ParsingException;
import br.ufpa.adtn.routing.Message.TLVParser;
import br.ufpa.adtn.routing.MessageConnection.Provider;
import br.ufpa.adtn.util.Logger;

public abstract class MessageLinkConnection<MLC extends MessageLinkConnection<MLC, R, C, T>, R extends BundleRouter<R, MLC>, C extends MessageConnection<MLC, T, C>, T extends TLV> extends LinkConnection<MLC, R> implements MessageConnection.Connector<MLC, T, C> {
	private static final Logger LOGGER = new Logger("MessageLinkConnection");
	private final Provider<MLC, T, C> provider;
	private final TLVParser<T> parser;
	
	@SuppressWarnings("unchecked")
	protected MessageLinkConnection(TLVParser<T> parser) {
		this.provider = MessageConnection.createProvider(this, (MLC) this);
		this.parser = parser;
	}
	
	public Provider<MLC, T, C> getProvider() {
		return provider;
	}
	
	@Override
	protected void onBundleReceived(Bundle bundle) throws ParsingException {
		LOGGER.v("Bundle received");
		provider.delivery(Message.unpack(
				bundle.getPayload(),
				parser
		));
	}
	
	@Override
	public void sendMessage(Message<T> message) {
		LOGGER.v("Sending message: " + getRegistrationEndpointID());
		
		final ByteBuffer buffer = ByteBuffer.allocate(0x10000);
		message.serialize(buffer);
		send(new Bundle(
				getLocalEndpointID(),
				getRegistrationEndpointID(),
				buffer
		));
	}
}
