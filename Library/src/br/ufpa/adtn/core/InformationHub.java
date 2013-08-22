package br.ufpa.adtn.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.routing.dlife.SocialInformation;
import br.ufpa.adtn.util.Logger;
import br.ufpa.adtn.util.PeriodicEvent;
import br.ufpa.adtn.util.TrafficMeter;

public class InformationHub {
	public final static TrafficMeter COMPRESSED_CONVERGENCE_LAYER_METER;
	public final static TrafficMeter CONVERGENCE_LAYER_METER;
	public final static TrafficMeter DISCOVERY_METER;
	
	public final static BluetoothHub BLUETOOTH;
	public final static BundleHub DATA_BUNDLE;
	public final static BundleHub META_BUNDLE;
	public final static BundleHub BUNDLE;
	
	public final static ProphetHub PROPHET;
	public final static DLifeHub DLIFE;
	
	static {
		COMPRESSED_CONVERGENCE_LAYER_METER = new TrafficMeter();
		CONVERGENCE_LAYER_METER = new TrafficMeter();
		DATA_BUNDLE = new BundleHub("BundleData");
		META_BUNDLE = new BundleHub("BundleMeta");
		BUNDLE = new BundleHub("BundleGeneral");
		DISCOVERY_METER = new TrafficMeter();
		BLUETOOTH = new BluetoothHub();
		PROPHET = new ProphetHub();
		DLIFE = new DLifeHub();
	}
	

	public static void onTransferred(Bundle bundle, EID next, boolean isFinal) {
		(bundle.getInfo().isMeta() ? META_BUNDLE : DATA_BUNDLE) 
			.onTransferred(bundle, next, isFinal);

		BUNDLE.onTransferred(bundle, next, isFinal);
	}
	
	public static void onTransferStarted(Bundle bundle, EID next) {
		(bundle.getInfo().isMeta() ? META_BUNDLE : DATA_BUNDLE)
			.onTransferStarted(bundle, next);

		BUNDLE.onTransferStarted(bundle, next);
	}
	
	public static void onTransferAborted(Bundle bundle, EID next) {
		(bundle.getInfo().isMeta() ? META_BUNDLE : DATA_BUNDLE)
			.onTransferAborted(bundle, next);

		BUNDLE.onTransferAborted(bundle, next);
	}
	
	public static void onReceived(Bundle bundle, EID prev) {
		(bundle.getInfo().isMeta() ? META_BUNDLE : DATA_BUNDLE)
			.onReceived(bundle, prev);

		BUNDLE.onReceived(bundle, prev);
	}
	
	public static void onCreation(Bundle bundle) {
		(bundle.getInfo().isMeta() ? META_BUNDLE : DATA_BUNDLE)
			.onCreation(bundle);
	
		BUNDLE.onCreation(bundle);
	}
	
	public static void onDeleted(Bundle bundle, boolean dropped) {
		(bundle.getInfo().isMeta() ? META_BUNDLE : DATA_BUNDLE)
			.onDeleted(bundle, dropped);
	
		BUNDLE.onDeleted(bundle, dropped);
	}
	
	public static class BundleHub {
		private final static Logger LOGGER = new Logger("BundleHub");
		private final String hubname;
		private int payloadReceived;
		private int dataReceived;
		private int payloadSent;
		private int delivered;
		private int dataSent;
		private int received;
		private int created;
		private int relayed;
		private int sent;

		private final Map<Long, Long> receivedTime;
		
		private BundleHub(String hubname) {
			this.hubname = hubname;

			this.receivedTime = new HashMap<Long, Long>();
			this.payloadReceived = 0;
			this.dataReceived = 0;
			this.payloadSent = 0;
			this.delivered = 0;
			this.dataSent = 0;
			this.received = 0;
			this.created = 0;
			this.relayed = 0;
			this.sent = 0;
		}
		
		public int getReceivedOverhead() {
			return dataReceived - payloadReceived;
		}
		
