/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.impl.protocol.sip.xcap.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.prescontent.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import java.util.*;

/**
 * SIP server stored account information. Supports the user avatar during
 * pres-content specification.
 *
 * @author Grigorii Balutsel
 */
public class OperationSetServerStoredAccountInfoSipImpl
        implements OperationSetServerStoredAccountInfo,
        RegistrationStateChangeListener
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
    public Iterator<GenericDetail> getDetailsAndDescendants(
            Class<? extends GenericDetail> detailClass)
    {
        List<GenericDetail> result = new Vector<GenericDetail>();
        if (ImageDetail.class.isAssignableFrom(detailClass) &&
                isImageDetailSupported())
        {
            ImageDetail imageDetail = getAccountImage();
            if (imageDetail != null)
            {
                result.add(getAccountImage());
            }
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
        return ImageDetail.class.isAssignableFrom(detailClass) &&
                isImageDetailSupported();
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
        removeDetail(currentDetailValue);
        addDetail(newDetailValue);
        return true;
    }

    /**
     * Determines if image details is supported.
     *
     * @return true if supported, false otherwise.
     */
    private boolean isImageDetailSupported()
    {
        XCapClient xCapClient = provider.getXCapClient();
        return xCapClient != null &&
                xCapClient.isConnected() &&
                xCapClient.isPresContentSupported();
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
    private ImageDetail getImageDetail() throws OperationFailedException
    {
        ImageDetail imageDetail;
        XCapClient xCapClient = provider.getXCapClient();
        try
        {
            ContentType presContent = xCapClient.getPresContent(
                    ProtocolProviderServiceSipImpl.PRES_CONTENT_IMAGE_NAME);
            if (presContent == null)
            {
                return null;
            }
            String description = null;
            byte[] content = null;
            if (presContent.getDescription().size() > 0)
            {
                description = presContent.getDescription().get(0).getValue();
            }
            if (presContent.getData() != null)
            {
                content = Base64.decode(presContent.getData().getValue());
            }
            imageDetail = new ImageDetail(description, content);
        }
        catch (XCapException e)
        {
            throw new OperationFailedException("Cannot get image detail",
                    OperationFailedException.NETWORK_FAILURE);
        }
        return imageDetail;
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
        XCapClient xCapClient = provider.getXCapClient();
        ContentType presContent = new ContentType();
        MimeType mimeType = new MimeType();
        mimeType.setValue("image/png");
        presContent.setMimeType(mimeType);
        EncodingType encoding = new EncodingType();
        encoding.setValue("base64");
        presContent.setEncoding(encoding);
        String encodedImageContent =
                new String(Base64.encode(imageDetail.getBytes()));
        DataType data = new DataType();
        data.setValue(encodedImageContent);
        presContent.setData(data);
        try
        {
            xCapClient.putPresContent(presContent,
                    ProtocolProviderServiceSipImpl.PRES_CONTENT_IMAGE_NAME);
        }
        catch (XCapException e)
        {
            throw new OperationFailedException("Cannot put image detail",
                    OperationFailedException.NETWORK_FAILURE);
        }
    }

    /**
     * Deletes the image detail from the XCAP server.
     *
     * @throws OperationFailedException if there is some error during operation.
     */
    private void deleteImageDetail()
            throws OperationFailedException
    {
        XCapClient xCapClient = provider.getXCapClient();
        try
        {
            xCapClient.deletePresContent(
                    ProtocolProviderServiceSipImpl.PRES_CONTENT_IMAGE_NAME);
        }
        catch (XCapException e)
        {
            throw new OperationFailedException("Cannot delete image detail",
                    OperationFailedException.NETWORK_FAILURE);
        }
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
                evt.getNewState().equals(RegistrationState.CONNECTION_FAILED))
        {
            isAccountImageLoaded = false;
            accountImage = null;
        }
    }
}
