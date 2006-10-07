/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.impl.protocol.icq.message.usrinfo.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.snac.*;

/**
 * @author Damian Minkov
 */
public class OperationSetServerStoredAccountInfoIcqImpl
    implements OperationSetServerStoredAccountInfo
{
    private static final Logger logger =
        Logger.getLogger(OperationSetServerStoredAccountInfoIcqImpl.class);

    private InfoRetreiver infoRetreiver = null;
    private String uin = null;

    /**
     * The icq provider that created us.
     */
    private ProtocolProviderServiceIcqImpl icqProvider = null;


    public OperationSetServerStoredAccountInfoIcqImpl
        (InfoRetreiver infoRetreiver, String uin,
         ProtocolProviderServiceIcqImpl icqProvider)
    {
        this.infoRetreiver = infoRetreiver;
        this.uin = uin;
        this.icqProvider = icqProvider;
    }

    /**
     * Returns all details currently available and set for our account.
     *
     * @return a java.util.Iterator over all details currently set our
     *   account.
     */
    public Iterator getAllAvailableDetails()
    {
        return infoRetreiver.getContactDetails(uin).iterator();
    }

    /**
     * Returns an iterator over all details that are instances of exactly the
     * same class as the one specified.
     *
     * @param detailClass one of the detail classes defined in the
     *   ServerStoredDetails class, indicating the kind of details we're
     *   interested in. <p>
     * @return a java.util.Iterator over all details of specified class.
     */
    public Iterator getDetails(Class detailClass)
    {
        return infoRetreiver.getDetails(uin, detailClass);
    }

    /**
     * Returns an iterator over all details that are instances or descendants
     * of the specified class.
     *
     * @param detailClass one of the detail classes defined in the
     *   ServerStoredDetails class, indicating the kind of details we're
     *   interested in. <p>
     * @return a java.util.Iterator over all details that are instances or
     *   descendants of the specified class.
     */
    public Iterator getDetailsAndDescendants(Class detailClass)
    {
        return infoRetreiver.getDetailsAndDescendants(uin, detailClass);
    }

    /**
     * The method returns the number of instances supported for a particular
     * detail type.
     *
     * @param detailClass the class whose max instance number we'd like to
     *   find out. <p>
     * @return int the maximum number of detail instances.
     */
    public int getMaxDetailInstances(Class detailClass)
    {
        return ((int[])FullInfoCmd.supportedTypes.get(detailClass))[0];
    }

    /**
     * Returns all detail Class-es that the underlying implementation
     * supports setting.
     *
     * @return a java.util.Iterator over all detail classes supported by the
     *   implementation.
     */
    public Iterator getSupportedDetailTypes()
    {
        return FullInfoCmd.supportedTypes.keySet().iterator();
    }

    /**
     * Determines wheter a detail class represents a detail supported by the
     * underlying implementation or not.
     *
     * @param detailClass the class the support for which we'd like to
     *   determine. <p>
     * @return true if the underlying implementation supports setting
     *   details of this type and false otherwise.
     */
    public boolean isDetailClassSupported(Class detailClass)
    {
        return FullInfoCmd.supportedTypes.get(detailClass) != null;
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
     * max instances number has been atteined or if the underlying
     * implementation does not support setting details of the corresponding
     * class.
     * @throws OperationFailedException with code Network Failure if putting the
     * new value online has failed
     * @throws java.lang.ArrayIndexOutOfBoundsException if the number of
     * instances currently registered by the application is already equal to the
     * maximum number of supported instances (@see getMaxDetailInstances())
     */
    public void addDetail(GenericDetail detail) throws IllegalArgumentException,
        OperationFailedException, ArrayIndexOutOfBoundsException
    {
        if(!isDetailClassSupported(detail.getClass()))
            throw new IllegalArgumentException(
                "implementation does not support such details " +
                detail.getClass());

        Vector alreadySetDetails = new Vector();
        Iterator iter = getDetails(detail.getClass());
        while (iter.hasNext())
        {
            alreadySetDetails.add(iter.next());
        }

        if(alreadySetDetails.size() >= getMaxDetailInstances(detail.getClass()))
            throw new ArrayIndexOutOfBoundsException(
                "Max count for this detail is already reached");

        // everything is ok , so set it
        alreadySetDetails.add(detail);

        SuccessResponseListener responseListener = new SuccessResponseListener();
        icqProvider.getAimConnection().getInfoService().
            sendSnacRequest(new FullInfoCmd(uin, alreadySetDetails, null), responseListener);

        responseListener.waitForEvent(5000);

        if(!responseListener.success)
            if(responseListener.timeout)
                throw new OperationFailedException("Adding Detail Failed!",
                            OperationFailedException.NETWORK_FAILURE);
            else
                throw new OperationFailedException("Adding Detail Failed!",
                            OperationFailedException.GENERAL_ERROR);

        infoRetreiver.detailsChanged(uin);
    }

    /**
     * Removes the specified detail from the list of details stored online
     * for this account.
     *
     * @param detail the detail to remove
     * @return true if the specified detail existed and was successfully
     *   removed and false otherwise.
     * @throws OperationFailedException with code Network Failure if
     *   removing the detail from the server has failed
     */
    public boolean removeDetail(GenericDetail detail) throws
        OperationFailedException
    {
        // as there is no remove method for the details we will
        // set it with empty or default value

        boolean isFound = false;
        // as there is items like langusge, which must be changed all the values
        // we write not only the changed one but and the other found
        ArrayList foundValues = new ArrayList();
        Iterator iter = infoRetreiver.getDetails(uin, detail.getClass());
        while (iter.hasNext())
        {
            GenericDetail item = (GenericDetail) iter.next();
            if(item.equals(detail))
            {
                isFound = true;
                foundValues.add(detail);
            }
            else
                foundValues.add(item);
        }
        // current detail value does not exist
        if(!isFound)
            return false;

        List removeValues = new ArrayList();
        removeValues.add(detail);

        SuccessResponseListener responseListener = new SuccessResponseListener();
        icqProvider.getAimConnection().getInfoService().
            sendSnacRequest(new FullInfoCmd(uin, foundValues, removeValues), responseListener);

        responseListener.waitForEvent(5000);

        if(!responseListener.success && responseListener.timeout)
            throw new OperationFailedException("Replacing Detail Failed!",
                            OperationFailedException.NETWORK_FAILURE);

        if(responseListener.success)
        {
            infoRetreiver.detailsChanged(uin);
            return true;
        }
        else
            return false;
    }

    /**
     * Replaces the currentDetailValue detail with newDetailValue and returns
     * true if the operation was a success or false if currentDetailValue did
     * not previously exist (in this case an additional call to addDetail is
     * required).
     *
     * @param currentDetailValue the detail value we'd like to replace.
     * @param newDetailValue the value of the detail that we'd like to
     *   replace currentDetailValue with.
     * @throws ClassCastException if newDetailValue is not an instance of
     *   the same class as currentDetailValue.
     * @throws OperationFailedException with code Network Failure if putting
     *   the new value back online has failed
     * @return boolean
     */
    public boolean replaceDetail(GenericDetail currentDetailValue,
                                 GenericDetail newDetailValue) throws
        ClassCastException, OperationFailedException
    {
        if(!newDetailValue.getClass().equals(currentDetailValue.getClass()))
            throw new ClassCastException("New value to be replaced is not as the current one");

        boolean isFound = false;
        Vector alreadySetDetails = new Vector();
        Iterator iter = infoRetreiver.getDetails(uin, currentDetailValue.getClass());
        while (iter.hasNext())
        {
            GenericDetail item = (GenericDetail) iter.next();
            if(item.equals(currentDetailValue))
            {
                isFound = true;
                // add the details to the list. We will save the list on one pass
                // most of the multiple details require saving at one time, like Spoken Language
                // we are placing it at the right place. replacing the old one
                alreadySetDetails.add(newDetailValue);
            }
            else
                alreadySetDetails.add(item);
        }
        // current detail value does not exist
        if(!isFound)
            return false;

        SuccessResponseListener responseListener = new SuccessResponseListener();
        icqProvider.getAimConnection().getInfoService().
            sendSnacRequest(
                new FullInfoCmd(uin, alreadySetDetails, null), responseListener);

        responseListener.waitForEvent(5000);

        if(!responseListener.success && responseListener.timeout)
            throw new OperationFailedException("Replacing Detail Failed!",
                            OperationFailedException.NETWORK_FAILURE);

        if(responseListener.success)
        {
            infoRetreiver.detailsChanged(uin);
            return true;
        }
        else
            return false;
    }

    /**
     * Waiting for Acknowledge package and success byte.
     * To set that the operation was succesful
     */
    private class SuccessResponseListener
        implements SnacRequestListener
    {
        public Object waitingForResponseLock = new Object();

        private boolean ran = false;
        boolean success = false;

        private boolean timeout = false;

        public void handleSent(SnacRequestSentEvent evt)
        {}

        public void handleTimeout(SnacRequestTimeoutEvent event)
        {
            logger.trace("Timeout!");

            synchronized(waitingForResponseLock)
            {
                if (ran)
                    return;

                ran = true;
                timeout = true;
                waitingForResponseLock.notifyAll();
            }
        }

        public void handleResponse(SnacResponseEvent evt)
        {
            synchronized(waitingForResponseLock)
            {
                if (ran)
                    return;
                ran = true;
                if (evt.getSnacCommand() instanceof FullInfoAck)
                {
                    FullInfoAck cmd = (FullInfoAck) evt.getSnacCommand();
                    if (cmd.isCommandSuccesful())
                    {
                        success = true;
                    }
                }
                waitingForResponseLock.notifyAll();
            }
        }

        public void waitForEvent(int milliseconds)
        {
            synchronized (waitingForResponseLock){
                if(ran)
                    return;

                try
                {
                    waitingForResponseLock.wait(milliseconds);
                }
                catch (InterruptedException exc)
                {
                    logger.error("Interrupted while waiting for response."
                                 , exc);
                }
            }
        }
    }
}
