/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.configuration.event;

import java.util.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.util.*;

/**
 * A VetoableChange event gets fired whenever a property is about to change.
 * One can register a VetoableChangeListener with the ConfigurationService so as
 * to be notified in advance of any property updates. The purpose of a
 * VetoableChaneListener is that it allows registered instances to veto or in
 * other words cancel events by throwing a PropertyVetoException. In case
 * none of the registered listeners has thrown an exception, the property is
 * changed and a propertyChange event is dispatched to all registered
 * PropertyChangeListener-s
 *
 * @author Emil Ivov
 */
public interface VetoableChangeListener
    extends EventListener
{

    /**
     * This method gets called when a constrained property is about to change.
     * Note that the method only warns about the change and in case none of
     * the interested listeners vetos it (i.e. no PropertyVetoException
     * is thrown) the propertyChange method will be called next to indicate
     * that the change has taken place. In case you don't want to be notified
     * for pending changes over constrained properties you should provide
     * an empty implementation of the method.
     *
     * @param     evt a <tt>PropertyChangeEvent</tt> object describing the
     *                event source and the property that has changed.
     * @exception PropertyVetoException if the recipient wishes the property
     *              change to be rolled back.
     */
    void vetoableChange(PropertyChangeEvent evt)
                throws PropertyVetoException;
}
