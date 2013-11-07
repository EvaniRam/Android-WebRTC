package co.jp.fujixerox.FXWebRTC;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created with IntelliJ IDEA.
 * User: haiyang
 * Date: 10/30/13
 * Time: 3:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class SignalingService extends Service implements PeerConnectionClientObserver{

    public static final String TAG="SignalingService";

    public static final int SLEEP_INTVAL=15;

    private PeerConnectionClient peerConnectionClient;


    // Defines and instantiates an object for handling status updates.
    private BroadcastNotifier mbroadcast=new BroadcastNotifier(this);

    private Boolean shouldStop;

    private Thread networkthread;


    @Override
    public void onCreate() {
        super.onCreate();


        shouldStop=false;


        //create peer connection client
        peerConnectionClient=new PeerConnectionClient();
        peerConnectionClient.registerObserver(this);


        if(SettingActivity.Settings.getContext()==null)
        {
            SettingActivity.Settings.setContext(getApplicationContext());
        }
        String server_host=SettingActivity.Settings.getServerHost();
        int server_port=SettingActivity.Settings.getServerPort();

        String client_name="client_android";
        String ipAdress=getLocalIpAddress();
        if(ipAdress!=null && ipAdress.length()!=0)
            client_name=client_name+"@"+ipAdress;

        peerConnectionClient.setParam(server_host,server_port,client_name);

        peerConnectionClient.initialize();


        networkthread=new NetworkThread();
        networkthread.start();


    }

    @Override
    public void onDestroy()
    {

           if(networkthread.isAlive())


               try {
                   networkthread.join();
               } catch (InterruptedException e) {

                   Log.d(TAG,"error while waiting for network thread to finish");
                   e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
               }

        Log.d(TAG,"service is stopped");
    }




    public IBinder onBind(Intent intent) {
        return null;
    }





    private class NetworkThread extends Thread{


        public void run()
        {

            Looper.prepare();
            peerConnectionClient.connect();

            while(!shouldStop)
            {

                peerConnectionClient.processMessage();

                // new CheckNetworkTask().execute();
                peerConnectionClient.IncomingDataCheck();

                SystemClock.sleep(SLEEP_INTVAL);

            }
        }
    }


    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }





    //implementation of observer methods from PeerConnectionClientObserver
    @Override
    public void OnSignedIn() {

        mbroadcast.broadcastIntentWithState(Constants.CLIENT_SIGNED_IN);

        Log.d(TAG, "client signed in message is sent!");
    }

    @Override
    public void OnDisconnected() {

        mbroadcast.broadcastIntentWithState(Constants.CLIENT_DISCONNECTED);

    }

    @Override
    public void OnPeerConnected(int id, String name) {

        mbroadcast.broadcastIntentWithState(Constants.PEER_CONNECTED,id,name);

    }

    @Override
    public void OnPeerDisconnected(int peer_id) {

        mbroadcast.broadcastIntentWithState(Constants.PEER_DISCONNECTED,peer_id);
    }

    @Override
    public void OnMessageFromPeer(int peer_id, String message) {

        mbroadcast.broadcastIntentWithState(Constants.MESSAGE_FROM_PEER,peer_id,message);

    }

    @Override
    public void OnMessageSent(int err) {

        mbroadcast.broadcastIntentWithState(Constants.MESSAGE_SENT,err);

    }

    @Override
    public void OnServerConnectionFailure() {



        mbroadcast.broadcastIntentWithState(Constants.SERVER_CONNECTION_FAILURE);

        shouldStop=true;

    }
}
