package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * @author Yana Stamcheva
 *
 * The MainFrame of the application. 
 */
public class MainFrame extends JFrame{	
	
	private JPanel			mainPanel	= new JPanel(new BorderLayout());
	private JPanel 			menusPanel 	= new JPanel(new BorderLayout());
	private Menu 			menu 		= new Menu();	
	private QuickMenu 		quickMenu 	= new QuickMenu();	
	private CallPanel 		callPanel 	= new CallPanel();	
	private StatusPanel		statusPanel;
	private MainTabbedPane 	tabbedPane;
	
	
	public MainFrame(ContactList clist, User user){	
				
		tabbedPane = new MainTabbedPane(clist);
		statusPanel = new StatusPanel(user.getProtocols());
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setInitialBounds();
		
		this.init();		
	}

	private void init(){		
		this.menusPanel.add(menu, BorderLayout.NORTH);
		this.menusPanel.add(quickMenu, BorderLayout.CENTER);
		
		this.mainPanel.add(tabbedPane, BorderLayout.CENTER);
		this.mainPanel.add(callPanel, BorderLayout.SOUTH);
		
		this.getContentPane().add(menusPanel, BorderLayout.NORTH);		
		this.getContentPane().add(mainPanel, BorderLayout.CENTER);
		this.getContentPane().add(statusPanel, BorderLayout.SOUTH);
	}
	
	private void setInitialBounds(){
		this.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width - MainFrame.WIDTH, 50);
		
		this.getContentPane().setSize(	LookAndFeelConstants.MAINFRAME_WIDTH, 
										LookAndFeelConstants.MAINFRAME_HEIGHT);
			
		this.tabbedPane.setPreferredSize(new Dimension(LookAndFeelConstants.MAINFRAME_WIDTH, 
				LookAndFeelConstants.MAINFRAME_HEIGHT));
	}
}
