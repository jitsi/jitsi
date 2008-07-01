/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.sip.communicator.service.protocol;

/**
 * An operation set that allows plugins to register filters which could 
 * intercept instant messages and determine whether or not they should be 
 * dispatched to regular listeners. <tt>EventFilter</tt>-s allow implementating
 * features that use standard instant messaging channels to exchange 
 * 
 * @author Keio Kraaner
 */

import net.java.sip.communicator.service.protocol.event.*;

public interface OperationSetInstantMessageFiltering
    extends OperationSet
{
    /**
     * Registeres an <tt>EventFilter</tt> with this operation set so that 
     * events, that do not need processing, are filtered out.
     *
     * @param filter the <tt>EventFilter</tt> to register.
     */
    public void addEventFilter(EventFilter filter);

    /**
     * Unregisteres an <tt>EventFilter</tt> so that it won't check any more 
     * if an event should be filtered out.
     *
     * @param filter the <tt>EventFilter</tt> to unregister.
     */
    public void removeEventFilter(EventFilter filter);
}
