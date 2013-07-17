package br.ufpa.adtn.android.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import br.ufpa.adtn.util.Logger;

public final class DiscoveryManager {
	public static final long DEFAULT_RESTART_DELAY = 3500;
	public static final long DEFAULT_WDT_TIMEOUT = 5500;
	
	private static final Logger LOGGER = new Logger("DiscoveryManager");
	private static final int MSG_REGISTER_TO_CACHE = 0x01;
	private static final int MSG_REMOVE_FROM_CACHE = 0x02;
	private static final int MSG_CHECK_FOR_SERVICE = 0x03;
	private static final int MSG_CANCEL_DISCOVERY = 0x04;
	private static final int MSG_REGISTER_DEVICE = 0x05;
	private static final int MSG_START_DISCOVERY = 0x06;
	private static final int MSG_SUBMIT_WACTHER = 0x07;
	private static final int MSG_REMOVE_DEVICE = 0x08;
	private static final int MSG_WDT_TIMEOUT = 0x09;
	
	public static Service createService(Context context, String local_eid, UUID discovery_uuid, UUID service_uuid, DiscoveryListener listener) throws IOException {
		final Service service = new Service(context, local_eid, discovery_uuid, service_uuid);
		service.setListener(listener);
		return service;
	}
	
	
	public static class Service extends Handler {
		private static final Logger LOGGER = new Logger(DiscoveryManager.LOGGER, "Service");
		private static final short HEADER_HANDSHAKE	= (short) 0x10D0;
		private static final short HEADER_REFUSE	= (short) 0xDEAD;
		
		private class CacheEntry {
			private final String address;
			private final String eid;
			private final UUID uuid;
			
			private CacheEntry(String address, String eid, UUID uuid) {
				this.address = address;
				this.uuid = uuid;
				this.eid = eid;
			}
		}
		

		private final Set<BluetoothDevice> connected;
		private final Map<String, CacheEntry> cache;
		private final Set<BluetoothDevice> devices;
		private final BluetoothServerSocket record;
		private final BroadcastReceiver receiver;
		private final BluetoothAdapter adapter;
		private final ExecutorService executor;
		private final UUID discovery_uuid;
		private final UUID service_uuid;
		private final Context context;
		private final Thread thread;
		private final String eid;

		private boolean discoveryPausedRequested;
		private boolean discoveryResumeRequested;
		private DiscoveryListener listener;
		private long discoveryRestartDelay;
		private boolean discoveryRunning;
		private boolean discoveryPaused;
		private boolean started;
		private boolean stoped;
		private long wdtTimeout;
		private long cacheTTL;

		private Service(Context context, String local_eid, UUID discovery_uuid, UUID service_uuid) throws IOException {
			super(LooperFactory.createLooper());
			this.adapter = BluetoothAdapter.getDefaultAdapter();
			this.record = adapter.listenUsingInsecureRfcommWithServiceRecord(LOGGER.getTag(), discovery_uuid);
			
			this.executor = Executors.newCachedThreadPool();
			this.connected = new HashSet<BluetoothDevice>();
			this.cache = new HashMap<String, CacheEntry>();
			this.devices = new HashSet<BluetoothDevice>();

			this.discoveryRestartDelay = DEFAULT_RESTART_DELAY;
			this.discoveryResumeRequested = false;
			this.discoveryPausedRequested = false;
			this.wdtTimeout = DEFAULT_WDT_TIMEOUT;
			this.discovery_uuid = discovery_uuid;
			this.service_uuid = service_uuid;
			this.discoveryRunning = false;
			this.discoveryPaused = true;
			this.cacheTTL = 60000L;
			this.context = context;
			this.listener = null;
			this.eid = local_eid;
			this.started = false;
			this.stoped = false;

			this.thread = new Thread(LOGGER + "-MainThread") {
				@Override
				public void run() {
					mainLoop();
				}
			};

			this.receiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					final String action = intent.getAction();
					if (action.equals(BluetoothDevice.ACTION_FOUND)) {
						deviceDiscovered(
								(BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE),
								intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0)
						);
						return;
					}
					
					if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
						discoveryStarted();
						return;
					}
					
