/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.msofficecomm;

import java.beans.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Represents the Java counterpart of a native <tt>IMessenger</tt>
 * implementation.
 *
 * @author Lyubomir Marinov
 */
public class Messenger
{
    static final int CONVERSATION_TYPE_AUDIO = 8;

    static final int CONVERSATION_TYPE_IM = 1;

    static final int CONVERSATION_TYPE_LIVEMEETING = 4;

    static final int CONVERSATION_TYPE_PHONE = 2;

    static final int CONVERSATION_TYPE_PSTN = 32;

    static final int CONVERSATION_TYPE_VIDEO = 16;

    static final int MISTATUS_AWAY = 0x0022;

    static final int MISTATUS_MAY_BE_AVAILABLE = 0x00A2;

    static final int MISTATUS_OFFLINE = 0x0001;

    static final int MISTATUS_ONLINE = 0x0002;

    static final int MISTATUS_UNKNOWN = 0x0000;

    /**
     * The <tt>BundleContext</tt> in which the <tt>msofficecomm</tt> bundle has
     * been started.
     */
    private static BundleContext bundleContext;

    private static final Map<String, Self> selves = new HashMap<String, Self>();

    /**
     * The <tt>ServiceListener</tt> which listens to the <tt>BundleContext</tt>
     * in which the <tt>msofficecomm</tt> bundle has been started for service
     * changes.
     */
    private static final ServiceListener serviceListener
        = new ServiceListener()
        {
            public void serviceChanged(ServiceEvent event)
            {
                Messenger.serviceChanged(event);
            }
        };

    static
    {
        System.loadLibrary("jmsofficecomm");
    }

    private static void addSelf(
            String signinName,
            ProtocolProviderService pps,
            OperationSetPresence presenceOpSet)
    {
        Self self = selves.get(signinName);

        if (self == null)
        {
            self = new Self(signinName);
            selves.put(signinName, self);
        }
        self.addProtocolProviderService(pps, presenceOpSet);
    }

    private static String getSigninName(
            Contact contact,
            ProtocolProviderService pps)
    {
        String address
            = (contact == null)
                ? pps.getAccountID().getAccountAddress()
                : contact.getAddress();
        String signinName;

        if (address.contains("@"))
        {
            String protocol = pps.getProtocolName() + ":";

            if (address.toLowerCase().startsWith(protocol.toLowerCase()))
                signinName = address.substring(protocol.length());
            else
                signinName = address;
        }
        else
            signinName = null;
        return signinName;
    }

    static int getStatus(MessengerContact messengerContact)
    {
        String signinName = messengerContact.signinName;
        int presenceStatus;

        if (signinName == null)
            presenceStatus = Integer.MIN_VALUE;
        else
        {
            Self self = selves.get(signinName);

            if (self == null)
            {
                presenceStatus = Integer.MIN_VALUE;
                for (Self aSelf : selves.values())
                {
                    int aPresenceStatus = aSelf.getPresenceStatus(signinName);

                    if (presenceStatus < aPresenceStatus)
                    {
                        presenceStatus = aPresenceStatus;
                        if (presenceStatus >= PresenceStatus.MAX_STATUS_VALUE)
                            break;
                    }
                }
            }
            else
                presenceStatus = self.getPresenceStatus();
        }

        return presenceStatusToMISTATUS(presenceStatus);
    }

    static boolean isSelf(MessengerContact messengerContact)
    {
        String signinName = messengerContact.signinName;

        return (signinName == null) ? false : selves.containsKey(signinName);
    }

    private static native void onContactStatusChange(
            String signinName,
            int status);

    private static int presenceStatusToMISTATUS(int presenceStatus)
    {
        int mistatus;

        if (presenceStatus == Integer.MIN_VALUE)
            mistatus = MISTATUS_UNKNOWN;
        else
        {
            if (presenceStatus < PresenceStatus.ONLINE_THRESHOLD)
                mistatus = MISTATUS_OFFLINE;
            else if (presenceStatus < PresenceStatus.AWAY_THRESHOLD)
                mistatus = MISTATUS_MAY_BE_AVAILABLE;
            else if (presenceStatus < PresenceStatus.AVAILABLE_THRESHOLD)
                mistatus = MISTATUS_AWAY;
            else
                mistatus = MISTATUS_ONLINE;
        }
        return mistatus;
    }

    private static void removeSelf(
            String signinName,
            ProtocolProviderService pps)
    {
        Self self = selves.get(signinName);

        if ((self != null) && (self.removeProtocolProviderService(pps) < 1))
            selves.remove(signinName);
    }

    private static void serviceChanged(ServiceEvent event)
    {
        Object service = bundleContext.getService(event.getServiceReference());

        if (service instanceof ProtocolProviderService)
        {
            ProtocolProviderService pps = (ProtocolProviderService) service;
            /*
             * The Messenger class implements an integration of Jitsi presence
             * into Microsoft Office so the only accounts of interest to it are
             * the ones which support presence.
             */
            OperationSetPresence presenceOpSet
                = pps.getOperationSet(OperationSetPresence.class);

            if (presenceOpSet != null)
            {
                String signinName = getSigninName(null, pps);

                if (signinName != null)
                {
                    switch (event.getType())
                    {
                    case ServiceEvent.REGISTERED:
                        addSelf(signinName, pps, presenceOpSet);
                        break;
                    case ServiceEvent.UNREGISTERING:
                        removeSelf(signinName, pps);
                        break;
                    }
                }
            }
        }
    }

    static void start(BundleContext bundleContext)
        throws Exception
    {
        Messenger.bundleContext = bundleContext;

        bundleContext.addServiceListener(serviceListener);
        for (ServiceReference reference
                : bundleContext.getServiceReferences(
                        ProtocolProviderService.class.getName(),
                        null))
        {
            serviceListener.serviceChanged(
                    new ServiceEvent(ServiceEvent.REGISTERED, reference));
        }
    }

