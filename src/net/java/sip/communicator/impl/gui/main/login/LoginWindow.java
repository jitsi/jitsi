/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.login;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.java.sip.communicator.impl.gui.main.customcontrols.TransparentBackground;
import net.java.sip.communicator.impl.gui.main.customcontrols.TransparentFrameBackground;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.AccountProperties;
import net.java.sip.communicator.service.protocol.ProtocolNames;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

public class LoginWindow extends JDialog 
    implements ActionListener {

	private JLabel uinLabel = new JLabel(Messages.getString("uin"));

	private JLabel passwdLabel = new JLabel(Messages.getString("passwd"));

	private JTextField uinTextField = new JTextField(15);

	private JPasswordField passwdField = new JPasswordField(15);

    private JButton loginButton = new JButton(Messages.getString("login"));
    
    private JButton cancelButton = new JButton(Messages.getString("cancel"));
    
	private JPanel labelsPanel = new JPanel(new GridLayout(0, 1, 5, 5));

	private JPanel textFieldsPanel = new JPanel(new GridLayout(0, 1, 5, 5));

	private JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
    
    private JPanel buttonsPanel 
                            = new JPanel(new FlowLayout(FlowLayout.CENTER));
    
    private TransparentFrameBackground bg;
    
    private AccountManager accountManager;    
   
    LoginManager loginManager;
    
	public LoginWindow(Frame owner){
        
        super(owner);
        
        this.setModal(true);
        
        this.bg = new TransparentFrameBackground(this);
        
        this.bg.setLayout(new FlowLayout(FlowLayout.RIGHT));
        
        this.bg.setBorder(BorderFactory.createEmptyBorder(60, 5, 5, 5));
        
        this.getContentPane().setLayout(new BorderLayout());
        
        this.init();
        
        this.getContentPane().add(bg, BorderLayout.CENTER);
        
        this.pack();
        
        this.setSize(370, 240);
        
        this.setResizable(false);
	}

	private void init() {     
        
        this.setTransparent(true);
        
        this.passwdField.setEchoChar('*');
        
		this.labelsPanel.add(uinLabel);
		this.labelsPanel.add(passwdLabel);

		this.textFieldsPanel.add(uinTextField);
		this.textFieldsPanel.add(passwdField);

        this.buttonsPanel.add(loginButton);
        this.buttonsPanel.add(cancelButton);
        
		this.mainPanel.add(labelsPanel, BorderLayout.WEST);
		this.mainPanel.add(textFieldsPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);        
        
        this.bg.add(mainPanel, BorderLayout.CENTER);
        
        this.loginButton.setName("login");
        this.cancelButton.setName("cancel");
        
        
        this.loginButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
	}

    private void setTransparent(boolean transparent){
        
        this.mainPanel.setOpaque(!transparent);
        this.labelsPanel.setOpaque(!transparent);
        this.textFieldsPanel.setOpaque(!transparent);
        this.buttonsPanel.setOpaque(!transparent);
    }
    
	public void showWindow(){

        this.setWindowLocation();       
        
        this.setVisible(true);
	}
    
    private void setWindowLocation(){
        
        int x = (Toolkit.getDefaultToolkit().getScreenSize().width 
                - this.getWidth()) / 2;
        
        int y = (Toolkit.getDefaultToolkit().getScreenSize().height 
                - this.getHeight()) / 2;
        
        this.setLocation(x, y);
    }
    
    public static void main(String[] args){
        
        LoginWindow login = new LoginWindow(null);
        
        login.showWindow();
    }

    public AccountManager getAccountManager() {
        return accountManager;
    }

    public void actionPerformed(ActionEvent e) {
       
        JButton button = (JButton)e.getSource();
        String buttonName = button.getName();
        
        if(buttonName.equals("login")){
            
            this.loginManager.login(uinTextField.getText(),
                                    new String(passwdField.getPassword()));
            
            this.dispose();
        }
        else{
            this.dispose();
        }
    }

    public LoginManager getLoginManager() {
        return loginManager;
    }

    public void setLoginManager(LoginManager loginManager) {
        this.loginManager = loginManager;
    }
    
}
