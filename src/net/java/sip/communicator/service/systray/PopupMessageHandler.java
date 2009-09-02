/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.systray;

import net.java.sip.communicator.service.systray.event.*;

/**
 * The <tt>PopupMessageHandler</tt> role is to give differents methods to
 * display <tt>PopupMessage</tt> and listen for events (user click)
 * coming from that popup.
 *
 * @author Symphorien Wanko
 */
public interface PopupMessageHandler
{

    /**
     * Register a listener to be informed of systray popup events.
     *
     * @param listener the listened which will be informed of systray popup
     * events
     */
    public void addPopupMessageListener(SystrayPopupMessageListener listener);

    /**
     * Removes a listener previously added with <tt>addPopupMessageListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removePopupMessageListener(SystrayPopupMessageListener listener);

    /**
     * Shows the given <tt>PopupMessage</tt>. Any given <tt>PopupMessage</tt>
     * will provides a minimum of two values : a message title and
     * a message body. thoose two values are respectively available via
     * <tt>PopupMessage#getMessageTitle()</tt> and
     * <tt>PopupMessage#getMessage()</tt>
     *
     * @param popupMessage the message to show
     */
    public void showPopupMessage(PopupMessage popupMessage);
    
    /**
     * Returns a preference index, which indicates how many features the handler 
     * implements. Implementer should calculate preference index by adding "1" 
     * for each of the the following features that the implementation supports: 
     * 
     * 1) showing images
     * 2) detecting clicks
     * 3) being able to match a click to a message
     * 4) using a native popup mechanism
     * 
     * @return an integer representing preference index of this popup handler
     */
    public int getPreferenceIndex();

    /**
     * Returns a readable description of this popup handler. It is expected
     * to be a localized string.
     *
     * @return a string describing this popup handler
     */
    @Override
    public String toString();
}
