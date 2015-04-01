/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.busylampfield;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.customcontactactions.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import java.util.*;

/**
 * Custom acction to show pickup call button.
 *
 * @author Damian Minkov
 */
public class CustomActionsBLFSourceContact
    implements CustomContactActionsService<SourceContact>
{
    /**
     * Our logger.
     */
    private final Logger logger = Logger.getLogger(
        CustomActionsBLFSourceContact.class);

    /**
     * List of actions.
     */
    private final List<ContactAction<SourceContact>> actionsList
        = new LinkedList<ContactAction<SourceContact>>();

    public CustomActionsBLFSourceContact()
    {
        actionsList.add(new PickupAction());
    }

    /**
     * Returns the template class that this service has been initialized with
     *
     * @return the template class
     */
    @Override
    public Class<SourceContact> getContactSourceClass()
    {
        return SourceContact.class;
    }

    /**
     * Returns all custom actions defined by this service.
     *
     * @return an iterator over a list of <tt>ContactAction</tt>s
     */
    @Override
    public Iterator<ContactAction<SourceContact>> getCustomContactActions()
    {
        return actionsList.iterator();
    }

    /**
     * Returns all custom actions menu items defined by this service.
     *
     * @return an iterator over a list of <tt>ContactActionMenuItem</tt>s
     */
    @Override
    public Iterator<ContactActionMenuItem<SourceContact>>
        getCustomContactActionsMenuItems()
    {
        return null;
    }

    /**
     * Action that represents pickup button.
     */
    private class PickupAction
        implements ContactAction<SourceContact>
    {
        /**
         * Invoked when an action occurs.
         *
         * @param actionSource the source of the action
         * @param x            the x coordinate of the action
         * @param y            the y coordinate of the action
         */
        @Override
        public void actionPerformed(final SourceContact actionSource, int x, int y)
            throws
            OperationFailedException
        {
            if(!(actionSource instanceof BLFSourceContact))
                return;

            new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        OperationSetTelephonyBLF.Line line
                            = ((BLFSourceContact)actionSource).getLine();
                        OperationSetTelephonyBLF opset =
                            line.getProvider().getOperationSet(
                                OperationSetTelephonyBLF.class);
                        opset.pickup(line);
                    }
                    catch(Exception ex)
                    {
                        logger.error("Error picking up call", ex);
                    }
                }
            }).start();
        }

        /**
         * The icon used by the UI to visualize this action.
         * @return the button icon.
         */
        @Override
        public byte[] getIcon()
        {
            return ResourceManagementServiceUtils.getService(
                BLFActivator.bundleContext).getImageInBytes(
                    "plugin.busylampfield.PICKUP_CALL");
        }

        /**
         * The icon used by the UI to visualize this action.
         * @return the button icon.
         */
        @Override
        public byte[] getRolloverIcon()
        {
            return ResourceManagementServiceUtils.getService(
                BLFActivator.bundleContext).getImageInBytes(
                "plugin.busylampfield.PICKUP_CALL_ROLLOVER");
        }

        /**
         * The icon used by the UI to visualize this action.
         * @return the button icon.
         */
        @Override
        public byte[] getPressedIcon()
        {
            return ResourceManagementServiceUtils.getService(
                BLFActivator.bundleContext).getImageInBytes(
                "plugin.busylampfield.PICKUP_CALL_PRESSED");
        }

        /**
         * Returns the tool tip text of the component to create for this contact
         * action.
         *
         * @return the tool tip text of the component to create for this contact
         * action
         */
        @Override
        public String getToolTipText()
        {
            return ResourceManagementServiceUtils.getService(
                BLFActivator.bundleContext).getI18NString(
                "plugin.busylampfield.PICKUP");
        }

        /**
         * Indicates if this action is visible for the
         * given <tt>actionSource</tt>.
         *
         * @param actionSource the action source for which we're verifying the
         * action.
         * @return <tt>true</tt> if the action should be visible for the given
         * <tt>actionSource</tt>, <tt>false</tt> - otherwise
         */
        @Override
        public boolean isVisible(SourceContact actionSource)
        {
            if(actionSource.getPresenceStatus()
                    .equals(BLFPresenceStatus.BLF_RINGING))
                return true;
            else
                return false;
        }
    }
}