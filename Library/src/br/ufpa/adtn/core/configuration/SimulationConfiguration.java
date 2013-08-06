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
package br.ufpa.adtn.core.configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.ufpa.adtn.core.ParsingException;
import br.ufpa.adtn.util.Logger;

/**
 * Base class to define configuration parameters of Simulation
 * Note: {@code BPAgent} must be in simulated mode.
 * 
 * @author Dórian Langbeck
 */
public class SimulationConfiguration {
	private static final Logger LOGGER = new Logger("SimulationConfiguration");

    private static final SimpleDateFormat DATE_PARSER;
    private static final SimpleDateFormat TIME_PARSER;
    private static final Pattern PATTERN_REGISTER;
    private static final Pattern PATTERN_CONTACT;
    private static final Pattern PATTERN_DEFINE;
    private static final Pattern PATTERN_BEGIN;
    private static final Pattern PATTERN_LINE;

    static {
        PATTERN_CONTACT = Pattern.compile("^(\\d+):(\\d{2}):(\\d{2})\\s+(\\d+):(\\d{2}):(\\d{2})\\s+(\\w+)$");
        PATTERN_REGISTER = Pattern.compile("^(([0-9A-F]{2}){6})\\s+(\\w+)$");
        PATTERN_DEFINE = Pattern.compile("^(\\w+)\\s+([\\w\\s\\-:.]+)$");
        PATTERN_LINE = Pattern.compile("^\\s*(\\w+)(\\s+(.*))?$");
        PATTERN_BEGIN = Pattern.compile("^(\\w+)$");

        DATE_PARSER = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US);
        TIME_PARSER = new SimpleDateFormat("HH:mm:ss", Locale.US);
        TIME_PARSER.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
    

    private final Map<String, DeviceInfo> addressRef;
    private final Map<String, DeviceInfo> aliasesRef;
    private DeviceInfo currentDevice;
    private double timescale;
    private boolean inBlock;
    private boolean isStart;
    private boolean ignore;
    private Date offset;
    private Date start;

    public SimulationConfiguration(Reader source) throws ParsingException, IOException {
    	this.addressRef = new HashMap<String, DeviceInfo>();
        this.aliasesRef = new HashMap<String, DeviceInfo>();
        this.currentDevice = null;
        this.inBlock = false;
        this.isStart = true;
        this.ignore = false;
        this.timescale = 1;
        this.offset = null;
        this.start = null;

        final BufferedReader reader = new BufferedReader(source);
        for (int ln = 1;; ln++) {
            final String rline = reader.readLine();
            if (rline == null)
                break;
            
            final String line = rline.trim();
            if (line.equals("") || line.charAt(0) == '#')
                continue;

            final Matcher lineMatcher = PATTERN_LINE.matcher(line);
            if (lineMatcher.find()) {
                final String args = lineMatcher.group(3);
                final String cmd = lineMatcher.group(1);
                
                if (cmd.equals("register")) {
                    final Matcher matcher = PATTERN_REGISTER.matcher(args);
                    if (!matcher.find())
                        throw new ParsingException("Invalid arguments for command register in line" + ln);
                    
                    processRegister(matcher.group(1), matcher.group(3));
                } else if (cmd.equals("define")) {
                    final Matcher matcher = PATTERN_DEFINE.matcher(args);
                    if (!matcher.find())
                        throw new ParsingException("Invalid arguments for command define in line" + ln);
                    
                    processDefine(matcher.group(1), matcher.group(2));
                } else if (cmd.equals("contact")) {
                    final Matcher matcher = PATTERN_CONTACT.matcher(args);
                    if (!matcher.find())
                        throw new ParsingException("Invalid arguments for command contact in line " + ln);
                    
                    final int smin = Integer.parseInt(matcher.group(2));
                    final int ssec = Integer.parseInt(matcher.group(3));
                    final int emin = Integer.parseInt(matcher.group(5));
                    final int esec = Integer.parseInt(matcher.group(6));
                    
                    if (smin > 60 || ssec > 60 || emin > 60 || esec > 60)
                        throw new ParsingException("Invalid time in line " + ln);
                    
                    processContact(
                            (Integer.parseInt(matcher.group(1)) * 3600) + (smin * 60) + ssec,
                            (Integer.parseInt(matcher.group(4)) * 3600) + (emin * 60) + esec,
                            matcher.group(7)
                    );
                } else if (cmd.equals("begin")) {
                    final Matcher matcher = PATTERN_BEGIN.matcher(args);
                    if (!matcher.find())
                        throw new ParsingException("Invalid arguments for command begin in line " + ln);
                    
                    processBegin(matcher.group(1));
                } else if (cmd.equals("end")) {
                    processEnd();
                } else {
                    throw new ParsingException(String.format(
                    		"Unknow command in line %d: %s",
                			ln, cmd
        			));
                }
            }
        }
        
        if (timescale <= 0)
            throw new ParsingException("Timescale can not be lower than zero");
        
        if (start == null)
            throw new ParsingException("Start time not defined");
    }

