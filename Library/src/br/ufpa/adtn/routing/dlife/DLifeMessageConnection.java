package br.ufpa.adtn.routing.dlife;

import br.ufpa.adtn.core.BPAgent;
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
	private enum State { UNDEFINED, WAITING_HELLO_HEL, SOCIAL_EXCHANGE, FINISHED }
	
	private DLifeLinkConnection conn;
	private SocialInformation sInfo;
	private State state;
	private int storage;
	
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
		case WAITING_HELLO_HEL:
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
					hType, getEndpointID()
			));
			return;
		}
			
		storage = hello.getStorage();
		sendHelloACK(id);
		
		LOGGER.v("Hello.HEL received from " + getEndpointID());
	}

	private void sendHello() {
		sendMessage(new HelloResponseListener(), new DLifeTLV.Hello(
				HelloType.HEL,
				getEndpointID(),
				0,
				BPAgent.getStorageCapacity()
		));
		
		changeState(State.WAITING_HELLO_HEL);
	}
	
	private void sendHelloACK(int id) {
		sendResponse(id, null, new DLifeTLV.Hello(
				HelloType.ACK,
				getEndpointID(),
				0,
				0
		));
		
		LOGGER.i("Hello.ACK sent to " + getEndpointID());
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