/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform.zrtp;

import java.awt.Color;
import java.awt.event.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.media.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.impl.media.*;

import javax.swing.*;

import gnu.java.zrtp.*;
import org.osgi.framework.*;

/**
 * The user callback class used by the ZRTP4J.
 * This class provides means to communicate events to the user through
 * GUI elements, and also allows the user to control the ZRTP activation
 * and deactivation.
 * 
 * @author Emanuel Onica 
 *
 */
public class SCCallback 
	extends ZrtpUserCallback
	implements ActionListener
{
	private static final Logger logger
    = Logger.getLogger(SCCallback.class);
    
	/**
	 * The UI Service needed to provide the means to display
	 * popup dialogs and other possible informative elements
	 */
    private UIService uiService;
    
    /**
     * The popup dialog used to display announcements to the user
     */
    private PopupDialog popupDialog;
    
    /**
     * The label from the ZRTP GUI plugin used to display the SAS
     */
    private JLabel zrtpLabel = null;
    
    /**
     * The button from the ZRTP GUI plugin used to activate and
     * deactivate the secure state of the call
     */
    private JButton zrtpButton = null;
    
    /**
     * The panel from the ZRTP GUI plugin holding the ZRTP related
     * GUI components
     */
    private JPanel zrtpPanel = null;
    
    /**
     * Flags needed for specific GUI setting indicating if the call
     * securing change was issued by a GoClear/GoSecure request from the other peer 
     */
    private boolean gcgsByPeerFlag = true; 
    private boolean firstSecuring = true;
    
    ActionListener [] listeners;
    
    private CallSession callSession = null;
    
    /**
     * The class constructor.
     * Gets a reference to the ZRTP GUI plugin and initializes the 
     * ZRTP GUI component members.
     */
    public SCCallback(CallSession callSession)
    {
        BundleContext bc = MediaActivator.getBundleContext();
        
        ServiceReference uiServiceRef = 
            bc.getServiceReference(UIService.class.getName());
        
        uiService = (UIService)bc.getService(uiServiceRef);
        
        popupDialog = uiService.getPopupDialog();
        
        ServiceReference[] serRefs = null;
        
        this.callSession = callSession;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_MAIN_TOOL_BAR.getID()+")";

        zrtpButton = (JButton) callSession.getCall().getSecureGUIComponent("secureButton");
        zrtpLabel = (JLabel) callSession.getCall().getSecureGUIComponent("secureLabel");
    }

    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#init()
     */
    public void init()
    {
    	gcgsByPeerFlag = true;
    	firstSecuring = true;
    	
    	listeners = zrtpButton.getActionListeners();
    	
    	for (int i=0; i<listeners.length; i++)
    		zrtpButton.removeActionListener(listeners[i]);
    	
    	zrtpButton.addActionListener(this);
    	
    	zrtpButton.setActionCommand("firstZRTPTrigger");
    	
    	logger.info("ZRTP engine Initialized");
    	System.err.println("ZRTP engine Initialized");
    }
    
    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#secureOn(java.lang.String)
     */
    public void secureOn(String cipher) 
    {
        logger.info("Cipher: " + cipher);
        System.err.println("Cipher: " + cipher);
               
        zrtpButton.setActionCommand("defaultZRTPAction");
        zrtpButton.setToolTipText(SCCallbackResources.TOGGLE_OFF_SECURITY);
    }
    
    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#showSAS(java.lang.String, boolean)
     */
    public void showSAS(String sas, boolean verified) 
    {
        logger.info("SAS: " + sas);
        System.err.println("SAS: " + sas);
        
        zrtpLabel.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        zrtpLabel.setText(SCCallbackResources.SAS_SECURED_MESSAGE+sas);
        zrtpLabel.setToolTipText(SCCallbackResources.SAS_SECURED_TOOLTIP);
        showTooltip();
        
        if (!firstSecuring && gcgsByPeerFlag)
        {
        	zrtpButton.setActionCommand("goSecureRemoteToggle");
            zrtpButton.doClick();
            zrtpButton.setActionCommand("defaultZRTPAction");
        }
        else
        if (!firstSecuring)	
        {
        	// the case of call securing initiated by peer after a previous GoClear
        	// - be sure to reset the flag
        	gcgsByPeerFlag = true;
        }
        
        firstSecuring = false;
    }

    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#showMessage(gnu.java.zrtp.ZrtpCodes.MessageSeverity, java.util.EnumSet)
     */
    public void showMessage(ZrtpCodes.MessageSeverity sev, EnumSet<?> subCode) 
    {
        Iterator ii = subCode.iterator();
        Object msgCode = ii.next();
        logger.info("Show message sub code: " + msgCode);
        System.err.println("Show message sub code: " + msgCode);
        
        if (msgCode.equals(CallSessionImpl.ZRTPCustomInfoCodes.ZRTPNotEnabledByUser))
        {
        	zrtpLabel.setBorder(BorderFactory.createLineBorder(Color.RED));
            zrtpLabel.setText(SCCallbackResources.SAS_NOT_SECURED_MESSAGE);
            zrtpLabel.setToolTipText(SCCallbackResources.SAS_NOT_SECURED_TOOLTIP);
            showTooltip();
        }
        
        if (msgCode.equals(CallSessionImpl.ZRTPCustomInfoCodes.ZRTPDisabledByCallEnd))
        {
            zrtpLabel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
            zrtpLabel.setText(SCCallbackResources.DEFAULT_SAS_MESSAGE);
            zrtpLabel.setToolTipText(SCCallbackResources.DEFAULT_SAS_TOOLTIP);
            
            String command = zrtpButton.getActionCommand();
            if (!command.equals("startSecureMode"))
            {
            	zrtpButton.removeActionListener(this);
            
            	for (int i=0; i<listeners.length; i++)
            		zrtpButton.addActionListener(listeners[i]);
            
            	zrtpButton.setActionCommand("startSecureMode");
            }
        }
        
        if (msgCode.equals(CallSessionImpl.ZRTPCustomInfoCodes.ZRTPEngineInitFailure))
        {
        	zrtpLabel.setBorder(BorderFactory.createLineBorder(Color.RED));
        	zrtpLabel.setText(SCCallbackResources.ENGINE_FAIL_SAS_MESSAGE);
        	zrtpLabel.setToolTipText(SCCallbackResources.ENGINE_FAIL_SAS_TOOLTIP);
        	zrtpButton.setActionCommand("zrtpInitFail");
        	zrtpButton.doClick();
        	showTooltip();
        }
        
    }

    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#zrtpNegotiationFailed(gnu.java.zrtp.ZrtpCodes.MessageSeverity, java.util.EnumSet)
     */
    public void zrtpNegotiationFailed(ZrtpCodes.MessageSeverity severity,
                EnumSet<?> subCode) 
    {
        Iterator ii = subCode.iterator();
        Object msgCode = ii.next();
        logger.warn("Negotiation failed sub code: " + msgCode);
        System.err.println("Negotiation failed sub code: " + msgCode);
        
        zrtpLabel.setBorder(BorderFactory.createLineBorder(Color.RED));
        zrtpLabel.setText(SCCallbackResources.SAS_NOT_SECURED_MESSAGE);
        zrtpLabel.setToolTipText(SCCallbackResources.SAS_SECURING_FAIL_TOOLTIP);
        
        String prevCommand = zrtpButton.getActionCommand();
        zrtpButton.setActionCommand("revertToUnsecured");
        zrtpButton.doClick();
        zrtpButton.setActionCommand(prevCommand);
        
        showTooltip();
    }
    
    public void goClearProcedureFailed(ZrtpCodes.MessageSeverity severity,
            EnumSet<?> subCode, boolean maintainSecurity) 
    {
    	Iterator ii = subCode.iterator();
    	Object msgCode = ii.next();
    	logger.warn("ZRTP negotiation failed sub code: " + msgCode);
    	System.err.println("ZRTP negotiation failed sub code: " + msgCode);
    	
    	String prevCommand = zrtpButton.getActionCommand();
    	
    	if (msgCode.equals(ZrtpCodes.WarningCodes.WarningGoClearRequestInvalid))
        {
        	zrtpButton.setActionCommand("revertFromAllowClearFailure");
        	zrtpButton.doClick();
        }
    	else
    	if (maintainSecurity == false)
    	{
    		zrtpLabel.setBorder(BorderFactory.createLineBorder(Color.RED));
    		zrtpLabel.setText(SCCallbackResources.SAS_NOT_SECURED_MESSAGE);
    		zrtpLabel.setToolTipText(SCCallbackResources.SAS_SECURING_FAIL_TOOLTIP);
        	
    		zrtpButton.setActionCommand("revertToUnsecured");
    		zrtpButton.doClick();
    		zrtpButton.setActionCommand(prevCommand);
    	}
    	else
    	if (maintainSecurity == true)
    	{
    		zrtpButton.setActionCommand("revertToSecured");
    		zrtpButton.doClick();
    		zrtpButton.setActionCommand(prevCommand);
    	}
    	
    	showTooltip();
    }

    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#secureOff()
     */
    public void secureOff() 
    {
        logger.info("Security off");
        System.err.println("Security off");
        
        // this is the case of locally GoClear request
        // be sure to reset the flag after handling it
        if (!gcgsByPeerFlag) 
        {
        	zrtpLabel.setBorder(BorderFactory.createLineBorder(Color.RED));
        	zrtpLabel.setText(SCCallbackResources.SAS_NOT_SECURED_MESSAGE);
        	zrtpLabel.setToolTipText(SCCallbackResources.SAS_UNSECURED_AT_REQUEST_TOOLTIP);
        	showTooltip();
        	gcgsByPeerFlag = true;
        }
    }

    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#zrtpNotSuppOther()
     */
    public void zrtpNotSuppOther() 
    {
        logger.info("ZRTP not supported");
        System.err.println("ZRTP not supported");
        
    	zrtpButton.setToolTipText(SCCallbackResources.PEER_UNSUPORTED_SECURITY);
        zrtpLabel.setBorder(BorderFactory.createLineBorder(Color.RED));
        zrtpLabel.setText(SCCallbackResources.SAS_NOT_SECURED_MESSAGE);
        zrtpLabel.setToolTipText(SCCallbackResources.SAS_PEER_UNSUPORTED_TOOLTIP);
        showTooltip();
    }
    
    /*
     * (non-Javadoc)
     * @see gnu.java.zrtp.ZrtpUserCallback#confirmGoClear()
     */
    public void confirmGoClear()
    {
    	logger.info("GoClear confirmation requested");
    	System.err.println("GoClear confirmation requested");
    	
    	popupDialog.showMessagePopupDialog(SCCallbackResources.PEER_TOGGLED_SECURITY_OFF_MESSAGE, 
    									   SCCallbackResources.PEER_TOGGLED_SECURITY_OFF_CAPTION, 
    									   PopupDialog.INFORMATION_MESSAGE);
    	zrtpLabel.setBorder(BorderFactory.createLineBorder(Color.RED));
        zrtpLabel.setText(SCCallbackResources.SAS_NOT_SECURED_MESSAGE);
        zrtpLabel.setToolTipText(SCCallbackResources.SAS_UNSECURED_AT_PEER_REQUEST_TOOLTIP);
        zrtpButton.setActionCommand("goClearRemoteToggle");
        zrtpButton.doClick();
        zrtpButton.setActionCommand("defaultZRTPAction");
        showTooltip();
    }

	public void setGCGSByPeerFlag(boolean gcgsByPeerFlag) 
	{
		this.gcgsByPeerFlag = gcgsByPeerFlag;
	}
	
	/**
	 * Force tooltip show function
	 * (Not really necessary but could draw user's attention on some events;
	 *  Seems no version from below works however...
	 *  TODO: Search some other way - or better see what's wrong with the ones found)
	 * 
	 */
	public void showTooltip()
	{
		//version 1
		/*ToolTipManager.sharedInstance().mouseMoved(
    	        new MouseEvent(zrtpLabel, 0, 0, 0,
    	                	   0, 0, 
    	                	   0, false));*/
		
		//version 2
		/*Action toolTipAction = component.getActionMap().get("postTip");
		if (toolTipAction != null)
		{
			ActionEvent postTip = new ActionEvent(component, ActionEvent.ACTION_PERFORMED, "");
			toolTipAction.actionPerformed( postTip );
		}*/

		//version 3
		zrtpLabel.dispatchEvent(new KeyEvent
				(
					zrtpLabel,
					KeyEvent.KEY_PRESSED,
					0,
					KeyEvent.CTRL_MASK,
					KeyEvent.VK_F1,
					KeyEvent.CHAR_UNDEFINED
				));
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
    {
		String command = e.getActionCommand();
		
		
		if (command.equals("defaultZRTPAction"))
        {        	
			if(callSession.getSecureCommunicationStatus())
            {
            	updateSecureButton(false);
            		
            	callSession.
            	setSecureCommunicationStatus(false,
            								 OperationSetSecuredTelephony.
            								 SecureStatusChangeSource.
            								 SECURE_STATUS_CHANGE_BY_LOCAL);
            }
            else
            {
            	updateSecureButton(true);
            		
            	callSession.
            	setSecureCommunicationStatus(true,
            								 OperationSetSecuredTelephony.
            								 SecureStatusChangeSource.
            							     SECURE_STATUS_CHANGE_BY_LOCAL);
            }
        }
		else
		if (command.equals("firstZRTPTrigger"))
        {
        	ToolTipManager.sharedInstance().mouseMoved(
        	        new MouseEvent(zrtpButton, 0, 0, 0,
        	                	   0, 0, 
        	                	   0, false));
        }
        else 
        if (command.equals("revertFromAllowClearFailure"))
        {
        	updateSecureButton(true);
        	
        	callSession.
			setSecureCommunicationStatus(true,
										 OperationSetSecuredTelephony.
										 SecureStatusChangeSource.
										 SECURE_STATUS_REVERTED);
        	
        	zrtpButton.setToolTipText(SCCallbackResources.GOCLEAR_REQUEST_AC_FLAG_FAILURE);
        }
        else
        if (command.equals("zrtpInitFail"))
        {
        	updateSecureButton(false);
        	
        	callSession.
        	setSecureCommunicationStatus(false,
        								 OperationSetSecuredTelephony.
        								 SecureStatusChangeSource.
        								 SECURE_STATUS_REVERTED);
        	
        	zrtpButton.setToolTipText(SCCallbackResources.ZRTP_ENGINE_INIT_FAILURE);
        }
        else
        if (command.equals("revertToSecured"))
        {
        	updateSecureButton(true);
        	
        	callSession.
			setSecureCommunicationStatus(true,
										 OperationSetSecuredTelephony.
										 SecureStatusChangeSource.
										 SECURE_STATUS_REVERTED);
        	
        }
        else
        if (command.equals("revertToUnsecured"))
        {
        	updateSecureButton(false);
        	
        	callSession.
			setSecureCommunicationStatus(false,
										 OperationSetSecuredTelephony.
										 SecureStatusChangeSource.
										 SECURE_STATUS_REVERTED);
        	
        }
        else
        if (command.equals("goClearRemoteToggle"))
        {
        	updateSecureButton(false);
        	
        	callSession.
        	setSecureCommunicationStatus(false,
        							     OperationSetSecuredTelephony.
        							     SecureStatusChangeSource.
        							     SECURE_STATUS_CHANGE_BY_REMOTE);
        	
        }
        else
        if (command.equals("goSecureRemoteToggle"))
        {
        	updateSecureButton(true);
        	
        	callSession.
    		setSecureCommunicationStatus(true,
    									 OperationSetSecuredTelephony.
    									 SecureStatusChangeSource.
    									 SECURE_STATUS_CHANGE_BY_REMOTE);
        	
        }
    }

	/**
     * The method used to update the secure button state (pressed or not pressed)
     * 
     * @param isSecured parameter reflecting the current button state
     */
    public void updateSecureButton(boolean isSecured)
    {
        if(isSecured)
        {
            zrtpButton.setIcon(
                new ImageIcon(SCCallbackResources.getImage
                				(SCCallbackResources.SECURE_ON_ICON)));
            zrtpButton.setToolTipText(SCCallbackResources.TOGGLE_OFF_SECURITY);
        }
        else
        {
            zrtpButton.setIcon(
                new ImageIcon(SCCallbackResources.getImage
                				(SCCallbackResources.SECURE_OFF_ICON)));
            zrtpButton.setToolTipText(SCCallbackResources.TOGGLE_ON_SECURITY);
        }
    }
}