		public int getSentOverhead() {
			return dataSent - payloadSent;
		}

		public int getReceivedPayload() {
			return payloadReceived;
		}
		
		public int getSentPayload() {
			return payloadSent;
		}

		public int getReceivedData() {
			return dataReceived;
		}
		
		public int getSentData() {
			return dataSent;
		}

		public int getDeliveredBundles() {
			return delivered;
		}
		
		public int getReceivedBundles() {
			return received;
		}
		
		public int getRelayedBundles() {
			return relayed;
		}

		public int getSentBundles() {
			return sent;
		}
		
		public int getCreated() {
			return created;
		}
		
		public void onTransferred(Bundle bundle, EID next, boolean isFinal) {
			if (isFinal) {
				LOGGER.i(String.format(
						"(%s) %s - Delivered: [ ID: %016x ; To: %s ]",
						new Date(SystemClock.millis()),
						hubname,
						bundle.getUniqueID(),
						next
				));
				delivered++;
			} else {
				LOGGER.i(String.format(
						"(%s) %s - Relayed: [ ID: %016x ; To: %s ]",
						new Date(SystemClock.millis()),
						hubname,
						bundle.getUniqueID(),
						next
				));
				
				relayed++;
			}

			payloadSent += bundle.getPayloadLength();
			dataSent += bundle.getDataLength();
			sent++;
		}
		
		public void onTransferStarted(Bundle bundle, EID next) {
			LOGGER.i(String.format(
					"(%s) %s - TransferStarted: [ ID: %016x ; To: %s ]",
					new Date(SystemClock.millis()),
					hubname,
					bundle.getUniqueID(),
					next
			));
		}
		
		public void onTransferAborted(Bundle bundle, EID next) {
			LOGGER.i(String.format(
					"(%s) %s - TransferAborted: [ ID: %016x ; To: %s ]",
					new Date(SystemClock.millis()),
					hubname,
					bundle.getUniqueID(),
					next
			));
		}

		public void onReceived(Bundle bundle, EID prev) {
			final long uniqueID = bundle.getUniqueID();
			LOGGER.i(String.format(
					"(%s) %s - Received: [ ID: %016x ; From: %s ]",
					new Date(SystemClock.millis()),
					hubname,
					uniqueID,
					prev
			));
			
			if (receivedTime.containsKey(uniqueID)) {
				LOGGER.w(String.format(
						"Bundle %016x already was received [IGNORING]",
						uniqueID
				));
				return;
			}

			receivedTime.put(uniqueID, SystemClock.millis());
			payloadReceived += bundle.getPayloadLength();
			dataReceived += bundle.getDataLength();
			received++; 
		}

		public void onCreation(Bundle bundle) {
			final long uniqueID = bundle.getUniqueID();
			LOGGER.i(String.format(
					"(%s) %s - Created: [ ID: %016x ; Destination: %s ]",
					new Date(SystemClock.millis()),
					hubname,
					uniqueID,
					bundle.getDestination()
			));
			
			created++;
		}

		public void onDeleted(Bundle bundle, boolean dropped) {
			final long uniqueID = bundle.getUniqueID();
			
			if (dropped) {
				LOGGER.i(String.format(
						"(%s) %s - Dropped: [ ID: %016x ]",
						new Date(SystemClock.millis()),
						hubname,
						uniqueID,
						bundle.getDestination()
				));
			} else {
				final Long t = receivedTime.remove(uniqueID);
				LOGGER.i(String.format(
						"(%s) %s - Deleted: [ ID: %016x ; BufferTime: %s ]",
						new Date(SystemClock.millis()),
						hubname,
						uniqueID,
						bundle.getDestination(),
						(t == null) ? "unknow" : SystemClock.millis() - t
				));
			}
		}
		