    static void stop(BundleContext bundleContext)
        throws Exception
    {
        bundleContext.removeServiceListener(serviceListener);

        Messenger.bundleContext = null;
    }

    /**
     * Initializes a new <tt>Messenger</tt> instance which is to represent the
     * Java counterpart of a native <tt>IMessenger</tt> implementation.
     */
    public Messenger()
    {
    }

    public void startConversation(
            final int conversationType,
            final String[] participants)
    {
        SwingUtilities.invokeLater(
                new Runnable()
                {
                    public void run()
                    {
                        BundleContext bundleContext = Messenger.bundleContext;

                        if (bundleContext != null)
                        {
                            UIService uiService
                                = ServiceUtils.getService(
                                        bundleContext,
                                        UIService.class);

                            if (uiService != null)
                            {
                                switch (conversationType)
                                {
                                case CONVERSATION_TYPE_AUDIO:
                                case CONVERSATION_TYPE_PHONE:
                                case CONVERSATION_TYPE_PSTN:
                                    uiService.createCall(participants);
                                    break;
                                case CONVERSATION_TYPE_IM:
                                    uiService.startChat(participants);
                                    break;
                                }
                            }
                        }
                    }
                });
    }

    private static class Self
        implements ContactPresenceStatusListener,
                   ProviderPresenceStatusListener
    {
        private final Map<ProtocolProviderService, OperationSetPresence> ppss
            = new HashMap<ProtocolProviderService, OperationSetPresence>();

        private int presenceStatus = Integer.MIN_VALUE;

        public final String signinName;

        public Self(String signinName)
        {
            this.signinName = signinName;
        }

        void addProtocolProviderService(
                ProtocolProviderService pps,
                OperationSetPresence presenceOpSet)
        {
            if (!ppss.containsKey(pps))
            {
                ppss.put(pps, presenceOpSet);

                presenceOpSet.addContactPresenceStatusListener(this);
                presenceOpSet.addProviderPresenceStatusListener(this);
                providerStatusChanged(null);
            }
        }

        public void contactPresenceStatusChanged(
                ContactPresenceStatusChangeEvent event)
        {
            String signinName
                = getSigninName(
                        event.getSourceContact(),
                        event.getSourceProvider());

            if (signinName != null)
            {
                PresenceStatus oldStatus = event.getOldStatus();

                Messenger.onContactStatusChange(
                        signinName,
                        presenceStatusToMISTATUS(
                                (oldStatus == null)
                                    ? Integer.MIN_VALUE
                                    : oldStatus.getStatus()));
            }
        }

        int getPresenceStatus()
        {
            return presenceStatus;
        }

        int getPresenceStatus(String signinName)
        {
            int presenceStatus;

            if (this.signinName.equals(signinName))
                presenceStatus = getPresenceStatus();
            else
            {
                presenceStatus = Integer.MIN_VALUE;
                for (OperationSetPresence presenceOpSet : ppss.values())
                {
                    try
                    {
                        Contact contact
                            = presenceOpSet.findContactByID(signinName);

                        if (contact != null)
                        {
                            PresenceStatus contactPresenceStatus
                                = contact.getPresenceStatus();

                            if (contactPresenceStatus != null)
                            {
                                int contactStatus
                                    = contactPresenceStatus.getStatus();

                                if (presenceStatus < contactStatus)
                                {
                                    presenceStatus = contactStatus;
                                    if (presenceStatus
                                            >= PresenceStatus.MAX_STATUS_VALUE)
                                        break;
                                }
                            }
                        }
                    }
                    catch (Throwable t)
                    {
                        /*
                         * It does not sound like it makes a lot of sense to
                         * fail the getting of the presence status of the
                         * specified signinName just because one of the possibly
                         * many OperationSetPresence instances has failed.
                         * Additionally, the native counterpart will swallow any
                         * Java exception anyway.
                         */
                        if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                        else
                            t.printStackTrace(System.err);
                    }
                }
            }
            return presenceStatus;
        }

        public void providerStatusChanged(
                ProviderPresenceStatusChangeEvent event)
        {
            PresenceStatus presenceStatus = null;

            for (OperationSetPresence presenceOpSet : ppss.values())
            {
                PresenceStatus presenceOpSetStatus
                    = presenceOpSet.getPresenceStatus();

                if (presenceOpSetStatus != null)
                {
                    if ((presenceStatus == null)
                            || (presenceStatus.compareTo(presenceOpSetStatus)
                                    < 0))
                        presenceStatus = presenceOpSetStatus;
                }
            }

            setPresenceStatus(presenceStatus);
        }

        public void providerStatusMessageChanged(PropertyChangeEvent event) {}

        int removeProtocolProviderService(ProtocolProviderService pps)
        {
            OperationSetPresence presenceOpSet = ppss.get(pps);

            if (presenceOpSet != null)
            {
                presenceOpSet.removeContactPresenceStatusListener(this);
                presenceOpSet.removeProviderPresenceStatusListener(this);
                ppss.remove(pps);
                providerStatusChanged(null);
            }
            return ppss.size();
        }

        private void setPresenceStatus(PresenceStatus presenceStatus)
        {
            int status
                = (presenceStatus == null)
                    ? Integer.MIN_VALUE
                    : presenceStatus.getStatus();

            if (this.presenceStatus != status)
            {
                int oldValue = this.presenceStatus;

                this.presenceStatus = status;

                Messenger.onContactStatusChange(
                        signinName,
                        presenceStatusToMISTATUS(oldValue));
            }
        }
    }
}
