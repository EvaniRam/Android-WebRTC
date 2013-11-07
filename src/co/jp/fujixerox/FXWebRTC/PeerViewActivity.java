package co.jp.fujixerox.FXWebRTC;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.content.DialogInterface;
import android.widget.Button;

/**
 * Created with IntelliJ IDEA.
 * User: haiyang
 * Date: 10/28/13
 * Time: 9:06 AM
 * To change this template use File | Settings | File Templates.
 */
public class PeerViewActivity extends Activity implements View.OnClickListener{

    public static final String TAG="PeerViewActivity";

    private BroadcastReceiver mclientstatereceiver;

    private ProgressDialog progressDialog;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.peerview);

        Button button=(Button)findViewById(R.id.button_peerview_logoff);
        button.setOnClickListener(this);

        //create a local broadcast receiver
        mclientstatereceiver=new ClientStateReceiver(this);
        // The filter's action is BROADCAST_ACTION
        IntentFilter clientstatusIntentFilter = new IntentFilter(
                Constants.BROADCAST_ACTION);
        clientstatusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mclientstatereceiver, clientstatusIntentFilter);

    }

    @Override
    public void onClick(View v) {



        switch(v.getId())
        {
            case R.id.button_peerview_logoff:


                showLogoffDialog();

                break;
            default:
                break;
        }

    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
                showLogoffDialog();
                break;
            default:
                break;

        }



        return false;
    }





    public void showLogoffDialog()
    {
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Do you want to log off?");
        builder.setInverseBackgroundForced(true);


        LogoffDialogListener listener=new LogoffDialogListener();

        builder.setPositiveButton("YES", listener);



        builder.setNegativeButton("NO", listener);

        builder.setNeutralButton("CANCEL",listener);



        AlertDialog dialog=builder.create();
        dialog.show();
    }


    public void showLogoffProgressDialog()
    {
        progressDialog=new ProgressDialog(this);
        progressDialog.setMessage("Loging off...");
        progressDialog.show();
    }


    private class LogoffDialogListener implements DialogInterface.OnClickListener
    {

        @Override
        public void onClick(DialogInterface dialog, int which) {

            switch(which)
            {
                case DialogInterface.BUTTON_POSITIVE:

                    dialog.dismiss();
                    showLogoffProgressDialog();


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
                    Log.d(TAG, "client is connected ");

                    //peer view  will not deal with this message;
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


                    break;
                default:
                    break;
            }


        }
    }

}

