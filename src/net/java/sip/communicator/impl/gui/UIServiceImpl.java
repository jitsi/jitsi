package net.java.sip.communicator.impl.gui;

import net.java.sip.communicator.service.gui.ConfigurationContainer;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

public class UIServiceImpl implements UIService {

	
	public UIServiceImpl(){
		
		
	}
	
	public void registerProvider(ProtocolProviderService provider) {
		// TODO Auto-generated method stub

	}

	public void setVisible(boolean visible) {
		// TODO Auto-generated method stub

	}

	public Call[] getActiveCalls() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getUiLibName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] getSupportedUiLibNames() {
		// TODO Auto-generated methodUIServiceImpl stub
		return null;
	}

	public void addMenuItem(String parent, Object menuItem)
			throws ClassCastException, IllegalArgumentException {
		// TODO Auto-generated method stub

	}

	public void addComponent(Object component, String constraint)
			throws ClassCastException, IllegalArgumentException {
		// TODO Auto-generated method stub

	}

	public void addUserActionListener() {
		// TODO Auto-generated method stub

	}

	public void requestAuthentication(String realm, String userName,
			char[] password) {
		// TODO Auto-generated method stub

	}

	public String getAuthenticationUserName() {
		// TODO Auto-generated method stub
		return null;
	}

	public ConfigurationContainer getConfigurationContainer() {
		
		return null;
	}

}
