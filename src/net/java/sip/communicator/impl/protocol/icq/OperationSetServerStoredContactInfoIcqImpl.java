/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * @author Damian Minkov
 */
public class OperationSetServerStoredContactInfoIcqImpl
    implements OperationSetServerStoredContactInfo
{
    private InfoRetreiver infoRetreiver;

    protected OperationSetServerStoredContactInfoIcqImpl
        (InfoRetreiver infoRetreiver)
    {
        this.infoRetreiver = infoRetreiver;
    }

    public Iterator getDetailsAndDescendants(Contact contat, Class detailClass)
    {
        return infoRetreiver.getDetailsAndDescendants(contat.getAddress(), detailClass);
    }

    public Iterator getDetails(Contact contat, Class detailClass)
    {
        return infoRetreiver.getDetails(contat.getAddress(), detailClass);
    }

    public Iterator getAllDetailsForContact(Contact contact)
    {
        return infoRetreiver.getContactDetails(contact.getAddress()).iterator();
    }
}
