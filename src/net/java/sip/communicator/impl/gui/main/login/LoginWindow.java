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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommPasswordField;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommTextField;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.protocol.AccountManager;

public class LoginWindow extends JDialog 
    implements ActionListener {

	private JLabel uinLabel = new JLabel(Messages.getString("uin"));

	private JLabel passwdLabel = new JLabel(Messages.getString("passwd"));
    
    //private JLabel protocolLabel = new JLabel(Messages.getString("protocol"));

	private SIPCommTextField uinTextField = new SIPCommTextField(15);

	private SIPCommPasswordField passwdField = new SIPCommPasswordField(15);
    
    private JComboBox protocolCombo = new JComboBox();

    private JButton loginButton = new JButton(Messages.getString("login"));
    
    private JButton cancelButton = new JButton(Messages.getString("cancel"));
    
	private JPanel labelsPanel = new JPanel(new GridLayout(0, 1, 8, 8));

	private JPanel textFieldsPanel = new JPanel(new GridLayout(0, 1, 8, 8));

	private JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
    
    private JPanel buttonsPanel 
                            = new JPanel(new FlowLayout(FlowLayout.CENTER));
    
    private LoginWindowBackground backgroundPanel = new LoginWindowBackground();
    
    private AccountManager accountManager;    
   
    private LoginManager loginManager;
    
    private MainFrame mainFrame;
    
	public LoginWindow(MainFrame mainFrame){
        
        super(mainFrame);
        
        this.mainFrame = mainFrame;
        
        this.setModal(true);
        
        this.backgroundPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        
        this.backgroundPanel.setBorder(BorderFactory.createEmptyBorder(60, 5, 5, 5));
        
        this.getContentPane().setLayout(new BorderLayout());
        
        this.init();
        
        this.getContentPane().add(backgroundPanel, BorderLayout.CENTER);
        
        this.pack();
        
        this.setSize(370, 240);
        
        this.setResizable(false);
	}

	private void init() {     
        
        this.setTransparent(true);
        
        this.passwdField.setEchoChar('*');
        
        //this.labelsPanel.add(protocolLabel);
		this.labelsPanel.add(uinLabel);
		this.labelsPanel.add(passwdLabel);        

        //this.textFieldsPanel.add(protocolCombo);
		this.textFieldsPanel.add(uinTextField);
		this.textFieldsPanel.add(passwdField);        
        
        this.buttonsPanel.add(loginButton);
        this.buttonsPanel.add(cancelButton);        
        
		this.mainPanel.add(labelsPanel, BorderLayout.WEST);
		this.mainPanel.add(textFieldsPanel, BorderLayout.CENTER);        
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);        
        
        this.backgroundPanel.add(mainPanel, BorderLayout.CENTER);
        
        this.loginButton.setName("login");
        this.cancelButton.setName("cancel");
                
        this.loginButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.getRootPane().setDefaultButton(loginButton);
	}

    private void setTransparent(boolean transparent){
        
        this.mainPanel.setOpaque(!transparent);
        this.labelsPanel.setOpaque(!transparent);
        this.textFieldsPanel.setOpaque(!transparent);
        this.protocolCombo.setOpaque(!transparent);
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
            
            this.mainFrame.getStatusPanel().startConnecting(Constants.ICQ);
            
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
    
    
    private class LoginWindowBackground extends JPanel {

        protected void paintComponent(Graphics g) {
            
            super.paintComponent(g);
            
            AntialiasingManager.activateAntialiasing(g);
            
            Graphics2D g2 = (Graphics2D) g;            
            
            g2.drawImage(ImageLoader.getImage(ImageLoader.LOGIN_WINDOW_LOGO),
                    0, 0, null);
            
            g2.setColor(new Color(255, 255, 255, 100));

            g2.fillRect(0, 0, getWidth(), getHeight());
                
        }
       
    }
}
