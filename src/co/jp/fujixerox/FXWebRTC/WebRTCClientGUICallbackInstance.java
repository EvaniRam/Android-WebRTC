package co.jp.fujixerox.FXWebRTC;

/**
 * Created by haiyang on 1/7/14.
 */


import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;


public class WebRTCClientGUICallbackInstance implements WebRTCClient.GUICallback  {


    private Handler messgageHandler;
    public WebRTCClientGUICallbackInstance(Handler handler)
    {
           this.messgageHandler=handler;
    }

    @Override
    public void AddPeer(Peers peer) {

        Message msg=messgageHandler.obtainMessage();



    }

    @Override
    public void RemovePeer(Peers peer) {

    }

    @Override
    public void UpdatePeer(Peers peer) {

    }

    @Override
    public void InvitationDeclined(String reason) {

    }

    @Override
    public void InvitationAccepted() {

    }

    @Override
    public void InvitationReceived(Peers peer) {

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
}
