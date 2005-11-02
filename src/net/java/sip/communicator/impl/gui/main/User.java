package net.java.sip.communicator.impl.gui.main;

public class User {
	private String[] userProtocols;
	
	public void setProtocols(String[] userProtocols){
		this.userProtocols = userProtocols;
	}
	
	public String[] getProtocols(){
		return this.userProtocols;
	}
}
