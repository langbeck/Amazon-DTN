package br.ufpa.adtn.android.virtual;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import br.ufpa.adtn.core.BPAgent;
import br.ufpa.adtn.core.configuration.SimulationConfiguration;
import br.ufpa.adtn.core.configuration.SimulationConfiguration.ContactInfo;
import br.ufpa.adtn.core.configuration.SimulationConfiguration.DeviceInfo;


public final class BluetoothContactManager {
	private static final String TAG = "BluetoothContactManager";
	
	
	private static class InstanceHolder {
		static final BluetoothContactManager INSTANCE = new BluetoothContactManager(BluetoothAdapter.getDefaultAdapter().getAddress()); 
	}
	
	public static BluetoothContactManager getInstance() {
		return InstanceHolder.INSTANCE;
	}
	
	public static boolean shouldIgnore(String address) {
		return BPAgent.isSimulated() && InstanceHolder.INSTANCE.shoudIgnore(address);
	}
	
	
    private final SimulationConfiguration config;
    private final Set<String> inContact;
    private final Timer timer;

    private BluetoothContactManager(String selfAddress) {
        this.config = SimulationConfiguration.getInstance();
        this.timer = new Timer("ContactManager");
        this.inContact = new HashSet<String>();
        
        selfAddress = selfAddress.replace(":", "");
        if (!selfAddress.matches("^[0-9A-F]{12}$"))
            throw new IllegalArgumentException();

        final DeviceInfo devInfo = config.getInfoByAddress(selfAddress);
        if (devInfo == null)
        	throw new IllegalArgumentException(String.format(
        			"Address %s is not present in configuration.",
        			selfAddress
			));
    	
        final ContactInfo[] contacts = devInfo.getContacts();
        
        final Date now = new Date();
        for (ContactInfo info : contacts) {
            if (info.getEnd().before(now))
                continue;
            
            timer.schedule(new ContactUp(info), info.getStart());
            timer.schedule(new ContactDown(info), info.getEnd());
        }
    }

    public boolean shoudIgnore(String address) {
        synchronized (inContact) {
            return !inContact.contains(address.replace(":", ""));
        }
    }
    
    
    private class ContactDown extends TimerTask {
        private final ContactInfo info;

        public ContactDown(ContactInfo info) {
            this.info = info;
        }

        @Override
        public void run() {
            synchronized (inContact) {
                Log.i(TAG, "Removing " + info.getAlias());
                inContact.remove(info.getAddress());
            }
        }
    }
    
    private class ContactUp extends TimerTask {
        private final ContactInfo info;

        public ContactUp(ContactInfo info) {
            this.info = info;
        }

        @Override
        public void run() {
            synchronized (inContact) {
            	Log.i(TAG, "Registering " + info.getAlias());
                inContact.add(info.getAddress());
            }
        }
    }
}
