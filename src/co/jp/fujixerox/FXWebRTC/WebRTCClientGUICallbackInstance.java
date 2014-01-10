package co.jp.fujixerox.FXWebRTC;

/**
 * Created by haiyang on 1/7/14.
 */


import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


public class WebRTCClientGUICallbackInstance implements WebRTCClient.GUICallback  {


    public static final String TAG="WebRTCClientGUICallBackInstance";
    private BroadcastNotifier mbroadcast;
    public WebRTCClientGUICallbackInstance(Context context)
    {
           this.mbroadcast=new BroadcastNotifier(context);
    }

    @Override
    public void AddPeer(int peerID,Peers peer) {


        mbroadcast.broadcastIntentWithState(Constants.PEER_JOIN,peerID);

        Log.d(TAG,"add peer message is sent");

    }

    @Override
    public void RemovePeer(int peerID,Peers peer) {

        mbroadcast.broadcastIntentWithState(Constants.PEER_LEAVE,peerID);

    }

    @Override
    public void UpdatePeer(int peerID,Peers peer) {

        mbroadcast.broadcastIntentWithState(Constants.PEER_UPDATE,peerID);

    }

    @Override
    public void UDPInvitationDeclined(String reason) {

        mbroadcast.broadcastIntentWithState(Constants.UDP_INVITATION_DECLINED);

    }

    @Override
    public void UDPInvitationAccepted() {

        mbroadcast.broadcastIntentWithState(Constants.UDP_INVITATION_ACCEPTED);

    }

    @Override
    public void UDPInvitationReceived(Peers peer) {

        mbroadcast.broadcastIntentWithState(Constants.UDP_INVITATION_RECEIVED,peer.getPeerId());

    }


    @Override
    public void VideoInvitationDeclined(String reason) {

        mbroadcast.broadcastIntentWithState(Constants.VIDEO_INVITATION_DECLINED);

    }

    @Override
    public void VideoInvitationAccepted() {

        mbroadcast.broadcastIntentWithState(Constants.VIDEO_INVITATION_ACCEPTED);

    }

    @Override
    public void VideoInvitationReceived(Peers peer) {

        mbroadcast.broadcastIntentWithState(Constants.VIDEO_INVITATION_RECEIVED,peer.getPeerId());

    }

    @Override
    public void SDPSent() {

    }

    @Override
    public void SDPReceived() {

    }

    @Override
    public void StartICEChecking() {

    }

    @Override
    public void ICEComplete() {

    }

    @Override
    public void ICETerminate() {

    }

    @Override
    public void ICEError() {

    }

    @Override
    public void OnUDPBWStart() {

    }

    @Override
    public void OnUDPBWFailure() {

    }

    @Override
    public void OnUDPBWFinish(String message) {

    }

    @Override
    public void onConnectionClosed(int code, String reason)
    {
        mbroadcast.broadcastIntentWithState(Constants.CONNECTION_CLOSED);
    }
}
