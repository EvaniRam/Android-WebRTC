package co.jp.fujixerox.FXWebRTC;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by haiyang on 1/10/14.
 */
public class UDPTestActivity extends Activity {


    public static final String TAG="UDPTestActivity";
    private Peers currentContactedPeer;
    private WebRTCClient webRTCClient;
    private BroadcastReceiver mclientstatereceiver;
    private boolean ifInitiator;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.udptestview);

        ImageView img = (ImageView)findViewById(R.id.imageViewUdpTestProgressAnimation);
        img.setBackgroundResource(R.drawable.progressindicator);

        AnimationDrawable animationDrawable=(AnimationDrawable)img.getBackground();
        animationDrawable.start();


        //create a local broadcast receiver
        mclientstatereceiver=new ClientStateReceiver(this);
        // The filter's action is BROADCAST_ACTION
        IntentFilter clientstatusIntentFilter = new IntentFilter(
                Constants.BROADCAST_ACTION);
        clientstatusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mclientstatereceiver, clientstatusIntentFilter);

        //get webrtc client instance
        ApplicationEx app=(ApplicationEx)getApplicationContext();
        webRTCClient=app.getWebRTCClient();

        if(webRTCClient==null)
        {
            Log.e(TAG, "cannot get webrtc client instance!");
        }


        //get current contacted peer id
        int peer_id=getIntent().getIntExtra("peer_id",-1);
        ifInitiator=getIntent().getBooleanExtra("initiator",false);

        currentContactedPeer=webRTCClient.getPeers().get(peer_id);


        TextView textview=(TextView)findViewById(R.id.UDPBandwidthMeasurementStatusText);


        if(ifInitiator)
        {
        textview.setText("Sending "+currentContactedPeer.getName()+" an invitation......");

        new sendInvitationTask().execute(webRTCClient);
        }
        else
        {
        textview.setText("Sending "+currentContactedPeer.getName()+" acceptance......");

        new InvitationAcceptTask().execute(webRTCClient);
        }


        //sent this peer an invitation message  through asyncTask


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.udp_test_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.udp_test_menu_terminate:
                showTerminateDialog();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
                showTerminateDialog();
                return true;
            default:
                break;

        }



        return false;
    }


    public void showTerminateDialog()
    {

    }

    public void startICECheck()
    {

    }


    private class sendInvitationTask extends AsyncTask<WebRTCClient,Integer,Boolean>
    {

        private WebRTCClient client;

        @Override
        protected Boolean doInBackground(WebRTCClient... params) {



            client=params[0];

            return client.sendUDPInvitation(currentContactedPeer.getPeerId());

        }



        protected void onPostExecute(Boolean result)
        {
            TextView textview=(TextView)findViewById(R.id.UDPBandwidthMeasurementStatusText);
             if(result)
             {

                 textview.setText("Invitation has been sent to "+currentContactedPeer.getName()+", waiting response......");
             }
             else
             {

                 textview.setText("Sending invitation to "+currentContactedPeer.getName()+"failed");
             }
        }
    }





    private class InvitationAcceptTask extends AsyncTask<WebRTCClient,Integer,Boolean>
    {

        private WebRTCClient client;

        @Override
        protected Boolean doInBackground(WebRTCClient... params) {



            client=params[0];

            return client.acceptPeer(currentContactedPeer.getPeerId());

        }



        protected void onPostExecute(Boolean result)
        {
            TextView textview=(TextView)findViewById(R.id.UDPBandwidthMeasurementStatusText);
            if(result)
            {

                textview.setText("Acceptance has been sent to "+currentContactedPeer.getName()+", start ICE Checking......");

                startICECheck();
            }
            else
            {

                textview.setText("Sending acceptance to "+currentContactedPeer.getName()+"failed");
            }
        }
    }


    private class ClientStateReceiver extends BroadcastReceiver
    {

        private Activity activity;
        private ClientStateReceiver(Activity activity)
        {
            this.activity=activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {



            int state=intent.getIntExtra(Constants.EXTENDED_DATA_STATUS,Constants.CLIENT_STATE_NULL);
            final int peer_id;

            switch(state)
            {



                case Constants.UDP_INVITATION_ACCEPTED:

                    peer_id=intent.getIntExtra(Constants.EXTENDED_DATA_ID,-1);


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Log.d(TAG,"udp invitation is accepted!");

                            TextView textview=(TextView)findViewById(R.id.UDPBandwidthMeasurementStatusText);

                            textview.setText("Invitation accepted. Start ICE Checking ......");

                            startICECheck();


                        }
                    });

                    break;
                case Constants.UDP_INVITATION_DECLINED:

                    peer_id=intent.getIntExtra(Constants.EXTENDED_DATA_ID,-1);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Log.d(TAG,"udp invitation is declined");

                            TextView textview=(TextView)findViewById(R.id.UDPBandwidthMeasurementStatusText);

                            textview.setText("Invitation is declined");


                        }
                    });


                    break;


            }


        }
    }



}