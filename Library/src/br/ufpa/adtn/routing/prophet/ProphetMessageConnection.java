package br.ufpa.adtn.routing.prophet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import br.ufpa.adtn.routing.MessageConnection;
import br.ufpa.adtn.routing.ResponseListener;
import br.ufpa.adtn.routing.prophet.ProphetTLV.BundleOffer;
import br.ufpa.adtn.routing.prophet.ProphetTLV.BundleResponse;
import br.ufpa.adtn.routing.prophet.ProphetTLV.Hello;
import br.ufpa.adtn.routing.prophet.ProphetTLV.HelloType;
import br.ufpa.adtn.routing.prophet.ProphetTLV.RoutingInformation;
import br.ufpa.adtn.routing.prophet.ProphetUtil.BundleSpec;
import br.ufpa.adtn.util.Logger;

public class ProphetMessageConnection extends MessageConnection<ProphetLinkConnection, ProphetTLV, ProphetMessageConnection> {
	private static final Logger LOGGER = new Logger("ProphetMessageConnection");
	public enum State {UNDEFINED, SYNSENT, ESTAB, INFO_EXCH, FINISHED}
	
	private ProphetLinkConnection conn;
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
		state = State.UNDEFINED;
		sendHello();
	}
	
	@Override
	public void onReceived(int id, ProphetTLV tlv) {
		switch (state) {
		case SYNSENT:
			processHello(tlv, id);
			break;
			
		case ESTAB:
		case INFO_EXCH:
			processRoutingInfo(tlv, id);
			break;
			
		default:
			break;
		}
		
	}

	private void processHello(ProphetTLV tlv, int id) {
		if (!(tlv instanceof Hello)) {
			LOGGER.e(String.format(
					"Hello TLV was expected, but type 0x%02X was received.",
					tlv.getType()
			));
			return;
		}
		
		final Hello hello = (Hello) tlv;
		final HelloType hType = hello.getHelloType();
		
		if (hType == HelloType.SYN) {
			sendHelloSYNACK(id);
			LOGGER.v("Hello.SYN received from " + getRegistrationEndpointID());
			
		} else if (hType == HelloType.ACK) {
			LOGGER.v("Hello.ACK received from " + getRegistrationEndpointID());
			
		} else {
			LOGGER.e(String.format(
					"Unexpected Hello.%s received from %s",
					hType, getRegistrationEndpointID()
			));
			return;
		}
	}
	
	private void processRoutingInfo(ProphetTLV tlv, int id) {
		if (!(tlv instanceof RoutingInformation)) {
			LOGGER.e(String.format(
					"RoutingInfo TLV was expected, but type 0x%02X was received.",
					tlv.getType()
			));
			return;
		}
		
		final RoutingInformation rinfo = (RoutingInformation) tlv;
		final BundleSpec[] offers = conn.update(rinfo.getPredicts());
		sendBundleOffers(id, offers);
	}
	
	private void sendHello() {
		sendMessage(new HelloResponseListener(), new ProphetTLV.Hello(
				HelloType.SYN,
				getLocalEndpointID(),
				0)
		);
		
		changeState(State.SYNSENT);
		LOGGER.i("Hello.SYN sent to " + getRegistrationEndpointID());
	}
	
	private void sendHelloSYNACK(int id) {
		sendResponse(id, null, new ProphetTLV.Hello(
				HelloType.SYNACK,
				getLocalEndpointID(),
				0)
		);
		
		LOGGER.i("Hello.SYNACK sent to " + getRegistrationEndpointID());
	}
	
	private void sendHelloACK(int id) {
		sendResponse(id, null, new ProphetTLV.Hello(
				HelloType.ACK,
				getLocalEndpointID(),
				0)
		);
		
		LOGGER.i("Hello.ACK sent to " + getRegistrationEndpointID());
	}
	
	private void sendRoutingInfo() {
		sendMessage(
				new RoutingInfoResponseListener(),
				new ProphetTLV.RoutingInformation(
						conn.
							getProphetDataRouting().
								getNeighborsPredicts())
		);
		
		changeState(State.INFO_EXCH);
		LOGGER.i("RoutingInfo sent to " + getRegistrationEndpointID());
	}
	
	private void sendBundleOffers(int id, BundleSpec[] offers) {
		sendResponse(
				id,
				new BundleOfferListener(),
				new ProphetTLV.BundleOffer(offers)
		);
		
		LOGGER.i("BundleOffer sent to " + getRegistrationEndpointID());
	}
	
	private void sendBundleResponses(int id, BundleSpec[] respos) {
		sendResponse(
				id,
				null,
				new ProphetTLV.BundleResponse(respos)
		);
		
		changeState(State.FINISHED);
		LOGGER.i("BundleResponse sent to " + getRegistrationEndpointID());
	}
	
	private class HelloResponseListener implements ResponseListener<ProphetTLV> {

		@Override
		public void onReceived(int id, ProphetTLV tlv) {
			if (tlv instanceof Hello) {
				final Hello hello = (Hello) tlv;
				
				if (hello.getHelloType() == HelloType.SYNACK) {
					sendHelloACK(id);
					changeState(State.ESTAB);
					sendRoutingInfo();
				} else {
					LOGGER.w(String.format(
							"HelloResponseListener received a Hello TLV with type %s, but an SYNACK was expected [IGNORING]",
							hello.getHelloType()
					));
				}
			} else {
				LOGGER.w("HelloResponseListener not received a Hello TLV [IGNORING]");
			}
			
		}
	}
	
	private class RoutingInfoResponseListener implements ResponseListener<ProphetTLV> {

		@Override
		public void onReceived(int id, ProphetTLV tlv) {
			if (tlv instanceof BundleOffer) {
				final BundleOffer offer = (BundleOffer) tlv;
				final Collection<BundleSpec> available = conn.getRouter().getCollectionBundles();
				final BundleSpec[] offers = offer.getOffers();
				final List<BundleSpec> temp_resp = new ArrayList<BundleSpec>();
				
				for (int i = 0, len = offers.length; i < len; i++) {
					BundleSpec bs = offers[i];
					if (!available.contains(bs))
						temp_resp.add(bs);
				}
				
				int len = temp_resp.size();
				final BundleSpec[] responses = new BundleSpec[len]; 
				for (int i = 0; i < len; i++)
					responses[i] = temp_resp.get(i);
				
				sendBundleResponses(id, responses);
				
				LOGGER.i("BundleOffer received. Sending response to " + getRegistrationEndpointID());
				
			} else {
				LOGGER.w("RoutingInfoResponseListener not received a Offer TLV [IGNORING]");
			}
			
		}
	}
	
	private class BundleOfferListener implements ResponseListener<ProphetTLV> {

		@Override
		public void onReceived(int id, ProphetTLV tlv) {
			if (tlv instanceof BundleResponse) {
				final BundleResponse response = (BundleResponse) tlv;
				BundleSpec[] responses = response.getResponses();
				// TODO Pushing Bundles in responses.
				LOGGER.i("BundleResponse received. Pushing Bundles to " + getRegistrationEndpointID());
			} else {
				LOGGER.w("BundleOfferListener not received a Offer TLV [IGNORING]");
			}
			
		}
		
	}
}
