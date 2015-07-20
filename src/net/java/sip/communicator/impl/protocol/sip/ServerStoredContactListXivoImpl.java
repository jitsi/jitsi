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

import gov.nist.javax.sip.address.*;

import java.io.*;
import java.net.*;
import java.net.URI;
import java.util.*;

import javax.sip.address.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;
import org.json.simple.*;

/**
 * Xivo server stored contact list. Currently no modifications are possible.
 * Just connecting and downloading server contact list.
 * @author Damian Minkov
 */
public class ServerStoredContactListXivoImpl
    extends ServerStoredContactList
    implements Runnable
{
    /**
     * Logger class
     */
    private static final Logger logger =
            Logger.getLogger(ServerStoredContactListXivoImpl.class);

    /**
     * The connection to the xivo server.
     */
    private Socket connection;

    /**
     * The reader from the connection.
     */
    private BufferedReader connectionReader;

    /**
     * The writer we use to send commands to server.
     */
    private PrintStream connectionWriter;

    /**
     * The reading thread reads till its not stopped.
     */
    private boolean stopped = false;

    /**
     * Creates a ServerStoredContactList wrapper for the specified BuddyList.
     *
     * @param sipProvider        the provider that has instantiated us.
     * @param parentOperationSet the operation set that created us and that
     *                           we could use for dispatching subscription events
     */
    ServerStoredContactListXivoImpl(
            ProtocolProviderServiceSipImpl sipProvider,
            OperationSetPresenceSipImpl parentOperationSet)
    {
        super(sipProvider, parentOperationSet);
    }

    /**
     * Initializes the server stored list. Synchronize server stored groups and
     * contacts with the local groups and contacts.
     */
    @Override
    public void init()
    {
        try
        {
            SipAccountIDImpl accountID
                    = (SipAccountIDImpl) sipProvider.getAccountID();

            if(!accountID.isXiVOEnable())
                return;

            boolean useSipCredentials
                    = accountID.isClistOptionUseSipCredentials();

            String serverAddress = accountID.getClistOptionServerUri();
            String username = accountID.getAccountPropertyString(
                              ProtocolProviderFactory.USER_ID);
            Address userAddress = sipProvider.parseAddressString(username);

            if (useSipCredentials)
            {
                username = ((SipUri)userAddress.getURI()).getUser();
            }
            else
            {
                username = accountID.getClistOptionUser();
            }

            try
            {
                connect(serverAddress);
            }
            catch(Throwable ex)
            {
                showError(ex, null, null);
                logger.error("Error connecting to server", ex);
                return;
            }

            Thread thread = new Thread(this, this.getClass().getName());
            thread.setDaemon(true);
            thread.start();

            if(!login(username))
            {
                showError(null, null,
                        "Unauthorized. Cannot login.");
                logger.error("Cannot login.");
                return;
            }
        }
        catch(Throwable t)
        {
            logger.error("Error init clist from xivo server");
        }
    }

    /**
     * Connects to the server.
     * @param serverAddress the address to connect, if null try to use our
     *  sip connection address.
     * @throws IOException
     */
    private void connect(String serverAddress)
        throws IOException
    {
        if(serverAddress != null)
            connection = new Socket(serverAddress, 5003);
        else // lets try using our sip connected address
            connection = new Socket(
                sipProvider.getConnection().getAddress().getAddress(), 5003);

        connectionWriter = new PrintStream(connection.getOutputStream());
    }

    /**
     * Destroys the server stored list.
     */
    @Override
    public void destroy()
    {
        stopped = true;

        try
        {
            if(connection != null)
            {
                connection.shutdownInput();
                connection.close();
                connection = null;
            }
        }
        catch(IOException e){}

        try
        {
            if(connectionReader != null)
            {
                connectionReader.close();
                connectionReader = null;
            }
        }
        catch(IOException ex)
        {}

        if(connectionWriter != null)
        {
            connectionWriter.close();
            connectionWriter = null;
        }
    }

    /**
     * The logic that runs in separate thread. Dispatching responses.
     */
    public void run()
    {
        if(connection == null)
        {
            logger.error("No connection.");
            return;
        }

        try
        {
            connectionReader = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));

            if (!connectionReader.readLine().contains("XiVO"))
            {
                logger.error("Error xivo with server!");
                destroy();
                return;
            }

            String line;
            while ((line = connectionReader.readLine()) != null || !stopped)
            {
                try
                {
                    if(logger.isTraceEnabled())
                        logger.trace("Read from server:" + line);

                    handle((JSONObject)JSONValue.parseWithException(line));
                }
                catch(Throwable ex)
                {
                    logger.error("Error parsing object:" + line, ex);
                }
            }
        }
        catch(IOException ex)
        {
            destroy();
        }
    }

    /**
     * Gets the pres-content image uri.
     *
     * @return the pres-content image uri.
     * @throws IllegalStateException if the user has not been connected.
     */
    @Override
    public URI getImageUri()
    {
        return null;
    }

    /**
     * Gets image from the specified uri.
     *
     * @param imageUri the image uri.
     * @return the image.
     */
    @Override
    public byte[] getImage(URI imageUri)
    {
        return new byte[0];
    }

    /**
     * Creates a group with the specified name and parent in the server stored
     * contact list.
     *
     * @param parentGroup the group where the new group should be created.
     * @param groupName   the name of the new group to create.
     * @param persistent  specify whether created contact is persistent ot not.
     * @return the newly created <tt>ContactGroupSipImpl</tt>.
     * @throws OperationFailedException with code NETWORK_FAILURE if creating
     *                                  the group fails because of server
     *                                  error or with code
     *                                  CONTACT_GROUP_ALREADY_EXISTS if contact
     *                                  group with such name already exists.
     */
    @Override
    public ContactGroupSipImpl createGroup(
            ContactGroupSipImpl parentGroup,
            String groupName,
            boolean persistent)
        throws OperationFailedException
    {
        throw new OperationFailedException("Modification not supported.",
                OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    /**
     * Renames the specified group from the server stored contact list.
     *
     * @param group   the group to rename.
     * @param newName the new name of the group.
     */
    @Override
    public void renameGroup(ContactGroupSipImpl group, String newName)
    {

    }

    /**
     * Removes the specified contact from its current parent and places it
     * under <tt>newParent</tt>.
     *
     * @param contact        the <tt>Contact</tt> to move
     * @param newParentGroup the <tt>ContactGroup</tt> where <tt>Contact</tt>
     *                       would be placed.
     * @throws OperationFailedException with code NETWORK_FAILURE if the
     *                                  operation if failed during network
     *                                  communication.
     */
    @Override
    public void moveContactToGroup(
            ContactSipImpl contact, ContactGroupSipImpl newParentGroup)
        throws OperationFailedException
    {
        throw new OperationFailedException("Modification not supported.",
                OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    /**
     * Removes the specified group from the server stored contact list.
     *
     * @param group the group to delete.
     */
    @Override
    public void removeGroup(ContactGroupSipImpl group)
    {
    }

    /**
     * Creates contact for the specified address and inside the
     * specified group . If creation is successful event will be fired.
     *
     * @param parentGroup the group where the unresolved contact is to be
     *                    created.
     * @param contactId   the sip id of the contact to create.
     * @param displayName the display name of the contact to create
     * @param persistent  specify whether created contact is persistent ot not.
     * @param contactType the contact type to create, if missing null.
     * @return the newly created <tt>ContactSipImpl</tt>.
     * @throws OperationFailedException with code NETWORK_FAILURE if the
     *                                  operation if failed during network
     *                                  communication.
     */
    @Override
    public ContactSipImpl createContact(
            ContactGroupSipImpl parentGroup,
            String contactId,
            String displayName,
            boolean persistent,
            String contactType)
        throws OperationFailedException
    {
        throw new OperationFailedException("Modification not supported.",
                OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    /**
     * Removes a contact. If creation is successful event will be fired.
     *
     * @param contact contact to be removed.
     * @throws OperationFailedException with code NETWORK_FAILURE if the
     *                                  operation if failed during network
     *                                  communication.
     */
    @Override
    public void removeContact(ContactSipImpl contact)
        throws OperationFailedException
    {
        throw new OperationFailedException("Modification not supported.",
                OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    /**
     * Renames the specified contact.
     *
     * @param contact the contact to be renamed.
     * @param newName the new contact name.
     */
    @Override
    public void renameContact(ContactSipImpl contact, String newName)
    {
    }

    /**
     * The user accepted authorization request for <tt>contact</tt>
     * @param contact the user has accepted.
     */
    @Override
    public void authorizationAccepted(ContactSipImpl contact)
    {
    }

    /**
     * The user rejected authorization request for <tt>contact</tt>
     * @param contact the user has rejected.
     */
    @Override
    public void authorizationRejected(ContactSipImpl contact)
    {
    }

    /**
     * The user ignored authorization request for <tt>contact</tt>
     * @param contact the user has ignored.
     */
    @Override
    public void authorizationIgnored(ContactSipImpl contact)
    {
    }

    /**
     * Get current account image from server if any.
     * @return the account image.
     */
    @Override
    public ServerStoredDetails.ImageDetail getAccountImage()
        throws OperationFailedException
    {
        throw new OperationFailedException("Modification not supported.",
                OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    /**
     * Deletes current account image from server.
     */
    @Override
    public void deleteAccountImage()
        throws OperationFailedException
    {
        throw new OperationFailedException("Modification not supported.",
                OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    /**
     * Change the image of the account on server.
     * @param newImageBytes the new image.
     */
    @Override
    public void setAccountImage(byte[] newImageBytes)
        throws OperationFailedException
    {
        throw new OperationFailedException("Modification not supported.",
                OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    /**
     * Whether current contact list supports account image.
     * @return does current contact list supports account image.
     */
    @Override
    public boolean isAccountImageSupported()
    {
        return false;
    }

    /**
     * Handles new incoming object.
     */
    private void handle(JSONObject incomingObject)
    {
        if(!incomingObject.containsKey("class"))
            return;

        try
        {
            String classField = (String)incomingObject.get("class");

            if (classField.equals("loginko"))
            {
                showError(null, null,
                        "Unauthorized. Cannot login: " +
                        incomingObject.get("errorstring"));

                logger.error("Error login: " +
                    incomingObject.get("errorstring"));

                destroy();

                return;
            }
            else if (classField.equals("login_id_ok"))
            {
                SipAccountIDImpl accountID
                        = (SipAccountIDImpl) sipProvider.getAccountID();

                boolean useSipCredentials
                        = accountID.isClistOptionUseSipCredentials();

                String password;
                if (useSipCredentials)
                {
                    password = SipActivator.getProtocolProviderFactory().
                            loadPassword(accountID);
                }
                else
                {
                    password = accountID.getClistOptionPassword();
                }

                if(!authorize((String)incomingObject.get("sessionid"), password))
                    logger.error("Error login authorization!");

                return;
            }
            else if (classField.equals("login_pass_ok"))
            {
                if(!sendCapas((JSONArray)incomingObject.get("capalist")))
                    logger.error("Error send capas!");

                return;
            }
            else if (classField.equals("login_capas_ok"))
            {
                if(!sendFeatures((String)incomingObject.get("astid"),
                            (String)incomingObject.get("xivo_userid")))
                    logger.error("Problem send features get!");

                return;
            }
            else if (classField.equals("features"))
            {
                if(!getPhoneList())
                    logger.error("Problem send get phones!");

                return;
            }
            else if (classField.equals("phones"))
            {
                phonesRecieved(incomingObject);
                return;
            }
            else if (classField.equals("disconn"))
            {
                destroy();
                return;
            }
            else
            {
                if(logger.isTraceEnabled())
                    logger.trace("unhandled classField: " + incomingObject);
                return;
            }
        }
        catch(Throwable t)
        {
            logger.error("Error handling incoming object", t);
        }
    }

    /**
     * Sends login command.
     * @param username the username.
     * @return is command successful.
     */
    @SuppressWarnings("unchecked")
    private boolean login(String username)
    {
        if(connection == null || username == null)
            return false;


        JSONObject obj = new JSONObject();
        try
        {
            obj.put("class","login_id");
            obj.put("company", "Jitsi");

            String os = "x11";
            if(OSUtils.IS_WINDOWS)
                os = "win";
            else if(OSUtils.IS_MAC)
                os = "mac";
            obj.put("ident", username + "@" + os);

            obj.put("userid", username);
            obj.put("version", "9999");
            obj.put("xivoversion", "1.1");

            return send(obj);
        }
        catch (Exception e)
        {
            logger.error("Error login", e);
            return false;
        }
    }

    /**
     * Sends password command.
     * @param sessionId the session id from previous command.
     * @param password the password to authorize.
     * @return is command successful.
     */
    @SuppressWarnings("unchecked")
    private boolean authorize(String sessionId, String password)
    {
        if(connection == null || sessionId == null || password == null)
            return false;

        JSONObject obj = new JSONObject();
        try
        {
            obj.put("class","login_pass");
            obj.put("hashedpassword",
                Sha1Crypto.encode(sessionId + ":" + password));

            return send(obj);
        }
        catch (Exception e)
        {
            logger.error("Error login with password", e);
            return false;
        }
    }

    /**
     * Sends login command.
     * @param capalistParam param from previous command.
     * @return is command successful.
     */
    @SuppressWarnings("unchecked")
    private boolean sendCapas(JSONArray capalistParam)
    {
        if(connection == null
            || capalistParam == null || capalistParam.isEmpty())
            return false;

        JSONObject obj = new JSONObject();
        try
        {
            obj.put("class", "login_capas");
            obj.put("capaid", capalistParam.get(0));
            obj.put("lastconnwins", "false");
            obj.put("loginkind", "agent");
            obj.put("state", "");

            return send(obj);
        }
        catch (Exception e)
        {
            logger.error("Error login", e);
            return false;
        }
    }

    /**
     * Send needed command for features.
     * @param astid param from previous command.
     * @param xivoUserId param from previous command.
     * @return is command successful.
     */
    @SuppressWarnings("unchecked")
    private boolean sendFeatures(String astid, String xivoUserId)
    {
        if(connection == null || astid == null || xivoUserId == null)
            return false;

        JSONObject obj = new JSONObject();
        try
        {
            obj.put("class","featuresget");
            obj.put("userid", astid + "/" + xivoUserId);

            return send(obj);
        }
        catch (Exception e)
        {
            logger.error("Error send features get command", e);
            return false;
        }
    }

    /**
     * Sends command to retrieve phones list.
     * @return is command successful.
     */
    @SuppressWarnings("unchecked")
    private boolean getPhoneList()
    {
        JSONObject obj = new JSONObject();
        try
        {
            obj.put("class", "phones");
            obj.put("function", "getlist");

            return send(obj);
        }
        catch (Exception e)
        {
            logger.error("Error retrieving phones");
            return false;
        }
    }

    /**
     * Sends command to server.
     * @return is command successful sent.
     */
    private boolean send(JSONObject obj)
    {
        if(connection == null || connectionWriter == null)
            return false;

        if(logger.isTraceEnabled())
            logger.trace("Send to server:" + obj);

        connectionWriter.println(obj);

        return true;
    }

    /**
     * parses received phones list and creates/resolves groups and contacts
     * @param objReceived the obj with data.
     */
    private void phonesRecieved(JSONObject objReceived)
    {
        try
        {
            if(!objReceived.get("function").equals("sendlist")
                    || !objReceived.containsKey("payload"))
                return;

            JSONObject payload = (JSONObject)objReceived.get("payload");
            /*
             * FIXME The following contains two very inefficient Map-iterating
             * loops.
             */
            Iterator iter = payload.keySet().iterator();
            List<JSONObject> phoneList = new ArrayList<JSONObject>();
            while(iter.hasNext())
            {
                JSONObject obj = (JSONObject)payload.get(iter.next());
                Iterator phonesIter = obj.keySet().iterator();
                while(phonesIter.hasNext())
                    phoneList.add(
                        (JSONObject)obj.get(phonesIter.next()));
            }

            for(JSONObject phone : phoneList)
            {
                try
                {
                    // don't handle non sip phones
                    if(!((String)phone.get("tech")).equalsIgnoreCase("sip"))
                        continue;

                    String groupName = (String)phone.get("context");

                    ContactGroupSipImpl parentGroup =
                        findGroupByName(groupName);

                    if(parentGroup == null)
                    {
                        parentGroup =
                            new ContactGroupSipImpl(groupName, sipProvider);
                        parentGroup.setPersistent(true);
                        getRootGroup().addSubgroup(parentGroup);

                        fireGroupEvent(parentGroup,
                            ServerStoredGroupEvent.GROUP_CREATED_EVENT);
                    }

                    String number = (String)phone.get("number");

                    Address address =
                            sipProvider.parseAddressString(number);

                    //if the contact is already in the contact list
                    ContactSipImpl contact =
                        parentOperationSet.resolveContactID(address.toString());

                    if(contact == null)
                    {
                        contact = new ContactSipImpl(address, sipProvider);
                        contact.setDisplayName(
                                phone.get("firstname") + " "
                                + phone.get("lastname"));
                        contact.setResolved(true);
                        parentGroup.addContact(contact);

                        fireContactAdded(parentGroup, contact);
                    }
                    else
                    {
                        contact.setDisplayName(
                                phone.get("firstname") + " "
                                + phone.get("lastname"));
                        contact.setResolved(true);

                        fireContactResolved(parentGroup, contact);
                    }
                }
                catch(Throwable t)
                {
                    logger.error("Error parsing " + phone);
                }
            }
        }
        catch(Throwable t)
        {
            logger.error("Error init list from server", t);
        }
    }

    /**
     * Shows an error and a short description.
     * @param ex the exception
     */
    static void showError(Throwable ex, String title, String message)
    {
        try
        {
            if(title == null)
                title = "Error in SIP contactlist storage";

            if(message == null)
                message = title + "\n" +
                    ex.getClass().getName() + ": " +
                    ex.getLocalizedMessage();

            if(SipActivator.getUIService() != null)
                SipActivator.getUIService().getPopupDialog()
                    .showMessagePopupDialog(
                        message,
                        title,
                        PopupDialog.ERROR_MESSAGE);
        }
        catch(Throwable t)
        {
            logger.error("Error for error dialog", t);
        }
    }
}
