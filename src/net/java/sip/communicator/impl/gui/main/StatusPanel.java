package net.java.sip.communicator.impl.gui.main;

import java.awt.FlowLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.customcontrols.StatusSelectorBox;

public class StatusPanel extends JPanel {

	private String[] userProtocols;

	public StatusPanel(String[] userProtocols) {

		this.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
				
		this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
				LookAndFeelConstants.CONTACTPANEL_MOVER_START_COLOR));

		this.userProtocols = userProtocols;

		this.init();
	}

	private void init() {

		for (int i = 0; i < userProtocols.length; i++) {

			ArrayList protocolStatusList = LookAndFeelConstants
					.getProtocolIcons(userProtocols[i]);

			StatusSelectorBox protocolStatusCombo = new StatusSelectorBox(
					protocolStatusList.toArray(), (Status)protocolStatusList.get(0));
									
			this.add(protocolStatusCombo);
		}
	}
}