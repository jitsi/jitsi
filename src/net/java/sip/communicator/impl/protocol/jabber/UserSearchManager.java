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
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.impl.protocol.jabber.extensions.usersearch.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.packet.*;

import java.util.*;

/**
 * The <tt>UserSearchManager</tt> implements the user search (XEP-0055). It
 * implements searching a contacts by string.
 *
 * @author Hristo Terezov
 */
public class UserSearchManager {
    /**
     * The object that represents the connection to the server.
     */
    private Connection con;

    /**
     * Last received search form from the server.
     */
    private UserSearchIQ userSearchForm = null;

    /**
     * The user search service name.
     */
    private String searchService;

    /**
     * Creates a new UserSearchManager.
     *
     * @param con the Connection to use.
     * @param searchService the user search service name.
     */
    public UserSearchManager(Connection con, String searchService) {
        this.con = con;
        this.searchService = searchService;
    }

    /**
     * Returns a list with the available user search service names for a given
     * provider.
     * @param provider the provider.
     * @return a list with the available user search service names for a given
     * provider.
     */
    public static List<String> getAvailableServiceNames(
        ProtocolProviderServiceJabberImpl provider)
    {
        final List<String> searchServices = new ArrayList<String>();
        ScServiceDiscoveryManager discoManager = provider.getDiscoveryManager();
        DiscoverItems items;
        try
        {
            items = discoManager.discoverItems(
                provider.getConnection().getServiceName());
        }
        catch (XMPPException e)
        {
            return searchServices;
        }
        Iterator<DiscoverItems.Item> iter = items.getItems();
        while (iter.hasNext())
        {
            DiscoverItems.Item item = iter.next();
            try
            {
                DiscoverInfo info;
                info = discoManager.discoverInfo(item.getEntityID());

                if (info.containsFeature("jabber:iq:search")
                    && !info.containsFeature("http://jabber.org/protocol/muc"))
                {
                    searchServices.add(item.getEntityID());
                }
            }
            catch (Exception e) {
                continue;
            }
        }
        return searchServices;
    }

    /**
     * Returns the form to fill out to perform a search.
     *
     * @return the form to fill out to perform a search.
     * @throws XMPPException thrown if a server error has occurred.
     */
    private void getSearchForm() throws XMPPException {
        UserSearchIQ search = new UserSearchIQ();
        search.setType(IQ.Type.GET);
        search.setTo(searchService);

        PacketCollector collector = con.createPacketCollector(
            new PacketIDFilter(search.getPacketID()));
        con.sendPacket(search);

        IQ response = (IQ) collector.nextResult(
            SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        userSearchForm = (UserSearchIQ)response;
    }

    /**
     * Performs user search for the searched string.
     *
     * @param searchedString
     * @return the <tt>ReportedData</tt> instance which represents the search
     * results.
     * @throws XMPPException thrown if a server error has occurred.
     */
    public ReportedData searchForString(String searchedString) throws XMPPException
    {
        if(userSearchForm == null)
            getSearchForm();
        UserSearchIQ search = new UserSearchIQ();
        search.setType(IQ.Type.SET);
        search.setTo(searchService);

        Form form = userSearchForm.getAnswerForm();
        if(form != null)
        {
            Iterator<FormField> fields = form.getFields();
            while (fields.hasNext())
            {
                FormField formField = fields.next();
                if(formField.getType().equals(FormField.TYPE_BOOLEAN))
                {
                    form.setAnswer(formField.getVariable(), true);
                }
                else if(formField.getType().equals(FormField.TYPE_TEXT_SINGLE))
                {
                    form.setAnswer(formField.getVariable(), searchedString);
                }
            }
            search.setForm(form.getDataFormToSend());
        }
        else
        {
            for(String field : userSearchForm.getFields())
                search.addField(field, searchedString);
        }

        PacketCollector collector = con.createPacketCollector(
            new PacketIDFilter(search.getPacketID()));

        con.sendPacket(search);

        UserSearchIQ response = (UserSearchIQ) collector.nextResult(
            SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }

        return response.getData();
    }

}
