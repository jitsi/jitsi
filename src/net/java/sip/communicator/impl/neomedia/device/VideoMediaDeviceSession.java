/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.device;

import java.awt.*;

import javax.media.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.util.*;

/**
 * Extends <tt>MediaDeviceSession</tt> to add video-specific functionality.
 *
 * @author Lubomir Marinov
 */
public class VideoMediaDeviceSession
    extends MediaDeviceSession
{

    /**
     * The <tt>Logger</tt> used by the <tt>VideoMediaDeviceSession</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(VideoMediaDeviceSession.class);

    /**
     * The facility which aids this instance in managing a list of
     * <tt>VideoListener</tt>s and firing <tt>VideoEvent</tt>s to them.
     */
    private final VideoNotifierSupport videoNotifierSupport
        = new VideoNotifierSupport(this);

    /**
     * Local <tt>Player</tt> for the local video.
     */
    private Player localPlayer = null;

    /**
     * Initializes a new <tt>VideoMediaDeviceSession</tt> instance which is to
     * represent the work of a <tt>MediaStream</tt> with a specific video
     * <tt>MediaDevice</tt>.
     *
     * @param device the video <tt>MediaDevice</tt> the use of which by a
     * <tt>MediaStream</tt> is to be represented by the new instance
     */
    public VideoMediaDeviceSession(AbstractMediaDevice device)
    {
        super(device);
    }

    /**
     * Adds a specific <tt>VideoListener</tt> to this instance in order to
     * receive notifications when visual/video <tt>Component</tt>s are being
     * added and removed.
     * <p>
     * Adding a listener which has already been added does nothing i.e. it is
     * not added more than once and thus does not receive one and the same
     * <tt>VideoEvent</tt> multiple times.
     * </p>
     *
     * @param listener the <tt>VideoListener</tt> to be notified when
     * visual/video <tt>Component</tt>s are being added or removed in this
     * instance
     */
    public void addVideoListener(VideoListener listener)
    {
        videoNotifierSupport.addVideoListener(listener);
    }

    /**
     * Creates the <tt>DataSource</tt> that this instance is to read captured
     * media from.
     *
     * @return the <tt>DataSource</tt> that this instance is to read captured
     * media from
     */
    @Override
    protected DataSource createCaptureDevice()
    {
        /* create our DataSource as Cloneable so we can both use it
         * to display local video and stream to remote peer
         */
        DataSource captureDevice = Manager.createCloneableDataSource(getDevice().createOutputDataSource());
        return captureDevice;
    }

    /**
     * Asserts that a specific <tt>MediaDevice</tt> is acceptable to be set as
     * the <tt>MediaDevice</tt> of this instance. Makes sure that its
     * <tt>MediaType</tt> is {@link MediaType#VIDEO}.
     *
     * @param device the <tt>MediaDevice</tt> to be checked for suitability to
     * become the <tt>MediaDevice</tt> of this instance
     * @see MediaDeviceSession#checkDevice(AbstractMediaDevice)
     */
    @Override
    protected void checkDevice(AbstractMediaDevice device)
    {
        if (!MediaType.VIDEO.equals(device.getMediaType()))
            throw new IllegalArgumentException("device");
    }

    /**
     * Releases the resources allocated by a specific <tt>Player</tt> in the
     * course of its execution and prepares it to be garbage collected. If the
     * specified <tt>Player</tt> is rendering video, notifies the
     * <tt>VideoListener</tt>s of this instance that its visual
     * <tt>Component</tt> is to no longer be used by firing a
     * {@link VideoEvent#VIDEO_REMOVED} <tt>VideoEvent</tt>.
     *
     * @param player the <tt>Player</tt> to dispose of
     * @see MediaDeviceSession#disposePlayer(Player)
     */
    @Override
    protected void disposePlayer(Player player)
    {
        /*
         * The player is being disposed so let the (interested) listeners know
         * its Player#getVisualComponent() (if any) should be released.
         */
        Component visualComponent = getVisualComponent(player);

        super.disposePlayer(player);

        if (visualComponent != null)
            fireVideoEvent(
                VideoEvent.VIDEO_REMOVED,
                visualComponent,
                VideoEvent.REMOTE);
    }

    /**
     * Notifies the <tt>VideoListener</tt>s registered with this instance about
     * a specific type of change in the availability of a specific visual
     * <tt>Component</tt> depicting video.
     *
     * @param type the type of change as defined by <tt>VideoEvent</tt> in the
     * availability of the specified visual <tt>Component</tt> depicting video
     * @param visualComponent the visual <tt>Component</tt> depicting video
     * which has been added or removed in this instance
     * @param origin {@link VideoEvent#LOCAL} if the origin of the video is
     * local (e.g. it is being locally captured); {@link VideoEvent#REMOTE} if
     * the origin of the video is remote (e.g. a remote peer is streaming it)
     * @return <tt>true</tt> if this event and, more specifically, the visual
     * <tt>Component</tt> it describes have been consumed and should be
     * considered owned, referenced (which is important because
     * <tt>Component</tt>s belong to a single <tt>Container</tt> at a time);
     * otherwise, <tt>false</tt>
     */
    protected boolean fireVideoEvent(
            int type,
            Component visualComponent,
            int origin)
    {
        if (logger.isTraceEnabled())
            logger
                .trace(
                    "Firing VideoEvent with type "
                        + VideoEvent.typeToString(type)
                        + " and origin "
                        + VideoEvent.originToString(origin));

        return
            videoNotifierSupport.fireVideoEvent(type, visualComponent, origin);
    }

    /**
     * Get the local <tt>Player</tt> if it exists, 
     * create it otherwise
     * @return local <tt>Player</tt>
     */
    private Player getLocalPlayer()
    {
        DataSource dataSource = ((SourceCloneable)getCaptureDevice()).createClone();

        /* create local player */
        if (localPlayer == null && dataSource != null)
        {
            Exception excpt = null;
            try
            {
                localPlayer = Manager.createPlayer(dataSource);
            }
            catch (Exception ex)
            {
                excpt = ex;
            }

            if(excpt == null)
            {
                localPlayer.addControllerListener(new ControllerListener()
                {
                    public void controllerUpdate(ControllerEvent event)
                    {
                        controllerUpdateForCreateLocalVisualComponent(event);
                    }
                });
                localPlayer.start();
            }
            else
            {
                logger.error("Failed to connect to "
                        + MediaStreamImpl.toString(dataSource),
                    excpt);
            }
        }

        return localPlayer;
    }

    /**
     * Gets notified about <tt>ControllerEvent</tt>s generated by
     * {@link #player}.
     *
     * @param event the <tt>ControllerEvent</tt> specifying the
     * <tt>Controller</tt> which is the source of the event and the very type of
     * the event
     */
    private void controllerUpdateForCreateLocalVisualComponent(
            ControllerEvent controllerEvent)
    {
        if (controllerEvent instanceof RealizeCompleteEvent)
        {
            Player player = (Player) controllerEvent.getSourceController();
            Component visualComponent = player.getVisualComponent();

            if (visualComponent != null)
            {
                if(!fireVideoEvent(
                    VideoEvent.VIDEO_ADDED,
                    visualComponent,
                    VideoEvent.LOCAL))
                {
                    /* no listener interrested by our event
                     * free resources
                     */
                    if(player == localPlayer)
                    {
                        localPlayer = null;
                    }

                    player.stop();
                    player.deallocate();
                    player.close();
                }
            }
        }
    }
              
    /**
     * Gets local visual <tt>Component</tt> of the local peer.
     *
     * @return visual <tt>Component</tt>
     */
    public Component createLocalVisualComponent()
    {
        Player player = getLocalPlayer();
        return null;
    }

    /**
     * Dispose local visual <tt>Component</tt> of the local peer.
     */
    public void disposeLocalVisualComponent()
    {
        Player player = getLocalPlayer();

        if(player != null)
        {
            disposeLocalPlayer(player);
        }
    }

    /**
     * Releases the resources allocated by a specific local <tt>Player</tt> in the
     * course of its execution and prepares it to be garbage collected. If the
     * specified <tt>Player</tt> is rendering video, notifies the
     * <tt>VideoListener</tt>s of this instance that its visual
     * <tt>Component</tt> is to no longer be used by firing a
     * {@link VideoEvent#VIDEO_REMOVED} <tt>VideoEvent</tt>.
     *
     * @param player the <tt>Player</tt> to dispose of
     * @see MediaDeviceSession#disposePlayer(Player)
     */
    protected void disposeLocalPlayer(Player player)
    {
        /*
         * The player is being disposed so let the (interested) listeners know
         * its Player#getVisualComponent() (if any) should be released.
         */
        Component visualComponent = getVisualComponent(player);
        
        if(localPlayer == player)
        {
            localPlayer = null;
        }

        player.stop();
        player.deallocate();
        player.close();

        if (visualComponent != null)
            fireVideoEvent(
                VideoEvent.VIDEO_REMOVED,
                visualComponent,
                VideoEvent.LOCAL);
    }

    /**
     * Returns the visual <tt>Component</tt> where video from the remote peer
     * is being rendered or <tt>null</tt> if no video is currently rendered.
     *
     * @return the visual <tt>Component</tt> where video from the remote peer
     * is being rendered or <tt>null</tt> if no video is currently rendered
     */
    public Component getVisualComponent()
    {
        Player player = getPlayer();

        return (player == null) ? null : getVisualComponent(player);
    }

    /**
     * Gets the visual <tt>Component</tt> of a specific <tt>Player</tt> if it
     * has one and ignores the failure to access it if the specified
     * <tt>Player</tt> is unrealized.
     *
     * @param player the <tt>Player</tt> to get the visual <tt>Component</tt> of
     * if it has one
     * @return the visual <tt>Component</tt> of the specified <tt>Player</tt> if
     * it has one; <tt>null</tt> if the specified <tt>Player</tt> does not have
     * a visual <tt>Component</tt> or the <tt>Player</tt> is unrealized
     */
    private static Component getVisualComponent(Player player)
    {
        Component visualComponent;

        try
        {
            visualComponent = player.getVisualComponent();
        }
        catch (NotRealizedError e)
        {
            visualComponent = null;

            if (logger.isDebugEnabled())
                logger
                    .debug(
                        "Called Player#getVisualComponent() "
                            + "on Unrealized player "
                            + player,
                        e);
        }
        return visualComponent;
    }

    /**
     * Notifies this instance that a specific <tt>Player</tt> of remote content
     * has generated a <tt>RealizeCompleteEvent</tt>.
     *
     * @param player the <tt>Player</tt> which is the source of a
     * <tt>RealizeCompleteEvent</tt>.
     * @see MediaDeviceSession#playerRealizeComplete(Processor)
     */
    @Override
    protected void playerRealizeComplete(Processor player)
    {
        super.playerRealizeComplete(player);

        Component visualComponent = getVisualComponent(player);

        if (visualComponent != null)
            fireVideoEvent(
                VideoEvent.VIDEO_ADDED,
                visualComponent,
                VideoEvent.REMOTE);
    }

    /**
     * Removes a specific <tt>VideoListener</tt> from this instance in order to
     * have to no longer receive notifications when visual/video
     * <tt>Component</tt>s are being added and removed.
     *
     * @param listener the <tt>VideoListener</tt> to no longer be notified when
     * visual/video <tt>Component</tt>s are being added or removed in this
     * instance
     */
    public void removeVideoListener(VideoListener listener)
    {
        videoNotifierSupport.removeVideoListener(listener);
    }
}
