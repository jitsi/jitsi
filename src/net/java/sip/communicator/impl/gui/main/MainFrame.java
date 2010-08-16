/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;


import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.OperationSet;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.swing.*;

/**
 * The main application window. This class is the core of this UI
 * implementation. It stores all available protocol providers and their
 * operation sets, as well as all registered accounts, the
 * <tt>MetaContactListService</tt> and all sent messages that aren't
 * delivered yet.
 * 
 * This is an abstract wrapper class that extends and implement all
 * base classes and interfaces for real GUI classes. The MainFrameStandard
 * and the MainFrameTouch GUI classes extend this class and implement
 * all missing functions definied in MainFrameInterface.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 * @outhor Werner Dittmann
 */
public abstract class MainFrame
    extends SIPCommFrame
    implements  ExportedWindow,
                PluginComponentListener, MainFrameInterface
{  
    /**
     * Returns the presence operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the
     * presence operation set is searched.
     * @return the presence operation set for the given protocol provider.
     */
    public static OperationSetPresence getProtocolPresenceOpSet(
            ProtocolProviderService protocolProvider)
    {
        OperationSet opSet
            = protocolProvider.getOperationSet(OperationSetPresence.class);

        return
            (opSet instanceof OperationSetPresence)
                ? (OperationSetPresence) opSet
                : null;
    }
}