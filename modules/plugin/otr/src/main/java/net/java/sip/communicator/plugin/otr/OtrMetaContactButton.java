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
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.security.*;

import javax.imageio.*;

import net.java.otr4j.*;
import net.java.otr4j.session.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * A {@link AbstractPluginComponent} that registers the Off-the-Record button in
 * the main chat toolbar.
 *
 * @author George Politis
 * @author Marin Dzhigarov
 */
public class OtrMetaContactButton
    extends AbstractPluginComponent
    implements ScOtrEngineListener,
               ScOtrKeyManagerListener
{
    /**
     * The logger
     */
    private final Logger logger = Logger.getLogger(OtrMetaContactButton.class);

    private SIPCommButton button;

    private OtrContact otrContact;

    private AnimatedImage animatedPadlockImage;

    private Image finishedPadlockImage;

    private Image verifiedLockedPadlockImage;

    private Image unverifiedLockedPadlockImage;

    private Image unlockedPadlockImage;

    private Image timedoutPadlockImage;

    public void sessionStatusChanged(OtrContact otrContact)
    {
        // OtrMetaContactButton.this.contact can be null.
        if (otrContact.equals(OtrMetaContactButton.this.otrContact))
        {
            setStatus(
                OtrActivator.scOtrEngine.getSessionStatus(otrContact));
        }
    }

    public void contactPolicyChanged(Contact contact)
    {
        // OtrMetaContactButton.this.contact can be null.
        if (OtrMetaContactButton.this.otrContact != null &&
            contact.equals(OtrMetaContactButton.this.otrContact.contact))
        {
            setPolicy(
                OtrActivator.scOtrEngine.getContactPolicy(contact));
        }
    }

    public void globalPolicyChanged()
    {
        if (OtrMetaContactButton.this.otrContact != null)
            setPolicy(
                OtrActivator.scOtrEngine.getContactPolicy(otrContact.contact));
    }

    public void contactVerificationStatusChanged(OtrContact otrContact)
    {
        // OtrMetaContactButton.this.contact can be null.
        if (otrContact.equals(OtrMetaContactButton.this.otrContact))
        {
            setStatus(
                OtrActivator.scOtrEngine.getSessionStatus(otrContact));
        }
    }

    public OtrMetaContactButton(Container container,
                                PluginComponentFactory parentFactory)
    {
        super(container, parentFactory);

        /*
         * XXX This OtrMetaContactButton instance cannot be added as a listener
         * to scOtrEngine and scOtrKeyManager without being removed later on
         * because the latter live forever. Unfortunately, the dispose() method
         * of this instance is never executed. OtrWeakListener will keep this
         * instance as a listener of scOtrEngine and scOtrKeyManager for as long
         * as this instance is necessary. And this instance will be strongly
         * referenced by the JMenuItems which depict it. So when the JMenuItems
         * are gone, this instance will become obsolete and OtrWeakListener will
         * remove it as a listener of scOtrEngine and scOtrKeyManager.
         */
        new OtrWeakListener<OtrMetaContactButton>(
            this,
            OtrActivator.scOtrEngine, OtrActivator.scOtrKeyManager);
    }

    /**
     * Gets the <code>SIPCommButton</code> which is the component of this
     * plugin. If the button doesn't exist, it's created.
     *
     * @return the <code>SIPCommButton</code> which is the component of this
     *         plugin
     */
    @SuppressWarnings("fallthrough")
    private SIPCommButton getButton()
    {
        if (button == null)
        {
            button = new SIPCommButton(null, null);
            button.setEnabled(false);
            button.setPreferredSize(new Dimension(25, 25));

            button.setToolTipText(OtrActivator.resourceService.getI18NString(
                "plugin.otr.menu.OTR_TOOLTIP"));

            Image i1 = null, i2 = null, i3 = null;
            try
            {
                i1 = ImageIO.read(
                        OtrActivator.resourceService.getImageURL(
                            "plugin.otr.LOADING_ICON1_22x22"));
                i2 = ImageIO.read(
                        OtrActivator.resourceService.getImageURL(
                            "plugin.otr.LOADING_ICON2_22x22"));
                i3 = ImageIO.read(
                        OtrActivator.resourceService.getImageURL(
                            "plugin.otr.LOADING_ICON3_22x22"));
                finishedPadlockImage = ImageIO.read(
                        OtrActivator.resourceService.getImageURL(
                            "plugin.otr.FINISHED_ICON_22x22"));
                verifiedLockedPadlockImage = ImageIO.read(
                        OtrActivator.resourceService.getImageURL(
                            "plugin.otr.ENCRYPTED_ICON_22x22"));
                unverifiedLockedPadlockImage = ImageIO.read(
                        OtrActivator.resourceService.getImageURL(
                            "plugin.otr.ENCRYPTED_UNVERIFIED_ICON_22x22"));
                unlockedPadlockImage = ImageIO.read(
                        OtrActivator.resourceService.getImageURL(
                            "plugin.otr.PLAINTEXT_ICON_22x22"));
                timedoutPadlockImage = ImageIO.read(
                        OtrActivator.resourceService.getImageURL(
                            "plugin.otr.BROKEN_ICON_22x22"));
            } catch (IOException e)
            {
                logger.debug("Failed to load padlock image");
            }

            animatedPadlockImage = new AnimatedImage(button, i1, i2, i3);

            button.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if (otrContact == null)
                        return;

                    switch (OtrActivator.scOtrEngine.getSessionStatus(otrContact))
                    {
                    case ENCRYPTED:
                        OtrPolicy policy =
                            OtrActivator.scOtrEngine.getContactPolicy(
                                otrContact.contact);
                        policy.setSendWhitespaceTag(false);
                        OtrActivator.scOtrEngine.setContactPolicy(
                            otrContact.contact, policy);
                    case FINISHED:
                    case LOADING:
                        // Default action for finished, encrypted and loading
                        // sessions is end session.
                        OtrActivator.scOtrEngine.endSession(otrContact);
                        break;
                    case TIMED_OUT:
                    case PLAINTEXT:
                        policy =
                            OtrActivator.scOtrEngine.getContactPolicy(
                                otrContact.contact);
                        OtrPolicy globalPolicy =
                            OtrActivator.scOtrEngine.getGlobalPolicy();
                        policy.setSendWhitespaceTag(
                            globalPolicy.getSendWhitespaceTag());
                        OtrActivator.scOtrEngine.setContactPolicy(
                            otrContact.contact, policy);
                        // Default action for timed_out and plaintext sessions
                        // is start session.
                        OtrActivator.scOtrEngine.startSession(otrContact);
                        break;
                    }
                }
            });
        }
        return button;
    }

    /*
     * Implements PluginComponent#getComponent(). Returns the SIPCommButton
     * which is the component of this plugin creating it first if it doesn't
     * exist.
     */
    public Object getComponent()
    {
        return getButton();
    }

    /*
     * Implements PluginComponent#getName().
     */
    public String getName()
    {
        return "";
    }

    /*
     * Implements PluginComponent#setCurrentContact(Contact).
     */
    @Override
    public void setCurrentContact(Contact contact)
    {
        setCurrentContact(contact, null);
    }

    public void setCurrentContact(Contact contact, String resourceName)
    {
        if (contact == null)
        {
            this.otrContact = null;
            this.setPolicy(null);
            this.setStatus(ScSessionStatus.PLAINTEXT);
            return;
        }

        if (resourceName == null)
        {
            OtrContact otrContact =
                OtrContactManager.getOtrContact(contact, null);
            if (this.otrContact == otrContact)
                return;
            this.otrContact = otrContact;
            this.setStatus(
                OtrActivator.scOtrEngine.getSessionStatus(otrContact));
            this.setPolicy(
                OtrActivator.scOtrEngine.getContactPolicy(contact));
            return;
        }
        for (ContactResource resource : contact.getResources())
        {
            if (resource.getResourceName().equals(resourceName))
            {
                OtrContact otrContact =
                    OtrContactManager.getOtrContact(contact, resource);
                if (this.otrContact == otrContact)
                    return;
                this.otrContact = otrContact;
                this.setStatus(
                    OtrActivator.scOtrEngine.getSessionStatus(otrContact));
                this.setPolicy(
                    OtrActivator.scOtrEngine.getContactPolicy(contact));
                return;
            }
        }
        logger.debug("Could not find resource for contact " + contact);
    }

    /*
     * Implements PluginComponent#setCurrentContact(MetaContact).
     */
    @Override
    public void setCurrentContact(MetaContact metaContact)
    {
        setCurrentContact((metaContact == null) ? null : metaContact
            .getDefaultContact());
    }

    /**
     * Sets the button enabled status according to the passed in
     * {@link OtrPolicy}.
     *
     * @param contactPolicy the {@link OtrPolicy}.
     */
    private void setPolicy(OtrPolicy contactPolicy)
    {
        getButton().setEnabled(
            contactPolicy != null && contactPolicy.getEnableManual());
    }

    /**
     * Sets the button icon according to the passed in {@link SessionStatus}.
     *
     * @param status the {@link SessionStatus}.
     */
    private void setStatus(ScSessionStatus status)
    {
        animatedPadlockImage.pause();
        Image image;
        String tipKey;
        switch (status)
        {
        case ENCRYPTED:
            PublicKey pubKey =
                OtrActivator.scOtrEngine.getRemotePublicKey(otrContact);
            String fingerprint =
                OtrActivator.scOtrKeyManager.
                    getFingerprintFromPublicKey(pubKey);
            image
                = OtrActivator.scOtrKeyManager.isVerified(
                        otrContact.contact, fingerprint)
                    ? verifiedLockedPadlockImage
                    : unverifiedLockedPadlockImage;
            tipKey = 
                OtrActivator.scOtrKeyManager.isVerified(
                        otrContact.contact, fingerprint)
                ? "plugin.otr.menu.VERIFIED"
                : "plugin.otr.menu.UNVERIFIED";
            break;
        case FINISHED:
            image = finishedPadlockImage;
            tipKey = "plugin.otr.menu.FINISHED";
            break;
        case PLAINTEXT:
            image = unlockedPadlockImage;
            tipKey = "plugin.otr.menu.START_OTR";
            break;
        case LOADING:
            image = animatedPadlockImage;
            animatedPadlockImage.start();
            tipKey = "plugin.otr.menu.LOADING_OTR";
            break;
        case TIMED_OUT:
            image = timedoutPadlockImage;
            tipKey = "plugin.otr.menu.TIMED_OUT";
            break;
        default:
            return;
        }

        SIPCommButton button = getButton();
        button.setIconImage(image);
        button.setToolTipText(OtrActivator.resourceService
            .getI18NString(tipKey));
        button.repaint();
    }

    @Override
    public void multipleInstancesDetected(OtrContact contact)
    {}

    @Override
    public void outgoingSessionChanged(OtrContact otrContact)
    {
        // OtrMetaContactButton.this.contact can be null.
        if (otrContact.equals(OtrMetaContactButton.this.otrContact))
        {
            setStatus(
                OtrActivator.scOtrEngine.getSessionStatus(otrContact));
        }
    }
}
