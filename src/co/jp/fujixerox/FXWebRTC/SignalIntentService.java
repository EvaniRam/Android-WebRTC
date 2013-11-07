package co.jp.fujixerox.FXWebRTC;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import android.os.Handler.Callback;

/**
 * Created with IntelliJ IDEA.
 * User: lapmore
 * Date: 10/24/13
 * Time: 9:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class SignalIntentService extends IntentService  implements PeerConnectionClientObserver {

    public static final String TAG="SignalIntentService";

    public static final int SLEEP_INTVAL=15;

    private PeerConnectionClient peerConnectionClient;

    private Handler mHandler;
    // Defines and instantiates an object for handling status updates.
    private BroadcastNotifier mbroadcast=new BroadcastNotifier(this);

    private Boolean shouldStop;




    /**
     * An IntentService must always have a constructor that calls the super constructor. The
     * string supplied to the super constructor is used to give a name to the IntentService's
     * background thread.
     */
    public SignalIntentService() {
        super("SignalIntentService");
    }



    /**
     * In an IntentService, onHandleIntent is run on a background thread.  As it
     * runs, it broadcasts its current status using the LocalBroadcastManager.
     * @param workIntent The Intent that starts the IntentService. This Intent contains the
     * URL of the web site from which the RSS parser gets data.
     */
    @Override
    protected void onHandleIntent(Intent workIntent) {

        shouldStop=false;


        //create peer connection client
        peerConnectionClient=new PeerConnectionClient();
        peerConnectionClient.registerObserver(this);



        /*
        HandlerThread handlerThread=new HandlerThread("SERVICE_HANDLER_THREAD");
        handlerThread.start();
        mHandler=new Handler(handlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg)
            {
                if(peerConnectionClient!=null)
                   peerConnectionClient.handleMessage(msg);


            }
        };
        */


      //  Looper.prepare();

     /*   mHandler=new Handler(Looper.myLooper())
        {
            @Override
            public void handleMessage(Message msg)
            {
                if(peerConnectionClient!=null)
                    peerConnectionClient.handleMessage(msg);


            }
        };


        peerConnectionClient.setHandler(mHandler);

        */





        if(SettingActivity.Settings.getContext()==null)
        {
            SettingActivity.Settings.setContext(getApplicationContext());
        }
        String server_host=SettingActivity.Settings.getServerHost();
        int server_port=SettingActivity.Settings.getServerPort();
        peerConnectionClient.setParam(server_host,server_port,"client_andriod");

        peerConnectionClient.initialize();




        //connect to server

        //Log.d(TAG, "going to connect to server!");
        peerConnectionClient.connect();
        //Log.d(TAG, "going to connect to server finished!");

        //check incoming data from sockets


        while(!shouldStop)
        {

           // Looper.loop();
            peerConnectionClient.processMessage();


            peerConnectionClient.IncomingDataCheck();



            SystemClock.sleep(SLEEP_INTVAL);


        }

        Log.d(TAG,"intent service quits");


    }


    //implementation of observer methods from PeerConnectionClientObserver
    @Override
    public void OnSignedIn() {

        mbroadcast.broadcastIntentWithState(Constants.CLIENT_SIGNED_IN);

        Log.d(TAG,"client signed in message is sent!");
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