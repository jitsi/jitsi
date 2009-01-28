/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;
import javax.sip.*;

/**
 * Implements utility methods to aid the manipulation of {@link Dialog}
 * instances and extend the mentioned type with additional functionality.
 * 
 * @author Lubomir Marinov
 */
public final class DialogUtils
{

    /**
     * Associates a specific subscription with the a given <code>Dialog</code>
     * in order to allow it to keep the dialog in question alive even after a
     * BYE request.
     * 
     * @param dialog the <code>Dialog</code> to associate the subscription with
     *            and to be kept alive after a BYE request because of the
     *            subscription
     * @param subscription the subscription to be associated with
     *            <code>dialog</code> and keep it alive after a BYE request
     * @return <tt>true</tt> if the specified subscription was associated with
     *         the given dialog; <tt>false</tt> if no changes were applied
     * @throws SipException
     */
    public static boolean addSubscription(Dialog dialog, Object subscription)
        throws SipException
    {
        synchronized (dialog)
        {
            DialogApplicationData applicationData =
                (DialogApplicationData) SipApplicationData.getApplicationData(
                        dialog, SipApplicationData.KEY_SUBSCRIPTIONS);
            if (applicationData == null)
            {
                applicationData = new DialogApplicationData();
                SipApplicationData.setApplicationData(
                        dialog,
                        SipApplicationData.KEY_SUBSCRIPTIONS,
                        applicationData);
            }

            if (applicationData.addSubscription(subscription))
            {
                try
                {
                    dialog.terminateOnBye(false);
                    return true;
                }
                catch (SipException ex)
                {

                    /*
                     * Since the subscription didn't quite register, undo the
                     * part of the registration which did succeed.
                     */
                    applicationData.removeSubscription(subscription);
                    throw ex;
                }
            }
            return false;
        }
    }

    /**
     * Determines whether a BYE request has already been processed in a specific
     * <code>Dialog</code> and thus allows determining whether the dialog in
     * question should be terminated when the last associated subscription is
     * terminated.
     * 
     * @param dialog the <code>Dialog</code> to be examined
     * @return <tt>true</tt> if a BYE request has already been processed in the
     *         specified <code>dialog</code>; <tt>false</tt>, otherwise
     */
    public static boolean isByeProcessed(Dialog dialog)
    {
        synchronized (dialog)
        {
            DialogApplicationData applicationData =
                (DialogApplicationData) SipApplicationData.getApplicationData(
                        dialog, SipApplicationData.KEY_SUBSCRIPTIONS);
            return (applicationData == null) ? false : applicationData
                .isByeProcessed();
        }
    }

