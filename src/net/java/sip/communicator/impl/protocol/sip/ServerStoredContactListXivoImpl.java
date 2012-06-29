/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
import org.json.*;

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
     * The name of the property under which the user may specify whether to use
     * or not xivo.
     */
    public static final String XIVO_ENABLE = "XIVO_ENABLE";

    /**
     * The name of the property under which the user may specify whether to use
     * original sip credentials for the xivo.
     */
    public static final String XIVO_USE_SIP_CREDETIALS =
            "XIVO_USE_SIP_CREDETIALS";

    /**
     * The name of the property under which the user may specify the xivo server
     * address.
     */
    public static final String XIVO_SERVER_ADDRESS = "XIVO_SERVER_URI";

    /**
     * The name of the property under which the user may specify the xivo user.
     */
    public static final String XIVO_USER = "XIVO_USER";

    /**
     * The name of the property under which the user may specify the xivo user
     * password.
     */
    public static final String XIVO_PASSWORD = "XIVO_PASSWORD";

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
    public void init()
    {
        try
        {
            AccountID accountID = sipProvider.getAccountID();
            boolean enableXivo =
                accountID.getAccountPropertyBoolean(XIVO_ENABLE, true);

            if(!enableXivo)
                return;

            boolean useSipCredentials =
                accountID.getAccountPropertyBoolean(
                                            XIVO_USE_SIP_CREDETIALS, true);
            String serverAddress =
                accountID.getAccountPropertyString(XIVO_SERVER_ADDRESS);
            String username = accountID.getAccountPropertyString(
                              ProtocolProviderFactory.USER_ID);
            Address userAddress = sipProvider.parseAddressString(username);

            if (useSipCredentials)
            {
                username = ((SipUri)userAddress.getURI()).getUser();
            }
            else
            {
                username = accountID.getAccountPropertyString(XIVO_USER);
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

                    handle(new JSONObject(line));
                }
                catch(JSONException ex)
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
    public void renameContact(ContactSipImpl contact, String newName)
    {
    }

    /**
     * The user accepted authorization request for <tt>contact</tt>
     * @param contact the user has accepted.
     */
    public void authorizationAccepted(ContactSipImpl contact)
    {
    }

    /**
     * The user rejected authorization request for <tt>contact</tt>
     * @param contact the user has rejected.
     */
    public void authorizationRejected(ContactSipImpl contact)
    {
    }

    /**
     * The user ignored authorization request for <tt>contact</tt>
     * @param contact the user has ignored.
     */
    public void authorizationIgnored(ContactSipImpl contact)
    {
    }

    /**
     * Get current account image from server if any.
     * @return the account image.
     */
    public ServerStoredDetails.ImageDetail getAccountImage()
        throws OperationFailedException
    {
        throw new OperationFailedException("Modification not supported.",
                OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    /**
     * Deletes current account image from server.
     */
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
    public boolean isAccountImageSupported()
    {
        return false;
    }

    /**
     * Handles new incoming object.
     */
    private void handle(JSONObject incomingObject)
    {
        if(!incomingObject.has("class"))
            return;

        try
        {
            String classField = incomingObject.getString("class");

            if (classField.equals("loginko"))
            {
                showError(null, null,
                        "Unauthorized. Cannot login: " +
                        incomingObject.getString("errorstring"));

                logger.error("Error login: " +
                    incomingObject.getString("errorstring"));

                destroy();

                return;
            }
            else if (classField.equals("login_id_ok"))
            {
                AccountID accountID = sipProvider.getAccountID();
                boolean useSipCredentials =
                    accountID.getAccountPropertyBoolean(
                                            XIVO_USE_SIP_CREDETIALS, true);
                String password;
                if (useSipCredentials)
                {
                    password = SipActivator.getProtocolProviderFactory().
                            loadPassword(accountID);
                }
                else
                {
                    password = accountID.getAccountPropertyString(XIVO_PASSWORD);
                }

                if(!authorize(incomingObject.getString("sessionid"), password))
                    logger.error("Error login authorization!");

                return;
            }
            else if (classField.equals("login_pass_ok"))
            {
                if(!sendCapas(incomingObject.getJSONArray("capalist")))
                    logger.error("Error send capas!");

                return;
            }
            else if (classField.equals("login_capas_ok"))
            {
                if(!sendFeatures(incomingObject.getString("astid"),
                            incomingObject.getString("xivo_userid")))
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
    private boolean login(String username)
    {
        if(connection == null || username == null)
            return false;

        JSONObject obj = new JSONObject();
        try
        {
            obj.accumulate("class","login_id");
            obj.accumulate("company", "Jitsi");

            String os = "x11";
            if(OSUtils.IS_WINDOWS)
                os = "win";
            else if(OSUtils.IS_MAC)
                os = "mac";
            obj.accumulate("ident", username + "@" + os);

            obj.accumulate("userid", username);
            obj.accumulate("version", "9999");
            obj.accumulate("xivoversion", "1.1");

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
    private boolean authorize(String sessionId, String password)
    {
        if(connection == null || sessionId == null || password == null)
            return false;

        JSONObject obj = new JSONObject();
        try
        {
            obj.accumulate("class","login_pass");
            obj.accumulate("hashedpassword",
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
    private boolean sendCapas(JSONArray capalistParam)
    {
        if(connection == null
            || capalistParam == null || capalistParam.length() < 1)
            return false;

        JSONObject obj = new JSONObject();
        try
        {
            obj.accumulate("class", "login_capas");
            obj.accumulate("capaid", capalistParam.getString(0));
            obj.accumulate("lastconnwins", "false");
            obj.accumulate("loginkind", "agent");
            obj.accumulate("state", "");

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
    private boolean sendFeatures(String astid, String xivoUserId)
    {
        if(connection == null || astid == null || xivoUserId == null)
            return false;

        JSONObject obj = new JSONObject();
        try
        {
            obj.accumulate("class","featuresget");
            obj.accumulate("userid", astid + "/" + xivoUserId);

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
    private boolean getPhoneList()
    {
        JSONObject obj = new JSONObject();
        try
        {
            obj.accumulate("class", "phones");
            obj.accumulate("function", "getlist");

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
            if(!objReceived.getString("function").equals("sendlist")
                || !objReceived.has("payload"))
                return;

            JSONObject payload = objReceived.getJSONObject("payload");
            Iterator iter = payload.keys();
            List<JSONObject> phoneList = new ArrayList<JSONObject>();
            while(iter.hasNext())
            {
                JSONObject obj = (JSONObject)payload.get((String) iter.next());
                Iterator phonesIter = obj.keys();
                while(phonesIter.hasNext())
                    phoneList.add(
                        (JSONObject)obj.get((String)phonesIter.next()));

            }

            for(JSONObject phone : phoneList)
            {
                try
                {
                    // don't handle non sip phones
                    if(!phone.getString("tech").equalsIgnoreCase("sip"))
                        continue;

                    String groupName = phone.getString("context");

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

                    String number = phone.getString("number");

                    Address address =
                            sipProvider.parseAddressString(number);

                    //if the contact is already in the contact list
                    ContactSipImpl contact =
                        parentOperationSet.resolveContactID(address.toString());

                    if(contact == null)
                    {
                        contact = new ContactSipImpl(address, sipProvider);
                        contact.setDisplayName(
                                phone.getString("firstname") + " "
                                + phone.getString("lastname"));
                        contact.setResolved(true);
                        parentGroup.addContact(contact);

                        fireContactAdded(parentGroup, contact);
                    }
                    else
                    {
                        contact.setDisplayName(
                                phone.getString("firstname") + " "
                                + phone.getString("lastname"));
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
     * Finds a group with supplied name.
     * @param name the name to search for.
     * @return the group with <tt>name</tt> or name otherwise.
     */
    private ContactGroupSipImpl findGroupByName(String name)
    {
        for (int i = 0;
                 i < getRootGroup().countSubgroups();
                 i++)
        {
            ContactGroupSipImpl gr = (ContactGroupSipImpl)
                getRootGroup().getGroup(i);

            if(gr.getGroupName().equalsIgnoreCase(name))
            {
                return gr;
            }
        }

        return null;
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
