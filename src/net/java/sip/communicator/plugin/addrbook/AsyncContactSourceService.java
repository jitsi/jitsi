/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook;

import net.java.sip.communicator.service.contactsource.*;

/**
 * Declares the interface of a <tt>ContactSourceService</tt> which performs
 * <tt>ContactQuery</tt>s in a separate <tt>Thread</tt>.
 *
 * @author Lyubomir Marinov
 */
public interface AsyncContactSourceService
    extends ContactSourceService
{
    /**
     * Stops this <tt>ContactSourceService</tt>.
     */
    public void stop();
}