    /**
     * Processes a BYE request in a specific <code>Dialog</code> for the
     * purposes of subscription associations and returns an indicator which
     * determines whether the specified dialog should still be considered alive
     * after the processing of the BYE request.
     * 
     * @param dialog the <code>Dialog</code> in which a BYE request has arrived
     * @return <tt>true</tt> if <code>dialog</code> should still be considered
     *         alive after processing the mentioned BYE request; <tt>false</tt>
     *         if <code>dialog</code> is to be expected to die after processing
     *         the request in question
     * @throws SipException
     */
    public static boolean processByeThenIsDialogAlive(Dialog dialog)
        throws SipException
    {
        synchronized (dialog)
        {
            DialogApplicationData applicationData =
                (DialogApplicationData) SipApplicationData.getApplicationData(
                        dialog, SipApplicationData.KEY_SUBSCRIPTIONS);
            if (applicationData != null)
            {
                applicationData.setByeProcessed(true);

                if (applicationData.getSubscriptionCount() > 0)
                {
                    dialog.terminateOnBye(false);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Dissociates a specific subscription with a given <code>Dialog</code> in
     * order to no longer allow it to keep the dialog in question alive even
     * after a BYE request, deletes the dialog if there are no other
     * subscriptions associated with it and a BYE request has already been
     * received and returns an indicator which determines whether the specified
     * dialog is still alive after the dissociation of the given subscription.
     * 
     * @param dialog the <code>Dialog</code> to dissociate the subscription with
     *            and to no longer be kept alive after a BYE request because of
     *            the subscription
     * @param subscription the subscription to be dissociated with
     *            <code>dialog</code> and to no longer be kept alive after a BYE
     *            request because of the subscription
     * @return <tt>true</tt> if the dialog is still alive after the
     *         dissociation; <tt>false</tt> if the dialog was terminated because
     *         of the dissociation
     */
    public static boolean removeSubscriptionThenIsDialogAlive(Dialog dialog,
        Object subscription)
    {
        synchronized (dialog)
        {
            DialogApplicationData applicationData =
                (DialogApplicationData) SipApplicationData.getApplicationData(
                        dialog, SipApplicationData.KEY_SUBSCRIPTIONS);
            if ((applicationData != null)
                && applicationData.removeSubscription(subscription)
                && (applicationData.getSubscriptionCount() <= 0)
                && applicationData.isByeProcessed())
            {
                dialog.delete();
                return false;
            }
            return true;
        }
    }

    /**
     * Prevents the creation of <code>DialogUtils</code> instances.
     */
    private DialogUtils()
    {
    }

    /**
     * Represents the application-specific data which the SIP protocol provider
     * associates with {@link #Dialog} instances.
     * <p>
     * The implementation at the time of this writing allows tracking
     * subscriptions in a specific <code>Dialog</code> in order to make it
     * possible to determine whether a BYE request should terminate the
     * respective dialog.
     * </p>
     */
    private static class DialogApplicationData
    {

        /**
         * The indicator which determines whether a BYE request has already been
         * processed in the owning <code>Dialog</code> and thus allows
         * determining whether the dialog in question should be terminated when
         * the last associated subscription is terminated.
         */
        private boolean byeIsProcessed;

        /**
         * The set of subscriptions not yet terminated in the owning
         * <code>Dialog</code> i.e. keeping it alive even after a BYE request.
         */
        private final List<Object> subscriptions = new ArrayList<Object>();

        /**
         * Associates a specific subscription with the owning
         * <code>Dialog</code> in order to allow it to keep the dialog in
         * question alive even after a BYE request.
         * 
         * @param subscription the subscription with no specific type of
         *            interest to this implementation to be associated with the
         *            owning <code>Dialog</code>
         * @return <tt>true</tt> if the specified subscription caused a
         *         modification of the list of associated subscriptions;
         *         <tt>false</tt> if no change to the mentioned list was applied
         */
        public boolean addSubscription(Object subscription)
        {
            if (!subscriptions.contains(subscription))
            {
                return subscriptions.add(subscription);
            }
            return false;
        }

        /**
         * Determines whether a BYE request has already been processed in the
         * owning <code>Dialog</code> and thus allows determining whether the
         * dialog in question should be terminated when the last associated
         * subscription is terminated.
         * 
         * @return <tt>true</tt> if a BYE request has already been processed in
         *         the owning <code>Dialog</code>; <tt>false</tt>, otherwise
         */
        public boolean isByeProcessed()
        {
            return byeIsProcessed;
        }

        /**
         * Determines the number of subscriptions associated with the owning
         * <code>Dialog</code> i.e. keeping it alive even after a BYE request.
         * 
         * @return the number of subscriptions associated with the owning
         *         <code>Dialog</code> i.e. keeping it alive even after a BYE
         *         request
         */
        public int getSubscriptionCount()
        {
            return subscriptions.size();
        }

        /**
         * Dissociates a specific subscription with the owning
         * <code>Dialog</code> in order to no longer allow it to keep the dialog
         * in question alive even after a BYE request.
         * 
         * @param subscription the subscription with no specific type of
         *            interest to this implementation to be dissociated with the
         *            owning <code>Dialog</code>
         * @return <tt>true</tt> if the specified subscription caused a
         *         modification of the list of associated subscriptions;
         *         <tt>false</tt> if no change to the mentioned list was applied
         */
        public boolean removeSubscription(Object subscription)
        {
            return subscriptions.remove(subscription);
        }

        /**
         * Sets the indicator which determines whether a BYE request has already
         * been processed in the owning <code>Dialog</code> and thus allows
         * determining whether the dialog in question should be terminated when
         * the last associated subscription is terminated.
         * 
         * @param byeIsProcessed <tt>true</tt> if a BYE request has already been
         *            processed in the owning <code>Dialog</code>;
         *            <tt>false</tt>, otherwise
         */
        public void setByeProcessed(boolean byeIsProcessed)
        {
            this.byeIsProcessed = byeIsProcessed;
        }
    }
}
