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
package br.ufpa.adtn.routing.dlife;

import java.util.regex.Pattern;

import br.ufpa.adtn.core.BPAgent;
import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.routing.MessageConnection;
import br.ufpa.adtn.routing.ResponseListener;
import br.ufpa.adtn.routing.dlife.DLifeTLV.Ack;
import br.ufpa.adtn.routing.dlife.DLifeTLV.AckType;
import br.ufpa.adtn.routing.dlife.DLifeTLV.Hello;
import br.ufpa.adtn.routing.dlife.DLifeTLV.HelloType;
import br.ufpa.adtn.routing.dlife.DLifeTLV.Social;
import br.ufpa.adtn.util.Logger;

public class DLifeMessageConnection extends MessageConnection<DLifeLinkConnection, DLifeTLV, DLifeMessageConnection> {
	private static final Logger LOGGER = new Logger("DLifeMessageConnection");
	private enum State { UNDEFINED, HELLO_EXCHANGE, SOCIAL_EXCHANGE, FINISHED }
	
	private DLifeLinkConnection conn;
	private SocialInformation sInfo;
	private long storage;
	private State state;
	
	private void changeState(State to) {
		LOGGER.i(String.format(
				"Changing state from %s to %s",
				state,
				to
		));
		
		state = to;
	}

	@Override
	protected void init(boolean isInitiator) {
		LOGGER.v(String.format("init(%s)", isInitiator));
		conn = getLinkConnection();
		sInfo = conn.getSocialInformation();
		state = State.UNDEFINED;
		storage = 0;
		sendHello();
	}

	@Override
	public void onReceived(int id, DLifeTLV tlv) {
		switch (state) {
		case HELLO_EXCHANGE:
			processHello(tlv, id);
			break;

		case SOCIAL_EXCHANGE:
			processSocial(tlv, id);
			break;

		default:
			LOGGER.e(String.format(
					"Illegal state. No TLV is expected in %s state.",
					state
			));
		}
	}

	private void processSocial(DLifeTLV tlv, int id) {
		if (!(tlv instanceof Social)) {
			LOGGER.e(String.format(
					"Social TLV was expected, but type 0x%02X was received.",
					tlv.getType()
			));
			return;
		}
		
		final Social social = (Social) tlv;
		conn.update(
				social.getSWNIs(),
				social.getCarried(),
				social.getAcked(),
				storage,
				social.getImportance()
		);

		changeState(State.FINISHED);
		sendSocialACK(id);
		conn.unpark();
	}

	private void processHello(DLifeTLV tlv, int id) {
		if (!(tlv instanceof Hello)) {
			LOGGER.e(String.format(
					"Hello TLV was expected, but type 0x%02X was received.",
					tlv.getType()
			));
			return;
		}
		
		final Hello hello = (Hello) tlv;
		final HelloType hType = hello.getHelloType();
		
		if (hType != HelloType.HEL) {
			LOGGER.e(String.format(
					"Unexpected Hello.%s received from %s",
					hType, getRegistrationEndpointID()
			));
			return;
		}

		final EID registration_eid = getRegistrationEndpointID();
		final EID hello_eid = hello.getEID();
		if (!registration_eid.equals(hello_eid)) {
			LOGGER.w(String.format(
					"Received \"%s\" in Hello TLV but we are in \"%s\". [IGNORING]",
					hello_eid, registration_eid
			));
		}
		
		storage = hello.getStorage();
		sendHelloACK(id);
		
		LOGGER.v("Hello.HEL received from " + getRegistrationEndpointID());
	}

	private void sendHello() {
		sendMessage(new HelloResponseListener(), new DLifeTLV.Hello(
				HelloType.HEL,
				getLocalEndpointID(),
				0,
				BPAgent.getStorageAvailable()
		));
		
		changeState(State.HELLO_EXCHANGE);
	}
	
	private void sendHelloACK(int id) {
		sendResponse(id, null, new DLifeTLV.Hello(
				HelloType.ACK,
				getLocalEndpointID(),
				0,
				0
		));
		
		LOGGER.i("Hello.ACK sent to " + getRegistrationEndpointID());
	}
	
	private void sendSocial() {
		final DLifeBundleRouter router = conn.getRouter();
		sendMessage(
				new SocialResponseListener(), 
				new DLifeTLV.Social(
						sInfo.getSampleWeights(),
						router.getCarried(),
						router.getAcked(),
						sInfo.getTECDi()
				)
		);
	}
	
	private void sendSocialACK(int id) {
		sendResponse(id, null, new DLifeTLV.Ack(AckType.SOCIAL, null));
	}
	
	private class HelloResponseListener implements ResponseListener<DLifeTLV> {

		@Override
		public void onReceived(int id, DLifeTLV tlv) {
			if (tlv instanceof Hello) {
				final Hello hello = (Hello) tlv;
				
				if (hello.getHelloType() == HelloType.ACK) {
					changeState(State.SOCIAL_EXCHANGE);
					sendSocial();
				} else {
					LOGGER.w(String.format(
							"HelloResponseListener received a Hello TLV with type %s, but an ACK was expected [IGNORING]",
							hello.getHelloType()
					));
				}
			} else {
				LOGGER.w("HelloResponseListener not received a Hello TLV [IGNORING]");
			}
		}
	}
	
	private class SocialResponseListener implements ResponseListener<DLifeTLV> {

		@Override
		public void onReceived(int id, DLifeTLV tlv) {
			//TODO Check what we need to do now
			if (tlv instanceof Ack) {
				final Ack ack = (Ack) tlv;
				if (AckType.getAckType(ack.getType()) == AckType.SOCIAL) {
					//dlifeConn.setDirty(false);
					close();
				}
			} else {
				LOGGER.w("SocialResponseListener not received a Social ACK");
				close();
			}
		}
	}
}
