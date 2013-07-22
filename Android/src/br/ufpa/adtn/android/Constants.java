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
package br.ufpa.adtn.android;

public interface Constants {
	/**
	 * Constant value: "br.ufpa.adtn.permission.SEND_BUNDLE"
	 */
	public String PERMISSION_SEND_BUNDLE = "br.ufpa.adtn.permission.SEND_BUNDLE";
	
	/**
	 * Constant value: "br.ufpa.adtn.action.SEND_BUNDLE"
	 */
	public String ACTION_SEND_BUNDLE = "br.ufpa.adtn.action.SEND_BUNDLE";
	
	/**
	 * Type: String
	 * Constant value: "destination"
	 */
	public String EXTRA_DESTINATION = "destination";
	
	/**
	 * Type: Long
	 * Constant value: "lifetime"
	 */
	public String EXTRA_LIFETIME = "lifetime";
	
	/**
	 * Type: Blob
	 * Constant value: "payload"
	 */
	public String EXTRA_PAYLOAD = "payload";
}
