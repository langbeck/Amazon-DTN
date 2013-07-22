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
package br.ufpa.dtns.cl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.core.BPAgent;
import br.ufpa.adtn.core.ConvergenceLayer;
import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.util.Logger;
import br.ufpa.adtn.util.Properties;
import br.ufpa.dtns.cl.VirtualConvergenceLayer.VirtualAdapter;
import br.ufpa.dtns.cl.VirtualConvergenceLayer.VirtualConnection;

public class VirtualConvergenceLayer extends ConvergenceLayer<VirtualAdapter, VirtualConnection> {
	private static final Logger LOGGER = new Logger("VirtualCL");
	private static VirtualAdapter ADAPTER = null;
	
	public static VirtualAdapter getMainAdapter() {
		return ADAPTER;
	}
	

	@Override
	protected VirtualAdapter createAdapter(Properties configuration, Object data) {
		return new VirtualAdapter();
	}
	
	
	public class VirtualAdapter extends ConvergenceLayer<VirtualAdapter, VirtualConnection>.AbstractAdapter {
		private SocketAddress address;
		private ServerSocket sSocket;
		
		private VirtualAdapter() {
			super("VirtualAdapter");
			
			synchronized (VirtualConvergenceLayer.class) {
				if (ADAPTER != null)
					throw new IllegalStateException("Adapter already defined");
				
				ADAPTER = this;
			}
		}
		
		public void discovery(EID eid, SocketAddress addr) {
			notifyConnectionDiscovered(new VirtualConnection(this, addr, eid));
		}
		
		public SocketAddress getAddress() {
			return address;
		}
		
		@Override
		protected VirtualConnection accept() {
			try {
				final Socket socket = sSocket.accept();
				return new VirtualConnection(
						VirtualAdapter.this,
						socket
				);
			} catch (IOException e) {
				LOGGER.e("Accept failed", e);
				return null;
			}
		}

		@Override
		protected void doPreparations() throws Throwable {
			if (sSocket != null) {
				LOGGER.w("Server channel already defined. Skipping preparation");
				return;
			}
			
			sSocket = new ServerSocket();
			sSocket.bind(null);
			
			address = sSocket.getLocalSocketAddress();
			LOGGER.i("Server started at " + address);
		}

		@Override
		protected void doFinalizations() {
			if (sSocket == null) {
				LOGGER.w("Server channel not defined. Skipping finalization.");
				return;
			}

			try {
				sSocket.close();
			} catch (IOException e) { }

			sSocket = null;
			address = null;
		}
	}
	
	
	public class VirtualConnection extends ConvergenceLayer<VirtualAdapter, VirtualConnection>.AbstractConnection {
		private final Logger LOGGER = new Logger(VirtualConvergenceLayer.LOGGER, "Connection");
		private final BlockingQueue<Bundle> outputBundles;
		private final SocketAddress address;
		private Socket socket;
		
		{
			outputBundles = new LinkedBlockingQueue<Bundle>();
		}
		
		private VirtualConnection(VirtualAdapter adapter, Socket socket) throws IOException {
			super(adapter);
			
			this.address = socket.getLocalSocketAddress();
			this.socket = socket;

			LOGGER.v("Connection accepted from " + address);
			setupStream(
					new BufferedOutputStream(socket.getOutputStream()),
					new BufferedInputStream(socket.getInputStream())
			);
		}
		
		private VirtualConnection(VirtualAdapter adapter, SocketAddress address, EID eid) {
			super(adapter, eid);

			LOGGER.v("Connected to " + address);
			this.address = address;
			this.socket = null;
			register(eid);
		}

		@Override
		public void send(Bundle bundle) {
			outputBundles.offer(bundle);
		}

		@Override
		protected void processOutput(OutputStream out) throws IOException {
			try {
				final ObjectOutputStream oos = new ObjectOutputStream(out);
				
				//FIXME Each ConvergenceLayer must have your own EID (if needed)
				oos.writeUTF(BPAgent.getHostEID().toString());
				oos.flush();
				
				while (!Thread.interrupted()) {
					final Bundle b = outputBundles.take();
					oos.writeUnshared(b);
					oos.flush();
				}
			} catch (InterruptedException e) {
				LOGGER.w("Output Interrupted");
			} catch (Throwable t) {
				LOGGER.e("Output error", t);
			} finally {
				LOGGER.d("EXITING(processOutput)");
			}
		}

		@Override
		protected void processInput(InputStream in) throws IOException {
			final ObjectInputStream ois = new ObjectInputStream(in);
			final EID remote_eid = EID.get(ois.readUTF());
			if (!isRegistered())
				register(remote_eid);
			
			try {
				while (isConnected()) {
					final Object bundle;
					try {
						bundle = ois.readUnshared();
					} catch (IOException e) {
						LOGGER.w("Connection failure");
						break;
					}
					
					if (!(bundle instanceof Bundle)) {
						LOGGER.w("Non Bundle received. Ignoring this object.");
						continue;
					}
					
					bundleReceived((Bundle) bundle);
				}
			} catch (ClassNotFoundException e) {
				LOGGER.e("Input processing failure", e);
			} finally {
				LOGGER.d("EXITING(processInput)");
			}
		}

		@Override
		protected void openConnection() throws IOException {
			if (socket != null) {
				LOGGER.w("Channel already defined before openConnection()");
				return;
			}
			
			socket = new Socket();
			socket.connect(address);
			
			setupStream(
					new BufferedOutputStream(socket.getOutputStream()),
					new BufferedInputStream(socket.getInputStream())
			);
		}

		@Override
		protected void closeConnection() {
			if (socket == null)
				return;
			
			try {
				socket.close();
			} catch (IOException e) { }
		}
	}
}
