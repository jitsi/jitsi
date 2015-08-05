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
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.ImageDetail;
import net.java.sip.communicator.util.*;

/**
 * @author Damian Minkov
 */
public class OperationSetServerStoredContactInfoIcqImpl
    implements OperationSetServerStoredContactInfo
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetServerStoredContactInfoIcqImpl.class);

    private InfoRetreiver infoRetreiver;

    /**
     * If we got several listeners for the same contact lets retrieve once
     * but deliver result to all.
     */
    private Hashtable<String, List<DetailsResponseListener>>
        listenersForDetails =
            new Hashtable<String, List<DetailsResponseListener>>();

    /**
     * The icq provider that created us.
     */
    private ProtocolProviderServiceIcqImpl icqProvider = null;

    protected OperationSetServerStoredContactInfoIcqImpl
        (InfoRetreiver infoRetreiver, ProtocolProviderServiceIcqImpl icqProvider)
    {
        this.infoRetreiver = infoRetreiver;
        this.icqProvider = icqProvider;
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
        assertConnected();

        if(detailClass.equals(ImageDetail.class)
                && (contact.getImage() != null))
        {
            List<ImageDetail> res = new Vector<ImageDetail>();

            res.add(new ImageDetail("Image", contact.getImage()));

            @SuppressWarnings("unchecked")
            Iterator<T> tIt = (Iterator<T>) res.iterator();

            return tIt;
        }
        return
            infoRetreiver.getDetailsAndDescendants(
                    contact.getAddress(),
                    detailClass);
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
        assertConnected();

        if(detailClass.equals(ServerStoredDetails.ImageDetail.class) &&
            contact.getImage() != null)
        {
            List<GenericDetail> res = new Vector<GenericDetail>();
            res.add(new ServerStoredDetails.ImageDetail(
                "Image", contact.getImage()));
            return res.iterator();
        }
        return infoRetreiver.getDetails(contact.getAddress(), detailClass);
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
        assertConnected();

        List<GenericDetail> res
            = infoRetreiver.getContactDetails(contact.getAddress());

        if(contact.getImage() != null)
        {
            res.add(new ServerStoredDetails.ImageDetail(
                "Image", contact.getImage()));
        }

        return res.iterator();
    }

    /**
     * Utility method throwing an exception if the icq stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (icqProvider == null)
            throw new IllegalStateException(
                "The icq provider must be non-null and signed on the ICQ "
                +"service before being able to communicate.");
        if (!icqProvider.isRegistered())
            throw new IllegalStateException(
                "The icq provider must be signed on the ICQ service before "
                +"being able to communicate.");
    }

    /**
     * Requests all details existing for the specified contact.
     * @param contact the specified contact
     * @return a java.util.Iterator over all details existing for the specified
     * contact.
     */
    public Iterator<GenericDetail> requestAllDetailsForContact(
        final Contact contact, final DetailsResponseListener listener)
    {
        assertConnected();

        List<GenericDetail> res =
            infoRetreiver.getCachedContactDetails(contact.getAddress());

        if(res != null)
        {
            if(contact.getImage() != null)
            {
                res.add(new ServerStoredDetails.ImageDetail(
                    "Image", contact.getImage()));
            }
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
                    infoRetreiver.retrieveDetails(contact.getAddress());

                if(contact.getImage() != null && result != null)
                {
                    result.add(new ServerStoredDetails.ImageDetail(
                        "Image", contact.getImage()));
                }

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
}
