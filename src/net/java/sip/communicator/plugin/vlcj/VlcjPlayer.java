package net.java.sip.communicator.plugin.vlcj;
/*
 * This file is part of VLCJ.
 *
 * VLCJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VLCJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VLCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014 Caprica Software Limited.
 */

import com.sun.awt.AWTUtilities;
import com.sun.jna.platform.WindowUtils;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.logger.Logger;
import uk.co.caprica.vlcj.player.embedded.DefaultFullScreenStrategy;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.FullScreenStrategy;
import uk.co.caprica.vlcj.test.basic.EqualizerFrame;
import uk.co.caprica.vlcj.test.basic.PlayerControlsPanel;
import uk.co.caprica.vlcj.test.basic.PlayerVideoAdjustPanel;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import uk.co.caprica.vlcj.player.AudioOutput;
import uk.co.caprica.vlcj.player.Equalizer;
import uk.co.caprica.vlcj.player.MediaDetails;
import uk.co.caprica.vlcj.player.MediaMeta;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;

/**
    gui extension of uk.co.caprica.vlcj.player.MediaPlayer
    goal: have this be a vlcj instance that jitsi has an api to: DONE
    goal: be able to control from jitsi code: DONE
    goal: be able to control from jitsi OVER XMPP from another client through MessageListener: DONE
 */
public class VlcjPlayer {

    private final JFrame mainFrame;
    private final Canvas videoSurface;
    private final JPanel controlsPanel;
    private final JPanel videoAdjustPanel;

    private final JFrame equalizerFrame;

    private MediaPlayerFactory mediaPlayerFactory;

    private EmbeddedMediaPlayer mediaPlayer;

    private Equalizer equalizer;

    private final VlcjMediaPlayerController mediaPlayerController;

    public VlcjPlayer(VlcjMediaPlayerController mediaPlayerController) {
        this.mediaPlayerController = mediaPlayerController;
        videoSurface = new Canvas();

        videoSurface.setBackground(Color.black);
        videoSurface.setSize(800, 600); // Only for initial layout

        // Since we're mixing lightweight Swing components and heavyweight AWT
        // components this is probably a good idea
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        VlcjPlayerMouseListener mouseListener = new VlcjPlayerMouseListener();
        videoSurface.addMouseListener(mouseListener);
        videoSurface.addMouseMotionListener(mouseListener);
        videoSurface.addMouseWheelListener(mouseListener);
        videoSurface.addKeyListener(new TestPlayerKeyListener());

        List<String> vlcArgs = new ArrayList<String>();

        vlcArgs.add("--no-snapshot-preview");
        vlcArgs.add("--quiet");
        vlcArgs.add("--quiet-synchro");
        vlcArgs.add("--intf");
        vlcArgs.add("dummy");

        // Special case to help out users on Windows (supposedly this is not actually needed)...
        // if(RuntimeUtil.isWindows()) {
        // vlcArgs.add("--plugin-path=" + WindowsRuntimeUtil.getVlcInstallDir() + "\\plugins");
        // }
        // else {
        // vlcArgs.add("--plugin-path=/home/linux/vlc/lib");
        // }

        // vlcArgs.add("--plugin-path=" + System.getProperty("user.home") + "/.vlcj");

        Logger.debug("vlcArgs={}", vlcArgs);

        mainFrame = new JFrame("VLCJ Test Player");
        mainFrame.setIconImage(new ImageIcon(getClass().getResource("/icons/vlcj-logo.png")).getImage());

        FullScreenStrategy fullScreenStrategy = new DefaultFullScreenStrategy(mainFrame);

        mediaPlayerFactory = new MediaPlayerFactory(vlcArgs.toArray(new String[vlcArgs.size()]));
        mediaPlayerFactory.setUserAgent("vlcj test player");

        List<AudioOutput> audioOutputs = mediaPlayerFactory.getAudioOutputs();
        Logger.debug("audioOutputs={}", audioOutputs);

        mediaPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer(fullScreenStrategy);
        mediaPlayer.setVideoSurface(mediaPlayerFactory.newVideoSurface(videoSurface));
        mediaPlayer.setPlaySubItems(true);
        String options = formatRtspStream("0.0.0.0", 5555, "demo");
        mediaPlayer.setStandardMediaOptions(options, ":no-sout-rtp-sap",
                ":no-sout-standard-sap",
                ":sout-all",
                ":sout-keep");
        mediaPlayer.setEnableKeyInputHandling(false);
        mediaPlayer.setEnableMouseInputHandling(false);
        mediaPlayerController.setMediaPlayer(mediaPlayer);

        controlsPanel = new PlayerControlsPanel(mediaPlayer);
        videoAdjustPanel = new PlayerVideoAdjustPanel(mediaPlayer);

        mainFrame.setLayout(new BorderLayout());
        mainFrame.setBackground(Color.black);
        mainFrame.add(videoSurface, BorderLayout.CENTER);
        mainFrame.add(controlsPanel, BorderLayout.SOUTH);
        mainFrame.add(videoAdjustPanel, BorderLayout.EAST);
        mainFrame.setJMenuBar(buildMenuBar());
        mainFrame.pack();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                Logger.debug("windowClosing(evt={})", evt);

                if(mediaPlayer != null) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }

