/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.Iterator;

/**
 * The User Info Operation set is a means of accessing detailed information of
 * Contacts in your contact list that they have set (themselves) on the protocol
 * server or somewhere else in the protocol network. Examples of such details
 * are your picture, postal or e-mail addresses, work, hobbies, interests, and
 * many many others.
 * <p>
 * Details can be retrieved through the getObject() method or through one of the
 * other convenience methods (e.g. getString() or getBytes() for String and
 * binary details respectively).
 * <p>
 * Some detail names are specified in the ServerStoredDetails interface. If one
 * or more of the details defined there are available for a Contact then the
 * MUST be defined under that name.
 * <p>
 * You should know however that these static Strings are in no way exhaustive
 * and that an implementation of the user info operation set may contain none,
 * some, all of them or even details with names that are not defined there.
 * <p>
 * Note that this is a read only Operation Set, as it only provides access to
 * information stored by Contacts themselves.
 * <p>
 * The OperationSetServerStoredUserInfo only concerns Contact-s other than us.
 * For accessing and modifying the information of the user that we are logged in
 * with, one needs to use the OperationSetServerStoredAccountInfo
 *
 * @author Emil Ivov
 */
public interface OperationSetServerStoredUserInfo
    extends OperationSet
{
    /**
     * Returns a String containing the value of the detail named
     * <tt>detailName</tt> if such a detail exists for <tt>contact</tt> or null
     * if there is no such String detail for the specified contact. Note that the
     * method would return a non-null result if and only if the speicifed detail
     * exists as a String (i.e. if a Binary detail with the same name exists,
     * it would not be returned). If you need a blind way of retrieving a
     * detail - use the getObject() method.
     * <p>
     * String details generally include (but are not limited to) addresses,
     * names, contact information, interests, work etc.
     * <p>
     * @param contact the <tt>Contact</tt> whose details we'd like to retrieve.
     * @param detailName a <tt>String</tt> indicating the detail that we'd like
     * to retrieve (generally one of the static Strings defined in
     * ServerStoredDetails).
     *
     * @return a String containing the value corresponding to <tt>detail</tt>
     * or null if no such String detail exists for <tt>contact</tt>.
     */
    public String getString(Contact contact, String detailName);

    /**
     * Returns an iterator over all String details available for the specified
     * contact.
     * @param contact the Contact whose String details we'd like to retrieve
     * @return an Iterator over all String details (and only String details)
     * available for the specified Contact.
     */
    public Iterator getStringDetailNames(Contact contact);

    /**
     * Returns a byte array containg the value of <tt>detailName</tt> if such a
     * binary detail exists for <tt>contact</tt> or null if there is no such
     * binary detail for the specified <tt>contact</tt>.
     * <p>
     * Binary details generally include (but are not limited to) images, sounds,
     * files and others.
     * <p>
     * @param contact the <tt>Contact</tt> whose detail we'd like to retrieve.
     * @param detailName a <tt>String</tt> indicating the name of the binary
     * detail
     * that we'd like to retrieve (generally one of the static Strings defined
     * in ServerStoredDetails).
     * @return a byte array containing the value corresponding to the
     * <tt>detail</tt> string or null if no such String detail exists for
     * <tt>contact</tt>.
     */
    public byte[] getBytes(Contact contact, String detailName);

    /**
     * Returns an iterator over all Binary details available for the specified
     * contact.
     * <p>
     * Note that method would return a non-null result if and only if the
     * speicifed detail exists as a byte array (i.e. if a String detail with the
     * same name exists, it would not be returned). If you need a blind way of
     * retrieving a detail regardless of its class - use the getObject() method.
     * <p>
     * @param contact the Contact whose String details we'd like to retrieve
     * @return an Iterator over all Binary details (and only Binary details)
     * available for the specified Contact.
     */
    public Iterator getBinaryDetailNames(Contact contact);

    /**
     * Returns the value of the detail named <tt>detailName</tt> if it exists
     * for Contact <tt>contact</tt> or null otherwise.
     * @param contact the <tt>Contact</tt> whose details we're interested in.
     * @param detailName a String containing the name of the detail.
     * @return the value of the detail named <tt>detailName</tt> if it exists
     * for Contact <tt>contact</tt> or null otherwise.
     */
    public Object getObject(Contact contact, String detailName);

    /**
     * Returns an Iterator over all details available for this <tt>contact</tt>
     *
     * @param contact the contact whose details we're interested in.
     * @return an Iterator over all details available for this <tt>contact</tt>
     */
    public Iterator getDetailNames(Contact contact);
}
