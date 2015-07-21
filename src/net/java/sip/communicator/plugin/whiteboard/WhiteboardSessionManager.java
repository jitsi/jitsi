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
package net.java.sip.communicator.plugin.whiteboard;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.plugin.whiteboard.gui.*;
import net.java.sip.communicator.plugin.whiteboard.gui.whiteboardshapes.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.whiteboardobjects.*;
import net.java.sip.communicator.util.*;

/**
 * Whiteboard session manager.
 *
 * @author Julien Waechter
 */
public class WhiteboardSessionManager
    implements WhiteboardObjectListener
{
    private static final Logger logger =
        Logger.getLogger (WhiteboardSessionManager.class);

    /**
     * List active WhitboarFrame started.
     */
    private final java.util.List<WhiteboardFrame> wbFrames =
        new Vector<WhiteboardFrame>();

    private OperationSetWhiteboarding opSetWb;

    /**
     * Constructor.
     */
    public WhiteboardSessionManager()
    {
        List<OperationSetWhiteboarding> whiteboardOpSets
            = WhiteboardActivator.getWhiteboardOperationSets();

        if (whiteboardOpSets == null)
            return;

        for (OperationSetWhiteboarding whiteboardOpSet : whiteboardOpSets)
        {
            whiteboardOpSet.addInvitationListener(new InvitationListener());
            whiteboardOpSet.addPresenceListener(new PresenceListener());
        }
    }

    /**
     * Initialize (a new) Whiteboard with contact
     *
     * @param contact Contact used to init whiteboard
     */
    public void initWhiteboard (final Contact contact)
    {
        opSetWb
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetWhiteboarding.class);

        if (opSetWb == null)
        {
            if (logger.isInfoEnabled())
                logger.info("Contact does not support whiteboarding");
            return;
        }

        WhiteboardFrame wbf = getWhiteboardFrame (contact);
        if(wbf != null)
        {
            wbf.setVisible (true);
            return;
        }

        new Thread ()
        {
            @Override
            public void run()
            {
                try
                {
                    WhiteboardSession wbSession
                        = opSetWb.createWhiteboardSession (
                                contact.getDisplayName(),
                                null);

                    WhiteboardFrame wbFrame
                        = new WhiteboardFrame ( WhiteboardSessionManager.this,
                                                wbSession);

                    wbFrames.add (wbFrame);
                    wbFrame.setContact(contact);
                    wbFrame.setVisible (true);

                    wbSession.join();

                    wbSession.invite(contact.getAddress());

                }
                catch (OperationFailedException e)
                {
                    logger.error("Creating a whiteboard session failed.", e);
                }
                catch (OperationNotSupportedException e)
                {
                    logger.error(
                        "Do not support create of whiteboard session", e);
                }
            }
        }.start();
    }

    /**
     * Construct (with WhiteboardSession) and send
     * a WhiteboardObject to a contact.
     *
     * @param wbSession the white-board session, to which the object would be
     * send
     * @param ws WhiteboardShape to convert and send
     * @return WhiteboardObject sent
     */
    public WhiteboardObject sendWhiteboardObject (  WhiteboardSession wbSession,
                                                    WhiteboardShape ws)
        throws OperationFailedException
    {
        Vector<String> supportedWBO =  new Vector<String>(
          Arrays.asList (wbSession.getSupportedWhiteboardObjects ()));

        if(ws instanceof WhiteboardObjectPath)
        {
            if(!supportedWBO.contains (WhiteboardObjectPath.NAME))
                return null;
            WhiteboardObjectPath obj = (WhiteboardObjectPath)
            wbSession.createWhiteboardObject (WhiteboardObjectPath.NAME );
            ws.setID (obj.getID ());
            obj.setPoints (((WhiteboardObjectPath)ws).getPoints ());
            obj.setColor (ws.getColor ());
            obj.setThickness (ws.getThickness ());
            wbSession.sendWhiteboardObject (obj);
            return obj;
        }
        else if(ws instanceof WhiteboardObjectPolyLine)
        {
            if(!supportedWBO.contains (WhiteboardObjectPolyLine.NAME))
                return null;
            WhiteboardObjectPolyLine obj = (WhiteboardObjectPolyLine)
            wbSession.createWhiteboardObject (WhiteboardObjectPolyLine.NAME );
            ws.setID (obj.getID ());
            obj.setPoints (((WhiteboardObjectPolyLine)ws).getPoints ());
            obj.setColor (ws.getColor ());
            obj.setThickness (ws.getThickness ());
            wbSession.sendWhiteboardObject (obj);
            return obj;
        }
        else if(ws instanceof WhiteboardObjectPolygon)
        {
            if(!supportedWBO.contains (WhiteboardObjectPolygon.NAME))
                return null;
            WhiteboardObjectPolygon obj = (WhiteboardObjectPolygon)
            wbSession.createWhiteboardObject (WhiteboardObjectPolygon.NAME );
            ws.setID (obj.getID ());
            obj.setPoints (((WhiteboardObjectPolygon)ws).getPoints ());
            obj.setBackgroundColor ((
              (WhiteboardObjectPolygon)ws).getBackgroundColor ());
            obj.setFill (((WhiteboardObjectPolygon)ws).isFill ());
            obj.setColor (ws.getColor ());
            obj.setThickness (ws.getThickness ());
            wbSession.sendWhiteboardObject (obj);
            return obj;
        }
        else if(ws instanceof WhiteboardObjectLine)
        {
            if(!supportedWBO.contains (WhiteboardObjectLine.NAME))
                return null;
            WhiteboardObjectLine obj = (WhiteboardObjectLine)
            wbSession.createWhiteboardObject (WhiteboardObjectLine.NAME );
            ws.setID (obj.getID ());
            obj.setWhiteboardPointStart (
              ((WhiteboardObjectLine)ws).getWhiteboardPointStart ());
            obj.setWhiteboardPointEnd (
              ((WhiteboardObjectLine)ws).getWhiteboardPointEnd ());
            obj.setColor (ws.getColor ());
            obj.setThickness (ws.getThickness ());
            wbSession.sendWhiteboardObject (obj);
            return obj;
        }
        else if(ws instanceof WhiteboardObjectRect)
        {
            if(!supportedWBO.contains (WhiteboardObjectRect.NAME))
                return null;
            WhiteboardObjectRect obj = (WhiteboardObjectRect)
            wbSession.createWhiteboardObject (WhiteboardObjectRect.NAME );
            ws.setID (obj.getID ());
            obj.setFill (((WhiteboardObjectRect)ws).isFill ());
            obj.setHeight (((WhiteboardObjectRect)ws).getHeight ());
            obj.setWhiteboardPoint (
              ((WhiteboardObjectRect)ws).getWhiteboardPoint ());
            obj.setWidth ((((WhiteboardObjectRect)ws)).getWidth ());
            obj.setColor (ws.getColor ());
            obj.setThickness (ws.getThickness ());
            wbSession.sendWhiteboardObject (obj);
            return obj;
        }
        else if(ws instanceof WhiteboardObjectCircle)
        {
            if(!supportedWBO.contains (WhiteboardObjectCircle.NAME))
                return null;
            WhiteboardObjectCircle obj = (WhiteboardObjectCircle)
            wbSession.createWhiteboardObject (WhiteboardObjectCircle.NAME );
            ws.setID (obj.getID ());
            obj.setFill (((WhiteboardObjectCircle)ws).isFill ());
            obj.setRadius (((WhiteboardObjectCircle)ws).getRadius ());
            obj.setWhiteboardPoint (
              ((WhiteboardObjectCircle)ws).getWhiteboardPoint ());
            obj.setBackgroundColor (
              (((WhiteboardObjectCircle)ws)).getBackgroundColor ());
            obj.setColor (ws.getColor ());
            obj.setThickness (ws.getThickness ());
            wbSession.sendWhiteboardObject (obj);
            return obj;
        }
        else if(ws instanceof WhiteboardObjectText)
        {
            if(!supportedWBO.contains (WhiteboardObjectText.NAME))
                return null;
            WhiteboardObjectText obj = (WhiteboardObjectText)
            wbSession.createWhiteboardObject (WhiteboardObjectText.NAME );
            ws.setID (obj.getID ());
            obj.setFontName (((WhiteboardObjectText)ws).getFontName ());
            obj.setFontSize (((WhiteboardObjectText)ws).getFontSize ());
            obj.setText (((WhiteboardObjectText)ws).getText ());
            obj.setWhiteboardPoint (
              ((WhiteboardObjectText)ws).getWhiteboardPoint ());
            obj.setColor (ws.getColor ());
            obj.setThickness (ws.getThickness ());
            wbSession.sendWhiteboardObject (obj);
            return obj;
        }
        else if(ws instanceof WhiteboardObjectImage)
        {
            if(!supportedWBO.contains (WhiteboardObjectImage.NAME))
                return null;
            WhiteboardObjectImage obj = (WhiteboardObjectImage)
            wbSession.createWhiteboardObject (WhiteboardObjectImage.NAME );
            ws.setID (obj.getID ());
            obj.setBackgroundImage (
              ((WhiteboardObjectImage)ws).getBackgroundImage ());
            obj.setHeight (((WhiteboardObjectImage)ws).getHeight ());
            obj.setWhiteboardPoint (
              ((WhiteboardObjectImage)ws).getWhiteboardPoint ());
            obj.setWidth (
              ((WhiteboardObjectImage)ws).getWidth ());
            obj.setColor (ws.getColor ());
            obj.setThickness (ws.getThickness ());
            wbSession.sendWhiteboardObject (obj);

            return obj;
        }

        return null;
    }

    /**
     * Moves a <tt>WhiteboardShape</tt> from from one point to another on the
     * board.
     *
     * @param wbSession the white-board session, to which the moved object
     * belongs
     * @param ws the shape to move
     */
    public void moveWhiteboardObject (  WhiteboardSession wbSession,
                                        WhiteboardShape ws)
    {
        try
        {
            wbSession.moveWhiteboardObject (ws);
        }
        catch (OperationFailedException ex)
        {
            ex.printStackTrace ();
        }
    }

    /**
     * Deletes a <tt>WhiteboardShape</tt> from the white-board.
     *
     * @param wbSession the white-board session, to which the object belongs
     * @param ws the shape to delete
     */
    public void deleteWhiteboardObject (WhiteboardSession wbSession,
                                        WhiteboardShape ws)
    {
        try
        {
            wbSession.deleteWhiteboardObject (ws);
        }
        catch (OperationFailedException ex)
        {
            ex.printStackTrace ();
        }
    }

    /**
     * Called when a modified <tt>WhiteboardObject</tt> has been received.
     *
     * @param evt the <tt>WhiteboardObjectReceivedEvent</tt> containing the
     * modified whiteboardObject, its sender and other details.
     */
    public void whiteboardObjecModified (WhiteboardObjectModifiedEvent evt)
    {
        WhiteboardFrame wbf = getWhiteboardFrame (
            evt.getSourceWhiteboardSession());

        if(wbf == null)
            return;
        wbf.setVisible (true);
        WhiteboardObject wbo = evt.getSourceWhiteboardObject ();
        wbf.receiveWhiteboardObject (wbo);
    }

    /**
     * Called when a new incoming <tt>WhiteboardObject</tt> has been received.
     *
     * @param evt the <tt>WhiteboardObjectReceivedEvent</tt> containing
     * the newly received WhiteboardObject, its sender and other details.
     */
    public void whiteboardObjectReceived (WhiteboardObjectReceivedEvent evt)
    {
        /*
         * There are 2 cases when a message is received:
         * - an existing session
         * - or a new session
         */
        WhiteboardFrame wbFrame = getWhiteboardFrame (
            evt.getSourceWhiteboardSession());

        if(wbFrame == null)
        {
            if (logger.isDebugEnabled())
                logger.debug ("New WBParticipant"
              + evt.getSourceContact ().getDisplayName ());

            wbFrame = new WhiteboardFrame (
                    this,
                    evt.getSourceWhiteboardSession());

            wbFrames.add (wbFrame);
        }

        wbFrame.setVisible (true);
        WhiteboardObject wbObject = evt.getSourceWhiteboardObject ();
        wbFrame.receiveWhiteboardObject (wbObject);
    }

    /**
     * Called when the underlying implementation has received an indication
     * that a WhiteboardObject, sent earlier has been successfully
     * received by the destination.
     *
     * @param evt the WhiteboardObjectDeliveredEvent containing the id of the
     * WhiteboardObject that has caused the event.
     */
    public void whiteboardObjectDelivered (WhiteboardObjectDeliveredEvent evt)
    {
        if (logger.isDebugEnabled())
            logger.debug ("WBObjectDeliveredEvent: The following object: "
            + evt.getSourceWhiteboardObject()
            + " has been delivered to "
            + evt.getDestinationContact().getDisplayName());
    }

    /**
     * Called to indicate that delivery of a WhiteboardObject sent earlier
     * has failed.
     * Reason code and phrase are contained by the
     * <tt>WhiteboardObjectDeliveryFailedEvent</tt>
     *
     * @param evt the <tt>WhiteboardObjectDeliveryFailedEvent</tt>
     * containing the ID of the WhiteboardObject whose delivery has failed.
     */
    public void whiteboardObjectDeliveryFailed (
      WhiteboardObjectDeliveryFailedEvent evt)
    {
        String errorMessage = null;

        if (evt.getErrorCode()
            == WhiteboardObjectDeliveryFailedEvent.NETWORK_FAILURE)
        {
            errorMessage = "Network failure.";
        }
        else if (evt.getErrorCode()
            == WhiteboardObjectDeliveryFailedEvent
                .OFFLINE_MESSAGES_NOT_SUPPORTED)
        {
            errorMessage = "Offline messages aren't supported.";
        }
        else if (evt.getErrorCode()
            == WhiteboardObjectDeliveryFailedEvent
                .PROVIDER_NOT_REGISTERED)
        {
            errorMessage = "Protocol provider is not registered.";
        }
        else if (evt.getErrorCode()
            == WhiteboardObjectDeliveryFailedEvent
                .INTERNAL_ERROR)
        {
            errorMessage = "An internal error occured.";
        }
        else if (evt.getErrorCode()
            == WhiteboardObjectDeliveryFailedEvent
                .UNKNOWN_ERROR)
        {
            errorMessage = "An unknown error occured.";
        }

        String debugErrorMessage
            = "WBObjectDeliveryFailedEvent: The following object: "
                + evt.getSourceWhiteboardObject()
                + " has NOT been delivered to "
                + evt.getDestinationContact().getDisplayName()
                + " because of the following error: "
                + errorMessage;

        if (logger.isDebugEnabled())
            logger.debug (debugErrorMessage);

        WhiteboardActivator.getUiService().getPopupDialog()
            .showMessagePopupDialog(errorMessage,
                                    "Error",
                                    PopupDialog.ERROR_MESSAGE);
    }

    /**
     * Returns the WhiteboardFrame associated with the Contact.
     *
     * @param c contact
     * @return WhiteboardFrame with the Contact or null (if nothing found)
     */
    private WhiteboardFrame getWhiteboardFrame (WhiteboardSession session)
    {
        for (WhiteboardFrame whiteboardFrame : wbFrames)
        {
            if (whiteboardFrame.getWhiteboardSession().equals (session))
                return whiteboardFrame;
        }
        return null;
    }

    /**
     * Returns the WhiteboardFrame associated with the Contact.
     *
     * @param c contact
     * @return WhiteboardFrame with the Contact or null (if nothing found)
     */
    private WhiteboardFrame getWhiteboardFrame (Contact contact)
    {
        WhiteboardFrame whiteboardFrame = null;

        for(int i =0; i < wbFrames.size (); i++)
        {
            whiteboardFrame = wbFrames.get (i);

            if (whiteboardFrame.getContact() != null
                && whiteboardFrame.getContact().equals (contact))
                return whiteboardFrame;
        }
        return null;
    }

    /**
     * Called when a deleted <tt>WhiteboardObject</tt> has been received.
     *
     * @param evt the <tt>WhiteboardObjectDeletedEvent</tt> containing
     * the identification of the deleted WhiteboardObject, its sender and
     * other details.
     */
    public void whiteboardObjectDeleted (WhiteboardObjectDeletedEvent evt)
    {
        WhiteboardFrame wbf = getWhiteboardFrame (
            evt.getSourceWhiteboardSession());

        if(wbf == null)
        {
            return;
        }

        wbf.setVisible (true);
        String id = evt.getId ();
        wbf.receiveDeleteWhiteboardObject (id);
    }

    /**
     * Listens for <tt>WhiteboardInvitationReceivedEvent</tt>s and shows a
     * dialog to the user, where she could accept, reject or ignore an
     * incoming invitation.
     */
    private class InvitationListener implements WhiteboardInvitationListener
    {
        public void invitationReceived(WhiteboardInvitationReceivedEvent evt)
        {
            OperationSetWhiteboarding whiteboardOpSet
                = evt.getSourceOperationSet();

            InvitationReceivedDialog dialog = new InvitationReceivedDialog(
                WhiteboardSessionManager.this,
                whiteboardOpSet,
                evt.getInvitation());

            dialog.pack();

            dialog.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width/2
                    - dialog.getWidth()/2,
                Toolkit.getDefaultToolkit().getScreenSize().height/2
                    - dialog.getHeight()/2
                );

            dialog.setVisible(true);
        }
    }

    /**
     * Called to accept an incoming invitation. Adds the invitation chat room
     * to the list of chat rooms and joins it.
     *
     * @param invitation the invitation to accept.
     */
    public void acceptInvitation(WhiteboardInvitation invitation)
    {
        WhiteboardSession whiteboard = invitation.getTargetWhiteboard();

        byte[] password = invitation.getWhiteboardPassword();

        try
        {
            if(password == null)
                whiteboard.join();
            else
                whiteboard.join(password);
        }
        catch (OperationFailedException e)
        {
            WhiteboardActivator.getUiService().getPopupDialog()
                .showMessagePopupDialog(
                    Resources.getString("failedToJoinWhiteboard",
                        new String[] {whiteboard.getWhiteboardID()}),
                    Resources.getString("service.gui.ERROR"),
                    PopupDialog.ERROR_MESSAGE);

            logger.error("Failed to join whiteboard: "
                + whiteboard.getWhiteboardID(), e);
        }
    }

    /**
     * Rejects the given invitation with the specified reason.
     *
     * @param whiteboardOpSet the operation set to use for rejecting the
     * invitation
     * @param invitation the invitation to reject
     * @param reason the reason for the rejection
     */
    public void rejectInvitation(   OperationSetWhiteboarding whiteboardOpSet,
                                    WhiteboardInvitation invitation,
                                    String reason)
    {
        whiteboardOpSet.rejectInvitation(invitation, reason);
    }

    /**
     * Listens for presence events.
     */
    private class PresenceListener
        implements WhiteboardSessionPresenceListener
    {
        /**
         * Implements the <tt>WhiteboardSessionPresenceListener
         * .whiteboardSessionPresenceChanged</tt> method.
         */
        public void whiteboardSessionPresenceChanged(
            WhiteboardSessionPresenceChangeEvent evt)
        {
            WhiteboardSession whiteboardSession = evt.getWhiteboardSession();

            if (evt.getEventType().equals(
                WhiteboardSessionPresenceChangeEvent.LOCAL_USER_JOINED))
            {
                whiteboardSession.addWhiteboardObjectListener (
                    WhiteboardSessionManager.this);

                WhiteboardFrame frame
                    = getWhiteboardFrame(evt.getWhiteboardSession());

                if (frame == null)
                {
                    frame = new WhiteboardFrame(
                        WhiteboardSessionManager.this,
                        whiteboardSession);

                    frame.setVisible (true);
                    wbFrames.add (frame);
                }
            }
            else if (evt.getEventType().equals(
                WhiteboardSessionPresenceChangeEvent.LOCAL_USER_JOIN_FAILED))
            {
                WhiteboardActivator.getUiService().getPopupDialog()
                    .showMessagePopupDialog (
                        Resources.getString("failedToJoinWhiteboard",
                            new String[]{whiteboardSession.getWhiteboardID()})
                                + evt.getReason(),
                        Resources.getString("service.gui.ERROR"),
                        PopupDialog.ERROR_MESSAGE);
            }
            else if (evt.getEventType().equals(
                WhiteboardSessionPresenceChangeEvent.LOCAL_USER_LEFT))
            {
                WhiteboardFrame frame = getWhiteboardFrame(whiteboardSession);

                if (frame == null)
                    return;

                wbFrames.remove(frame);
                frame.dispose();
                whiteboardSession.removeWhiteboardObjectListener(
                    WhiteboardSessionManager.this);
            }
            else if (evt.getEventType().equals(
                WhiteboardSessionPresenceChangeEvent.LOCAL_USER_KICKED))
            {

            }
            else if (evt.getEventType().equals(
                WhiteboardSessionPresenceChangeEvent.LOCAL_USER_DROPPED))
            {

            }
        }
    }

    /**
     * Removes a white board frame.
     *
     * @param frame the frame to remove
     */
    public void removeWhiteboardWindow(WhiteboardFrame frame)
    {
        synchronized (wbFrames)
        {
            wbFrames.remove(frame);
        }
    }
}
