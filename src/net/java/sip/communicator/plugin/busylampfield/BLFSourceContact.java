/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.busylampfield;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

import java.util.*;

/**
 * The BLF source contact.
 * @author Damian Minkov
 */
public class BLFSourceContact
    extends GenericSourceContact
{
    private final OperationSetTelephonyBLF.Line line;

    /**
     * Initializes a new <tt>AddrBookSourceContact</tt> instance.
     *
     * @param contactSource  the <tt>ContactSourceService</tt> which is creating
     *                       the new instance
     */
    public BLFSourceContact(ContactSourceService contactSource,
                            OperationSetTelephonyBLF.Line line)
    {
        super(contactSource,
            line.getName() != null ? line.getName() : line.getAddress(),
            new ArrayList<ContactDetail>());

        this.line = line;
    }

    /**
     * Returns the line displayed.
     * @return
     */
    public OperationSetTelephonyBLF.Line getLine()
    {
        return line;
    }
}
