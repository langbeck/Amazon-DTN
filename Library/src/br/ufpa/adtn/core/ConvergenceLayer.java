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
import java.io.InputStream;
import java.io.OutputStream;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.util.Logger;

/**
 * A simple implementation of ConvergenceLayer to provide the most basics resources
 * to developer.
 *  
 * @author Dórian Langbeck
 *
 * @param <TAdapter>
 * @param <TConnection>
 */
public abstract class ConvergenceLayer<TAdapter extends ConvergenceLayer<TAdapter, TConnection>.AbstractAdapter, TConnection extends ConvergenceLayer<TAdapter, TConnection>.AbstractConnection> extends BaseCL<TAdapter, TConnection> {
	private static final Logger LOGGER = new Logger("ConvergenceLayer");
	private static final ThreadGroup CL_GROUP;
	
	static {
		CL_GROUP = new ThreadGroup("ConvergenceLayers-ThreadGroup");
		CL_GROUP.setDaemon(true);
	}
	
	public abstract class AbstractAdapter implements BaseCL.IAdapter {
		private final Logger LOGGER = new Logger(ConvergenceLayer.LOGGER, "Adapter");
		protected final ConvergenceLayerConnector connector;
		private final ThreadGroup tGroup;
		private final String name;
		
		private Throwable execException;
		private IDiscovery discovery;
		private boolean execFailed;
		private boolean running;
		private boolean started;
		private boolean ready;
		private Thread thread;
		
		protected AbstractAdapter() {
			this(null);
		}
		
		protected AbstractAdapter(String name) {
			this.connector = new ConvergenceLayerConnector(this);
			this.execException = null;
			this.execFailed = false;
			this.discovery = null;
			this.running = false;
			this.started = false;
			this.thread = null;
			this.ready = false;

			this.name = (name == null) ?
						getClass().getName().replaceFirst("^.*\\.", "") :
						name;

			/**
			 * ThreadGroup used to keep all threads generated from this adapter
			 * with the same parent group.
			 */
			this.tGroup = new ThreadGroup(CL_GROUP, String.format("%s-ThreadGroup", name));
			
			LOGGER.i(this.name + " created");
		}
		
		private void run() {
			LOGGER.i(name + " is starting");
			
			synchronized (this) {
				running = true;
				notifyAll();
			}
			
			connector.notifyAdapterStarted();
			try {
				/*
				 * Give a space to adapter implementation be prepared to accept
				 * connections.
				 */
				doPreparations();

				synchronized (this) {
					ready = true;
					notifyAll();
				}
				
				while (started && !thread.isInterrupted()) {
					/*
					 * Wait for a connection. If accept() return null means no
					 * more connections will be accepted by this adapter.
					 */
					final TConnection connection = accept();
					if (connection == null) {
						LOGGER.i(name + ".accept() returned null");
						break;
					}

					if (!connection.isConnected())
						throw new IllegalStateException("A disconnected-connection was accepted (WFT?)");
					
					connection.initResources();
				}
				
				connector.notifyAdapterStoped(null);
			} catch (Throwable t) {
				connector.notifyAdapterStoped(t);

				synchronized (this) {
					execException = t;
					execFailed = true;
					notifyAll();
				}
			} finally {
				LOGGER.i(name + " stoped");
				
				/*
				 * Clear running flag and call doFinalizations() to let this
				 * adapter implementation release any resource requested so far.
				 */
				started = false;
				doFinalizations();
			}
		}
		
