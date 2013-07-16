package br.ufpa.adtn.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.core.LinkConnection;
import br.ufpa.adtn.util.Logger;

public abstract class MessageConnection<LConn extends LinkConnection<?, ?>, T extends TLV, C extends MessageConnection<LConn, T, C>> implements ResponseListener<T> {
	private static final Logger LOGGER = new Logger("MessageConnection");

	public static interface Connector<LConn extends LinkConnection<?, ?>, T extends TLV, C extends MessageConnection<LConn, T, C>> {
		public void sendMessage(Message<T> message);
		public C createMessageConnection();
	}
	
	/**
	 * The generated provider are not Thread-safe
	 * @return
	 */
	public static <LConn extends LinkConnection<?, ?>, T extends TLV, C extends MessageConnection<LConn, T, C>> Provider<LConn, T, C> createProvider(Connector<LConn, T, C> connector, LConn connection) {
		return new Provider<LConn, T, C>(connector, connection);
	}
	

	public final static class Provider<LConn extends LinkConnection<?, ?>, T extends TLV, C extends MessageConnection<LConn, T, C>> {
		private static final Logger LOGGER = new Logger(MessageConnection.LOGGER, "Provider");

		private final Connector<LConn, T, C> connector;
		private final Map<Short, C> receivers;
		private final LConn connection;
		private final Random random;
		
		public Provider(Connector<LConn, T, C> connector, LConn connection) {
			this.receivers = new HashMap<Short, C>();
			this.connection = connection;
			this.connector = connector;
			this.random = new Random();
		}

		public LConn getLinkConnection() {
			return connection;
		}
		
		public C create() {
			return createConnection((short) 0);
		}
		
		private C createConnection(short receiver) {
			LOGGER.v("Requesting for a new MessageConnection");
			final C mConn = connector.createMessageConnection();
			
			short identifier;
			do {
				identifier = (short) (random.nextInt(0xFFFF) + 1);
			} while (receivers.containsKey(identifier));
			
			mConn.connection = connection;
			mConn.connector = connector;
			mConn.receiver = receiver;
			mConn.sender = identifier;
			
			LOGGER.v(String.format(
					"MessageConnection created: 0x%04X/0x%04X",
					identifier,
					receiver
			));
			mConn.init(receiver == 0);
			receivers.put(identifier, mConn);
			return mConn;
		}
		
		public void delivery(Message<T> message) {
			final short receiver = message.getReceiver();
			final short sender = message.getSender();
			
			if (sender == 0) {
				LOGGER.e("Message received containing a null sender instance");
				return;
			}
			
			if (receiver == 0) {
				LOGGER.v("Empty receiver. Creating a new one.");
				createConnection(sender).delivery(message);
				return;
			}
			
			final C conn = receivers.get(receiver);
			if (conn == null) {
				LOGGER.w("Message to an unknow receiver instance received");
				return;
			}
			
			if (conn.receiver == 0)
				conn.receiver = sender;
			
			conn.delivery(message);
		}
	}
	

	private final Map<Integer, ResponseListener<T>> listeners;
	private Connector<LConn, T, C> connector;
	private LConn connection;
	private short receiver;
	private short sender;
	
	protected MessageConnection() {
		this.listeners = new HashMap<Integer, ResponseListener<T>>();
	}

	private void delivery(Message<T> message) {
		final int id = message.getID();
		
		final ResponseListener<T> listener = listeners.get(id);
		if (listener != null) {
			for (T tlv : message.getTLVs())
				listener.onReceived(id, tlv);
			
			listeners.remove(id);
		} else {
			for (T tlv : message.getTLVs())
				onReceived(id, tlv);
		}
	}
	
	public final LConn getLinkConnection() {
		return connection;
	}
	
	public final EID getEndpointID() {
		return connection.getRegistrationEndpointID();
	}
	
	protected final void sendResponse(int id, ResponseListener<T> listener, T ... tlvs) {
		LOGGER.v("Generating response message to send");

		if (tlvs == null || tlvs.length == 0)
			throw new IllegalArgumentException();
		
		final Message<T> reqMessage = Message.create(
				(byte) 0,
				(byte) 0,
				sender,
				receiver,
				id
		);
		
		for (T tlv : tlvs)
			reqMessage.add(tlv);
		
		if (listener != null)
			listeners.put(id, listener);
		
		connector.sendMessage(reqMessage);
	}

	protected final void sendMessage(ResponseListener<T> listener, T ... tlvs) {
		LOGGER.v("Generating request message to send");
		
		if (tlvs == null || tlvs.length == 0)
			throw new IllegalArgumentException();
		
		int identifier;
		do {
			final long nano = System.nanoTime();
			identifier = (int) (nano >> 32) ^ (int) nano;
		} while (listeners.containsKey(identifier));
		
		final Message<T> reqMessage = Message.create(
				(byte) 0,
				(byte) 0,
				sender,
				receiver,
				identifier
		);
		
		for (T tlv : tlvs)
			reqMessage.add(tlv);
		
		if (listener != null)
			listeners.put(identifier, listener);
		
		connector.sendMessage(reqMessage);
	}
	
	protected final void close() {
		//TODO Dispose this connection
	}
	
	protected void init(boolean isInitiator) { }
}
