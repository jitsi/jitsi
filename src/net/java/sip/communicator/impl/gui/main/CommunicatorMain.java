package net.java.sip.communicator.impl.gui.main;

import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;

import net.java.sip.communicator.impl.gui.main.utils.Constants;

import com.l2fprod.gui.plaf.skin.Skin;
import com.l2fprod.gui.plaf.skin.SkinLookAndFeel;

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
	    
	    //TODO: To be removed when the contact list service is ready
	    ContactList clist = new ContactList();
	    
	    ContactItem citem1 = new ContactItem("Ivancho");
	    ContactItem citem2 = new ContactItem("Traiancho");
	    ContactItem citem3 = new ContactItem("Glupancho");
	    
	    citem1.setUserIcon(new ImageIcon(Constants.USER_ONLINE_ICON));
	    citem2.setUserIcon(new ImageIcon(Constants.USER_ONLINE_ICON));
	    citem3.setUserIcon(new ImageIcon(Constants.USER_ONLINE_ICON));
	    
	    citem1.setPhoto(Constants.DEFAULT_CHAT_USER_PHOTO);
	    citem2.setPhoto(Constants.DEFAULT_CHAT_USER_PHOTO);
	    citem3.setPhoto(Constants.DEFAULT_CHAT_USER_PHOTO);
	    
	    clist.addContact(citem1);
		clist.addContact(citem2);
		clist.addContact(citem3);
		
		User user = new User();
		
		user.setName("Yana");
		user.setProtocols(new String[]{"SIP", "ICQ", "MSN"});
		
		//////////////////////////////////////////////////////////////////////
		
	    MainFrame mainFrame = new MainFrame(clist, user);
	    	    
	      
	    //In order to have the same icon when using option panes
	    JOptionPane.getRootFrame().setIconImage(Constants.SIP_LOGO);
	    	    
	    mainFrame.pack();
	    
	    mainFrame.setVisible(true);	    			
	}
}
