package br.ufpa.adtn.core;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.util.EventQueue;
import br.ufpa.adtn.util.Logger;
import br.ufpa.adtn.util.EventQueue.Event;

public abstract class LinkConnection<LC extends LinkConnection<LC, R>, R extends BundleRouter<R, LC>> {
	private static final Logger LOGGER = new Logger("LinkConnection");
	private EventQueue eventQueue;
	private boolean parked;
	private boolean ready;
	private Link link;
	private R router;
	
	protected LinkConnection() {
		this.parked = false;
		this.ready = false;
	}
	
	private void checkState() {
		if (!ready)
			throw new IllegalStateException("Link connection is not ready yet");
	}
	
	final void bind(Link link, R router) {
		this.eventQueue = router.getEventQueue();
		this.router = router;
		this.link = link;
		
		this.ready = true;
	}
	
	public final void unpark() {
		checkState();

		// FIXME That is not the right way to unpark
		parked = false;
//		link.requestUnpark(router);
	}
	
	/**
	 * @param await - if {@code true}, this call will block until this
	 * request be processed in the Router event queue.
	 */
	void notifyParked() {
		eventQueue.post(new Event() {
			@Override
			public void execute() throws Throwable {
				if (parked) {
					LOGGER.w("Park requested in an already parked connection");
					return;
				}
				
				LOGGER.i("Parking to " + getRegistrationEndpointID());
				onParked();
				parked = true;
			}
		});
	}
	
	/**
	 * @param await - if {@code true}, this call will block until this
	 * request be processed in the Router event queue.
	 */
	void notifyUnparked() {
		eventQueue.post(new Event() {
			@Override
			public void execute() throws Throwable {
				if (!parked) {
					LOGGER.w("Unpark requested in a not parked connection");
					return;
				}
				
				onUnparked();
				parked = false;
			}
		});
		
	}
	
	void notifyBundleReceived(final Bundle bundle) {
		eventQueue.post(new Event() {
			@Override
			public void execute() throws Throwable {
				onBundleReceived(bundle);
			}
		});
	}
	
	public final R getRouter() {
		return router;
	}
	
	public final boolean isParked() {
		return parked;
	}
	
	public EID getLocalEndpointID() {
		return router.getLocalEID();
	}

	public EID getRegistrationEndpointID() {
		final EID eid = link.getEndpointID();
		return EID.get(
				router.getRegistration(),
				eid.getSSP()
		);
	}
	
	public EID getEndpointID() {
		return link.getEndpointID();
	}
	
	public Link getLink() {
		return link;
	}
	
	protected final void send(Bundle bundle) {
		checkState();
		
		if (!eventQueue.isOnInternalThread())
			throw new IllegalAccessError("Send bundle request must be requested by Router event queue Thread");
		
		LOGGER.v("Sending bundle");
		link.send(bundle);
	}
	
	protected abstract void onBundleReceived(Bundle bundle) throws ParsingException;
	protected void onUnparked() { }
	protected void onCreated() { }
	protected void onParked() { }
}
