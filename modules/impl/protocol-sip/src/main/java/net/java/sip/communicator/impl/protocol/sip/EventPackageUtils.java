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

import javax.sip.*;

/**
 * Implements utility methods to aid the manipulation of {@link Dialog}
 * instances and extend the mentioned type with additional functionality.
 *
 * @author Lubomir Marinov
 */
public final class EventPackageUtils
{

    /**
     * Associates a specific subscription with the a given <tt>Dialog</tt>
     * in order to allow it to keep the dialog in question alive even after a
     * BYE request.
     *
     * @param dialog the <tt>Dialog</tt> to associate the subscription with
     * and to be kept alive after a BYE request because of the subscription
     * @param subscription the subscription to be associated with
     * <tt>dialog</tt> and keep it alive after a BYE request
     *
     * @return <tt>true</tt> if the specified subscription was associated with
     * the given dialog; <tt>false</tt> if no changes were applied
     *
     * @throws SipException if the dialog is already terminated.
     */
    public static boolean addSubscription(Dialog dialog, Object subscription)
        throws SipException
    {
        synchronized (dialog)
        {
            DialogApplicationData appData =
                (DialogApplicationData) SipApplicationData.getApplicationData(
                        dialog, SipApplicationData.KEY_SUBSCRIPTIONS);
            if (appData == null)
            {
                appData = new DialogApplicationData();
                SipApplicationData.setApplicationData(
                    dialog, SipApplicationData.KEY_SUBSCRIPTIONS, appData);
            }

            if (appData.addSubscription(subscription))
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
                    appData.removeSubscription(subscription);
                    throw ex;
                }
            }
            return false;
        }
    }

    /**
     * Determines whether a BYE request has already been processed in a specific
     * <tt>Dialog</tt> and thus allows determining whether the dialog in
     * question should be terminated when the last associated subscription is
     * terminated.
     *
     * @param dialog the <tt>Dialog</tt> to be examined
     *
     * @return <tt>true</tt> if a BYE request has already been processed in the
     * specified <tt>dialog</tt>; <tt>false</tt>, otherwise
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
     * Processes a BYE request in a specific <tt>Dialog</tt> for the
     * purposes of subscription associations and returns an indicator which
     * determines whether the specified dialog should still be considered alive
     * after the processing of the BYE request.
     *
     * @param dialog the <tt>Dialog</tt> in which a BYE request has arrived
     *
     * @return <tt>true</tt> if <tt>dialog</tt> should still be considered
     * alive after processing the mentioned BYE request; <tt>false</tt> if
     * <tt>dialog</tt> is to be expected to die after processing the request in
     * question
     *
     * @throws SipException if the dialog is already terminated.
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
     * Dissociates a specific subscription with a given <tt>Dialog</tt> in
     * order to no longer allow it to keep the dialog in question alive even
     * after a BYE request, deletes the dialog if there are no other
     * subscriptions associated with it and a BYE request has already been
     * received and returns an indicator which determines whether the specified
     * dialog is still alive after the dissociation of the given subscription.
     *
     * @param dialog the <tt>Dialog</tt> to dissociate the subscription with
     * and to no longer be kept alive after a BYE request because of the
     * subscription
     * @param subscription the subscription to be dissociated with
     * <tt>dialog</tt> and to no longer be kept alive after a BYE request
     * because of the subscription
     * @return <tt>true</tt> if the dialog is still alive after the
     * dissociation; <tt>false</tt> if the dialog was terminated because of the
     * dissociation
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
     * Prevents the creation of <tt>DialogUtils</tt> instances.
     */
    private EventPackageUtils()
    {
    }

    /**
     * Represents the application-specific data which the SIP protocol provider
     * associates with <tt>javax.sip.Dialog</tt> instances.
     * <p>
     * The implementation at the time of this writing allows tracking
     * subscriptions in a specific <tt>Dialog</tt> in order to make it
     * possible to determine whether a BYE request should terminate the
     * respective dialog.
     * </p>
     */
    private static class DialogApplicationData
    {

        /**
         * The indicator which determines whether a BYE request has already been
         * processed in the owning <tt>Dialog</tt> and thus allows
         * determining whether the dialog in question should be terminated when
         * the last associated subscription is terminated.
         */
        private boolean byeIsProcessed;

        /**
         * The set of subscriptions not yet terminated in the owning
         * <tt>Dialog</tt> i.e. keeping it alive even after a BYE request.
         */
        private final List<Object> subscriptions = new ArrayList<Object>();

        /**
         * Associates a specific subscription with the owning
         * <tt>Dialog</tt> in order to allow it to keep the dialog in
         * question alive even after a BYE request.
         *
         * @param subscription the subscription with no specific type of
         * interest to this implementation to be associated with the
         * owning <tt>Dialog</tt>
         * @return <tt>true</tt> if the specified subscription caused a
         * modification of the list of associated subscriptions; <tt>false</tt>
         * if no change to the mentioned list was applied
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
         * owning <tt>Dialog</tt> and thus allows determining whether the
         * dialog in question should be terminated when the last associated
         * subscription is terminated.
         *
         * @return <tt>true</tt> if a BYE request has already been processed in
         * the owning <tt>Dialog</tt>; <tt>false</tt>, otherwise
         */
        public boolean isByeProcessed()
        {
            return byeIsProcessed;
        }

        /**
         * Determines the number of subscriptions associated with the owning
         * <tt>Dialog</tt> i.e. keeping it alive even after a BYE request.
         *
         * @return the number of subscriptions associated with the owning
         * <tt>Dialog</tt> i.e. keeping it alive even after a BYE request
         */
        public int getSubscriptionCount()
        {
            return subscriptions.size();
        }

        /**
         * Dissociates a specific subscription with the owning
         * <tt>Dialog</tt> in order to no longer allow it to keep the dialog
         * in question alive even after a BYE request.
         *
         * @param subscription the subscription with no specific type of
         * interest to this implementation to be dissociated with the owning
         * <tt>Dialog</tt>
         * @return <tt>true</tt> if the specified subscription caused a
         * modification of the list of associated subscriptions; <tt>false</tt>
         * if no change to the mentioned list was applied
         */
        public boolean removeSubscription(Object subscription)
        {
            return subscriptions.remove(subscription);
        }

        /**
         * Sets the indicator which determines whether a BYE request has already
         * been processed in the owning <tt>Dialog</tt> and thus allows
         * determining whether the dialog in question should be terminated when
         * the last associated subscription is terminated.
         *
         * @param byeIsProcessed <tt>true</tt> if a BYE request has already been
         * processed in the owning <tt>Dialog</tt>; <tt>false</tt>, otherwise
         */
        public void setByeProcessed(boolean byeIsProcessed)
        {
            this.byeIsProcessed = byeIsProcessed;
        }
    }
}