					if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
						discoveryFinished();
						return;
					}
				}
			};
		}
		
		private void checkService() {
			if (!started || stoped || !thread.isAlive())
				throw new IllegalStateException("Service is not running.");
		}

		public void resumeDiscovery() {
			checkService();
			
			if (!discoveryPaused) {
				LOGGER.v("Discovery resume requested but discovery is not paused.");
				return;
			}
			
			LOGGER.v("Discovery resume requested.");
			discoveryPausedRequested = false;
			discoveryResumeRequested = true;
			startDiscovery();
		}
		
		public void pauseDiscovery() {
			checkService();
			
			if (discoveryPaused) {
				LOGGER.v("Discovery pause requested but discovery is already paused.");
				return;
			}
			
			LOGGER.v("Discovery pause requested.");
			discoveryResumeRequested = false;
			discoveryPausedRequested = true;
			cancelDiscovery();
		}
		
		public void setListener(DiscoveryListener listener) {
			this.listener = listener;
		}
		
		public synchronized void start() {
			if (stoped) {
				LOGGER.w("This service already have been stoped. You need use a new instance.");
				return;
			}
			
			if (started) {
				LOGGER.w("Main thread already started. You need use a new instance.");
				return;
			}
			
			thread.start();
			started = true;
		}
		
		public synchronized void stop() {
			checkService();
			
			LOGGER.v("Shutting down server record " + record);
			context.unregisterReceiver(receiver);
			
			executor.shutdownNow();
			thread.interrupt();
			try {
				record.close();
			} catch (IOException e) { }
			
			stoped = true;
		}
		
		private void mainLoop() {
			final Logger LOGGER = new Logger(Service.LOGGER, "Record");
			
			LOGGER.v("Registering broadcast receiver for bluetooth");
			final IntentFilter intent = new IntentFilter();
			intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
			intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
			intent.addAction(BluetoothDevice.ACTION_FOUND);
			
			context.registerReceiver(receiver, intent, null, this);

			discoveryResumeRequested = false;
			discoveryPausedRequested = false;
			discoveryPaused = false;

			LOGGER.v("Requesting discovery start");
			startDiscovery();

			LOGGER.v("Starting reception loop");
			while (!Thread.interrupted()) {
				BluetoothSocket socket = null;
				try {
					socket = record.accept();
					new InfoExchanger(socket).run();
				} catch (Exception e) {
					if (e instanceof NullPointerException)
						e.printStackTrace();
					
					LOGGER.e("Connection error", e);
				} finally {
					if (socket != null)
						try {
							socket.close();
						} catch (IOException e) { }
				}
			}

			context.unregisterReceiver(receiver);
		}

		private void discoveryStarted() {
			if (discoveryPaused) {
				LOGGER.w("Discovery started, but discovery is paused. Probably an external program are using bluetooth adapter.");
				return;
			}

			if (discoveryResumeRequested) {
				LOGGER.v("Discovery resumed.");
				discoveryResumeRequested = false;
				discoveryPaused = false;
			} else {
				LOGGER.v("Discovery started");
			}

			if (wdtTimeout != 0) {
				LOGGER.v("Creating watchdog timer. Timeout: " + wdtTimeout);
				registerWDT(wdtTimeout);
			}

			discoveryRunning = true;
		}

		private void discoveryFinished() {
			if (discoveryPaused) {
				LOGGER.w("Discovery finished, but discovery is paused. Probably an external program are using bluetooth adapter.");
				return;
			}
			
//			LOGGER.d("Releasing permits");
//			LinkPolicy.releasePermits();
			if (discoveryPausedRequested) {
				LOGGER.v("Discovery paused.");
				discoveryPausedRequested = false;
				discoveryPaused = true;
				devices.clear();
			} else {
				LOGGER.v("Discovery finished.");
	
				if (devices.isEmpty()) {
					LOGGER.v("No devices found.");
					startDiscovery();
				} else {
					if (hasMessages(MSG_REGISTER_DEVICE)) {
						LOGGER.w("Discovery service contains register requests pending. Cleaning resquests.");
						removeMessages(MSG_REGISTER_DEVICE);
					}
					
					final int ndevs = devices.size();
					LOGGER.v("Starting service checker against " + ndevs + " devices.");
					for (BluetoothDevice device : devices) {
						sendMessage(obtainMessage(
								MSG_CHECK_FOR_SERVICE,
								new InfoExchanger(device)
						));
					}
				}
			}

			discoveryRunning = false;
		}


		private class InfoExchanger implements Runnable {
			private final Logger LOGGER = new Logger("InformationExchanger");
			private final BluetoothDevice device;
			private final String address;
			private BluetoothSocket socket;
			
			public InfoExchanger(BluetoothDevice device) {
				this.address = device.getAddress();
				this.device = device;
				this.socket = null;
			}
			
			public InfoExchanger(BluetoothSocket socket) {
				this.device = socket.getRemoteDevice();
				this.address = device.getAddress();
				this.socket = socket;
			}
			
			private boolean isAlreadyConnected() {
				synchronized (connected) {
					if (connected.contains(device)) {
						LOGGER.v("Already have a connection open to " + address);
						return true;
					}
					
					connected.add(device);
					return false;
				}
			}

			public void run() {
				final boolean isOutputConnection = (socket == null);
				try {
					if (isOutputConnection) {
						synchronized (connected) {
							if (connected.contains(device)) {
								LOGGER.w("Skiping connection request. Connection already open with " + address);
								return;
							}

							LOGGER.v("Trying to connect to discovery service of device " + device.getAddress());
							socket = device.createInsecureRfcommSocketToServiceRecord(discovery_uuid);
							socket.connect();

							LOGGER.v("Connected to device " + address);
							connected.add(device);
						}
					} else {
						LOGGER.d("Connected to device " + address);
						if (isAlreadyConnected()) {
							LOGGER.w("Rejecting connection");
							final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
							out.writeShort(HEADER_REFUSE);
							out.flush();
							out.close();
							return;
						}
					}

					final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
					final DataInputStream in = new DataInputStream(socket.getInputStream());

					out.writeShort(HEADER_HANDSHAKE);
					out.flush();

					final short header = in.readShort();
					if (header == HEADER_REFUSE) {
						LOGGER.w("Connection refused");
						return;
					}

					LOGGER.v("Sending discovery information");
					out.writeLong(service_uuid.getMostSignificantBits());
					out.writeLong(service_uuid.getLeastSignificantBits());
					out.writeUTF(eid);
					out.flush();
					
					LOGGER.v("Reading discovery information");
					final UUID uuid = new UUID(in.readLong(), in.readLong());
					final String remote_eid = in.readUTF();
					
					LOGGER.d("Information exchange completed");
					socket.close();
					socket = null;
					
					LOGGER.i("New node discovered " + remote_eid + " on UUID " + uuid);
					if (listener == null) {
						LOGGER.d("Node ignored. Listener not defined.");
						return;
					}

					registerToCache(remote_eid, address, uuid);
					listener.notifyNeighborFound(remote_eid, device, uuid);
				} catch (IOException e) {
					LOGGER.e("Discovery connection error: " + e.toString());
				} finally {
					if (socket != null) {
						try {
							socket.close();
						} catch (IOException e) { }
					}
					
					synchronized (connected) {
						connected.remove(device);
					}

					if (isOutputConnection) {
						/*
						 * We will only remove devices that were found by
						 * discovery (ie devices not from incoming connections)
						 */
						unregisterDevice(device);
					}
				}
			}
		}
		
		private void registerToCache(String eid, String addr, UUID uuid) {
			sendMessage(obtainMessage(MSG_REGISTER_TO_CACHE, new CacheEntry(addr, eid, uuid)));
			sendMessageDelayed(obtainMessage(MSG_REMOVE_FROM_CACHE, addr), cacheTTL);
		}
		
		private void unregisterDevice(BluetoothDevice device) {
			sendMessage(obtainMessage(MSG_REMOVE_DEVICE, device));
		}

		private void deviceDiscovered(BluetoothDevice device, short rssi) {
			sendMessage(obtainMessage(MSG_REGISTER_DEVICE, rssi, 0, device));
		}
		
		private void registerWDT(long timeout) {
			sendEmptyMessageDelayed(MSG_WDT_TIMEOUT, timeout);
		}
		
		
		private void cancelDiscovery() {
			LOGGER.v("Discovery cancelation requested");
			sendEmptyMessage(MSG_CANCEL_DISCOVERY);
		}
		
		private void startDiscovery() {
			LOGGER.v("Discovery start requested");
			removeMessages(MSG_WDT_TIMEOUT);
			sendEmptyMessageDelayed(MSG_START_DISCOVERY, discoveryRestartDelay);
			
//			sendEmptyMessageDelayed(MSG_SUBMIT_WACTHER, discoveryRestartDelay);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_REGISTER_TO_CACHE: {
					final CacheEntry entry = (CacheEntry) msg.obj;
					
					LOGGER.v("Saving discovery information from device " + entry.address + " in cache");
					cache.put(entry.address, entry);
					break;
				}
				
				case MSG_REMOVE_FROM_CACHE: {
					final String address = (String) msg.obj;
					LOGGER.v("Removing device " + address + " from cache");
					cache.remove(address);
					break;
				}
				
				case MSG_CHECK_FOR_SERVICE: {
					final InfoExchanger exchanger = (InfoExchanger) msg.obj;
					final String address = exchanger.device.getAddress();
					final CacheEntry data = cache.get(address);

//					if (BluetoothContactManager.shouldIgnore(address)) {
//						LOGGER.w("Ignoring invalid device: " + address);
//						removeDeviceFromList(exchanger.device);
//						return;
//					}

					if (data != null) {
						LOGGER.v(address + " was found in cache.");
						listener.notifyNeighborFound(data.eid, exchanger.device, data.uuid);
						unregisterDevice(exchanger.device);
					} else {
						LOGGER.v(address + " was not found in cache.");
						executor.submit(exchanger);
					}
					
					break;
				}
				
				case MSG_REMOVE_DEVICE: {
					if (discoveryRunning) {
						LOGGER.e("Device remove requested while discovery is running.");
						return;
					}
					
					final BluetoothDevice device = (BluetoothDevice) msg.obj;
					LOGGER.i("+ MSG_REMOVE_DEVICE: " + device.getAddress() + " / Contains: " + devices);
					devices.remove(device);
					LOGGER.i("- MSG_REMOVE_DEVICE: " + device.getAddress() + " / Contains: " + devices);
					
					if (devices.isEmpty()) {
						LOGGER.v("All service checkers ended.");
						startDiscovery();
					}
					break;
				}
				
				case MSG_REGISTER_DEVICE: {
					if (!discoveryRunning) {
						LOGGER.e("Device resgister requested while discovery is not running.");
						return;
					}
					
					final BluetoothDevice device = (BluetoothDevice) msg.obj;
					LOGGER.v("Registering device: " + device.getAddress() + " [RSSI=" + msg.arg1 + "]");
					devices.add(device);
					break;
				}
				
				case MSG_WDT_TIMEOUT: {
					LOGGER.v("Discovery watchdog timer trigged.");
					BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
					break;
				}
				
				case MSG_START_DISCOVERY: {
					LOGGER.v("Starting discovery.");
					BluetoothAdapter.getDefaultAdapter().startDiscovery();
					break;
				}
				
				case MSG_CANCEL_DISCOVERY: {
					LOGGER.v("Stoping discovery.");
					BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
					break;
				}
				
				case MSG_SUBMIT_WACTHER: {
					executor.submit(new Callable<Void>() {
						public Void call() throws Exception {
							final long start = System.currentTimeMillis();
							try {
								LOGGER.v("Waiting for open connections.");
//								LinkPolicy.getPermits(3000);
								
								final long delta = discoveryRestartDelay - (System.currentTimeMillis() - start);
								if (delta > 0) {
									LOGGER.v("All connections closed. Posting start discovery request in " + delta + "ms");
									sendEmptyMessageDelayed(MSG_START_DISCOVERY, delta);
								} else {
									LOGGER.v("All connections closed. Posting start discovery request now.");
									sendEmptyMessage(MSG_START_DISCOVERY);
								}
							} catch (Throwable t) {
								LOGGER.e("Error while waiting actives connections ends.", t);
								
								final long delta = discoveryRestartDelay - (System.currentTimeMillis() - start);
								if (delta > 0) {
									LOGGER.v("Posting start discovery request in " + delta + "ms");
									sendEmptyMessageDelayed(MSG_START_DISCOVERY, delta);
								} else {
									LOGGER.v("Posting start discovery request now.");
									sendEmptyMessage(MSG_START_DISCOVERY);
								}
							}
							
							return null;
						}
					});
					break;
				}
				
				default:
					LOGGER.w(String.format("Invalid message received: 0x%02X", msg.what));
			}
			
			//Return this Message to the global pool
//			msg.recycle();
		}
	}
	
	public static interface DiscoveryListener {
		public void notifyNeighborFound(String eid, BluetoothDevice device, UUID uuid);
	}
	
	private DiscoveryManager() { }
}
