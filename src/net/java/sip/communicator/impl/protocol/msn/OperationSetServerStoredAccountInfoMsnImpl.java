/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.io.*;
import java.util.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.sf.jml.*;

import javax.imageio.*;

/**
 * Saves account avatar image. If one is already saved we set it as initial one
 * for the MsnOwner.
 *
 * @author SR
 * @author Damian Minkov
 */
public class OperationSetServerStoredAccountInfoMsnImpl
    extends AbstractOperationSetServerStoredAccountInfo
    implements RegistrationStateChangeListener
{
    /**
     * Logger for this class.
     */
    private static final Logger logger =
            Logger.getLogger(OperationSetServerStoredAccountInfoMsnImpl.class);

    /**
     * The msn provider that created us.
     */
    private ProtocolProviderServiceMsnImpl msnProvider = null;
    /**
     * Our account uin=email address.
     */
    private String uin = null;

    /**
     * A place to store our own picture.
     */
    private static final String STORE_DIR = "avatarcache" + File.separator;

    /**
     * Here is kept all the details retrieved so far.
     */
    private Hashtable<String,List<GenericDetail>> retrievedDetails
            = new Hashtable<String,List<GenericDetail>>();

    /**
     * Constructor.
     * @param msnProvider MSN service provider
     * @param uin MSN UIN
     */
    protected OperationSetServerStoredAccountInfoMsnImpl(
            ProtocolProviderServiceMsnImpl msnProvider,
            String uin)
    {
        this.msnProvider = msnProvider;
        this.uin = uin;

        this.msnProvider.addRegistrationStateChangeListener(this);
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

        List<GenericDetail> details = getContactDetails(uin);
        List<GenericDetail> result = new LinkedList<GenericDetail>();

        Iterator<GenericDetail> iter = details.iterator();
        while (iter.hasNext())
        {
            GenericDetail item = iter.next();
            if (detailClass.isInstance(item))
            {
                result.add(item);
            }
        }

        return result.iterator();
    }

    /**
     * request the full info for the given contactAddress
     * waits and return this details
     *
     * @param contactAddress String
     * @return Vector the details
     */
    List<GenericDetail> getContactDetails(String contactAddress)
    {
        List<GenericDetail> result = retrievedDetails.get(contactAddress);

        if (result == null)
        {
            result = new LinkedList<GenericDetail>();
            try
            {
                MsnMessenger messenger = msnProvider.getMessenger();

                if (messenger == null)
                {
                    return null;
                }

                Email email = Email.parseStr(contactAddress);

                String tmp = null;
                byte[] imageBytes;
                if (messenger.getOwner().getEmail().equals(email))
                {
                    MsnOwner owner = messenger.getOwner();
                    tmp = owner.getDisplayName();
                    result.add(new ServerStoredDetails.DisplayNameDetail(tmp));

                    MsnObject image = owner.getDisplayPicture();
                    if (image != null)
                    {
                        imageBytes = image.getMsnObj();
                        if (imageBytes != null && imageBytes.length > 0)
                        {
                            result.add(new ServerStoredDetails.ImageDetail(
                                    "Image", imageBytes));
                        }
                    }
                } else
                {
                    MsnContact contact =
                            messenger.getContactList().getContactByEmail(email);
                    tmp = contact.getDisplayName();
                    result.add(new ServerStoredDetails.DisplayNameDetail(tmp));
                    imageBytes = contact.getAvatar().getMsnObj();
                    if (imageBytes != null && imageBytes.length > 0)
                    {
                        result.add(new ServerStoredDetails.ImageDetail(
                                "Image", imageBytes));
                    }
                }

            } catch (Exception exc)
            {
                logger.error("Cannot load details for contact " + this
                        + " : " + exc.getMessage(), exc);
            }
        }

        retrievedDetails.put(contactAddress, result);

        return new LinkedList<GenericDetail>(result);
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

        return getDetails(uin, detailClass);
    }

    /**
     * Returns all details currently available and set for our account.
     * <p>
     * @return a java.util.Iterator over all details currently set our account.
     */
    public Iterator<GenericDetail> getAllAvailableDetails()
    {
        assertConnected();

        return getContactDetails(uin).iterator();
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
        List<GenericDetail> details = getContactDetails(uin);
        List<Class<? extends GenericDetail>> result
                = new LinkedList<Class<? extends GenericDetail>>();

        Iterator<GenericDetail> iter = details.iterator();
        while (iter.hasNext())
        {
            GenericDetail obj = iter.next();
            result.add(obj.getClass());
        }

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
        List<GenericDetail> details = getContactDetails(uin);
        Iterator<GenericDetail> iter = details.iterator();
        while (iter.hasNext())
        {
            GenericDetail obj = iter.next();
            if (detailClass.isAssignableFrom(obj.getClass()))
            {
                return true;
            }
        }
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
     * returns the user details from the specified class
     * exactly that class not its descendants
     *
     * @param uin String
     * @param detailClass Class
     * @return Iterator
     */
    private Iterator<GenericDetail> getDetails(String uin,
            Class<? extends GenericDetail> detailClass)
    {
        List<GenericDetail> details = getContactDetails(uin);
        List<GenericDetail> result = new LinkedList<GenericDetail>();

        Iterator<GenericDetail> iter = details.iterator();
        while (iter.hasNext())
        {
            GenericDetail item = iter.next();
            if (detailClass.equals(item.getClass()))
            {
                result.add(item);
            }
        }

        return result.iterator();
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

        /*Currently as the function only provied the list of classes that currently have data associatd with them
         * in Jabber InfoRetreiver we have to skip this check*/
//        if (!isDetailClassSupported(detail.getClass())) {
//            throw new IllegalArgumentException(
//                    "implementation does not support such details " +
//                    detail.getClass());
//        }

        Iterator<GenericDetail> iter = getDetails(detail.getClass());
        int currentDetailsSize = 0;
        while (iter.hasNext())
        {
            currentDetailsSize++;
        }
        if (currentDetailsSize >= getMaxDetailInstances(detail.getClass())) {
            throw new ArrayIndexOutOfBoundsException(
                    "Max count for this detail is already reached");
        }

        MsnOwner owner = msnProvider.getMessenger().getOwner();

        if (detail instanceof ImageDetail)
        {
            try
            {
                String path = storePicture(((ImageDetail) detail).getBytes());

                FileInputStream in = new FileInputStream(path);
                byte[] b = new byte[in.available()];
                in.read(b);
                in.close();

                owner.setDisplayPicture(MsnObject.getInstance(
                    owner.getEmail().getEmailAddress(),
                    b));
            } catch(Exception e)
            {
                logger.error("Error setting own avatar.", e);

                // on error return to skip details change
                return;
            }
        }

        fireServerStoredDetailsChangeEvent(msnProvider,
                ServerStoredDetailsChangeEvent.DETAIL_ADDED,
                null,
                detail);
    }

    /**
     * Stores the picture.
     * @param data data to store
     * @return the picture path.
     * @throws Exception if the storage of the picture failed
     */
    private String storePicture(byte[] data)
        throws Exception
    {
        String imagePath = STORE_DIR
                + msnProvider.getAccountID().getAccountUniqueID() + ".jpg";

        File storeDir = MsnActivator.getFileAccessService()
            .getPrivatePersistentDirectory(STORE_DIR);

        // if dir doesn't exist create it
        storeDir.mkdirs();

        File file = MsnActivator.getFileAccessService()
            .getPrivatePersistentFile(imagePath);

        ImageIO.write(
            ImageIO.read(new ByteArrayInputStream(data)),
            "jpg",
            file);

        return file.getPath();
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
     * @return true if the operation was a success or false if
     * currentDetailValue did not previously exist (in this case an additional
     * call to addDetail is required).
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
            throw new ClassCastException("New value to be replaced is not " +
                    "as the current one");
        }
        // if values are the same no change
        if (currentDetailValue.equals(newDetailValue))
        {
            return true;
        }

        boolean isFound = false;
        Iterator<GenericDetail> iter = getDetails(uin, currentDetailValue.getClass());
        while (iter.hasNext())
        {
            GenericDetail item = iter.next();
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

        MsnOwner owner = msnProvider.getMessenger().getOwner();

        if (newDetailValue instanceof ImageDetail)
        {
            try
            {
                String path = storePicture(
                    ((ImageDetail) newDetailValue).getBytes());

                FileInputStream in = new FileInputStream(path);
                byte[] b = new byte[in.available()];
                in.read(b);
                in.close();

                owner.setDisplayPicture(MsnObject.getInstance(
                    owner.getEmail().getEmailAddress(),
                    b));

                fireServerStoredDetailsChangeEvent(msnProvider,
                        ServerStoredDetailsChangeEvent.DETAIL_REPLACED,
                        currentDetailValue,
                        newDetailValue);

                return true;
            } catch(Exception e)
            {
                logger.error("Error setting own avatar.", e);
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
    private void assertConnected()
            throws IllegalStateException
    {
        if (msnProvider == null)
        {
            throw new IllegalStateException(
                    "The msn provider must be non-null and signed on "
                    + "before being able to communicate.");
        }

        if (!msnProvider.isRegistered())
        {
            throw new IllegalStateException(
                    "The msn provider must be signed on before "
                    + "being able to communicate.");
        }
    }

    /**
     * The method is called by a <code>ProtocolProviderService</code>
     * implementation whenever a change in the registration state of the
     * corresponding provider had occurred.
     *
     * @param evt the event describing the status change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if(evt.getNewState() == RegistrationState.REGISTERING)
        {
            try
            {
                String imagePath = STORE_DIR
                    + msnProvider.getAccountID().getAccountUniqueID() + ".jpg";

                File file = MsnActivator.getFileAccessService()
                    .getPrivatePersistentFile(imagePath);

                if(file.exists())
                {
                    FileInputStream in = new FileInputStream(file);
                    byte[] b = new byte[in.available()];
                    in.read(b);
                    in.close();

                    MsnOwner owner = msnProvider.getMessenger().getOwner();

                    owner.setInitDisplayPicture(MsnObject.getInstance(
                        owner.getEmail().getEmailAddress(),
                        b));
                }
            }
            catch(Exception ex)
            {
                logger.error("Cannot obtain own avatar image.", ex);
            }
        }
    }
}
