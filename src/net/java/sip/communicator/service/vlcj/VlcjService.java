package net.java.sip.communicator.service.vlcj;

/**
 * Created by frank on 10/1/14.
 */
public interface VlcjService {
    public void open(String mrl);
    public void play();
    public void pause();
    public void stream();
    public void connect(String dstIp, int port);
}
