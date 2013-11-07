package co.jp.fujixerox.FXWebRTC;

/**
 * Created with IntelliJ IDEA.
 * User: haiyang
 * Date: 10/25/13
 * Time: 2:26 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PeerConnectionClientObserver {
    public void OnSignedIn();  // Called when we're logged on.
    public void OnDisconnected();
    public void OnPeerConnected(int id,String name);
    public void OnPeerDisconnected(int peer_id) ;
    public void OnMessageFromPeer(int peer_id,String message);
    public void OnMessageSent(int err);
    public void OnServerConnectionFailure();

}