		@Override
		public synchronized final boolean start() throws Throwable {
			if (started)
				throw new IllegalStateException("Adapter was already started");
			
			if (thread != null)
				throw new IllegalStateException("Adapter was not running but the main thread is already defined");
			
			
			thread = new Thread(tGroup, "CL-" + name) {
				@Override
				public void run() {
					AbstractAdapter.this.run();
				}
			};

			thread.start();
			started = true;
			
			LOGGER.i("Waiting for thread start running");
			for (int i = 0; i < 3 && !running && !execFailed; i++)
				wait(250);
			
			if (execFailed) {
				LOGGER.e("Startup process failed");
				throw execException;
			}

			if (!running) {
				LOGGER.w("Startup process is taking too long to start running");
				return false;
			}

			LOGGER.i("Adapter are running. Waiting for adapter get ready.");
			for (int i = 0; i < 6 && !ready; i++)
				wait(250);
			
			if (!ready) {
				LOGGER.w("Startup process is taking too long to get ready");
				return false;
			}

			LOGGER.i("Adapter ready");
			return true;
		}
		
		public boolean isReady() {
			return ready;
		}
		
		protected final void setupDiscovery(IDiscovery discovery) {
			this.discovery = discovery;
		}

		@Override
		public final IDiscovery getDiscovery() {
			return discovery;
		}

		@Override
		public final boolean isRunning() {
			return started;
		}
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		public void stop() {
			synchronized (this) {
				if (!started)
					throw new IllegalStateException("Adapter is not running");
				
				if (thread == null)
					throw new IllegalStateException("Adapter thread is not defined");
				
				tGroup.interrupt();
				started = false;
				thread = null;
			}
		}
		
		
		protected abstract TConnection accept();
		
		protected abstract void doPreparations() throws Throwable;
		protected abstract void doFinalizations();
	}
	
	
	/**
	 * This class create two threads to handle with IO.
	 * 
	 * @see IConnection
	 * @author Langbeck
	 */
	public abstract class AbstractConnection implements BaseCL.IConnection {
		private boolean streamConfigured;
		private AbstractAdapter adapter;
		private ThreadGroup ctGroup;
		
		private OutputStream output;
		private boolean outputDone;
		
		private InputStream input;
		private boolean inputDone;
		
		private boolean connected;
		private boolean closed;

		private final EID expected_eid;
		private boolean registered;
		private EID registered_eid;
		private Link link;

		{
			this.streamConfigured = false;
			this.registered = false;
			this.ctGroup = null;
			this.closed = false;
			this.output = null;
			this.input = null;
			this.link = null;
		}
		
		protected AbstractConnection(TAdapter adapter) {
			if (adapter == null)
				throw new NullPointerException("Adapter can not be null");
			
			this.expected_eid = null;
			this.adapter = adapter;
			this.connected = true;
		}
		
		protected AbstractConnection(TAdapter adapter, EID expected_eid) {
			if (adapter == null)
				throw new NullPointerException("Adapter can not be null");
			
			this.expected_eid = expected_eid;
			this.adapter = adapter;
			this.connected = false;
		}

		protected final void register(EID eid) throws IllegalStateException {
			if (eid == null)
				throw new IllegalStateException("We get a null endpoint id");
			
			if (expected_eid != null && eid != expected_eid) {
				LOGGER.w(String.format(
						"Expected EID is \"%s\" but EID \"%s\" was informed",
						expected_eid.toString(),
						eid.toString()
				));
			}
			
			synchronized (this) {
				if (registered)
					throw new IllegalStateException("ConvergenceLayer already registered");
				
				link = Link.get(eid);
				link.notifyConnectionRegistered(this);
				
				registered_eid = eid;
				registered = true;
			}
		}
		
		public synchronized final boolean isRegistered() {
			return registered;
		}
		
		@Override
		public final EID getEndpointID() {
			return registered ? registered_eid : expected_eid;
		}
		
