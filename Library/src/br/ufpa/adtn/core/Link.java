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
package br.ufpa.adtn.core;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.BaseCL.IConnection;
import br.ufpa.adtn.util.EventQueue;
import br.ufpa.adtn.util.EventQueue.Event;
import br.ufpa.adtn.util.Logger;

public final class Link {
	private static final Map<EID, Link> _ref = new HashMap<EID, Link>();
	private static final Logger LOGGER = new Logger("Link");
	private static final EventQueue EVENTS;
	
	static {
		EVENTS = new EventQueue() {
			@Override
			protected void unhandledExceptionCacther(Throwable t) {
				LOGGER.e("Unhandled exception", t);
			}
		};
	}
	
	public static Link get(EID eid) {
		if (eid == null)
			throw new NullPointerException("EID can not be null");
		
		synchronized (_ref) {
			Link link = _ref.get(eid);
			if (link != null)
				return link;
			
			link = new Link(eid);
			_ref.put(eid, link);
			return link;
		}
	}


	private final Map<BundleRouter<?, ?>, LinkConnection<?, ?>> linkConnections;
	private final Queue<IConnection> availableConnections;
	private final Queue<IConnection> openConnections;
	private final Queue<IConnection> allConnections;
	private final EID eid;
	
	private Link(EID eid) {
		if (eid == null)
			throw new NullPointerException();

		this.linkConnections = new HashMap<BundleRouter<?, ?>, LinkConnection<?, ?>>();
		this.availableConnections = new LinkedList<IConnection>();
		this.openConnections = new LinkedList<IConnection>();
		this.allConnections = new LinkedList<IConnection>();
		this.eid = eid;
	}
	
	public EID getEndpointID() {
		return eid;
	}
	
	public boolean isAvailable() {
		return false;
	}
	
	private boolean openConnection(IConnection conn) {
		EVENTS.checkSync();

		LOGGER.d("Trying open existing connection to " + eid);
		if (!allConnections.contains(conn)) {
			LOGGER.w("Bad behavior");
			return false;
		}
		
		if (conn.isConnected()) {
			LOGGER.d("Connection already connected");
			
			if (!openConnections.contains(conn)) {
				LOGGER.w("An open connection is not properly registered");
				openConnections.add(conn);
			}
			
			return true;
		}
		
		try {
			conn.connect();
			if (!conn.isConnected()) {
				LOGGER.w("Bad behavior");
				return false;
			}
			
			openConnections.add(conn);
			return true;
		} catch (IOException e) {
			LOGGER.e("Connection failure", e);
			return false;
		}
	}
	
	@SuppressWarnings("unused")
	private void closeConnection(IConnection conn) {
		EVENTS.checkSync();
		
		
	}
	
	@SuppressWarnings("unused")
	private boolean openConnection() {
		EVENTS.checkSync();
		
		
		return false;
	}
	
