package br.ufpa.dtns;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegisterNotifier extends Remote {
	
	public static class Register implements RegisterNotifier, Serializable {
		private static final long serialVersionUID = 6044977775251049129L;
		
		
		private String name;
		
		public Register() throws RemoteException { }
		
		@Override
		public void register(String name) throws RemoteException {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
	}
	
	public void register(String name) throws RemoteException;
}
