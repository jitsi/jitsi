/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.packet.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;

/**
 *
 * @author Damian Minkov
 */
public class OperationSetServerStoredAccountInfoJabberImpl
    implements OperationSetServerStoredAccountInfo
{
    private InfoRetreiver infoRetreiver = null;

    /**
     * The jabber provider that created us.
     */
    private ProtocolProviderServiceJabberImpl jabberProvider = null;

    /**
     * Our account uin
     */
    private String uin = null;

    protected OperationSetServerStoredAccountInfoJabberImpl(
        ProtocolProviderServiceJabberImpl jabberProvider,
        InfoRetreiver infoRetreiver,
        String uin)
    {
        this.infoRetreiver = infoRetreiver;
        this.jabberProvider = jabberProvider;
        this.uin = uin;
    }

    /**
     * Returns an iterator over all details that are instances or descendants of
     * the specified class. If for example an our account has a work address
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
    public Iterator<GenericDetail> getDetailsAndDescendants(
        Class<? extends GenericDetail> detailClass)
    {
        assertConnected();

        return infoRetreiver.getDetailsAndDescendants(uin, detailClass);
    }

    /**
     * Returns an iterator over all details that are instances of exactly the
     * same class as the one specified. Not that, contrary to the
     * getDetailsAndDescendants() method this one would only return details
     * that are instances of the specified class and not only its descendants.
     * If for example our account has both a work address and an address detail,
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
        Class<? extends GenericDetail> detailClass)
    {
        assertConnected();

        return infoRetreiver.getDetails(uin, detailClass);
    }

    /**
     * Returns all details currently available and set for our account.
     * <p>
     * @return a java.util.Iterator over all details currently set our account.
     */
    public Iterator<GenericDetail> getAllAvailableDetails()
    {
        assertConnected();

        return infoRetreiver.getContactDetails(uin).iterator();
    }

    /**
     * Returns all detail Class-es that the underlying implementation supports
     * setting. Note that if you call one of the modification methods (add
     * remove or replace) with a detail not contained by the iterator returned
     * by this method, an IllegalArgumentException will be thrown.
     * <p>
     * @return a java.util.Iterator over all detail classes supported by the
     * implementation.
     */
    public Iterator<Class<? extends GenericDetail>> getSupportedDetailTypes()
    {
        List<GenericDetail> details = infoRetreiver.getContactDetails(uin);
        List<Class<? extends GenericDetail>> result
            = new Vector<Class<? extends GenericDetail>>();

        for (GenericDetail obj : details)
            result.add(obj.getClass());

        return result.iterator();
    }

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
        Class<? extends GenericDetail> detailClass)
    {
        List<GenericDetail> details = infoRetreiver.getContactDetails(uin);

        for (GenericDetail obj : details)
            if(detailClass.isAssignableFrom(obj.getClass()))
                return true;
        return false;
    }

    /**
     * The method returns the number of instances supported for a particular
     * detail type. Some protocols offer storing multiple values for a
     * particular detail type. Spoken languages are a good example.
     * @param detailClass the class whose max instance number we'd like to find
     * out.
     * <p>
     * @return int the maximum number of detail instances.
     */
    public int getMaxDetailInstances(Class<? extends GenericDetail> detailClass)
    {
        return 1;
    }

    /**
     * Adds the specified detail to the list of details registered on-line
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
     * @throws OperationFailedException with code Network Failure if putting the
     * new value online has failed
     * @throws java.lang.ArrayIndexOutOfBoundsException if the number of
     * instances currently registered by the application is already equal to the
     * maximum number of supported instances (@see getMaxDetailInstances())
     */
    public void addDetail(ServerStoredDetails.GenericDetail detail)
        throws IllegalArgumentException,
               OperationFailedException,
               ArrayIndexOutOfBoundsException
    {
        assertConnected();

        /*
        Currently as the function only provided the list of classes that
        currently have data associated with them
         in Jabber InfoRetreiver we have to skip this check*/
        //if (!isDetailClassSupported(detail.getClass())) {
        //    throw new IllegalArgumentException(
        //            "implementation does not support such details " +
        //            detail.getClass());
        //}

        Iterator iter = getDetails(detail.getClass());
        int currentDetailsSize = 0;
        while (iter.hasNext()) {
            currentDetailsSize++;
        }

        if (currentDetailsSize >= getMaxDetailInstances(detail.getClass()))
        {
            throw new ArrayIndexOutOfBoundsException(
                    "Max count for this detail is already reached");
        }

        if(detail instanceof ImageDetail)
        {
            try
            {
                VCard v1 = new VCard();
                v1.load(jabberProvider.getConnection());

                v1.setAvatar(((ImageDetail) detail).getBytes());

                v1.save(jabberProvider.getConnection());
            } catch (XMPPException xmppe)
            {
                xmppe.printStackTrace();
            }
        }
    }

    /**
     * Removes the specified detail from the list of details stored online for
     * this account. The method returns a boolean indicating if such a detail
     * was found (and removed) or not.
     * <p>
     * @param detail the detail to remove
     * @return true if the specified detail existed and was successfully removed
     * and false otherwise.
     * @throws OperationFailedException with code Network Failure if removing the
     * detail from the server has failed
     */
    public boolean removeDetail(ServerStoredDetails.GenericDetail detail)
        throws OperationFailedException
    {
        return false;
    }

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
        throws ClassCastException, OperationFailedException
    {
        assertConnected();

        if (!newDetailValue.getClass().equals(currentDetailValue.getClass()))
        {
            throw new ClassCastException(
                    "New value to be replaced is not as the current one");
        }
        // if values are the same no change
        if (currentDetailValue.equals(newDetailValue))
        {
            return true;
        }

        boolean isFound = false;
        Iterator iter =
                infoRetreiver.getDetails(uin, currentDetailValue.getClass());

        while (iter.hasNext())
        {
            GenericDetail item = (GenericDetail) iter.next();
            if (item.equals(currentDetailValue))
            {
                isFound = true;
                break;
            }
        }
        // current detail value does not exist
        if (!isFound)
        {
            return false;
        }

        if(newDetailValue instanceof ImageDetail)
        {
            try
            {
                VCard v1 = new VCard();
                v1.load(jabberProvider.getConnection());

                v1.setAvatar(((ImageDetail) newDetailValue).getBytes());
                v1.save(jabberProvider.getConnection());

                return true;
            } catch (XMPPException xmppe)
            {
                xmppe.printStackTrace();
            }
        }

        return false;
    }

    /**
     * Utility method throwing an exception if the icq stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (jabberProvider == null)
            throw new IllegalStateException(
                "The jabber provider must be non-null and signed on "
                +"before being able to communicate.");
        if (!jabberProvider.isRegistered())
            throw new IllegalStateException(
                "The jabber provider must be signed on before "
                +"being able to communicate.");
    }
}
