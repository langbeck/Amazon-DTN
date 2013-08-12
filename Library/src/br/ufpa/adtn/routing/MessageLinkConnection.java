/**
 * Amazon-DTN - Lightweight Delay Tolerant Networking Implementation
 * Copyright (C) 2013  Dórian C. Langbeck
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package br.ufpa.adtn.routing;

import java.io.IOException;
import java.nio.ByteBuffer;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.bundle.BundleBuilder;
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
	
	public Provider<MLC, T, C> getMessageProvider() {
		return provider;
	}
	
	@Override
	protected void onBundleReceived(Bundle bundle) throws ParsingException {
		LOGGER.v("Bundle received");
		try {
			provider.delivery(Message.unpack(
					bundle.getPayload().read(),
					parser
			));
		} catch (IOException e) {
			throw new ParsingException(e);
		}
	}
	
	@Override
	public void sendMessage(Message<T> message) {
		LOGGER.v("Sending message: " + getRegistrationEndpointID());
		
		final ByteBuffer buffer = ByteBuffer.allocate(0x10000);
		message.serialize(buffer);
		buffer.flip();
		
		send(new BundleBuilder()
			.setDestination(getRegistrationEndpointID())
			.setSource(getLocalEndpointID())
			.setPayload(buffer)
			.build()
		);
	}
}
