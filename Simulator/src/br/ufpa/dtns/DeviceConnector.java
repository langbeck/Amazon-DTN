package br.ufpa.dtns;

import java.net.SocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.util.Logger.Priority;

public interface DeviceConnector extends Remote {
	public void discovery(String eid, SocketAddress address) throws RemoteException;
	public void addBundle(Bundle bundle) throws RemoteException;
	public void init(Priority priority) throws RemoteException;
	public SocketAddress getAddress() throws RemoteException;
}
