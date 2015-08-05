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
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;

/**
 * The User Info Operation set is a means of accessing detailed information of
 * Contacts that have made it available on-line (on a protocol server, p2p net
 * or others). Examples of such details are your picture, postal or e-mail
 * addresses, work, hobbies, interests, and many many others.
 * <p>
 * Various types of details have been defined in the ServerStoredDetails class
 * and can be used with the get methods of this interface. Implementors may also
 * define their own details by extending or instantiating the
 * ServerStoredDetails.GenericDetail class.
 * <p>
 * Note that this is a read only Operation Set, as it only provides access to
 * information stored by Contacts themselves, and not notes that you have been
 * adding for them..
 * <p>
 * The OperationSetServerStoredContactInfo only concerns Contact-s other than us.
 * For accessing and modifying the information of the user that we are logged in
 * with, we need to use the OperationSetServerStoredAccountInfo
 *
 * @author Emil Ivov
 */
public interface OperationSetServerStoredContactInfo
    extends OperationSet
{
    /**
     * Returns an iterator over all details that are instances or descendants of
     * the specified class. If for example an existing contact has a workaddress
     * and an address detail, a call to this method with AddressDetail.class
     * would return both of them.
     * <p>
     * @param detailClass one of the detail classes defined in the
     * ServerStoredDetails class, indicating the kind of details we're
     * interested in.
     * @param contact the contact whose details we're interested in.
     * <p>
     * @return a java.util.Iterator over all details that are instances or
     * descendants of the specified class.
     */
    public <T extends GenericDetail> Iterator<T> getDetailsAndDescendants(
        Contact contact,
        Class<T> detailClass);

    /**
     * Returns an iterator over all details that are instances of exactly the
     * same class as the one specified. Not that, contrary to the
     * getDetailsAndDescendants() method this one would only return details
     * that are instances of the specified class and not only its descendants.
     * If for example an existing contact has both a workaddress
     * and an address detail, a call to this method with AddressDetail.class
     * would return only the AddressDetail instance and not the
     * WorkAddressDetail instance.
     * <p>
     * @param detailClass one of the detail classes defined in the
     * ServerStoredDetails class, indicating the kind of details we're
     * interested in.
     * @param contact the contact whose details we're interested in.
     * <p>
     * @return a java.util.Iterator over all details of specified class.
     */
    public Iterator<GenericDetail> getDetails(
        Contact contact,
        Class<? extends GenericDetail> detailClass);

    /**
     * Returns all details existing for the specified contact.
     * @param contact the specified contact
     * @return a java.util.Iterator over all details existing for the specified
     * contact.
     */
    public Iterator<GenericDetail> getAllDetailsForContact(Contact contact);

    /**
     * Requests all details existing for the specified contact.
     * @param contact the specified contact
     * @return a java.util.Iterator over all details existing for the specified
     * contact. If there are missing in the local cache null value will
     * be returned and they will be scheduled for retrieve.
     * The <tt>listener</tt> will be used to inform that retrieve has finished.
     */
    public Iterator<GenericDetail> requestAllDetailsForContact(
        Contact contact, DetailsResponseListener listener);

    /**
     * Retrieving details can take some time, this listener will inform
     * when retrieving has ended and will return the details if any.
     */
    public interface DetailsResponseListener
    {
        /**
         * Informs for details retrieved.
         * @param detailIterator the details retrieved if any.
         */
        public void detailsRetrieved(Iterator<GenericDetail> detailIterator);
    }
}
