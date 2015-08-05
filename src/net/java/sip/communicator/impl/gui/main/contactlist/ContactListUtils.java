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
package net.java.sip.communicator.impl.gui.main.contactlist;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

public class ContactListUtils
{
    private static final Logger logger
        = Logger.getLogger(ContactListUtils.class);

    public static void addContact(
                                final ProtocolProviderService protocolProvider,
                                final MetaContactGroup group,
                                final String contactAddress)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    GuiActivator.getContactListService()
                                .createMetaContact( protocolProvider,
                                                    group,
                                                    contactAddress);
                }
                catch (MetaContactListException ex)
                {
                    logger.error(ex);
                    ex.printStackTrace();
                    int errorCode = ex.getErrorCode();

                    if (errorCode
                            == MetaContactListException
                                .CODE_CONTACT_ALREADY_EXISTS_ERROR)
                    {
                        new ErrorDialog(
                            GuiActivator.getUIService().getMainFrame(),
                            GuiActivator.getResources().getI18NString(
                            "service.gui.ADD_CONTACT_ERROR_TITLE"),
                            GuiActivator.getResources().getI18NString(
                                    "service.gui.ADD_CONTACT_EXIST_ERROR",
                                    new String[]{contactAddress}),
                            ex)
                        .showDialog();
                    }
                    else if (errorCode
                            == MetaContactListException
                                .CODE_NETWORK_ERROR)
                    {
                        new ErrorDialog(
                            GuiActivator.getUIService().getMainFrame(),
                            GuiActivator.getResources().getI18NString(
                            "service.gui.ADD_CONTACT_ERROR_TITLE"),
                            GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_CONTACT_NETWORK_ERROR",
                                new String[]{contactAddress}),
                            ex)
                        .showDialog();
                    }
                    else if (errorCode
                            == MetaContactListException
                                .CODE_NOT_SUPPORTED_OPERATION)
                    {
                        new ErrorDialog(
                            GuiActivator.getUIService().getMainFrame(),
                            GuiActivator.getResources().getI18NString(
                            "service.gui.ADD_CONTACT_ERROR_TITLE"),
                            GuiActivator.getResources().getI18NString(
                                "service.gui.ADD_CONTACT_NOT_SUPPORTED",
                                new String[]{contactAddress}),
                            ex)
                        .showDialog();
                    }
                    else
                    {
                        new ErrorDialog(
                            GuiActivator.getUIService().getMainFrame(),
                            GuiActivator.getResources().getI18NString(
                            "service.gui.ADD_CONTACT_ERROR_TITLE"),
                            GuiActivator.getResources().getI18NString(
                                    "service.gui.ADD_CONTACT_ERROR",
                                    new String[]{contactAddress}),
                            ex)
                        .showDialog();
                    }
                }
            }
        }.start();
    }
}
