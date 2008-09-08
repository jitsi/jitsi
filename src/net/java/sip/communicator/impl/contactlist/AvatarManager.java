/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.contactlist;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;

/**
 * Schedules loading of avatars at a fixed period of time.
 * 
 * @author Yana Stamcheva
 */
public class AvatarManager
{
    private static Logger logger = Logger.getLogger(AvatarManager.class);

    private static Timer avatarLoadTimer = new Timer();

    private static ArrayList unresolvedAvatarList = new ArrayList();

    /**
     * Schedules loading of avatars at a fixed period of time.
     * 
     * @param rootGroup The root <tt>MetaContactGroup</tt>.
     */
    public static void loadAvatars(MetaContactGroup rootGroup)
    {
        initUnresolvedList(rootGroup);

        avatarLoadTimer.scheduleAtFixedRate(new AvatarTimerTask(), 60000, 5000);
    }

    /**
     * Loads the next avatar.
     */
    private static class AvatarTimerTask extends TimerTask
    {
        @Override
        public void run()
        {
            Iterator<MetaContact> unresolvedIter
                = unresolvedAvatarList.iterator();

            if (unresolvedIter.hasNext())
            {
                MetaContact metaContact = unresolvedIter.next();

                metaContact.getAvatar();

                logger.info("Avatar resolved for contact: "
                        + metaContact.getDisplayName());

                unresolvedAvatarList.remove(metaContact);
            }
            else
            {
                this.cancel();
            }
        }
    }

    /**
     * Initializes the list of unresolved avatar contacts.
     * 
     * @param group The group to initialize.
     */
    private static void initUnresolvedList(MetaContactGroup group)
    {
        Iterator<MetaContact> childrenIter = group.getChildContacts();

        while (childrenIter.hasNext())
        {
            unresolvedAvatarList.add(childrenIter.next());
        }

        Iterator<MetaContactGroup> subrgroupsIter = group.getSubgroups();

        while (subrgroupsIter.hasNext())
        {
            MetaContactGroup subgroup = subrgroupsIter.next();

            initUnresolvedList(subgroup);
        }
    }
}
