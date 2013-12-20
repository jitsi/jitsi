package net.java.sip.communicator.plugin.otr.authdialog;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.security.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.Timer;

import net.java.otr4j.session.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

public class OtrV3OutgoingSessionSwitcher
    extends SIPCommMenuBar
    implements PluginComponent,
               ActionListener,
               ScOtrEngineListener,
               ScOtrKeyManagerListener
{

    private static final Logger logger
        = Logger.getLogger(OtrV3OutgoingSessionSwitcher.class);

    private static final long serialVersionUID = 0L;

    private final SelectorMenu menu = new SelectorMenu();

    private ButtonGroup buttonGroup = new ButtonGroup();

    Contact contact;

    private final Map<Session, JMenuItem> outgoingSessions
        = new HashMap<Session, JMenuItem>();
    private static class SelectorMenu
        extends SIPCommMenu
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        Image image = OtrActivator.resourceService.getImage(
                            "service.gui.icons.DOWN_ARROW_ICON").getImage();

        private static float alpha = 0.95f;

        private final Timer alphaChanger = new Timer(20, new ActionListener() {

                private float incrementer = -.03f;

                private int fadeCycles = 0;

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    float newAlpha = alpha + incrementer;
                    if (newAlpha < 0.2f)
                    {
                        newAlpha = 0.2f;
                        incrementer = -incrementer;
                    } else if (newAlpha > 0.85f)
                    {
                        newAlpha = 0.85f;
                        incrementer = -incrementer;
                        fadeCycles++;
                    }
                    alpha = newAlpha;
                    if (fadeCycles == 3)
                    {
                        alphaChanger.stop();
                        alpha = 1f;
                    }
                    SelectorMenu.this.repaint();
                }
            });

        @Override
        public void paintComponent(Graphics g)
        {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setComposite(
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(image, getWidth() - image.getWidth(this) - 1,
                (getHeight() - image.getHeight(this) - 1) / 2, this);

            super.paintComponent(g2d);
        }

        public void fadeAnimation()
        {
            alphaChanger.stop();
            alpha = 0.85f;
            repaint();
            alphaChanger.start();
        }
    };

    private final PluginComponentFactory parentFactory;

    public OtrV3OutgoingSessionSwitcher(Container container,
        PluginComponentFactory parentFactory)
    {
        this.parentFactory = parentFactory;

        setPreferredSize(new Dimension(30, 28));
        setMaximumSize(new Dimension(30, 28));
        setMinimumSize(new Dimension(30, 28));

        this.menu.setPreferredSize(new Dimension(30, 45));
        this.menu.setMaximumSize(new Dimension(30, 45));

        this.add(menu);

        this.setBorder(null);
        this.menu.setBorder(null);
        this.menu.setOpaque(false);
        this.setOpaque(false);
        this.menu.setVisible(false);

        buildMenu(contact);

        /*
         * XXX This OtrV3OutgoingSessionSwitcher instance cannot be added as a
         * listener to scOtrEngine and scOtrKeyManager without being removed
         * later on because the latter live forever. Unfortunately, the
         * dispose() method of this instance is never executed. OtrWeakListener
         * will keep this instance as a listener of scOtrEngine and
         * scOtrKeyManager for as long as this instance is necessary. And this
         * instance will be strongly referenced by the JMenuItems which depict
         * it. So when the JMenuItems are gone, this instance will become
         * obsolete and OtrWeakListener will remove it as a listener of
         * scOtrEngine and scOtrKeyManager.
         */
        new OtrWeakListener<OtrV3OutgoingSessionSwitcher>(
            this,
            OtrActivator.scOtrEngine, OtrActivator.scOtrKeyManager);
        
        try
        {
            finishedPadlockImage = new ImageIcon(ImageIO.read(
                    OtrActivator.resourceService.getImageURL(
                        "plugin.otr.FINISHED_ICON_16x16")));
            verifiedLockedPadlockImage = new ImageIcon(ImageIO.read(
                    OtrActivator.resourceService.getImageURL(
                        "plugin.otr.ENCRYPTED_ICON_16x16")));
            unverifiedLockedPadlockImage = new ImageIcon(ImageIO.read(
                    OtrActivator.resourceService.getImageURL(
                        "plugin.otr.ENCRYPTED_UNVERIFIED_ICON_16x16")));
            unlockedPadlockImage = new ImageIcon(ImageIO.read(
                    OtrActivator.resourceService.getImageURL(
                        "plugin.otr.PLAINTEXT_ICON_16x16")));
        } catch (IOException e)
        {
            logger.debug("Failed to load padlock image");
        }
    }

    @Override
    public int getPositionIndex()
    {
        return -1;
    }

    @Override
    public void setCurrentContact(Contact contact)
    {
        if (this.contact == contact)
            return;

        this.contact = contact;
        System.out.println("ot tuk1");
        buildMenu(contact);
    }

    @Override
    public void setCurrentContact(MetaContact metaContact)
    {
        setCurrentContact((metaContact == null) ? null : metaContact
            .getDefaultContact());
    }

    @Override
    public void setCurrentContactGroup(MetaContactGroup metaGroup) {}

    @Override
    public void setCurrentAccountID(AccountID accountID) {}

    @Override
    public PluginComponentFactory getParentFactory()
    {
        return parentFactory;
    }

    @Override
    public void contactVerificationStatusChanged(Contact contact)
    {
        if (contact == null || this.contact != contact) return;
    }

    @Override
    public void contactPolicyChanged(Contact contact) {}

    @Override
    public void globalPolicyChanged() {}

    @Override
    public void sessionStatusChanged(Contact contact)
    {
        System.out.println("ot tuk2");
        buildMenu(contact);
    }

    @Override
    public void multipleInstancesDetected(Contact contact)
    {
        System.out.println("ot tuk3");
        buildMenu(contact);
    }

    private ImageIcon verifiedLockedPadlockImage;
    private ImageIcon unverifiedLockedPadlockImage;
    private ImageIcon finishedPadlockImage;
    private ImageIcon unlockedPadlockImage;
    void buildMenu(Contact contact)
    {
        if (contact == null || this.contact != contact) {
            return;
        }
        menu.removeAll();

        java.util.List<Session> multipleInstances =
            OtrActivator.scOtrEngine.getSessionInstances(contact);

        int index = 0;
        for (Session session : multipleInstances)
        {
            index++;
            if (!outgoingSessions.containsKey(session))
                outgoingSessions.put(session, new JRadioButtonMenuItem());

            JMenuItem menuItem = outgoingSessions.get(session);
            menuItem.setText("Session " + index);

            ImageIcon imageIcon = null;
            switch (session.getSessionStatus(session.getReceiverInstanceTag()))
            {
            case ENCRYPTED:
                PublicKey pubKey = 
                    session.getRemotePublicKey(session.getReceiverInstanceTag());
                String fingerprint =
                    OtrActivator.scOtrKeyManager.
                        getFingerprintFromPublicKey(pubKey);
                imageIcon
                    = OtrActivator.scOtrKeyManager.isVerified(contact, fingerprint)
                        ? verifiedLockedPadlockImage
                        : unverifiedLockedPadlockImage;
                break;
            case FINISHED:
                imageIcon = finishedPadlockImage;
                break;
            case PLAINTEXT:
                imageIcon = unlockedPadlockImage;
                break;
            }
            menuItem.setIcon(imageIcon);

            menu.add(menuItem);
            SelectedObject selectedObject =
                new SelectedObject(imageIcon, session);
            this.menu.setSelected(selectedObject);
            
            buttonGroup.add(menuItem);
            menuItem.addActionListener(this);
            setSelected(menu.getItem(0));
        }
        System.out.println("da");
        updateEnableStatus();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        for (Map.Entry<Session, JMenuItem> entry : outgoingSessions.entrySet())
        {
            JMenuItem menuItem = (JRadioButtonMenuItem) e.getSource();
            if (menuItem.equals(entry.getValue()))
            {
                OtrActivator.scOtrEngine.setOutgoingSession(
                    contact, entry.getKey().getReceiverInstanceTag());
                break;
            }
        }
    }

    @Override
    public void outgoingSessionChanged(Contact contact)
    {
        buildMenu(contact);
    }

    /**
     * Sets the menu to enabled or disabled. The menu is enabled, as soon as it
     * contains two or more items. If it is empty, it is disabled.
     */
    private void updateEnableStatus()
    {
        this.menu.setVisible(this.menu.getItemCount() > 1);
        this.menu.fadeAnimation();
    }
}
