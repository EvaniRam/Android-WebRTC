package co.jp.fujixerox.FXWebRTC;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class FXWebRTCMainActivity extends Activity implements View.OnClickListener {
    /**
     * Called when the activity is first created.
     */
    public static final String TAG="FXWebRTCMainActivity";
    private Intent mSignalingservice;
    private BroadcastReceiver mclientstatereceiver;

    private ProgressDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);




        //settings of action bar
        ActionBar actionBar=getActionBar();
        actionBar.setTitle(R.string.app_name);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.show();

        Button button=(Button)findViewById(R.id.button_connect);
        button.setOnClickListener(this);




        dialog=new ProgressDialog(this);

       //create a local broadcast receiver
        mclientstatereceiver=new ClientStateReceiver(this);

        // The filter's action is BROADCAST_ACTION
        IntentFilter clientstatusIntentFilter = new IntentFilter(
                Constants.BROADCAST_ACTION);

        clientstatusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);




        LocalBroadcastManager.getInstance(this).registerReceiver(mclientstatereceiver, clientstatusIntentFilter);


    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
          switch(keyCode)
          {
              case KeyEvent.KEYCODE_BACK:
                  AlertDialog.Builder builder=new AlertDialog.Builder(this);
                  builder.setCancelable(false);
                  builder.setTitle("Do you want to quit the program?");
                  builder.setInverseBackgroundForced(true);


                  QuitDialogListener listener=new QuitDialogListener();

                  builder.setPositiveButton("YES", listener);



                  builder.setNegativeButton("NO", listener);

                  builder.setNeutralButton("CANCEL",listener);



                  AlertDialog dialog=builder.create();
                  dialog.show();




                  break;
              default:
                  break;

          }



        return false;
    }


    @Override
    public void onDestroy() {


        if (mclientstatereceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mclientstatereceiver);
            mclientstatereceiver= null;
        }


        // Must always call the super method at the end.
        super.onDestroy();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.settings_menu:
                Intent intent=new Intent(this,SettingActivity.class);
                startActivity(intent);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }



    public void onClientSignedin()
    {
        dialog.dismiss();
        Intent intent=new Intent(this,PeerViewActivity.class);
        startActivity(intent);

    }

    public void onServerFailure()
    {

         dialog.dismiss();
        // SystemClock.sleep(1000);
         String msg="Cannot connect to server, please try again later";

         Toast.makeText(FXWebRTCMainActivity.this,msg,Toast.LENGTH_LONG).show();



         Log.d(TAG,"toast is shown!");

         stopService(mSignalingservice);

    }









    //button press callback
    @Override
    public void onClick(View v) {


          switch(v.getId())
          {
              case R.id.button_connect:


                 mSignalingservice=new Intent(this,SignalingService.class);
                 startService(mSignalingservice);

                 dialog.setMessage("Connecting to Server, Please wait ...");
                 dialog.show();

                  break;


              default:
                  break;
          }



    }



    private class QuitDialogListener implements DialogInterface.OnClickListener
    {

        @Override
        public void onClick(DialogInterface dialog, int which) {


            switch(which)
            {
                case DialogInterface.BUTTON_POSITIVE:

                    dialog.dismiss();
                    FXWebRTCMainActivity.this.finish();


                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    dialog.dismiss();
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    dialog.dismiss();
                    break;


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


            switch(state)
            {
                case Constants.CLIENT_SIGNED_IN:
                     Log.d(TAG,"client is connected ");

                     onClientSignedin();
                     break;
                case Constants.CLIENT_DISCONNECTED:
                     Log.d(TAG,"client is disconnected");
                     break;
                case Constants.PEER_CONNECTED:
                     Log.d(TAG,"peer is connected");
                     break;
                case Constants.PEER_DISCONNECTED:
                     Log.d(TAG,"peer is disconnected");
                     break;
                case Constants.MESSAGE_FROM_PEER:
                     Log.d(TAG,"received message from peer");
                     break;
                case Constants.MESSAGE_SENT:
                     Log.d(TAG,"message sent");
                     break;
                case Constants.SERVER_CONNECTION_FAILURE:
                     Log.d(TAG,"server connection failed");

                     onServerFailure();
                     break;
                default:
                    break;
            }


        }
    }


    public enum CallbackID {
        MEDIA_CHANNELS_INITIALIZED,
        PEER_CONNECTION_CLOSED,
        SEND_MESSAGE_TO_PEER,
        PEER_CONNECTION_ERROR,
        NEW_STREAM_ADDED,
        STREAM_REMOVED
    };
}
