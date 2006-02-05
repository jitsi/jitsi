/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.FlowLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.customcontrols.SelectorBox;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.SelectorBoxItem;

public class StatusPanel extends JPanel {

	private String[] userProtocols;

	public StatusPanel(String[] userProtocols) {

		this.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));

		this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
				Constants.CONTACTPANEL_MOVER_START_COLOR));

		this.userProtocols = userProtocols;

		this.init();
	}

	private void init() {

		for (int i = 0; i < userProtocols.length; i++) {

			ArrayList protocolStatusList = Constants
					.getProtocolStatusIcons(userProtocols[i]);

			//TODO:to change this line!!!!!!!!!!!!
			SelectorBox protocolStatusCombo = new SelectorBox(
					protocolStatusList.toArray(), (SelectorBoxItem) protocolStatusList
							.get(protocolStatusList.size() - 2));

			this.add(protocolStatusCombo);
		}
	}
}