    public Collection<String> getAddresses() {
    	return Collections.unmodifiableCollection(addressRef.keySet());
    }

    public Collection<String> getAliases() {
    	return Collections.unmodifiableCollection(aliasesRef.keySet());
    }
    
    public DeviceInfo getInfoByAddress(String address) {
        return addressRef.get(address);
    }
    
    public DeviceInfo getInfoByAlias(String alias) {
        return aliasesRef.get(alias);
    }
    
    public double getTimescale() {
        return timescale;
    }
    
    public Date getStart() {
        return start;
    }
    
    private void processRegister(String address, String alias) throws ParsingException {
        if (!isStart)
            throw new ParsingException("Register command after start of begin-end blocks declaration");
        
        if (addressRef.containsKey(address))
            throw new ParsingException("Device address already defined");
        
        if (aliasesRef.containsKey(alias))
            throw new ParsingException("Device alias already defined");
        
        final DeviceInfo info = new DeviceInfo(alias, address);
        addressRef.put(address, info);
        aliasesRef.put(alias, info);
    }

    private void processDefine(String key, String value) throws ParsingException {
        if (!isStart)
            throw new ParsingException("Define command after start of begin-end blocks declaration");
        
        if (key.equals("scale")) {
            timescale = Float.parseFloat(value);
        } else if (key.equals("start")) {
            try {
				start = DATE_PARSER.parse(value);
			} catch (ParseException e) {
				throw new ParsingException(e.getMessage());
			}
        } else if (key.equals("offset")) {
        	try {
				offset = TIME_PARSER.parse(value);
			} catch (ParseException e) {
				throw new ParsingException(e.getMessage());
			}
        } else {
            throw new ParsingException("Invalid define requested: " + key);
        }
    }
    
    private void processContact(long start, long end, String alias) throws ParsingException {
        if (!inBlock)
            throw new ParsingException("Contact command used out of a begin-end block");
        
        if (ignore)
        	return;
        
        currentDevice.addContact(alias, start, end);
    }
    
    private void processBegin(String device) throws ParsingException {
        if (inBlock)
            throw new ParsingException("Start of a new block while another block is open");
        
        final DeviceInfo info = aliasesRef.get(device);
        if (info == null) {
        	LOGGER.w(String.format("Alias \"%s\" not declared [IGNORING]", device));
        	ignore = true;
        }
        
        if (isStart) {
        	if (start == null) {
        		final Date now = new Date(System.currentTimeMillis() + 10000);
        		LOGGER.w("No start time defined. Using 10 seconds ahead: " + now);
        		start = now;
        	}
        	
        	if (offset == null)
        		offset = new Date(0);
        	
            isStart = false;
        }
        
        currentDevice = info;
        inBlock = true;
    }
    
    private void processEnd() throws ParsingException {
        if (!inBlock)
            throw new ParsingException("End of block without have been started");
        
        if (ignore)
        	ignore = false;
        
        currentDevice = null;
        inBlock = false;
    }
    
    
    public class ContactInfo implements Comparable<ContactInfo> {
        private final String withAddress;
        private final String withAlias;
        private final Date dateStart;
        private final Date dateEnd;

        private ContactInfo(String address, String alias, long ts, long te) {
            if (ts >= te)
                throw new IllegalArgumentException("End time must be greater than start time");

            final long _offset = offset.getTime();
            final long _start = start.getTime();
            
            this.dateStart = new Date((long) ((ts * 1000 - _offset) * timescale) + _start);
            this.dateEnd = new Date((long) ((te * 1000 - _offset) * timescale) + _start);
            this.withAddress = address;
            this.withAlias = alias;
        }

        public int compareTo(ContactInfo o) {
            return dateStart.compareTo(o.dateStart);
        }

        public String getAddress() {
            return withAddress;
        }
        
        public String getAlias() {
            return withAlias;
        }

        public Date getStart() {
            return dateStart;
        }

        public Date getEnd() {
            return dateEnd;
        }

        @Override
        public String toString() {
            return String.format(
                    "ContactInfo[with=%s,start=%s,end=%s]",
                    withAlias,
                    dateStart,
                    dateEnd
            );
        }
    }
    
    public class DeviceInfo {
        private final List<ContactInfo> contacts;
        private ContactInfo[] contactData;
        private final String address;
        private final String alias;

        private DeviceInfo(String alias, String address) {
            this.contacts = new ArrayList<ContactInfo>();
            this.contactData = null;
            this.address = address;
            this.alias = alias;
        }
        
        private void addContact(String alias, long start, long end) {
            contacts.add(new ContactInfo(
                    aliasesRef.get(alias).address,
                    alias,
                    start,
                    end
            ));
        }
        
        public ContactInfo[] getContacts() {
            if (contactData == null) {
                contactData = contacts.toArray(new ContactInfo[0]);
                Arrays.sort(contactData);
            }
            return contactData;
        }

        public String getAddress() {
            return address;
        }

        public String getAlias() {
            return alias;
        }
    }
}
