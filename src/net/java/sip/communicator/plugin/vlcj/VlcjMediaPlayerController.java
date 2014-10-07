package net.java.sip.communicator.plugin.vlcj;

import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

/**
 * Created by frank on 10/3/14.
 */
public class VlcjMediaPlayerController {

    //TODO: should encapsulate the mediaPlayer with api calls
    //TODO: is mediaPlayer thread safe? (won't matter if we properly encapsulate)
    //TODO: write an init method that actually creates the VlcjPlayer, don't need to have the VlcjPlayer set this reference....
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public void setMediaPlayer(EmbeddedMediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    private volatile EmbeddedMediaPlayer mediaPlayer = null;

    VlcjMediaPlayerController(EmbeddedMediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    public void pause() {
        if (mediaPlayer == null) return;
        mediaPlayer.pause();
    }
}