		private String getStatus(Date now) {
			return String.format(
					"(%s) %s [ Number: %d / (%d %d %d) ; Payload: %d / %d ; Data: %d / %d ; Overhead: %d / %d ; Created: %d ]",
					now,
					hubname,
					getReceivedBundles(),
					getDeliveredBundles(),
					getRelayedBundles(),
					getSentBundles(),
					getReceivedPayload(),
					getSentPayload(),
					getReceivedData(),
					getSentData(),
					getReceivedOverhead(),
					getSentOverhead(),
					getCreated()
			);
		}
	}
	
	
	public static class DLifeHub {
		private OnChangeListener<SocialInformation> listener;
		
		private DLifeHub() { }
		
		public void setListener(OnChangeListener<SocialInformation> listener) {
			this.listener = listener;
		}
		
		public void update(SocialInformation sInfo) {
			if (listener == null)
				return;
			
			listener.onChanged(sInfo);
		}
	}
	
	
	public static class ProphetHub {
		private OnChangeListener<SocialInformation> listener;
		
		private ProphetHub() { }
		
		public void setListener(OnChangeListener<SocialInformation> listener) {
			this.listener = listener;
		}
		
		public void update(SocialInformation sInfo) {
			if (listener == null)
				return;
			
			listener.onChanged(sInfo);
		}
	}
	
	
	public static class BluetoothHub {
		private final List<String> devicesNear;
		
		private BluetoothHub() {
			devicesNear = new ArrayList<String>();
		}
		
		public void removeDeviceNear(String device) {
			devicesNear.remove(device);
		}
		
		public void addDeviceNear(String device) {
			devicesNear.add(device);
		}
	}
	
	
	public static interface OnChangeListener<T> {
		public void onChanged(T data);
	}
	
	

	@SuppressWarnings("unused")
	public static class InfoLogger extends PeriodicEvent {
		private final static Logger LOGGER = new Logger("InfoLogger");
		private boolean log_prophet;
		private boolean log_dlife;
		
		public InfoLogger(long interval, TimeUnit unit) {
			super(interval, unit);
			
			this.log_prophet = false;
			this.log_dlife = false;
		}
		
		public void enableProphetRouter() {
			log_prophet = true;
		}
		
		public void enableDLifeRouter() {
			log_dlife = true;
		}

		@Override
		protected void onEvent() {
			final Date now = new Date(SystemClock.millis());

			LOGGER.i(DATA_BUNDLE.getStatus(now));
			LOGGER.i(META_BUNDLE.getStatus(now));
			LOGGER.i(BUNDLE.getStatus(now));
			
			TrafficMeter meter;

			meter = InformationHub.COMPRESSED_CONVERGENCE_LAYER_METER;
			final int cl_received_compressed = meter.getTotalReceived();
			final int cl_sent_compressed = meter.getTotalSent();

			meter = InformationHub.CONVERGENCE_LAYER_METER;
			final int cl_received_normal = meter.getTotalReceived();
			final int cl_sent_normal = meter.getTotalSent();

			LOGGER.i(String.format(
					"(%s) CL-Traffic [ Normal: %d / %d ; Compressed: %d / %d ; Ratio: %.2f / %.2f ]",
					now,
					cl_received_normal,
					cl_sent_normal,
					cl_received_compressed,
					cl_sent_compressed,
					(cl_received_compressed * 100f) / cl_received_normal,
					(cl_sent_compressed * 100f) / cl_sent_normal
			));
			
			meter = InformationHub.DISCOVERY_METER;
			LOGGER.i(String.format(
					"(%s) Discovery-Traffic [ %d / %d ]",
					now,
					meter.getTotalReceived(),
					meter.getTotalSent()
			));
			
			LOGGER.i(String.format(
					"(%s) Storage [ %d ]",
					BPAgent.getStorageAvailable()
			));
			
//			if (log_dlife) {
//				LOGGER.i(String.format(
//						"(%s) DLife [ %d / %d ]",
//						now,
//						meter.getTotalReceived(),
//						meter.getTotalSent()
//				));
//			}
		}
	}
}
