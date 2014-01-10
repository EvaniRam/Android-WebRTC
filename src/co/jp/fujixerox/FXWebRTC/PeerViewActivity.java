package co.jp.fujixerox.FXWebRTC;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.*;
import android.content.DialogInterface;
import android.widget.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: haiyang
 * Date: 10/28/13
 * Time: 9:06 AM
 * To change this template use File | Settings | File Templates.
 */
public class PeerViewActivity extends Activity implements View.OnClickListener, PopupMenu.OnMenuItemClickListener{

    public static final String TAG="PeerViewActivity";

    private BroadcastReceiver mclientstatereceiver;

    private ProgressDialog progressDialog;

    private WebRTCClient webRTCClient;

    private boolean darkblue=true;

    private Peers currentContactPeer;

    private HashMap<Integer,View> currentShownPeers=new HashMap<Integer, View>();






    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.peerview);

       // Button button=(Button)findViewById(R.id.button_peerview_logoff);
       // button.setOnClickListener(this);

        ApplicationEx app=(ApplicationEx)getApplicationContext();
        webRTCClient=app.getWebRTCClient();

        if(webRTCClient==null)
        {
            Log.e(TAG,"cannot get webrtc client instance!");
        }


        int id=webRTCClient.getUserID();
        String username=webRTCClient.getUsername();

        TextView textViewuseridusername=(TextView) findViewById(R.id.TextViewUserIDUserName);

        textViewuseridusername.setText("ID:  "+id+"\nUsername: "+username);



        //create a local broadcast receiver
        mclientstatereceiver=new ClientStateReceiver(this);
        // The filter's action is BROADCAST_ACTION
        IntentFilter clientstatusIntentFilter = new IntentFilter(
                Constants.BROADCAST_ACTION);
        clientstatusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mclientstatereceiver, clientstatusIntentFilter);

        addPeerStartup();

       // Log.d(TAG,"local broadcast receiver is registered");
    }


    public void addPeerStartup()
    {
        if(webRTCClient.getPeers().isEmpty()) return;

        Log.d(TAG,"add peer at startup!");

        HashMap<Integer,Peers> peersHashMap=webRTCClient.getPeers();

        for(Map.Entry<Integer,Peers> entry: peersHashMap.entrySet())
        {
            addPeer(entry.getKey());
        }

    }


    public void onNetworkConnectionClosed()
    {



          AlertDialog.Builder builder=new AlertDialog.Builder(this);
          builder.setCancelable(false);
          builder.setTitle("Error");
          builder.setIcon(android.R.drawable.ic_dialog_alert);
          builder.setMessage("Connection to server has been lost!");
          builder.setInverseBackgroundForced(true);


          AlertDialog dialog;

        builder.setPositiveButton("OK",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
                showLogoffProgressDialog();
                new LogoutTask().execute();
            }
        });







        dialog=builder.create();
        dialog.show();




    }

    public void addPeer(int peer_id)
    {



        if(peer_id<0) return;
        Peers peer=webRTCClient.getPeers().get(peer_id);

        if(peer==null)
        {
            Log.e(TAG,"cannot find peer with id "+peer_id);
            return;
        }


        if(currentShownPeers.get(peer_id)!=null)
        {
            Log.e(TAG,"cannot add peer with id "+peer_id+" : already exits");
            return;
        }

        Log.d(TAG,"going to add peer "+peer.getName());


        TableLayout tableLayout=(TableLayout)findViewById(R.id.TableLayoutOnlinePeers);

        //add a TableRow view
        LayoutInflater layoutInflater= (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

       // TableRow tr=new TableRow(this);
        View view=layoutInflater.inflate(R.layout.peertablerowview,tableLayout,false);

       //set background color
        if(darkblue)
        {
            view.setBackgroundResource(R.color.darker_blue);
        }
        else
        {
            view.setBackgroundResource(R.color.light_blue);
        }

        darkblue=!darkblue;


        TextView userid=(TextView)(view.findViewById(R.id.TableRowUserID));

        userid.setText(Integer.toString(peer.getPeerId()));

        TextView username=(TextView)view.findViewById(R.id.TableRowUserName);
        username.setText(peer.getName());

        ImageView platform=(ImageView)view.findViewById(R.id.TableRowPlatformPic);
        if(peer.getPlatform().equalsIgnoreCase("web"))
            platform.setImageResource(R.drawable.browser_icon);
        else if(peer.getPlatform().equalsIgnoreCase("android"))
            platform.setImageResource(R.drawable.android_icon);


        ImageView status=(ImageView)view.findViewById(R.id.TableRowStatusPic);
        if(peer.getStatus()== Peers.Status.STATUS_BUSY)
            status.setImageResource(R.drawable.msn_busy_onphone);
        else if(peer.getStatus()== Peers.Status.STATUS_IDLE)
            status.setImageResource(R.drawable.msn_online);

        ImageView udp=(ImageView)view.findViewById(R.id.TableRowUDPPic);
        if(peer.isUdp())
            udp.setImageResource(R.drawable.corrent_icon);
        else
            udp.setImageResource(R.drawable.wrong_icon);

        view.setTag(peer_id);


        view.setClickable(true);
        view.setOnClickListener(this);


        tableLayout.addView(view);

        Log.d(TAG, "view class is of type: " + view.getClass().getName());

        currentShownPeers.put(peer_id, view);



    }

    public void removePeer(int peer_id)
    {
       if(peer_id<0) return;

       if(currentShownPeers.get(peer_id)==null)
       {
           Log.e(TAG,"cannot remove peer "+peer_id);
           return;
       }


       //remove the TableRow view
       Log.d(TAG,"peer removed from thread "+android.os.Process.myTid());

       Log.d(TAG,"going to remove peer "+peer_id);

       TableLayout tableLayout=(TableLayout)findViewById(R.id.TableLayoutOnlinePeers);


       View view=currentShownPeers.get(peer_id);
       view.setOnClickListener(null);
       tableLayout.removeView(view);

       currentShownPeers.remove(peer_id);
    }


    public void removeAllPeers()
    {
        if(currentShownPeers.isEmpty())
            return;



        TableLayout tableLayout=(TableLayout)findViewById(R.id.TableLayoutOnlinePeers);
        for(Map.Entry<Integer,View> entry:currentShownPeers.entrySet())
        {
            View view=entry.getValue();
            view.setOnClickListener(null);
            tableLayout.removeView(view);
        }

        currentShownPeers.clear();
    }


    public void updatePeer(int peer_id)
    {
        if(peer_id<0) return;

        if(currentShownPeers.get(peer_id)==null)
        {
            Log.e(TAG,"cannot update peer "+peer_id+" not found");
            return;
        }

        Log.d(TAG,"going to update status for peer "+peer_id);

        View view=currentShownPeers.get(peer_id);

        Peers peer=webRTCClient.getPeers().get(peer_id);

        ImageView status=(ImageView)view.findViewById(R.id.TableRowStatusPic);
        if(peer.getStatus()== Peers.Status.STATUS_BUSY)
            status.setImageResource(R.drawable.msn_busy_onphone);
        else if(peer.getStatus()== Peers.Status.STATUS_IDLE)
            status.setImageResource(R.drawable.msn_online);

    }

    @Override
    public void onClick(View v) {



        switch(v.getId())
        {
            case R.id.TableRowPeer:
                doActiononPeer(v);
                break;
            default:
                break;
        }

    }

    public void doActiononPeer(final View v)
    {
         Log.d(TAG,"view item is pressed");

         currentContactPeer=webRTCClient.getPeers().get(v.getTag());


         PopupMenu popupMenu=new PopupMenu(this,v);
         popupMenu.inflate(R.menu.peer_action_menu);
         popupMenu.setOnMenuItemClickListener(this);


         popupMenu.show();

    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch(keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
                showLogoffDialog();
                return true;
            default:
                break;

        }



        return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.peer_view_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.peer_view_menu_logout:
                showLogoffDialog();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }




    public void switchtoMainActivity(boolean logoutSuccess)
    {
       // Intent intent=new Intent(this,FXWebRTCMainActivity.class);
      //  startActivity(intent);


        removeAllPeers();
        currentContactPeer=null;
        finish();
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
        progressDialog.setMessage("Logging off...");
        progressDialog.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {


        switch (item.getItemId())
        {
            case R.id.PeerActionMenuCall:

                if(currentContactPeer.getStatus()== Peers.Status.STATUS_BUSY)
                {
                    AlertDialog.Builder builder=new AlertDialog.Builder(this);
                    builder.setCancelable(false);
                    builder.setTitle("WARNING");
                    builder.setIcon(android.R.drawable.ic_dialog_alert);

                    builder.setMessage("Client "+currentContactPeer.getName()+" is busy, try again later");
                    builder.setInverseBackgroundForced(true);




                    builder.setPositiveButton("OK",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });







                    AlertDialog dialog=builder.create();
                    dialog.show();
                }
                break;
            case R.id.PeerActionMenuMSG:
                break;
            case R.id.PeerActionMenuUDP:

                if(currentContactPeer.getStatus()== Peers.Status.STATUS_BUSY)
                {
                    AlertDialog.Builder builder=new AlertDialog.Builder(this);
                    builder.setCancelable(false);
                    builder.setTitle("WARNING");
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage("Client "+currentContactPeer.getName()+" is busy, try again later");
                    builder.setInverseBackgroundForced(true);




                    builder.setPositiveButton("OK",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });







                    AlertDialog dialog=builder.create();
                    dialog.show();

                }

                else if(currentContactPeer.getPlatform().equalsIgnoreCase("wweb"))
                {
                    AlertDialog.Builder builder=new AlertDialog.Builder(this);
                    builder.setCancelable(false);
                    builder.setTitle("WARNING");
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setMessage("Client "+currentContactPeer.getName()+" is logged on from a browser, cannot do UDP measurement!");
                    builder.setInverseBackgroundForced(true);




                    builder.setPositiveButton("YES",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });







                    AlertDialog dialog=builder.create();
                    dialog.show();

                }
                else
                {

                Intent intent=new Intent(this,UDPTestActivity.class);
                intent.putExtra("peer_id",currentContactPeer.getPeerId());
                intent.putExtra("initiator",true);
                startActivity(intent);
                }

                break;
            default:
                break;

        }

        return false;
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

                    new LogoutTask().execute(webRTCClient);


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


    public void onUserResponseforInvitation(int peerID, InvitationResponse response)
    {
           if(response.equals(InvitationResponse.INVITATION_RESPONSE_DECLINE))
           {
               new InvitationDeclineTask().execute(webRTCClient);
           }
           else
           {
               Intent intent=new Intent(this,UDPTestActivity.class);
               intent.putExtra("peer_id",currentContactPeer.getPeerId());
               intent.putExtra("initiator",false);
               startActivity(intent);
           }
    }


    public void onInvitationReceived(InvitationType type,int peerId)
    {


        currentContactPeer=webRTCClient.getPeers().get(peerId);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("INVITATION");
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setMessage("Client " + currentContactPeer.getName() + " sends you an invitation for " +
                (type.equals(InvitationType.INVITATION_UDP) ? "UDP Bandwidth Measurement" : "Video Call"));
        builder.setInverseBackgroundForced(true);


        final int local_peerID=peerId;

        builder.setPositiveButton("ACCEPT",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();

                onUserResponseforInvitation(local_peerID,InvitationResponse.INVITATION_RESPONSE_ACCEPT);

            }
        });


        builder.setNegativeButton("DECLINE",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();

                onUserResponseforInvitation(local_peerID,InvitationResponse.INVITATION_RESPONSE_DECLINE);

            }
        });


        AlertDialog dialog=builder.create();
        dialog.show();

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



                case Constants.PEER_JOIN:

                    peer_id=intent.getIntExtra(Constants.EXTENDED_DATA_ID,-1);


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                         Log.d(TAG,"add peer through broadcast receiver!");

                         addPeer(peer_id);
                        }
                    });

                    break;
                case Constants.PEER_LEAVE:

                    peer_id=intent.getIntExtra(Constants.EXTENDED_DATA_ID,-1);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            removePeer(peer_id);
                        }
                    });


                    break;

                case Constants.PEER_UPDATE:

                    peer_id=intent.getIntExtra(Constants.EXTENDED_DATA_ID,-1);


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            updatePeer(peer_id);
                        }
                    });


                    break;

                case Constants.CONNECTION_CLOSED:

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            onNetworkConnectionClosed();
                        }
                    });
                    break;


                case Constants.VIDEO_INVITATION_RECEIVED:

                    peer_id=intent.getIntExtra(Constants.EXTENDED_DATA_ID,-1);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            onInvitationReceived(InvitationType.INVITATION_VIDEO, peer_id);
                        }
                    });
                   break;

                case Constants.UDP_INVITATION_RECEIVED:

                    peer_id=intent.getIntExtra(Constants.EXTENDED_DATA_ID,-1);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            onInvitationReceived(InvitationType.INVITATION_UDP, peer_id);
                        }
                    });
                    break;




                default:
                    break;


            }


        }
    }


    private class LogoutTask extends AsyncTask<WebRTCClient,Integer,Boolean>
    {

        private WebRTCClient client;

        @Override
        protected Boolean doInBackground(WebRTCClient... params) {



            client=params[0];

            return client.LogOut();

        }



        protected void onPostExecute(Boolean result)
        {

            progressDialog.dismiss();
            switchtoMainActivity(result);
        }
    }

    private class InvitationDeclineTask extends AsyncTask<WebRTCClient,Integer,Boolean>
    {

        private WebRTCClient client;

        @Override
        protected Boolean doInBackground(WebRTCClient... params) {



            client=params[0];

            return client.declinePeer(currentContactPeer.getPeerId(),"USER");

        }



        protected void onPostExecute(Boolean result)
        {

            currentContactPeer=null;
        }
    }


    private enum InvitationType{
        INVITATION_VIDEO,
        INVITATION_UDP
    }

    private enum InvitationResponse{
        INVITATION_RESPONSE_ACCEPT,
        INVITATION_RESPONSE_DECLINE
    }

}

