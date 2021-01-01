/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;

/**
 * @author Damian Minkov
 */
public class OperationSetServerStoredContactInfoJabberImpl
    implements OperationSetServerStoredContactInfo
{
    /**
     * The logger.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperationSetServerStoredContactInfoJabberImpl.class);

    private InfoRetreiver infoRetreiver = null;

    /**
     * If we got several listeners for the same contact lets retrieve once
     * but deliver result to all.
     */
    private final Hashtable<String, List<DetailsResponseListener>>
        listenersForDetails = new Hashtable<>();

    protected OperationSetServerStoredContactInfoJabberImpl(
        InfoRetreiver infoRetreiver)
    {
        this.infoRetreiver = infoRetreiver;
    }

    /**
     * Returns the info retriever.
     * @return the info retriever.
     */
    InfoRetreiver getInfoRetriever()
    {
        return infoRetreiver;
    }

    /**
     * returns the user details from the specified class or its descendants
     * the class is one from the
     * net.java.sip.communicator.service.protocol.ServerStoredDetails
     * or implemented one in the operation set for the user info
     *
     * @param contact Contact
     * @param detailClass Class
     * @return Iterator
     */
    public <T extends GenericDetail> Iterator<T> getDetailsAndDescendants(
        Contact contact,
        Class<T> detailClass)
    {
        if(isPrivateMessagingContact(contact) || !(contact instanceof ContactJabberImpl))
        {
            return new LinkedList<T>().iterator();
        }

        List<GenericDetail> details
            = infoRetreiver.getContactDetails(((ContactJabberImpl) contact)
                .getAddressAsJid()
                .asEntityBareJidOrThrow());
        List<T> result = new LinkedList<>();
        if(details == null)
            return result.iterator();

        for (GenericDetail item : details)
            if(detailClass.isInstance(item))
            {
                @SuppressWarnings("unchecked")
                T t = (T) item;

                result.add(t);
            }

        return result.iterator();
    }

    /**
     * returns the user details from the specified class
     * exactly that class not its descendants
     *
     * @param contact Contact
     * @param detailClass Class
     * @return Iterator
     */
    public Iterator<GenericDetail> getDetails(
        Contact contact,
        Class<? extends GenericDetail> detailClass)
    {
        if(isPrivateMessagingContact(contact) || !(contact instanceof ContactJabberImpl))
            return new LinkedList<GenericDetail>().iterator();

        List<GenericDetail> details
            = infoRetreiver.getContactDetails(((ContactJabberImpl) contact)
                .getAddressAsJid()
                .asEntityBareJidOrThrow());
        List<GenericDetail> result = new LinkedList<>();

        if(details == null)
            return result.iterator();

        for (GenericDetail item : details)
            if(detailClass.equals(item.getClass()))
                result.add(item);

        return result.iterator();
    }

    /**
     * request the full info for the given uin
     * waits and return this details
     *
     * @param contact Contact
     * @return Iterator
     */
    public Iterator<GenericDetail> getAllDetailsForContact(Contact contact)
    {
        if(isPrivateMessagingContact(contact) ||!(contact instanceof ContactJabberImpl))
            return new LinkedList<GenericDetail>().iterator();

        List<GenericDetail> details
            = infoRetreiver.getContactDetails(((ContactJabberImpl) contact)
                .getAddressAsJid()
                .asEntityBareJidOrThrow());

        if(details == null)
            return new LinkedList<GenericDetail>().iterator();
        else
            return new LinkedList<>(details).iterator();
    }

    /**
     * Requests all details existing for the specified contact.
     * @param contact the specified contact
     * @return a java.util.Iterator over all details existing for the specified
     * contact.
     */
    public Iterator<GenericDetail> requestAllDetailsForContact(
        final Contact contact, DetailsResponseListener listener)
    {
        if (!(contact instanceof ContactJabberImpl))
        {
            return null;
        }

        List<GenericDetail> res =
            infoRetreiver.getCachedContactDetails(((ContactJabberImpl) contact)
                .getAddressAsJid().asEntityBareJidOrThrow());

        if(res != null)
        {
            return res.iterator();
        }

        synchronized(listenersForDetails)
        {
            List<DetailsResponseListener> ls =
                listenersForDetails.get(contact.getAddress());

            boolean isFirst = false;
            if(ls == null)
            {
                ls = new ArrayList<DetailsResponseListener>();
                isFirst = true;
                listenersForDetails.put(contact.getAddress(), ls);
            }

            if(!ls.contains(listener))
                ls.add(listener);

            // there is already scheduled retrieve, will deliver at listener.
            if(!isFirst)
                return null;
        }

        new Thread(new Runnable()
        {
            public void run()
            {
                List<GenericDetail> result =
                    infoRetreiver.retrieveDetails(((ContactJabberImpl) contact)
                        .getAddressAsJid()
                        .asEntityBareJidOrThrow());

                List<DetailsResponseListener> listeners;

                synchronized(listenersForDetails)
                {
                    listeners =
                        listenersForDetails.remove(contact.getAddress());
                }

                if(listeners == null || result == null)
                    return;

                for(DetailsResponseListener l : listeners)
                {
                    try
                    {
                        l.detailsRetrieved(result.iterator());
                    }
                    catch(Throwable t)
                    {
                        logger.error(
                            "Error delivering for retrieved details", t);
                    }
                }
            }
        }, getClass().getName() + ".RetrieveDetails").start();

        // return null as there is no cache and we will try to retrieve
        return null;
    }

    /**
     * Checks whether a contact is a private messaging contact for chat rooms.
     * @param contact the contact to check.
     * @return <tt>true</tt> if contact is private messaging contact
     * for chat room.
     */
    private boolean isPrivateMessagingContact(Contact contact)
    {
        if(contact instanceof VolatileContactJabberImpl)
            return ((VolatileContactJabberImpl) contact)
                .isPrivateMessagingContact();

        return false;
    }
}