                if(mediaPlayerFactory != null) {
                    mediaPlayerFactory.release();
                    mediaPlayerFactory = null;
                }
            }
        });

        if(mediaPlayerFactory.isEqualizerAvailable()) {
            equalizer = mediaPlayerFactory.newEqualizer();
            equalizerFrame = new EqualizerFrame(mediaPlayerFactory.getEqualizerBandFrequencies(), mediaPlayerFactory.getEqualizerPresetNames(), mediaPlayerFactory, mediaPlayer, equalizer);
        }
        else {
            equalizerFrame = null;
        }

        // Global AWT key handler, you're better off using Swing's InputMap and
        // ActionMap with a JFrame - that would solve all sorts of focus issues too
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent event) {
                if(event instanceof KeyEvent) {
                    KeyEvent keyEvent = (KeyEvent)event;
                    if(keyEvent.getID() == KeyEvent.KEY_PRESSED) {
                        if(keyEvent.getKeyCode() == KeyEvent.VK_F12) {
                            controlsPanel.setVisible(!controlsPanel.isVisible());
                            videoAdjustPanel.setVisible(!videoAdjustPanel.isVisible());
                            mainFrame.getJMenuBar().setVisible(!mainFrame.getJMenuBar().isVisible());
                            mainFrame.invalidate();
                            mainFrame.validate();
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_A) {
                            mediaPlayer.setAudioDelay(mediaPlayer.getAudioDelay() - 50000);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_S) {
                            mediaPlayer.setAudioDelay(mediaPlayer.getAudioDelay() + 50000);
                        }
                        // else if(keyEvent.getKeyCode() == KeyEvent.VK_N) {
                        // mediaPlayer.nextFrame();
                        // }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_1) {
                            mediaPlayer.setTime(60000 * 1);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_2) {
                            mediaPlayer.setTime(60000 * 2);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_3) {
                            mediaPlayer.setTime(60000 * 3);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_4) {
                            mediaPlayer.setTime(60000 * 4);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_5) {
                            mediaPlayer.setTime(60000 * 5);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_6) {
                            mediaPlayer.setTime(60000 * 6);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_7) {
                            mediaPlayer.setTime(60000 * 7);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_8) {
                            mediaPlayer.setTime(60000 * 8);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_9) {
                            mediaPlayer.setTime(60000 * 9);
                        }
                    }
                }
            }
        }, AWTEvent.KEY_EVENT_MASK);

        mainFrame.setVisible(true);

        if(mediaPlayerFactory.isEqualizerAvailable()) {
            equalizerFrame.pack();
            equalizerFrame.setVisible(true);
        }

        mediaPlayer.addMediaPlayerEventListener(new TestPlayerMediaPlayerEventListener());

        // Won't work with OpenJDK or JDK1.7, requires a Sun/Oracle JVM (currently)
        boolean transparentWindowsSupport = true;
        try {
            Class.forName("com.sun.awt.AWTUtilities");
        }
        catch(Exception e) {
            transparentWindowsSupport = false;
        }

        Logger.debug("transparentWindowsSupport={}", transparentWindowsSupport);

        if(transparentWindowsSupport) {
            final Window test = new Window(null, WindowUtils.getAlphaCompatibleGraphicsConfiguration()) {
                private static final long serialVersionUID = 1L;

                @Override
                public void paint(Graphics g) {
                    Graphics2D g2 = (Graphics2D)g;

                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                    g.setColor(Color.white);
                    g.fillRoundRect(100, 150, 100, 100, 32, 32);

                    g.setFont(new Font("Sans", Font.BOLD, 32));
                    g.drawString("Heavyweight overlay test", 100, 300);
                }
            };

            AWTUtilities.setWindowOpaque(test, false); // Doesn't work in full-screen exclusive
            // mode, you would have to use 'simulated'
            // full-screen - requires Sun/Oracle JDK
            test.setBackground(new Color(0, 0, 0, 0)); // This is what you do in JDK7

            // mediaPlayer.setOverlay(test);
            // mediaPlayer.enableOverlay(true);
        }

        // This might be useful
        // enableMousePointer(false);
    }

    public VlcjPlayer(VlcjMediaPlayerController mediaPlayerController, Object dummy) {
        this.mediaPlayerController = mediaPlayerController;
        this.videoSurface = new Canvas();

        //NOTE: moved this from between the videoSurface set up to above it
        // Since we're mixing lightweight Swing components and heavyweight AWT
        // components this is probably a good idea
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        setupVideoSurface();
        mainFrame = new JFrame("VLCJ Test Player");
        mainFrame.setIconImage(new ImageIcon(getClass().getResource("/icons/vlcj-logo.png")).getImage());
        FullScreenStrategy fullScreenStrategy = new DefaultFullScreenStrategy(mainFrame);
        //QUESTION: is setupMainFrame dependent on this?
        setupMediaPlayer(fullScreenStrategy);

        controlsPanel = new PlayerControlsPanel(mediaPlayer);
        videoAdjustPanel = new PlayerVideoAdjustPanel(mediaPlayer);

        mainFrame.setLayout(new BorderLayout());
        mainFrame.setBackground(Color.black);
        mainFrame.add(videoSurface, BorderLayout.CENTER);
        mainFrame.add(controlsPanel, BorderLayout.SOUTH);
        mainFrame.add(videoAdjustPanel, BorderLayout.EAST);
        mainFrame.setJMenuBar(buildMenuBar());
        mainFrame.pack();
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                Logger.debug("windowClosing(evt={})", evt);

                if(mediaPlayer != null) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }

                if(mediaPlayerFactory != null) {
                    mediaPlayerFactory.release();
                    mediaPlayerFactory = null;
                }
            }
        });

        if(mediaPlayerFactory.isEqualizerAvailable()) {
            equalizer = mediaPlayerFactory.newEqualizer();
            equalizerFrame = new EqualizerFrame(mediaPlayerFactory.getEqualizerBandFrequencies(), mediaPlayerFactory.getEqualizerPresetNames(), mediaPlayerFactory, mediaPlayer, equalizer);
        }
        else {
            equalizerFrame = null;
        }

        // Global AWT key handler, you're better off using Swing's InputMap and
        // ActionMap with a JFrame - that would solve all sorts of focus issues too
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {

            public void eventDispatched(AWTEvent event) {
                if(event instanceof KeyEvent) {
                    KeyEvent keyEvent = (KeyEvent)event;
                    if(keyEvent.getID() == KeyEvent.KEY_PRESSED) {
                        if(keyEvent.getKeyCode() == KeyEvent.VK_F12) {
                            controlsPanel.setVisible(!controlsPanel.isVisible());
                            videoAdjustPanel.setVisible(!videoAdjustPanel.isVisible());
                            mainFrame.getJMenuBar().setVisible(!mainFrame.getJMenuBar().isVisible());
                            mainFrame.invalidate();
                            mainFrame.validate();
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_A) {
                            mediaPlayer.setAudioDelay(mediaPlayer.getAudioDelay() - 50000);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_S) {
                            mediaPlayer.setAudioDelay(mediaPlayer.getAudioDelay() + 50000);
                        }
                        // else if(keyEvent.getKeyCode() == KeyEvent.VK_N) {
                        // mediaPlayer.nextFrame();
                        // }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_1) {
                            mediaPlayer.setTime(60000 * 1);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_2) {
                            mediaPlayer.setTime(60000 * 2);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_3) {
                            mediaPlayer.setTime(60000 * 3);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_4) {
                            mediaPlayer.setTime(60000 * 4);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_5) {
                            mediaPlayer.setTime(60000 * 5);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_6) {
                            mediaPlayer.setTime(60000 * 6);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_7) {
                            mediaPlayer.setTime(60000 * 7);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_8) {
                            mediaPlayer.setTime(60000 * 8);
                        }
                        else if(keyEvent.getKeyCode() == KeyEvent.VK_9) {
                            mediaPlayer.setTime(60000 * 9);
                        }
                    }
                }
            }
        }, AWTEvent.KEY_EVENT_MASK);

        mainFrame.setVisible(true);

        if(mediaPlayerFactory.isEqualizerAvailable()) {
            equalizerFrame.pack();
            equalizerFrame.setVisible(true);
        }

        mediaPlayer.addMediaPlayerEventListener(new TestPlayerMediaPlayerEventListener());

        // Won't work with OpenJDK or JDK1.7, requires a Sun/Oracle JVM (currently)
        boolean transparentWindowsSupport = true;
        try {
            Class.forName("com.sun.awt.AWTUtilities");
        }
        catch(Exception e) {
            transparentWindowsSupport = false;
        }

        Logger.debug("transparentWindowsSupport={}", transparentWindowsSupport);

        if(transparentWindowsSupport) {
            final Window test = new Window(null, WindowUtils.getAlphaCompatibleGraphicsConfiguration()) {
                private static final long serialVersionUID = 1L;

                @Override
                public void paint(Graphics g) {
                    Graphics2D g2 = (Graphics2D)g;

                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

                    g.setColor(Color.white);
                    g.fillRoundRect(100, 150, 100, 100, 32, 32);

                    g.setFont(new Font("Sans", Font.BOLD, 32));
                    g.drawString("Heavyweight overlay test", 100, 300);
                }
            };

            AWTUtilities.setWindowOpaque(test, false); // Doesn't work in full-screen exclusive
            // mode, you would have to use 'simulated'
            // full-screen - requires Sun/Oracle JDK
            test.setBackground(new Color(0, 0, 0, 0)); // This is what you do in JDK7

            // mediaPlayer.setOverlay(test);
            // mediaPlayer.enableOverlay(true);
        }

        // This might be useful
        // enableMousePointer(false);
    }

    private void setupVideoSurface() {

        videoSurface.setBackground(Color.black);
        videoSurface.setSize(800, 600); // Only for initial layout
        VlcjPlayerMouseListener mouseListener = new VlcjPlayerMouseListener();
        videoSurface.addMouseListener(mouseListener);
        videoSurface.addMouseMotionListener(mouseListener);
        videoSurface.addMouseWheelListener(mouseListener);
        videoSurface.addKeyListener(new TestPlayerKeyListener());
    }

    private void setupMediaPlayer(FullScreenStrategy fullScreenStrategy) {
        java.util.List<String> vlcArgs = new ArrayList<String>();

        vlcArgs.add("--no-snapshot-preview");
        vlcArgs.add("--quiet");
        vlcArgs.add("--quiet-synchro");
        vlcArgs.add("--intf");
        vlcArgs.add("dummy");

        // Special case to help out users on Windows (supposedly this is not actually needed)...
        // if(RuntimeUtil.isWindows()) {
        // vlcArgs.add("--plugin-path=" + WindowsRuntimeUtil.getVlcInstallDir() + "\\plugins");
        // }
        // else {
        // vlcArgs.add("--plugin-path=/home/linux/vlc/lib");
        // }

        // vlcArgs.add("--plugin-path=" + System.getProperty("user.home") + "/.vlcj");

        Logger.debug("vlcArgs={}", vlcArgs);


        mediaPlayerFactory = new MediaPlayerFactory(vlcArgs.toArray(new String[vlcArgs.size()]));
        mediaPlayerFactory.setUserAgent("vlcj test player");

        List<AudioOutput> audioOutputs = mediaPlayerFactory.getAudioOutputs();
        Logger.debug("audioOutputs={}", audioOutputs);

        mediaPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer(fullScreenStrategy);
        mediaPlayer.setVideoSurface(mediaPlayerFactory.newVideoSurface(videoSurface));
        mediaPlayer.setPlaySubItems(true);

        mediaPlayer.setEnableKeyInputHandling(false);
        mediaPlayer.setEnableMouseInputHandling(false);
        mediaPlayerController.setMediaPlayer(mediaPlayer);
    }

    private JMenuBar buildMenuBar() {
        // Menus are just added as an example of overlapping the video - they are
        // non-functional in this demo player

        JMenuBar menuBar = new JMenuBar();

        JMenu mediaMenu = new JMenu("Media");
        mediaMenu.setMnemonic('m');

        JMenuItem mediaPlayFileMenuItem = new JMenuItem("Play File...");
        mediaPlayFileMenuItem.setMnemonic('f');
        mediaMenu.add(mediaPlayFileMenuItem);

        JMenuItem mediaPlayStreamMenuItem = new JMenuItem("Play Stream...");
        mediaPlayFileMenuItem.setMnemonic('s');
        mediaMenu.add(mediaPlayStreamMenuItem);

        mediaMenu.add(new JSeparator());

        JMenuItem mediaExitMenuItem = new JMenuItem("Exit");
        mediaExitMenuItem.setMnemonic('x');
        mediaMenu.add(mediaExitMenuItem);

        menuBar.add(mediaMenu);

        JMenu playbackMenu = new JMenu("Playback");
        playbackMenu.setMnemonic('p');

        JMenu playbackChapterMenu = new JMenu("Chapter");
        playbackChapterMenu.setMnemonic('c');
        for(int i = 1; i <= 25; i ++ ) {
            JMenuItem chapterMenuItem = new JMenuItem("Chapter " + i);
            playbackChapterMenu.add(chapterMenuItem);
        }
        playbackMenu.add(playbackChapterMenu);

        JMenu subtitlesMenu = new JMenu("Subtitles");
        playbackChapterMenu.setMnemonic('s');
        String[] subs = {"01 English (en)", "02 English Commentary (en)", "03 French (fr)", "04 Spanish (es)", "05 German (de)", "06 Italian (it)"};
        for(int i = 0; i < subs.length; i ++ ) {
            JMenuItem subtitlesMenuItem = new JMenuItem(subs[i]);
            subtitlesMenu.add(subtitlesMenuItem);
        }
        playbackMenu.add(subtitlesMenu);

        menuBar.add(playbackMenu);

        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setMnemonic('t');

        JMenuItem toolsPreferencesMenuItem = new JMenuItem("Preferences...");
        toolsPreferencesMenuItem.setMnemonic('p');
        toolsMenu.add(toolsPreferencesMenuItem);

        menuBar.add(toolsMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('h');

        JMenuItem helpAboutMenuItem = new JMenuItem("About...");
        helpAboutMenuItem.setMnemonic('a');
        helpMenu.add(helpAboutMenuItem);

        menuBar.add(helpMenu);

        return menuBar;
    }


    /**
     * Set the standard look and feel.
     */
    protected static final void setLookAndFeel() {
        String lookAndFeelClassName = null;
        UIManager.LookAndFeelInfo[] lookAndFeelInfos = UIManager.getInstalledLookAndFeels();
        for(UIManager.LookAndFeelInfo lookAndFeel : lookAndFeelInfos) {
            if("Nimbus".equals(lookAndFeel.getName())) {
                lookAndFeelClassName = lookAndFeel.getClassName();
            }
        }
        if(lookAndFeelClassName == null) {
            lookAndFeelClassName = UIManager.getSystemLookAndFeelClassName();
        }
        try {
            UIManager.setLookAndFeel(lookAndFeelClassName);
        }
        catch(Exception e) {
            // Silently fail, it doesn't matter
        }
    }

    private final class TestPlayerMediaPlayerEventListener extends MediaPlayerEventAdapter {
        @Override
        public void mediaChanged(MediaPlayer mediaPlayer, libvlc_media_t media, String mrl) {
            Logger.debug("mediaChanged(mediaPlayer={},media={},mrl={})", mediaPlayer, media, mrl);
        }

        @Override
        public void finished(MediaPlayer mediaPlayer) {
            Logger.debug("finished(mediaPlayer={})", mediaPlayer);
        }

        @Override
        public void paused(MediaPlayer mediaPlayer) {
            Logger.debug("paused(mediaPlayer={})", mediaPlayer);
        }

        @Override
        public void playing(MediaPlayer mediaPlayer) {
            Logger.debug("playing(mediaPlayer={})", mediaPlayer);
            MediaDetails mediaDetails = mediaPlayer.getMediaDetails();
            Logger.info("mediaDetails={}", mediaDetails);
        }

        @Override
        public void stopped(MediaPlayer mediaPlayer) {
            Logger.debug("stopped(mediaPlayer={})", mediaPlayer);
        }

        @Override
        public void videoOutput(MediaPlayer mediaPlayer, int newCount) {
            Logger.debug("videoOutput(mediaPlayer={},newCount={})", mediaPlayer, newCount);
            if(newCount == 0) {
                return;
            }

            MediaDetails mediaDetails = mediaPlayer.getMediaDetails();
            Logger.info("mediaDetails={}", mediaDetails);

            MediaMeta mediaMeta = mediaPlayer.getMediaMeta();
            Logger.info("mediaMeta={}", mediaMeta);

            final Dimension dimension = mediaPlayer.getVideoDimension();
            Logger.debug("dimension={}", dimension);
            if(dimension != null) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        videoSurface.setSize(dimension);
                        mainFrame.pack();
                    }
                });
            }

            // You can set a logo like this if you like...
            File logoFile = new File("./etc/vlcj-logo.png");
            if(logoFile.exists()) {
                mediaPlayer.setLogoFile(logoFile.getAbsolutePath());
                mediaPlayer.setLogoOpacity(0.5f);
                mediaPlayer.setLogoLocation(10, 10);
                mediaPlayer.enableLogo(true);
            }

            // Demo the marquee
            mediaPlayer.setMarqueeText("vlcj java bindings for vlc");
            mediaPlayer.setMarqueeSize(40);
            mediaPlayer.setMarqueeOpacity(95);
            mediaPlayer.setMarqueeColour(Color.white);
            mediaPlayer.setMarqueeTimeout(5000);
            mediaPlayer.setMarqueeLocation(50, 120);
            mediaPlayer.enableMarquee(true);

            // Not quite sure how crop geometry is supposed to work...
            //
            // Assertions in libvlc code:
            //
            // top + height must be less than visible height
            // left + width must be less than visible width
            //
            // With DVD source material:
            //
            // Reported size is 1024x576 - this is what libvlc reports when you call
            // get video size
            //
            // mpeg size is 720x576 - this is what is reported in the native log
            //
            // The crop geometry relates to the mpeg size, not the size reported
            // through the API
            //
            // For 720x576, attempting to set geometry to anything bigger than
            // 719x575 results in the assertion failures above (seems like it should
            // allow 720x576) to me

            // mediaPlayer.setCropGeometry("4:3");
        }

        @Override
        public void error(MediaPlayer mediaPlayer) {
            Logger.debug("error(mediaPlayer={})", mediaPlayer);
        }

        @Override
        public void mediaSubItemAdded(MediaPlayer mediaPlayer, libvlc_media_t subItem) {
            Logger.debug("mediaSubItemAdded(mediaPlayer={},subItem={})", mediaPlayer, subItem);
        }

        @Override
        public void mediaDurationChanged(MediaPlayer mediaPlayer, long newDuration) {
            Logger.debug("mediaDurationChanged(mediaPlayer={},newDuration={})", mediaPlayer, newDuration);
        }

        @Override
        public void mediaParsedChanged(MediaPlayer mediaPlayer, int newStatus) {
            Logger.debug("mediaParsedChanged(mediaPlayer={},newStatus={})", mediaPlayer, newStatus);
        }

        @Override
        public void mediaFreed(MediaPlayer mediaPlayer) {
            Logger.debug("mediaFreed(mediaPlayer={})", mediaPlayer);
        }

        @Override
        public void mediaStateChanged(MediaPlayer mediaPlayer, int newState) {
            Logger.debug("mediaStateChanged(mediaPlayer={},newState={})", mediaPlayer, newState);
        }

        @Override
        public void mediaMetaChanged(MediaPlayer mediaPlayer, int metaType) {
            Logger.debug("mediaMetaChanged(mediaPlayer={},metaType={})", mediaPlayer, metaType);
        }
    }

    /**
     *
     *
     * @param enable
     */
    @SuppressWarnings("unused")
    private void enableMousePointer(boolean enable) {
        Logger.debug("enableMousePointer(enable={})", enable);
        if(enable) {
            videoSurface.setCursor(null);
        }
        else {
            Image blankImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            videoSurface.setCursor(Toolkit.getDefaultToolkit().createCustomCursor(blankImage, new Point(0, 0), ""));
        }
    }

    /**
     *
     */
    private final class VlcjPlayerMouseListener extends MouseAdapter {
        @Override
        public void mouseMoved(MouseEvent e) {
            Logger.trace("mouseMoved(e={})", e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            Logger.debug("mousePressed(e={})", e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            Logger.debug("mouseReleased(e={})", e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            Logger.debug("mouseClicked(e={})", e);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            Logger.debug("mouseWheelMoved(e={})", e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            Logger.debug("mouseEntered(e={})", e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            Logger.debug("mouseExited(e={})", e);
        }
    }

    /**
     *
     */
    private final class TestPlayerKeyListener extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            Logger.debug("keyPressed(e={})", e);
        }

        @Override
        public void keyReleased(KeyEvent e) {
            Logger.debug("keyReleased(e={})", e);
        }

        @Override
        public void keyTyped(KeyEvent e) {
            Logger.debug("keyTyped(e={})", e);
        }
    }

    private static String formatRtspStream(String serverAddress, int serverPort, String id) {
        StringBuilder sb = new StringBuilder(60);
        sb.append(":sout=#transcode{vcodec=h264,scale=0.5,audio-sync}:duplicate{dst=display,dst=rtp{sdp=rtsp://@");
        sb.append(serverAddress);
        sb.append(':');
        sb.append(serverPort);
        sb.append('/');
        sb.append(id);
        sb.append("}");
        return sb.toString();
    }
    private final String vlcOptions = ":sout=#transcode{vcodec=h264,scale=0.25,acodec=mpga,ab=128,channels=2,samplerate=44100,audio-sync} :sout-all :sout-keep";
}
