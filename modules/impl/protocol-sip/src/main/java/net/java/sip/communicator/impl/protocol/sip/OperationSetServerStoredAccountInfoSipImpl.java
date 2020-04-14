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
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.DisplayNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.ImageDetail;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * SIP server stored account information. Supports the user avatar during
 * pres-content specification.
 *
 * @author Grigorii Balutsel
 */
public class OperationSetServerStoredAccountInfoSipImpl
        extends AbstractOperationSetServerStoredAccountInfo
        implements RegistrationStateChangeListener
{
    /**
     * Logger class.
     */
    private static final Logger logger =
            Logger.getLogger(OperationSetServerStoredAccountInfoSipImpl.class);

    /**
     * The provider that is on top of us.
     */
    private ProtocolProviderServiceSipImpl provider;

    /**
     * Current image.
     */
    private ImageDetail accountImage;

    /**
     * Current display name if set.
     */
    private DisplayNameDetail displayNameDetail;

    /**
     * Flag whether account image is loaded.
     */
    private boolean isAccountImageLoaded = false;

    /**
     * Creates this op.set.
     * @param provider the parent provider.
     */
    public OperationSetServerStoredAccountInfoSipImpl(
            ProtocolProviderServiceSipImpl provider)
    {
        this.provider = provider;
        this.provider.addRegistrationStateChangeListener(this);
    }

    /**
     * Returns an iterator over all details of the specified class.
     *
     * @param detailClass one of the detail classes defined in the
     *                    ServerStoredDetails class, indicating the kind of
     *                    details we're interested in.
     * @return a java.util.Iterator over all details that are instances or
     *         descendants of the specified class.
     */
    public <T extends GenericDetail> Iterator<T> getDetailsAndDescendants(
            Class<T> detailClass)
    {
        List<T> result = new Vector<T>();

        if (ImageDetail.class.isAssignableFrom(detailClass) &&
                isImageDetailSupported())
        {
            ImageDetail imageDetail = getAccountImage();
            if (imageDetail != null)
            {
                @SuppressWarnings("unchecked")
                T t = (T) getAccountImage();

                result.add(t);
            }
        }
        else if(DisplayNameDetail.class.isAssignableFrom(detailClass)
                && displayNameDetail != null)
        {
            @SuppressWarnings("unchecked")
            T t = (T) displayNameDetail;

            result.add(t);
        }

        return result.iterator();
    }

    /**
     * Returns an iterator over all details that are instances of exactly the
     * same class as the one specified.
     *
     * @param detailClass one of the detail classes defined in the
     *                    ServerStoredDetails class, indicating the kind of
     *                    details we're interested in.
     * @return a java.util.Iterator over all details of specified class.
     */
    public Iterator<GenericDetail> getDetails(
            Class<? extends GenericDetail> detailClass)
    {
        List<GenericDetail> result = new ArrayList<GenericDetail>();
        if (ImageDetail.class.isAssignableFrom(detailClass) &&
                isImageDetailSupported())
        {
            ImageDetail imageDetail = getAccountImage();
            if (imageDetail != null)
            {
                result.add(getAccountImage());
            }
        }
        else if(DisplayNameDetail.class.isAssignableFrom(detailClass)
                && displayNameDetail != null)
        {
            result.add(displayNameDetail);
        }

        return result.iterator();
    }

    /**
     * Returns all details currently available and set for our account.
     *
     * @return a java.util.Iterator over all details currently set our account.
     */
    public Iterator<GenericDetail> getAllAvailableDetails()
    {
        List<GenericDetail> details = new ArrayList<GenericDetail>();
        if (isImageDetailSupported())
        {
            ImageDetail imageDetail = getAccountImage();
            if (imageDetail != null)
            {
                details.add(getAccountImage());
            }
        }

        if(displayNameDetail != null)
        {
            details.add(displayNameDetail);
        }

        return details.iterator();
    }

    /**
     * Returns all detail Class-es that the underlying implementation supports
     * setting.
     *
     * @return a java.util.Iterator over all detail classes supported by the
     *         implementation.
     */
    public Iterator<Class<? extends GenericDetail>> getSupportedDetailTypes()
    {
        List<Class<? extends GenericDetail>> result =
                new Vector<Class<? extends GenericDetail>>();
        if (isImageDetailSupported())
        {
            result.add(ImageDetail.class);
        }

        result.add(DisplayNameDetail.class);

        return result.iterator();
    }

    /**
     * Determines whether a detail class represents a detail supported by the
     * underlying implementation or not.
     *
     * @param detailClass the class the support for which we'd like to
     *                    determine.
     * @return true if the underlying implementation supports setting details of
     *         this type and false otherwise.
     */
    public boolean isDetailClassSupported(
            Class<? extends GenericDetail> detailClass)
    {
        return (ImageDetail.class.isAssignableFrom(detailClass)
                && isImageDetailSupported())
                || DisplayNameDetail.class.isAssignableFrom(detailClass);
    }

    /**
     * Determines whether the underlying implementation supports the edition
     * of this detail class.
     * <p>
     * @param detailClass the class whose edition we'd like to determine if it's
     * possible
     * @return true if the underlying implementation supports edition of this
     * type of detail and false otherwise.
     */
    public boolean isDetailClassEditable(
        Class<? extends GenericDetail> detailClass)
    {
        return
            isDetailClassSupported(detailClass)
            && ImageDetail.class.isAssignableFrom(detailClass);
    }

    /**
     * The method returns the number of instances supported for a particular
     * detail type.
     *
     * @param detailClass GenericDetail subclass
     * @return int the maximum number of detail instances.
     */
    public int getMaxDetailInstances(Class<? extends GenericDetail> detailClass)
    {
        if (ImageDetail.class.isAssignableFrom(detailClass) &&
                isImageDetailSupported())
        {
            return 1;
        }
        else if(DisplayNameDetail.class.isAssignableFrom(detailClass))
        {
            return 1;
        }

        return 0;
    }

    /**
     * Adds the specified detail to the list of details registered on-line
     * for this account.
     *
     * @param detail the detail that we'd like registered on the server.
     * @throws IllegalArgumentException       if such a detail already exists
     *                                        and its max instances number has
     *                                        been atteined or if the underlying
     *                                        implementation does not support
     *                                        setting details of the
     *                                        corresponding class.
     * @throws OperationFailedException       with code Network Failure if
     *                                        putting the new value online has
     *                                        failed.
     * @throws ArrayIndexOutOfBoundsException if the number of instances
     *                                        currently registered by the
     *                                        application is already equal to
     *                                        the maximum number of supported
     *                                        instances.
     */
    public void addDetail(GenericDetail detail)
            throws IllegalArgumentException,
                   OperationFailedException,
                   ArrayIndexOutOfBoundsException
    {
        addDetail(detail, true);
    }

    /**
     * Adds the specified detail to the list of details registered on-line
     * for this account.
     *
     * @param detail the detail that we'd like registered on the server.
     * @param fireChangeEvents whether to fire change events.
     * @throws IllegalArgumentException       if such a detail already exists
     *                                        and its max instances number has
     *                                        been atteined or if the underlying
     *                                        implementation does not support
     *                                        setting details of the
     *                                        corresponding class.
     * @throws OperationFailedException       with code Network Failure if
     *                                        putting the new value online has
     *                                        failed.
     * @throws ArrayIndexOutOfBoundsException if the number of instances
     *                                        currently registered by the
     *                                        application is already equal to
     *                                        the maximum number of supported
     *                                        instances.
     */
    public void addDetail(GenericDetail detail, boolean fireChangeEvents)
            throws IllegalArgumentException, OperationFailedException,
            ArrayIndexOutOfBoundsException
    {
        if (!isDetailClassSupported(detail.getClass()))
        {
            throw new IllegalArgumentException(
                    "Implementation does not support such details " +
                            detail.getClass());
        }
        List<GenericDetail> alreadySetDetails = new Vector<GenericDetail>();
        Iterator<GenericDetail> iter = getDetails(detail.getClass());
        while (iter.hasNext())
        {
            alreadySetDetails.add(iter.next());
        }
        if (alreadySetDetails.size() >=
                getMaxDetailInstances(detail.getClass()))
        {
            throw new ArrayIndexOutOfBoundsException(
                    "Max count for this detail is already reached");
        }
        if (ImageDetail.class.isAssignableFrom(detail.getClass()) &&
                isImageDetailSupported())
        {
            ImageDetail imageDetail = (ImageDetail) detail;
            putImageDetail(imageDetail);
            accountImage = imageDetail;
            isAccountImageLoaded = true;
        }
        else if(DisplayNameDetail.class.isAssignableFrom(detail.getClass()))
        {
            displayNameDetail = (DisplayNameDetail)detail;
        }

        if(fireChangeEvents)
            fireServerStoredDetailsChangeEvent(provider,
                    ServerStoredDetailsChangeEvent.DETAIL_ADDED,
                    null,
                    detail);
    }

    /**
     * Removes the specified detail from the list of details stored online for
     * this account.
     *
     * @param detail the detail to remove
     * @return true if the specified detail existed and was successfully removed
     *         and false otherwise.
     * @throws OperationFailedException with code Network Failure if removing
     *                                  the detail from the server has failed
     */
    public boolean removeDetail(GenericDetail detail)
            throws OperationFailedException
    {
        return removeDetail(detail, true);
    }

    /**
     * Removes the specified detail from the list of details stored online for
     * this account.
     *
     * @param detail the detail to remove
     * @param fireChangeEvents whether to fire change events.
     * @return true if the specified detail existed and was successfully removed
     *         and false otherwise.
     * @throws OperationFailedException with code Network Failure if removing
     *                                  the detail from the server has failed
     */
    private boolean removeDetail(GenericDetail detail, boolean fireChangeEvents)
            throws OperationFailedException
    {
        boolean isFound = false;
        Iterator<?> iter = getAllAvailableDetails();
        while (iter.hasNext())
        {
            GenericDetail item = (GenericDetail) iter.next();
            if (item.equals(detail))
            {
                isFound = true;
            }
        }
        // Current detail value does not exist
        if (!isFound)
        {
            return false;
        }
        if (ImageDetail.class.isAssignableFrom(detail.getClass()) &&
                isImageDetailSupported())
        {
            deleteImageDetail();
            accountImage = null;
        }

        if(fireChangeEvents)
            fireServerStoredDetailsChangeEvent(provider,
                    ServerStoredDetailsChangeEvent.DETAIL_REMOVED,
                    detail,
                    null);

        return true;
    }

    /**
     * Replaces the currentDetailValue detail with newDetailValue and returns
     * true if the operation was a success or false if currentDetailValue did
     * not previously exist (in this case an additional call to addDetail is
     * required).
     *
     * @param currentDetailValue the detail value we'd like to replace.
     * @param newDetailValue     the value of the detail that we'd like to
     *                           replace currentDetailValue with.
     * @return             true if the operation was a success or false if
     *                     currentDetailValue did not previously exist (in this
     *                     case an additional call to addDetail is required)
     * @throws ClassCastException       if newDetailValue is not an instance of
     *                                  the same class as currentDetailValue.
     * @throws OperationFailedException with code Network Failure if putting the
     *                                  new value back online has failed
     */
    public boolean replaceDetail(
            GenericDetail currentDetailValue,
            GenericDetail newDetailValue)
            throws ClassCastException, OperationFailedException
    {
        if (!newDetailValue.getClass().equals(currentDetailValue.getClass()))
        {
            throw new ClassCastException(
                    "New value to be replaced is not as the current one");
        }
        // If values are the same no change
        if (currentDetailValue.equals(newDetailValue))
        {
            return true;
        }
        removeDetail(currentDetailValue, false);
        addDetail(newDetailValue, false);

        fireServerStoredDetailsChangeEvent(provider,
                        ServerStoredDetailsChangeEvent.DETAIL_REPLACED,
                        currentDetailValue,
                        newDetailValue);

        return true;
    }

    /*
     * (non-Javadoc)
     * @see net.java.sip.communicator.service.protocol.OperationSetServerStoredAccountInfo#save()
     * This method is currently unimplemented.
     * The idea behind this method is for users to call it only once, meaning 
     * that all ServerStoredDetails previously modified by addDetail/removeDetail
     * and/or replaceDetail will be saved online on the server in one step.
     * Currently, addDetail/removeDetail/replaceDetail methods are doing the
     * actual saving but in the future the saving part must be carried here. 
     */
    public void save() throws OperationFailedException {}

    /**
     * Determines if image details is supported.
     *
     * @return true if supported, false otherwise.
     */
    private boolean isImageDetailSupported()
    {
        OperationSetPresenceSipImpl opSet = (OperationSetPresenceSipImpl)
            provider.getOperationSet(OperationSetPersistentPresence.class);

        if(opSet == null)
            return false;

        return opSet.getSsContactList().isAccountImageSupported();
    }

    /**
     * Gets the user avatar from the server or returns cached value.
     *
     * @return the image detail.
     */
    private ImageDetail getAccountImage()
    {
        if (isAccountImageLoaded)
        {
            return accountImage;
        }
        isAccountImageLoaded = true;
        try
        {
            accountImage = getImageDetail();
        }
        catch (OperationFailedException e)
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Avatar image cannot be loaded", e);
            }
        }
        return accountImage;
    }

    /**
     * Gets image detail from the XCAP server.
     *
     * @return the image detail.
     * @throws OperationFailedException if there is some error during operation.
     */
    private ImageDetail getImageDetail()
        throws OperationFailedException
    {
        OperationSetPresenceSipImpl opSet = (OperationSetPresenceSipImpl)
            provider.getOperationSet(OperationSetPersistentPresence.class);

        if(opSet == null)
            return null;

        return opSet.getSsContactList().getAccountImage();
    }

    /**
     * Puts the image detail to the XCAP server.
     *
     * @param imageDetail the image detail.
     * @throws OperationFailedException if there is some error during operation.
     */
    private void putImageDetail(ImageDetail imageDetail)
            throws OperationFailedException
    {
        OperationSetPresenceSipImpl opSet = (OperationSetPresenceSipImpl)
            provider.getOperationSet(OperationSetPersistentPresence.class);

        if(opSet == null)
            return;

        opSet.getSsContactList().setAccountImage(imageDetail.getBytes());
    }

    /**
     * Deletes the image detail from the XCAP server.
     *
     * @throws OperationFailedException if there is some error during operation.
     */
    private void deleteImageDetail()
            throws OperationFailedException
    {
        OperationSetPresenceSipImpl opSet = (OperationSetPresenceSipImpl)
            provider.getOperationSet(OperationSetPersistentPresence.class);

        if(opSet == null)
            return;

        opSet.getSsContactList().deleteAccountImage();
    }

    /**
     * Lister method for protocol provider registration event. If state is
     * UNREGISTERED or CONNECTION_FAILED it will clear the cache.
     *
     * @param evt the event describing the status change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if (evt.getNewState().equals(RegistrationState.UNREGISTERED) ||
                evt.getNewState().equals(
                        RegistrationState.AUTHENTICATION_FAILED) ||
                evt.getNewState().equals(RegistrationState.CONNECTION_FAILED))
        {
            isAccountImageLoaded = false;
            accountImage = null;
        }
    }

    /**
     * Changes the display name string.
     */
    void setOurDisplayName(String newDisplayName)
    {
        DisplayNameDetail oldDisplayName = displayNameDetail;
        DisplayNameDetail newDisplayNameDetail
                = new DisplayNameDetail(newDisplayName);

        List<GenericDetail> alreadySetDetails = new Vector<GenericDetail>();
        Iterator<GenericDetail> iter
                = getDetails(newDisplayNameDetail.getClass());
        while (iter.hasNext())
        {
            alreadySetDetails.add(iter.next());
        }

        try
        {
            if(alreadySetDetails.size() > 0)
                replaceDetail(oldDisplayName, newDisplayNameDetail);
            else
                addDetail(newDisplayNameDetail);
        }
        catch(OperationFailedException e)
        {
            logger.error("Filed to set display name", e);
        }
    }

    /**
     * Frees allocated resources.
     */
    void shutdown()
    {
        provider.removeRegistrationStateChangeListener(this);
    }
}