	void cleanup() {
		try {
			EVENTS.postAndWait(new CleanupEvent());
		} catch (ExecutionException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof RuntimeException)
				throw (RuntimeException) cause;
			
			throw new RuntimeException(cause);
		}
	}
	
	private void innerCleanup() {
		EVENTS.checkSync();

		LOGGER.i("Cleanup: starting");
		LOGGER.i("Cleanup: scaning list of open connections");
		for (Iterator<IConnection> it = openConnections.iterator(); it.hasNext();) {
			final IConnection conn = it.next();
			
			//This should never happen
			if (conn.isClosed()) {
				LOGGER.w("Cleanup: closed Connection found in open connections list (removing)");
				
				//Remove reference from groups
				allConnections.remove(conn);
				it.remove();
				continue;
			}
			
			//Neither this
			if (!conn.isConnected()) {
				LOGGER.w("Cleanup: disconnected Connection found in open connections list (removing)");
				it.remove();
				
				if (!availableConnections.contains(conn)) {
					LOGGER.w("Cleanup: connection is not in the available list (appending)");
					availableConnections.add(conn);
				}
				
				if (!allConnections.contains(conn)) {
					LOGGER.w("Cleanup: connection is not in general list (appending)");
					allConnections.add(conn);
				}
				
				continue;
			}

			if (availableConnections.contains(conn)) {
				LOGGER.w("Cleanup: connected Connection present in the connected list (removing)");
				availableConnections.remove(conn);
			}

			if (!allConnections.contains(conn)) {
				LOGGER.w("Cleanup: connection is not in general list (appending)");
				allConnections.add(conn);
			}
		}

		LOGGER.i("Cleanup: scaning list of available connections");
		for (Iterator<IConnection> it = availableConnections.iterator(); it.hasNext();) {
			final IConnection conn = it.next();
			
			if (conn.isConnected()) {
				LOGGER.w("Cleanup: connected Connection present in the connected list (removing)");
				it.remove();
				
				if (!openConnections.contains(conn)) {
					LOGGER.w("Cleanup: connection is not present in the list of open connections (removing)");
					openConnections.add(conn);
				}

				if (!allConnections.contains(conn)) {
					LOGGER.w("Cleanup: connection is not in general list (appending)");
					allConnections.add(conn);
				}
				
				continue;
			}
			
			if (openConnections.contains(conn)) {
				LOGGER.w("Cleanup: disconnected Connection present in the connected list (removing)");
				openConnections.remove(conn);
			}

			if (!allConnections.contains(conn)) {
				LOGGER.w("Cleanup: connection is not in general list (appending)");
				allConnections.add(conn);
			}
		}

		LOGGER.i("Cleanup: scaning list of available connections");
		for (Iterator<IConnection> it = allConnections.iterator(); it.hasNext();) {
			final IConnection conn = it.next();
			
			if (conn.isConnected()) {
				if (!openConnections.contains(conn)) {
					LOGGER.i("Cleanup: connected Connection are not properly listed (appending)");
					openConnections.add(conn);
				}
			} else if (!availableConnections.contains(conn)) {
				LOGGER.i("Cleanup: disconnected Connection are not properly listed (appending)");
				availableConnections.add(conn);
			}
		}
		
		LOGGER.i("Cleanup: completed");
	}
	
	private boolean tryOpenConnection() {
		LOGGER.i("Trying open a new connection to " + eid);
		if (availableConnections.isEmpty()) {
			LOGGER.i("No connections available " + eid);
			return false;
		}
		
		for (IConnection conn : availableConnections) {
			if (openConnection(conn))
				return true;
		}
		
		return false;
	}
	
	private <R extends BundleRouter<R, LC>, LC extends LinkConnection<LC, R>> void innerPark(R router) {
		if (router == null)
			throw new IllegalArgumentException();

		final LC conn = getConnection(router);
		if (conn == null) {
			LOGGER.w("Link connection can not be created.");
			return;
		}
		
		if (!openConnections.isEmpty()) {
			conn.notifyParked();
		} else if (tryOpenConnection()) {
			synchronized (linkConnections) {
				linkConnections.put(router, conn);
				parkConnections();
			}
		}
	}
	
	private <R extends BundleRouter<R, LC>, LC extends LinkConnection<LC, R>> void innerUnpark(R router) {
		if (router == null)
			throw new IllegalArgumentException();
		
		final LinkConnection<?, ?> conn;
		synchronized (linkConnections) {
			conn = linkConnections.get(router);
		}
		
		if (conn == null || !conn.isParked()){
			LOGGER.w("Unpark requested with a router not parked before.");
			return;
		}
			
		if (!openConnections.isEmpty()) {
			for (IConnection connection : openConnections)
				connection.close();
			
			unparkConnections();
		}
	}
	
	@SuppressWarnings("unchecked")
	public <R extends BundleRouter<R, LC>, LC extends LinkConnection<LC, R>> LC getConnection(R router) {
		try {
			synchronized (linkConnections) {
				LC conn = (LC) linkConnections.get(router);
				if (conn == null) {
					conn = createConnection(router);
					if (conn == null)
						return null;
					
					linkConnections.put(router, conn);
				}
				
				return conn;
			}
		} catch (ClassCastException e) {
			throw new InternalError();
		}
	}
	
	private <R extends BundleRouter<R, LC>, LC extends LinkConnection<LC, R>> LC createConnection(R router) {
		final LC conn = router.createConnection(this);
		if (conn == null) {
			LOGGER.e("LinkConnection creation failure");
			return null;
		}
		
		conn.bind(this, router);
		conn.onCreated();
		return conn;
	}


	private void onConnectionClosed(IConnection connection) {
		if (availableConnections.contains(connection)) {
			LOGGER.v("Unregistering connection.");
			availableConnections.remove(connection);
			if (availableConnections.isEmpty())
				unparkConnections();
		} else {
			LOGGER.v("Trying unregister a unregistered connection. [Ignored]");
		}
	}
	
	private void onConnectionRegistered(IConnection connection) {
		if (allConnections.contains(connection)) {
			LOGGER.w("Trying to register duplicate connection. [Ignored]");
		} else {
			LOGGER.v("Registering connection: Connected(" + connection.isConnected() + ")");
			allConnections.add(connection);
			
			if (connection.isConnected()) {
				final boolean firstOpened = openConnections.isEmpty();
				openConnections.add(connection);
				
				if (firstOpened)
					parkConnections();
			} else {
				availableConnections.add(connection);
			}
		}
	}
		
	private void unparkConnections() {
		synchronized (linkConnections) {
			for (LinkConnection<?, ?> conn : linkConnections.values())
				conn.notifyUnparked();
		}
	}
	
	private void parkConnections() {
		EVENTS.checkSync();
		
		for (LinkConnection<?, ?> conn : linkConnections.values())
			conn.notifyParked();
	}
	
	private IConnection getConnectionToSend() {
		IConnection conn = openConnections.peek();
		if (conn != null)
			return conn;
		
		LOGGER.d("No opened connections. Searching for available connections.");
		conn = availableConnections.peek();
		if (conn == null) {
			LOGGER.d("No available connections");
			return null;
		}
		
		try {
			conn.connect();
			return conn;
		} catch (IOException e) {
			LOGGER.e("Connection failure", e);
			return null;
		}
	}
	
	void sendAll(Collection<Bundle> bundles) {
		final IConnection conn = getConnectionToSend();
		if (conn != null)
			for (Bundle bundle : bundles)
				conn.send(bundle);
	}
	
	void send(Bundle bundle) {
		final IConnection conn = getConnectionToSend();
		if (conn != null)
			conn.send(bundle);
	}

	
	/*
	 * Connectors
	 */
	
	void notifyConnectionClosed(IConnection connection) {
		EVENTS.post(new ConnectionClosedEvent(connection));
	}
	
	void notifyConnectionRegistered(IConnection connection) {   
		try {
			EVENTS.postAndWait(new ConnectionRegisteredEvent(connection));
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	<R extends BundleRouter<R, LC>, LC extends LinkConnection<LC, R>> void requestUnpark(final R router) {
		EVENTS.post(new UnparkEvent<R, LC>(router));
	}

	<R extends BundleRouter<R, LC>, LC extends LinkConnection<LC, R>> void requestPark(final R router) {
		EVENTS.post(new ParkEvent<R, LC>(router));
	}


	/*
	 * Events
	 */
	
	private class ConnectionClosedEvent extends Event {
		private final IConnection connection;

		public ConnectionClosedEvent(IConnection connection) {
			this.connection = connection;
		}

		@Override
		public void execute() throws Throwable {
			onConnectionClosed(connection);
		}
	}
	
	private class ConnectionRegisteredEvent implements Runnable {
		private final IConnection connection;
		
		private ConnectionRegisteredEvent(IConnection connection) {
			if (connection == null)
				throw new NullPointerException("A null connection can not be registered");
			
			this.connection = connection;
		}

		@Override
		public void run() {
			onConnectionRegistered(connection);
		}
	}
	
	private class CleanupEvent implements Runnable {

		@Override
		public void run() {
			innerCleanup();
		}
	}

	private class ParkEvent <R extends BundleRouter<R, LC>, LC extends LinkConnection<LC, R>> extends Event {
		private final R router;

		private ParkEvent(R router) {
			this.router = router;
		}

		@Override
		public void execute() throws Throwable {
			innerPark(router);
		}
	}

	private class UnparkEvent <R extends BundleRouter<R, LC>, LC extends LinkConnection<LC, R>> extends Event {
		private final R router;

		private UnparkEvent(R router) {
			this.router = router;
		}

		@Override
		public void execute() throws Throwable {
			innerUnpark(router);
		}
	}
}
