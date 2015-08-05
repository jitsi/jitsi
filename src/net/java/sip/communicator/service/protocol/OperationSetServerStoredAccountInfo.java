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
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The Account Info Operation set is a means of accessing and modifying detailed
 * information on the user/account that is currently logged in through this
 * provider. This operation set is more or less reciproce to
 * OperationSetServerStoredContactInfo with the most essential difference being the
 * fact that the account info operation set allows you to modify data in
 * addition to reading it (quite natural given that it's your own info that
 * you're dealing with).
 * <p>
 * Examples of account details are your picture, postal or e-mail addresses,
 * work, hobbies, interests, and many many others.
 * <p>
 * Various types of details have been defined in the ServerStoredDetails class
 * and can be used with the get methods of this interface. Implementors may also
 * define their own details by extending or instantiating the
 * ServerStoredDetails.GenericDetail class. Yet, they are encouraged to use
 * existing detail types as fully as possible. Defining your own detail type
 * may lead to limited visualization of its value.
 * <p>
 * As mentioned earlier the operation set supports adding, removing or replaing
 * various details. The exact set of details that can be manipulated through
 * this operation set depends on the implementation and can be retrieved through
 * the getSupportedDetailTypes() method. The maximum number of detail instances
 * supported for a given type of details can be retrieved through the
 * getMaxDetailInstances() method.
 * <p>
 * The OperationSetServerStoredAccountInfo only concerns us (the user currently
 * logged through this provider) and our own details. In order to query details
 * concerning Contacts in our contact list we'd need to use the
 * OperationSetServerStoredContactInfo
 *
 * @author Emil Ivov
 */
public interface OperationSetServerStoredAccountInfo
    extends OperationSet
{
    /**
     * Returns an iterator over all details that are instances or descendants of
     * the specified class. If for example an our account has a workaddress
     * and an address detail, a call to this method with AddressDetail.class
     * would return both of them.
     * <p>
     * @param detailClass one of the detail classes defined in the
     * ServerStoredDetails class, indicating the kind of details we're
     * interested in.
     * <p>
     * @return a java.util.Iterator over all details that are instances or
     * descendants of the specified class.
     */
    public <T extends GenericDetail> Iterator<T> getDetailsAndDescendants(
        Class<T> detailClass);

    /**
     * Returns an iterator over all details that are instances of exactly the
     * same class as the one specified. Not that, contrary to the
     * getDetailsAndDescendants() method this one would only return details
     * that are instances of the specified class and not only its descendants.
     * If for example our account has both a workaddress and an address detail,
     * a call to this method with AddressDetail.class would return only the
     * AddressDetail instance and not the WorkAddressDetail instance.
     * <p>
     * @param detailClass one of the detail classes defined in the
     * ServerStoredDetails class, indicating the kind of details we're
     * interested in.
     * <p>
     * @return a java.util.Iterator over all details of specified class.
     */
    public Iterator<GenericDetail> getDetails(
        Class<? extends GenericDetail> detailClass);

    /**
     * Returns all details currently available and set for our account.
     * <p>
     * @return a java.util.Iterator over all details currently set our account.
     */
    public Iterator<GenericDetail> getAllAvailableDetails();

    /**
     * Returns all detail Class-es that the underlying implementation supports
     * setting. Note that if you call one of the modification methods (add
     * remove or replace) with a detail not contained by the iterator returned
     * by this method, an IllegalArgumentException will be thrown.
     * <p>
     * @return a java.util.Iterator over all detail classes supported by the
     * implementation.
     */
    public Iterator<Class<? extends GenericDetail>> getSupportedDetailTypes();

    /**
     * Determines whether a detail class represents a detail supported by the
     * underlying implementation or not. Note that if you call one of the
     * modification methods (add remove or replace) with a detail that this
     * method has determined to be unsupported (returned false) this would lead
     * to an IllegalArgumentException being thrown.
     * <p>
     * @param detailClass the class the support for which we'd like to
     * determine.
     * <p>
     * @return true if the underlying implementation supports setting details of
     * this type and false otherwise.
     */
    public boolean isDetailClassSupported(
        Class<? extends GenericDetail> detailClass);

    /**
     * Determines whether the underlying implementation supports edition
     * of this detail class.
     * <p>
     * @param detailClass the class whose edition we'd like to determine if it's
     * possible
     * @return true if the underlying implementation supports edition of this
     * type of detail and false otherwise.
     */
    public boolean isDetailClassEditable(
        Class<? extends GenericDetail> detailClass);

    /**
     * The method returns the number of instances supported for a particular
     * detail type. Some protocols offer storing mutliple values for a
     * particular detail type. Spoken languages are a good example.
     * @param detailClass the class whose max instance number we'd like to find
     * out.
     * <p>
     * @return int the maximum number of detail instances.
     */
    public int getMaxDetailInstances(
        Class<? extends GenericDetail> detailClass);

    /**
     * Adds the specified detail to the list of details ready to be saved online
     * for this account. If such a detail already exists its max instance number
     * is consulted and if it allows it - a second instance is added or otherwise
     * and illegal argument exception is thrown. An IllegalArgumentException is
     * also thrown in case the class of the specified detail is not supported by
     * the underlying implementation, i.e. its class name was not returned by the
     * getSupportedDetailTypes() method.
     * <p>
     * @param detail the detail that we'd like registered on the server.
     * <p>
     * @throws IllegalArgumentException if such a detail already exists and its
     * max instances number has been attained or if the underlying
     * implementation does not support setting details of the corresponding
     * class.
     * @throws java.lang.ArrayIndexOutOfBoundsException if the number of
     * instances currently registered by the application is already equal to the
     * maximum number of supported instances (@see getMaxDetailInstances())
     */
    public void addDetail(ServerStoredDetails.GenericDetail detail)
        throws IllegalArgumentException,
               OperationFailedException,
               ArrayIndexOutOfBoundsException;

    /**
     * Removes the specified detail from the list of details ready to be saved
     * online this account. The method returns a boolean indicating if such a
     * detail was found (and removed) or not.
     * <p>
     * @param detail the detail to remove
     * @return true if the specified detail existed and was successfully removed
     * and false otherwise.
     */
    public boolean removeDetail(ServerStoredDetails.GenericDetail detail)
        throws OperationFailedException;

    /**
     * Replaces the currentDetailValue detail with newDetailValue and returns
     * true if the operation was a success or false if currentDetailValue did
     * not previously exist (in this case an additional call to addDetail is
     * required).
     * <p>
     * @param currentDetailValue the detail value we'd like to replace.
     * @param newDetailValue the value of the detail that we'd like to replace
     * currentDetailValue with.
     * @throws ClassCastException if newDetailValue is not an instance of the
     * same class as currentDetailValue.
     * @throws OperationFailedException with code Network Failure if putting the
     * new value back online has failed
     */
    public boolean replaceDetail(
                    ServerStoredDetails.GenericDetail currentDetailValue,
                    ServerStoredDetails.GenericDetail newDetailValue)
        throws ClassCastException, OperationFailedException;

    /**
     * Saves the list of details for this account that were ready to be stored
     * online on the server. This method performs the actual saving of details
     * online on the server and is supposed to be invoked after addDetail(),
     * replaceDetail() and/or removeDetail().
     * <p>
     * @throws OperationFailedException with code Network Failure if putting the
     * new values back online has failed.
     */
    public void save() throws OperationFailedException;

    /**
     * Registers a ServerStoredDetailsChangeListener with this operation set so
     * that it gets notifications of details change.
     *
     * @param listener the <tt>ServerStoredDetailsChangeListener</tt>
     * to register.
     */
    public void addServerStoredDetailsChangeListener(
            ServerStoredDetailsChangeListener listener);

    /**
     * Unregisters <tt>listener</tt> so that it won't receive any further
     * notifications upon details change.
     *
     * @param listener the <tt>ServerStoredDetailsChangeListener</tt>
     * to unregister.
     */
    public void removeServerStoredDetailsChangeListener(
            ServerStoredDetailsChangeListener listener);
}
