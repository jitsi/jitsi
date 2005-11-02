package net.java.sip.communicator.impl.gui.main;

import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;

import com.l2fprod.gui.plaf.skin.Skin;
import com.l2fprod.gui.plaf.skin.SkinLookAndFeel;
import com.l2fprod.util.OS;

//import examples.demo;

/**
 * @author Yana Stamcheva
 *  
 * Starts the GUI application using the SkinLookAndFeel of l2fprod. 
 */
public class CommunicatorMain {

	public static void main(String[] args){
		
		try {
			//the theme could be passed as a parameter
			if (args.length > 0) {
				String themepack = args[0];
				if (themepack.endsWith(".xml")) {			    	  
					SkinLookAndFeel.setSkin(
							SkinLookAndFeel.loadThemePackDefinition(new File(args[0]).toURL()));
					UIManager.setLookAndFeel("com.l2fprod.gui.plaf.skin.SkinLookAndFeel");
				} else if (themepack.startsWith("class:")) {
					String classname = themepack.substring("class:".length());
					SkinLookAndFeel.setSkin((Skin)Class.forName(classname).newInstance());	
					UIManager.setLookAndFeel("com.l2fprod.gui.plaf.skin.SkinLookAndFeel");
				} else if (themepack.startsWith("theme:")) {
					String classname = themepack.substring("theme:".length());
					MetalTheme theme = (MetalTheme)Class.forName(classname).newInstance();
				    MetalLookAndFeel metal = new MetalLookAndFeel();	
				    MetalLookAndFeel.setCurrentTheme(theme);
				    UIManager.setLookAndFeel(metal);
				} else {
					SkinLookAndFeel.setSkin(SkinLookAndFeel.loadThemePack(args[0]));
					UIManager.setLookAndFeel("com.l2fprod.gui.plaf.skin.SkinLookAndFeel");	
				}
			}
			//the default theme is set if no theme is specified
			else{
				SkinLookAndFeel.setSkin(
						SkinLookAndFeel.loadThemePackDefinition(new File("src/net/java/sip/communicator/impl/gui/themepacks/aquathemepack/skinlf-themepack.xml").toURL()));
				UIManager.setLookAndFeel("com.l2fprod.gui.plaf.skin.SkinLookAndFeel");
			}
				
			//Decorates the frames and dialogs if we are running with jdk1.4 +
			/*
			if (OS.isOneDotFourOrMore()) {
				java.lang.reflect.Method method = JFrame.class.getMethod(
													"setDefaultLookAndFeelDecorated",
													new Class[] { boolean.class });
				method.invoke(null, new Object[] { Boolean.TRUE });

				method = JDialog.class.getMethod(
						"setDefaultLookAndFeelDecorated",
						new Class[] { boolean.class });
				method.invoke(null, new Object[] { Boolean.TRUE });
		    }*/
			
	    } catch (Exception e) { }
	    
	    //Image frameIcon =
	      //new ImageIcon(demo.class.getResource("windowicon.gif")).getImage();
	    // so option pane as same icon as us
	    //JOptionPane.getRootFrame().setIconImage(frameIcon);

	    //TODO: To be removed when the contact list service is ready
	    ContactList clist = new ContactList();
	    
	    clist.addContact(new ContactItem("user1"));
		clist.addContact(new ContactItem("user2"));
		clist.addContact(new ContactItem("user3"));
		
		User user = new User();
		
		user.setProtocols(new String[]{"ICQ", "MSN"});
		
	    MainFrame mainFrame = new MainFrame(clist, user);
	    //mainFrame.setIconImage(frameIcon);
	    	    
	    mainFrame.setTitle("SIP Communicator");
	    mainFrame.pack();
	    
	    mainFrame.setVisible(true);	    			
	}
}
