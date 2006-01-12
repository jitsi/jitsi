package net.java.sip.communicator.impl.gui.main;

public class User {
	
	private String[] userProtocols;	

	private String name;
	
	public void setProtocols(String[] userProtocols){
		this.userProtocols = userProtocols;
	}
	
	public String[] getProtocols(){
		return this.userProtocols;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
}