		private void initResources() throws IOException {
			//Check if setupStream was properly called in openConnection.
			if (!streamConfigured) {
				try {
					/* 
					 * Make sure we closed any open resource before throw an
					 * Exception.
					 */
					closeConnection();
				} catch (Throwable t) { }
				
				throw new IOException("Stream was not configured in openConnection() call. Did you forget call setupStream(in, out)?");
			}

			/*
			 * Create and configure the ThreadGroup and IO daemon treads.
			 * These threads will have the lowest priority.
			 */
			ctGroup = new ThreadGroup(adapter.tGroup, "MTConnection-ThreadGroup");
			ctGroup.setMaxPriority(Thread.MIN_PRIORITY);
			ctGroup.setDaemon(false);

			new Thread(ctGroup, "MTConnection-Output") {
				@Override
				public void run() {
					try {
						processInput(input);
					} catch (IOException e) {
						LOGGER.e("IOException has occurred during input processing", e);
						close(false);
					} finally {
						inputDone = true;
						if (!closed)
							close(true);
					}
				}
			}.start();
			
			new Thread(ctGroup, "MTConnection-Input") {
				@Override
				public void run() {
					try {
						processOutput(output);
					} catch (IOException e) {
						LOGGER.e("IOException has occurred during input processing", e);
						close(false);
					} finally {
						outputDone = true;
						if (!closed)
							close(true);
					}
				}
			}.start();
		}

		@Override
		public synchronized final void connect() throws IOException {
			if (connected)
				throw new IllegalStateException("Already connected");
			
			if (closed)
				throw new IOException("Already closed");
			
			if (adapter == null)
				throw new RuntimeException("Adapter was not defined");

			try {
				openConnection();
			} catch (Throwable t) {
				try {
					/* 
					 * Make sure we closed any open resource before throw an
					 * Exception.
					 */
					closeConnection();
				} catch (Throwable st) { }
				
				throw new IOException("Error while connecting.", t);
			}
			
			initResources();
			connected = true;
		}

		@Override
		public synchronized final void close() {
			if (!connected || closed)
				throw new IllegalStateException();
			
			ctGroup.interrupt();
			ctGroup = null;
			close(false);
		}
		
		private synchronized final void close(boolean check) {
			// TODO Ensure that output and input Threads has already closed.
			
			if (check && !(inputDone && outputDone))
				return;
			
			if (link != null) {
				link.notifyConnectionClosed(this);
				link = null;
			}
			
			try {
				closeConnection();
			} finally {
				connected = false;
				closed = true;
				output = null;
				input = null;
			}
		}

		@Override
		public final boolean isConnected() {
			return connected;
		}

		@Override
		public final boolean isClosed() {
			return closed;
		}

		/**
		 * This method MUST be called at {@code openConnection()}.
		 * 
		 * @param input
		 * @param output
		 * @throws IllegalStateException if {@code setupStream} was already called before
		 * @throws IllegalArgumentException if {@code input} or {@code output} are null
		 */
		protected synchronized final void setupStream(OutputStream output, InputStream input) throws IllegalStateException, IllegalArgumentException {
			if (input == null || output == null)
				throw new IllegalArgumentException();
			
			if (streamConfigured)
				throw new IllegalStateException();

			this.streamConfigured = true;
			this.outputDone = false;
			this.inputDone = false;
			this.output = output;
			this.input = input;
		}
		
		protected final void bundleReceived(Bundle bundle) {
			if (!registered) {
				LOGGER.d("Bundle received, but this ConvergenceLayer is not registered. [Ignoring]");
				return;
			}
			
			adapter.connector.notifyBundleReceived(this, bundle);
		}
		
		/**
		 * This method will be invoked in exclusively low-priority Thread to
		 * handle with data output.
		 */
		protected abstract void processOutput(OutputStream out) throws IOException;

		/**
		 * This method will be invoked in exclusively low-priority Thread to
		 * handle with data input.
		 */
		protected abstract void processInput(InputStream in) throws IOException;
		
		/**
		 * This method is called just once during connection process. If an
		 * IOException was thrown during the process, {@code closeConnection()}
		 * will be called before re-throw the exception.
		 * @throws IOException
		 */
		protected abstract void openConnection() throws IOException;
		
		/**
		 * This method is called at two distinct situations and should close
		 * and release all resources used by this connection.
		 * The first situation is if an error occur in {@code openConnection()}
		 * call (if an exception was thrown or if setupStream() was not called).
		 * The another situation is when the connection in closing.
		 */
		protected abstract void closeConnection();
	}
}
