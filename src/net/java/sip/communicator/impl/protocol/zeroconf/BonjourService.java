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
package net.java.sip.communicator.impl.protocol.zeroconf;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.zeroconf.jmdns.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Class dealing with JmDNS and treating all the
 * incoming connections on the bonjour port
 * @author Christian Vincenot
 */
public class BonjourService extends Thread
    implements  ServiceListener,
                DNSListener
{
    private static final Logger logger =
        Logger.getLogger(BonjourService.class);

    private int port = 5298;
    private ServerSocket sock = null;
    private String id = null;
    private JmDNS jmdns=null;
    private final Map<String, Object> props = new Hashtable<String, Object>();
    private ServiceInfo service = null;
    private boolean dead = false;

    private final List<ContactZeroconfImpl> contacts
        = new Vector<ContactZeroconfImpl>();

    private ProtocolProviderServiceZeroconfImpl pps;
    OperationSetPersistentPresenceZeroconfImpl opSetPersPresence;

    private ZeroconfAccountID acc;

    /* Should maybe better get the status directly from OperationSetPresence */
    private PresenceStatus status = ZeroconfStatusEnum.OFFLINE;

    /**
     * Returns the corresponding ProtocolProviderService
     * @return corresponding ProtocolProviderService
     */
    public ProtocolProviderServiceZeroconfImpl getPPS()
    {
        return pps;
    }

    /**
     * Returns the id of this service.
     * @return returns the id of this service.
     */
    String getID()
    {
        return id;
    }

    /**
     * Creates a new instance of the Bonjour service thread
     * @param port TCP Port number on which to try to start the Bonjour service
     * @param pps ProtocolProviderService instance
     *      which is creating this BonjourService
     */
    public BonjourService(int port,
                          ProtocolProviderServiceZeroconfImpl pps)
    {
        this.acc = (ZeroconfAccountID) pps.getAccountID();
        this.port = port;
        this.id = acc.getUserID();
        this.pps = pps;

        opSetPersPresence =
            (OperationSetPersistentPresenceZeroconfImpl) pps
                .getOperationSet(OperationSetPersistentPresence.class);

        // Gaim
        props.put("1st", (acc.getFirst() == null)? "":acc.getFirst());
        props.put("email", (acc.getMail() == null)? "":acc.getMail());
        props.put("jid", this.id);
        props.put("last", (acc.getLast() == null)?"":acc.getLast());
        props.put("msg", opSetPersPresence.getCurrentStatusMessage());
        props.put("status", "avail");

        //iChat
        props.put("phsh","000");
        //props.put("status","avail");
        //props.put("port.p2pj", "5298");
        props.put("vc", "C!");
        //props.put("1st", "John");
        props.put("txtvers","1");

        //XEP-0174 (Final paper)
        props.put("ext","");
        props.put("nick", (acc.getFirst() == null)? this.id:acc.getFirst());
        props.put("ver", "1");
        props.put("node", "SIP Communicator");

        //Ours
        props.put("client", "SIP Communicator");

        changeStatus(opSetPersPresence.getPresenceStatus());

        sock = createSocket(port);
        if (sock == null)
            return;

        port = sock.getLocalPort();

        if (logger.isDebugEnabled())
            logger.debug("ZEROCONF: ServerSocket bound to port "+port);

        props.put("port.p2pj", Integer.toString(port));
        this.setDaemon(true);
        this.start();
    }

    /* TODO: Better exception checking to avoid sudden exit and bonjour
     * service shutdown */

    /**
     * Walk?
     */
    @Override
    public void run()
    {
        if (logger.isDebugEnabled())
            logger.debug("ZEROCONF: Bonjour Service Thread up and running!");

        /* Put jmDNS in DEBUD Mode :
         * Following verbosity levels can be chosen :
         * "INFO" , "WARNING", "SEVERE", "ALL", "FINE", "FINER", "FINEST", etc
         */
        //System.setProperty("jmdns.debug", "0");

        while (dead == false)
        {
            if (sock == null || sock.isClosed())
            {
                sock = createSocket(port);
                /* What should we do now? TEMPORARY: shutdown()*/
                if (sock == null) shutdown();
                port = sock.getLocalPort();
                props.put("port.p2pj", Integer.toString(port));
                //TODO: update JmDNS in case the port had to be changed!
            }
            try
            {
                Socket connection = sock.accept();
                ContactZeroconfImpl contact = getContact(null,
                                                    connection.getInetAddress());
                /*if (status.equals(ZeroconfStatusEnum.OFFLINE)
                || status.equals(ZeroconfStatusEnum.INVISIBLE) */
                if (dead == true) break;

                if  ((contact == null)
                  || (contact.getClientThread() != null))
                {
                    if (contact == null)
                        logger.error("ZEROCONF: Connexion from "
                                + "unknown contact ["
                                + connection.getInetAddress()
                                +"]. REJECTING!");
                    else if (contact.getClientThread() == null)
                        logger.error("ZEROCONF: Redundant chat "
                                + "channel ["
                                + contact
                                +"]. REJECTING!");
                    connection.close();
                }
                else new ClientThread(connection, this);
            }
            catch(Exception e)
            {
                logger.error(e);
            }
        }

        if (logger.isDebugEnabled())
            logger.debug("ZEROCONF: Going Offline - "
                          +"BonjourService Thread exiting!");
    }

    /**
     * Might be used for shutdown...
     */
    public void shutdown()
    {
        if (logger.isDebugEnabled())
            logger.debug("ZEROCONF: Shutdown!");

        dead = true;
        try
        {   sock.close();   }
        catch (Exception ex)
        {   logger.error(ex);  }

        changeStatus(ZeroconfStatusEnum.OFFLINE);
        if(jmdns != null)
            jmdns.close();
    }

    private ServerSocket createSocket(int port)
    {
        ServerSocket sock=null;
        try
        {
            sock = new ServerSocket(port);
        }
        catch(Exception e)
        {
            logger.error("ZEROCONF: Couldn't bind socket to port "
                               +port+"! Switching to an other port...");
            try
            {
                sock = new ServerSocket(0);
            }
            catch (IOException ex)
            {
                logger.error("ZEROCONF: FATAL ERROR => "
                                  +"Couldn't bind to a port!!", ex);
            }
        }

        return sock;
    }

    /**
     * Changes the status of the local user.
     * @param stat New presence status
     */
    public void changeStatus(PresenceStatus stat)
    {
        /* [old_status == new_status ?] => NOP */
        if (stat.equals(status))
            return;

        /* [new_status == OFFLINE ?] => clean up everything */
        if (stat.equals(ZeroconfStatusEnum.OFFLINE))
        {
            if (logger.isDebugEnabled())
                logger.debug("ZEROCONF: Going OFFLINE");
            //jmdns.unregisterAllServices();
            jmdns.removeServiceListener("_presence._tcp.local.", this);
            jmdns.close();
            jmdns=null;
            //dead = true;

            // Erase all contacts by putting them OFFLINE
            opSetPersPresence.changePresenceStatusForAllContacts(
                    opSetPersPresence.getServerStoredContactListRoot(), stat);

            try
            {
                sleep(1000);
            } catch (InterruptedException ex)
            {
                logger.error(ex);
            }
        }

        /* [old_status == OFFLINE ?] => register service */
        else if (status.equals(ZeroconfStatusEnum.OFFLINE))
        {
            if (logger.isDebugEnabled())
                logger.debug("ZEROCONF: Getting out of OFFLINE state");
            props.put("status", stat.getStatusName());
            service = new ServiceInfo("_presence._tcp.local.", id,
                                      port, 0, 0, props);

            try
            {
                jmdns = new JmDNS();
                jmdns.registerServiceType("_presence._tcp.local.");
                jmdns.addServiceListener("_presence._tcp.local.", this);
                jmdns.registerService(service);

                /* In case the ID had to be changed */
                id = service.getName();
            }
            catch (Exception ex)
            {   logger.error(ex);   }

            //dead = false;

            /* Normal status change */
        }
        else
        {
            if (logger.isDebugEnabled())
                logger.debug("ZEROCONF : Changing status");

            props.put("status", stat.getStatusName());

            /* FIXME: Not totally race condition free since the 3 calls aren't
             * atomic, but that's not really critical since there's little
             * change chance of concurrent local contact change, and this
             * wouldn't have big consequences.
             */
            ServiceInfo info =
               jmdns.getLocalService(id.toLowerCase()+"._presence._tcp.local.");
            if (info == null)
               logger.error("ZEROCONF/JMDNS: PROBLEM GETTING "
                                 +"LOCAL SERVICEINFO !!");

            byte[] old = info.getTextBytes();
            info.setProps(props);
            jmdns.updateInfos(info, old);
        }

        status = stat;
    }

    private class AddThread extends Thread
    {
        private String type, name;
        public AddThread(String type, String name)
        {
            this.setDaemon(true);
            this.type = type;
            this.name = name;
            this.start();
        }

        @Override
        public void run()
        {
            ServiceInfo service = null;
            while ((service == null) && (dead == false)
            && !status.equals(ZeroconfStatusEnum.OFFLINE))
            {
                service = jmdns.getServiceInfo(type, name, 10000);
                if (service == null)
                    logger.error("BONJOUR: ERROR - Service Info of "
                                      + name +" not found in cache!!");
                try
                {
                    sleep(2);
                }
                catch (InterruptedException ex)
                {
                    logger.error(ex);
                }
            }
            if ((dead == false) && !status.equals(ZeroconfStatusEnum.OFFLINE))
                jmdns.requestServiceInfo(type, name);
            //} else handleResolvedService(name, type, service);
        }
    }

    /* Service Listener Implementation */

    /**
     * A service has been added.
     *
     * @param event The ServiceEvent providing the name and fully qualified type
     *              of the service.
     */
    public void serviceAdded(ServiceEvent event)
    {
        /* WARNING: DONT PUT ANY BLOCKING CALLS OR FLAWED LOOPS IN THIS METHOD.
         * JmDNS calls this method without creating a new thread, so if this
         * method doesn't return, jmDNS will hang !!
         */

        String name = event.getName();
        String type = event.getType();

        if (name.equals(id))
            return;

        if (logger.isDebugEnabled())
            logger.debug("BONJOUR: "+name
                          +"["+type+"] detected! Trying to get information...");
        try
        {
            sleep(2);
        }
        catch (InterruptedException ex)
        {
            logger.error(ex);
        }

        jmdns.printServices();

        new AddThread(type, name);
    }



    /**
     * A service has been removed.
     *
     * @param event The ServiceEvent providing the name and fully qualified type
     *              of the service.
     */
    public void serviceRemoved(ServiceEvent event)
    {
        String name = event.getName();
        if (name.equals(id))
            return;

        ContactZeroconfImpl contact = getContact(name, null);

        if(contact == null)
            return;

        opSetPersPresence.changePresenceStatusForContact(contact,
                                            ZeroconfStatusEnum.OFFLINE);
        if (logger.isDebugEnabled())
            logger.debug("BONJOUR: Received announcement that "
                          +name+" went offline!");

    }

    /**
     * A service has been resolved. Its details are now available in the
     * ServiceInfo record.
     *
     * @param event The ServiceEvent providing the name, the fully qualified
     *              type of the service, and the service info record,
     *              or null if the service could not be resolved.
     */
    public void serviceResolved(ServiceEvent event)
    {
        String contactID = event.getName();
        String type = event.getType();
        ServiceInfo info = event.getInfo();

        if (logger.isDebugEnabled())
            logger.debug("BONJOUR:    Information about "
                          +contactID+" discovered");

        handleResolvedService(contactID, type, info);
    }

    private void handleResolvedService(String contactID,
                                       String type,
                                       ServiceInfo info)
    {
        if (contactID.equals(id))
            return;

        if (info.getAddress().toString().length() > 15)
        {
              if (logger.isDebugEnabled())
                  logger.debug("ZEROCONF: Temporarily ignoring IPv6 addresses!");
              return;
        }

        ContactZeroconfImpl newFriend;

        synchronized(this)
        {
            if (getContact(contactID, info.getAddress()) != null)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Contact "
                              +contactID+" already in contact list! Skipping.");
                return;
            };
            if (logger.isDebugEnabled())
                logger.debug("ZEROCNF: ContactID " + contactID +
                " Address " + info.getAddress());

            if (logger.isDebugEnabled())
                logger.debug("            Address=>"+info.getAddress()
                          +":"+info.getPort());

            for (Iterator<String> names = info.getPropertyNames();
                    names.hasNext();)
            {
                String prop = names.next();
                if (logger.isDebugEnabled())
                    logger.debug("            "+prop+"=>"
                              +info.getPropertyString(prop));
            }

            /* Creating the contact */
            String name = info.getPropertyString("1st");
            if (info.getPropertyString("last") != null)
                name += " "+ info.getPropertyString("last");

            int port = Integer.valueOf(
                info.getPropertyString("port.p2pj")).intValue();

            if (port < 1)
            {
                logger.error("ZEROCONF: Flawed contact announced himself"
                              +"without necessary parameters : "+contactID);
                return;
            }

            if (logger.isDebugEnabled())
                logger.debug("ZEROCONF: Detected client "+name);

            newFriend =
                    opSetPersPresence.createVolatileContact(
                        contactID, this, name,
                        info.getAddress(), port);
        }
        /* Try to detect which client type it is */
        int clientType = ContactZeroconfImpl.XMPP;
        if (info.getPropertyString("client") != null
                && info.getPropertyString("client").
                compareToIgnoreCase("SIP Communicator") == 0)
                    clientType = ContactZeroconfImpl.SIPCOM;

        else if ((info.getPropertyString("jid") != null)
              && (info.getPropertyString("node") == null))
                    clientType = ContactZeroconfImpl.GAIM;

        else if (info.getPropertyString("jid") == null)
            clientType = ContactZeroconfImpl.ICHAT;

        newFriend.setClientType(clientType);
        if (logger.isDebugEnabled())
            logger.debug("ZEROCONF: CLIENT TYPE "+clientType);

        ZeroconfStatusEnum status =
                ZeroconfStatusEnum.statusOf(info.getPropertyString("status"));
        opSetPersPresence.
                changePresenceStatusForContact(newFriend,
                               status == null?ZeroconfStatusEnum.ONLINE:status);

        // Listening for changes
        jmdns.addListener(this, new DNSQuestion(info.getQualifiedName(),
                                                DNSConstants.TYPE_SRV,
                                                DNSConstants.CLASS_UNIQUE));
    }

    /**
     * Callback called by JmDNS to inform the
     * BonjourService of a potential status change of some contacts.
     * @param jmdns JmDNS instance responsible for this
     * @param now Timestamp
     * @param record DNSRecord which changed
     */
    public synchronized void updateRecord(  JmDNS jmdns,
                                            long now,
                                            DNSRecord record)
    {
        if (logger.isDebugEnabled())
            logger.debug("ZEROCONF/JMDNS: Received record update for "+record);

        int clazz = record.getClazz();
        int type = record.getType();

        /* Check the info returned by JmDNS since we can't really trust its
         * filtering. */
        if (!(((type & DNSConstants.TYPE_TXT) != 0) &&
                ((clazz & DNSConstants.CLASS_IN) != 0) &&
                record.isUnique() &&
                record.getName().endsWith("_presence._tcp.local.")))
            return;

        String name = record.getName().replaceAll("._presence._tcp.local.","");
        ContactZeroconfImpl contact;

        synchronized(this)
        {
            contact = getContact(name, null);

            if (contact == null) { //return;
                logger.error("ZEROCONF: BUG in jmDNS => Received update without "
                        +"previous contact annoucement. Trying to add contact");
                new AddThread("_presence._tcp.local.", name);
                return;
            }
        }

        if (logger.isDebugEnabled())
            logger.debug("ZEROCONF: "+ name
                         + " changed status. Requesting fresh data!");

        /* Since a record was updated, we can be sure that we can do a blocking
         * getServiceInfo without risk. (Still, we use the method with timeout
         * to avoid bad surprises). If some problems of status change refresh
         * appear, we'll have to fall back on the method with callback as we've
         * done for "ServiceAdded".
         */

        ServiceInfo info = jmdns.getServiceInfo("_presence._tcp.local.", name,
                                                1000);
        if (info == null)
        {
                logger.error("ZEROCONF/JMDNS: Problem!! The service "
                              +"information was not in cache. See comment in "
                              +"BonjourService.java:updateRecord !!");
                return;
        }

        /* Let's change what we can : status, message, etc */
        ZeroconfStatusEnum status =
                ZeroconfStatusEnum.statusOf(info.getPropertyString("status"));

        opSetPersPresence.
                changePresenceStatusForContact(contact,
                           status == null ? ZeroconfStatusEnum.ONLINE:status);

    }

    /**
     * Returns an Iterator over all contacts.
     *
     * @return a java.util.Iterator over all contacts
     */
    public Iterator<ContactZeroconfImpl> contacts()
    {
        return contacts.iterator();
    }

    /**
     * Adds a contact to the locally stored list of contacts
     * @param contact Zeroconf Contact to add to the local list
     */
    public void addContact(ContactZeroconfImpl contact)
    {
        if (contact == null)
            throw new IllegalArgumentException("contact");

        synchronized(contacts)
        {
            contacts.add(contact);
        }
    }
    /**
     * Returns the <tt>Contact</tt> with the specified identifier or IP address.
     *
     * @param id the identifier of the <tt>Contact</tt> we are
     *   looking for.
     * @param ip the IP address of the <tt>Contact</tt> we are looking for.
     * @return the <tt>Contact</tt> with the specified id or address.
     */
    public ContactZeroconfImpl getContact(String id, InetAddress ip)
    {
        if (id == null && ip == null) return null;

        synchronized(contacts)
        {
            Iterator<ContactZeroconfImpl> contactsIter = contacts();

            while (contactsIter.hasNext())
            {
                ContactZeroconfImpl contact = contactsIter.next();
                //System.out.println("ZEROCNF: Comparing "+id+ " "+ip+
                //" with "+ contact.getAddress()+ " " + contact.getIpAddress());
                if (((contact.getAddress().equals(id)) || (id == null))
                 && ((contact.getIpAddress().equals(ip)) || (ip == null)))
                    return contact;

            }
        }
        //System.out.println("ZEROCNF: ERROR - " +
        //"Couldn't find contact to get ["+id+" / "+ip+"]");
        return null;
    }

    /**
     * Removes the <tt>Contact</tt> with the specified identifier or IP address.
     *
     *
     * @param id the identifier of the <tt>Contact</tt> we are
     *   looking for.
     * @param ip the IP address of the <tt>Contact</tt> we are looking for.
     */
    public void removeContact(String id, InetAddress ip)
    {
        synchronized(contacts)
        {
            Iterator<ContactZeroconfImpl> contactsIter = contacts();
            while (contactsIter.hasNext())
            {
                ContactZeroconfImpl contact = contactsIter.next();
                if (((contact.getAddress().equals(id)) || (id == null))
                  &&((contact.getIpAddress().equals(ip)) || (ip == null)))
                {
                     if (contact.getClientThread() != null)
                         contact.getClientThread().cleanThread();
                     contacts.remove(contact);
                     return;
                 }
            };
        }
        logger.error(
            "ZEROCONF: ERROR - Couldn't find contact to delete ["+id+" / "+ip+"]");
     }
}
