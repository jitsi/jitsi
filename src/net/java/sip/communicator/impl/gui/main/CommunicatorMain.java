/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.io.File;
import java.net.MalformedURLException;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;

import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationFrame;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;

import com.l2fprod.gui.plaf.skin.Skin;
import com.l2fprod.gui.plaf.skin.SkinLookAndFeel;

//import examples.demo;

/**
 * @author Yana Stamcheva
 *
 * Starts the GUI application using the SkinLookAndFeel of l2fprod.
 */
public class CommunicatorMain {

	private MainFrame mainFrame;

	//private MetaContactListService contactList;

	public CommunicatorMain(){

		this.setDefaultThemePack();

		ConfigurationFrame 	configFrame = new ConfigurationFrame();

	    mainFrame = new MainFrame(this.getUser(), getContactList());

	    mainFrame.setConfigFrame(configFrame);

	    //In order to have the same icon when using option panes
	    JOptionPane.getRootFrame().setIconImage(ImageLoader.getImage(ImageLoader.SIP_LOGO));
	}

	/**
	 * To be removed after the real contact
	 * list is implemented.
	 */
	public ContactList getContactList(){

		ContactList clist = new ContactList();

	    ContactItem citem1 = new ContactItem("Ivancho");
	    ContactItem citem2 = new ContactItem("Traiancho");
	    ContactItem citem3 = new ContactItem("Glupancho");

	    citem1.setUserIcon(new ImageIcon(ImageLoader.getImage(ImageLoader.USER_ONLINE_ICON)));
	    citem2.setUserIcon(new ImageIcon(ImageLoader.getImage(ImageLoader.USER_ONLINE_ICON)));
	    citem3.setUserIcon(new ImageIcon(ImageLoader.getImage(ImageLoader.USER_ONLINE_ICON)));

	    citem1.setPhoto(ImageLoader.getImage(ImageLoader.DEFAULT_CHAT_USER_PHOTO));
	    citem2.setPhoto(ImageLoader.getImage(ImageLoader.DEFAULT_CHAT_USER_PHOTO));
	    citem3.setPhoto(ImageLoader.getImage(ImageLoader.DEFAULT_CHAT_USER_PHOTO));

	    clist.addContact(citem1);
		clist.addContact(citem2);
		clist.addContact(citem3);

		citem1.setProtocolList(new String[]{"SIP", "ICQ", "MSN"});
		citem2.setProtocolList(new String[]{"ICQ"});
		citem3.setProtocolList(new String[]{"SIP", "ICQ", "MSN"});

		return clist;
	}

	public User getUser(){

		User user = new User();

		user.setName("Yana");
		user.setProtocols(new String[]{"SIP", "ICQ"});

		return user;
	}

	public void setDefaultThemePack(){

		try {
			SkinLookAndFeel.setSkin(
					SkinLookAndFeel.loadThemePackDefinition(new File("src/net/java/sip/communicator/impl/gui/themepacks/aquathemepack/skinlf-themepack.xml").toURL()));


			UIManager.setLookAndFeel("com.l2fprod.gui.plaf.skin.SkinLookAndFeel");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setThemePack(String themePack){

		try {
			if (themePack.endsWith(".xml")) {

				SkinLookAndFeel.setSkin(
							SkinLookAndFeel.loadThemePackDefinition(new File(themePack).toURL()));

				UIManager.setLookAndFeel("com.l2fprod.gui.plaf.skin.SkinLookAndFeel");

			} else if (themePack.startsWith("class:")) {

				String classname = themePack.substring("class:".length());
				SkinLookAndFeel.setSkin((Skin)Class.forName(classname).newInstance());
				UIManager.setLookAndFeel("com.l2fprod.gui.plaf.skin.SkinLookAndFeel");

			} else if (themePack.startsWith("theme:")) {

				String classname = themePack.substring("theme:".length());
				MetalTheme theme = (MetalTheme)Class.forName(classname).newInstance();
			    MetalLookAndFeel metal = new MetalLookAndFeel();
			    MetalLookAndFeel.setCurrentTheme(theme);
			    UIManager.setLookAndFeel(metal);
			} else {

				SkinLookAndFeel.setSkin(SkinLookAndFeel.loadThemePack(themePack));
				UIManager.setLookAndFeel("com.l2fprod.gui.plaf.skin.SkinLookAndFeel");
			}
//			Decorates the frames and dialogs if we are running with jdk1.4 +

			/*
			if (OS.isOneDotFourOrMore()) {
				java.lang.reflect.Method method = JFrame.class.getMethod(
													"setDefaultLookAndFeelDecorated",
													new Class[] { boolean.class });
				method.invoke(null, new Object[] { Boolean.TRUE });

				method = JDialog.class.getMethod(
						"setDefaultLookAndFeelDecorated",
						new Class[] { boolean.class });
				method.invoke(null, neif (args.length > 0) {
		    }*/

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void show(){

		mainFrame.pack();

		mainFrame.setVisible(true);
	}

	public static void main(String args[]){

		CommunicatorMain communicatorMain = new CommunicatorMain();

		communicatorMain.show();
	}

	/*
	public void setContactList(MetaContactListService contactList) {
		this.contactList = contactList;

		this.mainFrame.setContactList(contactList);
	}
	*/
}
