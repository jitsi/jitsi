/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.history;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;

public class SearchPanel extends JPanel implements ActionListener {

	private JButton searchButton 
				= new JButton(Messages.getString("search"),
							  new ImageIcon(ImageLoader.getImage(ImageLoader.QUICK_MENU_SEARCH_ICON)));
	
	private JLabel searchLabel = new JLabel(Messages.getString("search") + ": "); 
	
	private JTextField searchTextField = new JTextField(20);
	
	private JRadioButton todayMessagesRadio = new JRadioButton(Messages.getString("today")); 
	
	private JRadioButton yesterdayMessagesRadio = new JRadioButton(Messages.getString("yesterday")); //$NON-NLS-1$
	
	private JRadioButton allMessagesRadio = new JRadioButton(Messages.getString("all"));	 //$NON-NLS-1$
	
	private ButtonGroup radiosGroup = new ButtonGroup();
	
	private JLabel dateLabel = new JLabel(Messages.getString("date") + ": "); //$NON-NLS-1$
	
	private JTextField dateTextField = new JTextField(10);
	
	private JLabel hourLabel = new JLabel(Messages.getString("hour") + ": "); //$NON-NLS-1$
	
	private JTextField hourTextField = new JTextField(10);
	
	private JLabel lastNMessagesLabel = new JLabel(Messages.getString("last") + ": "); //$NON-NLS-1$
	
	private JTextField lastNMessagesTextField = new JTextField(10);
	
	/*
	private SIPCommButton extendedSearchButton = new SIPCommButton
						   			   (Messages.getString("extendedCriteria"),  //$NON-NLS-1$
						   			    Constants.RIGHT_ARROW_ICON,
						   				Constants.RIGHT_ARROW_ROLLOVER_ICON);
	
	private SIPCommButton extendedSearchOpenedButton = new SIPCommButton
								   		(Messages.getString("extendedCriteria"),  //$NON-NLS-1$
								         Constants.BOTTOM_ARROW_ICON,
									     Constants.BOTTOM_ARROW_ROLLOVER_ICON);
	*/
	
	//////////////////////// Panels //////////////////////////////
	
	private JPanel datePanel = new JPanel(new GridLayout(0, 2, 10, 0));
	
	private JPanel dateCenteredPanel 
							 = new JPanel(new FlowLayout(FlowLayout.CENTER));
	
	private JPanel searchPanel = new JPanel(new BorderLayout());
	
	private JPanel detailsPanel = new JPanel(new BorderLayout());
	
	private JPanel detailsLabelsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
	
	private JPanel detailsFieldsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
	
	private JPanel checksPanel = new JPanel(new GridLayout(0, 1));
	
	private JPanel searchButtonPanel 
							= new JPanel(new FlowLayout(FlowLayout.CENTER));
	
	//private JPanel extendedSearchPanel = new JPanel(new BorderLayout());
	
	
	public SearchPanel(){
		super();
		
		this.setBorder(BorderFactory.createTitledBorder(Messages.getString("search"))); //$NON-NLS-1$
		
		this.init();
	}
	
	public void init(){
		
		this.searchPanel.add(searchLabel, BorderLayout.WEST);
		this.searchPanel.add(searchTextField, BorderLayout.CENTER);
				
		this.detailsLabelsPanel.add(dateLabel);
		this.detailsLabelsPanel.add(hourLabel);
		this.detailsLabelsPanel.add(lastNMessagesLabel);
				
		this.detailsFieldsPanel.add(dateTextField);
		this.detailsFieldsPanel.add(hourTextField);
		this.detailsFieldsPanel.add(lastNMessagesTextField);
		
		this.detailsPanel.add(detailsLabelsPanel, BorderLayout.WEST);
		this.detailsPanel.add(detailsFieldsPanel, BorderLayout.CENTER);
		
		this.radiosGroup.add(allMessagesRadio);
		this.radiosGroup.add(todayMessagesRadio);
		this.radiosGroup.add(yesterdayMessagesRadio);
		
		this.checksPanel.add(allMessagesRadio);		
		this.checksPanel.add(todayMessagesRadio);		
		this.checksPanel.add(yesterdayMessagesRadio);		
				
		this.datePanel.add(checksPanel);
		this.datePanel.add(detailsPanel);
		
		this.dateCenteredPanel.add(datePanel);		
		
		this.searchButton.setName("search");
		//this.extendedSearchButton.setName("extendedSearch");
		//this.extendedSearchOpenedButton.setName("extendedSearchOpened");
				
		this.searchButtonPanel.add(searchButton);		
		
		//this.extendedSearchPanel.add(extendedSearchButton, BorderLayout.CENTER);		
		
		this.add(searchPanel, BorderLayout.CENTER);
		this.add(searchButtonPanel, BorderLayout.EAST);
		
		//this.add(extendedSearchPanel, BorderLayout.SOUTH);		
		
		//this.extendedSearchButton.addActionListener(this);
		//this.extendedSearchOpenedButton.addActionListener(this);
		
		this.enableDefaultSearchSettings();
	}
	
	public void paint(Graphics g){
		AntialiasingManager.activateAntialiasing(g);
		
		super.paint(g);	
	}

	public void actionPerformed(ActionEvent e) {
		
		JButton button = (JButton) e.getSource();
		String buttonName = button.getName();
		
		if(buttonName.equalsIgnoreCase("search")){
			
		}
		/*else if(buttonName.equalsIgnoreCase("extendedSearch")){
				
				this.extendedSearchPanel.removeAll();
				this.extendedSearchPanel.add(extendedSearchOpenedButton, 
											BorderLayout.NORTH);
				this.extendedSearchPanel.add(dateCenteredPanel, BorderLayout.CENTER);
				
				this.getParent().validate();
		}	
		else if(buttonName.equalsIgnoreCase("extendedSearchOpened")){
			
			this.extendedSearchPanel.removeAll();
			this.extendedSearchPanel.add(extendedSearchButton, 
										BorderLayout.CENTER);				
			
			this.getParent().validate();
		}*/
	}	
	
	private void enableDefaultSearchSettings(){
		
		this.allMessagesRadio.setSelected(true);
	}
}
