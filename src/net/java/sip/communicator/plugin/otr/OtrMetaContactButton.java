/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.imageio.*;

import net.java.otr4j.*;
import net.java.otr4j.session.*;
import net.java.sip.communicator.plugin.desktoputil.*;
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

    private Contact contact;

    private AnimatedImage animatedPadlockImage;

    private Image finishedPadlockImage;

    private Image verifiedLockedPadlockImage;

    private Image unverifiedLockedPadlockImage;

    private Image unlockedPadlockImage;

    private Image timedoutPadlockImage;

    /**
     * The timer task that changes the padlock icon to "loading" and
     * then to "broken" if the specified timeout passed
     */
    public void sessionStatusChanged(Contact contact)
    {
        // OtrMetaContactButton.this.contact can be null.
        if (contact.equals(OtrMetaContactButton.this.contact))
        {

            setStatus(
                OtrActivator.scOtrEngine.getSessionStatus(contact));
        }
    }

    public void contactPolicyChanged(Contact contact)
    {
        // OtrMetaContactButton.this.contact can be null.
        if (contact.equals(OtrMetaContactButton.this.contact))
        {
            setPolicy(
                OtrActivator.scOtrEngine.getContactPolicy(contact));
        }
    }

    public void globalPolicyChanged()
    {
        if (OtrMetaContactButton.this.contact != null)
            setPolicy(
                OtrActivator.scOtrEngine.getContactPolicy(contact));
    }

    public void contactVerificationStatusChanged(Contact contact)
    {
        // OtrMetaContactButton.this.contact can be null.
        if (contact.equals(OtrMetaContactButton.this.contact))
        {
            setStatus(
                OtrActivator.scOtrEngine.getSessionStatus(contact));
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
                    if (contact == null)
                        return;

                    switch (OtrActivator.scOtrEngine.getSessionStatus(contact))
                    {
                    case ENCRYPTED:
                    case FINISHED:
                    case LOADING:
                        // Default action for finished, encrypted and loading
                        // sessions is end session.
                        OtrActivator.scOtrEngine.endSession(contact);
                        break;
                    case TIMED_OUT:
                    case PLAINTEXT:
                        // Default action for timed_out and plaintext sessions
                        // is start session.
                        OtrActivator.scOtrEngine.startSession(contact);
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
        if (this.contact == contact)
            return;

        this.contact = contact;
        if (contact != null)
        {
            this.setStatus(OtrActivator.scOtrEngine.getSessionStatus(contact));
            this.setPolicy(OtrActivator.scOtrEngine.getContactPolicy(contact));
        }
        else
        {
            this.setStatus(ScSessionStatus.PLAINTEXT);
            this.setPolicy(null);
        }
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
            image
                = OtrActivator.scOtrKeyManager.isVerified(contact)
                    ? verifiedLockedPadlockImage
                    : unverifiedLockedPadlockImage;
            tipKey = 
                OtrActivator.scOtrKeyManager.isVerified(contact)
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
}
