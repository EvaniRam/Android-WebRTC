package co.jp.fujixerox.FXWebRTC;

import android.util.Log;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Created by haiyang on 1/7/14.
 */
public class WebRTCClient implements WebSocketConnection.MessageHandler{

    public static final String TAG="WebRTCClient ";
    private String name;
    private String serverAddress;
    private int id;
    private Peers.Status status;
    private HashMap<Integer, Peers> peers;


    private WebSocketConnection signalConnection;

    private GUICallback guicallback;

    private CountDownLatch connectionLatch;
    private CountDownLatch msgResponseLatch;
    private CountDownLatch closeLatch;

    private boolean isWaitingResponse;

    private ScheduledExecutorService heartbeatService;
    private Future<?> currentHeartBeatTask;



    public interface GUICallback
    {
        void AddPeer(Peers peer);
        void RemovePeer(Peers peer);
        void UpdatePeer(Peers peer);
        void InvitationDeclined(String reason);
        void InvitationAccepted();
        void InvitationReceived(Peers peer);
        void SDPSent();
        void SDPReceived();
        void StartICEChecking();
        void ICEComplete();
        void ICETerminate();

        void ICEError();

        void OnUDPBWStart();
        void OnUDPBWFailure();
        void OnUDPBWFinish(String message);
    }


    public void setGUICallback(GUICallback callback)
    {
        this.guicallback=callback;
    }


    public WebRTCClient(String name, String serverAddress)
    {

        this.name=name;
        this.serverAddress="ws://"+serverAddress;
        id=-1;


        peers=new HashMap<Integer, Peers>();

        signalConnection=new WebSocketConnection();

        heartbeatService= Executors.newScheduledThreadPool(1);

        //workerService=Executors.newFixedThreadPool(1);



    }


    public String getUsername()
    {
        return this.name;
    }

    public int getUserID()
    {
        return this.id;
    }



    public void setName(String name)
    {
        this.name=name;
    }

    public void setServerAddress(String address)
    {
        serverAddress="ws://"+address;
    }


    public boolean LogIn()
    {

        //test if already logged in;
        if(id>0)
        {

            Log.d(TAG, "Already logged in!");
            return true;
        }


        if(signalConnection.getMessageHandler()==null)
            signalConnection.setMessageHandler(this);

        //check if connected, if not, connect first
        if(!signalConnection.isConnected())
        {
            signalConnection.connect(serverAddress);


            //wait for connection to complete;
            if(await(10, TimeUnit.SECONDS,connectionLatch)==false)
            {
                Log.e(TAG,"Connection timeout, cannot connect to "+serverAddress);
                return false;
            }

        }



        //prepare to login

        JSONObject obj=new JSONObject();
        obj.put("action", "login");
        obj.put("name",name);
        obj.put("platform","android");
        obj.put("udp","true");
        StringWriter out = new StringWriter();

        try
        {

            obj.writeJSONString(out);

        } catch(IOException e)

        {
            e.printStackTrace();
        }
        String jsonText=out.toString();


        isWaitingResponse=true;
        sendMessage(jsonText);

        Log.d(TAG,"Login message sent: "+jsonText);

        //wait for response;
        if(await(10, TimeUnit.SECONDS,msgResponseLatch)==false)
        {
            Log.e(TAG,"Login message sent, but no response from server "+serverAddress);
            return false;
        }



        return true;

    }



    public boolean LogOut()
    {


        if(!signalConnection.isConnected())
            return true;


        //not logged in;
        if(id<0)
            return true;


        if(currentHeartBeatTask!=null)
        {
            if(!currentHeartBeatTask.isCancelled() &&  !currentHeartBeatTask.isDone())
                currentHeartBeatTask.cancel(true);
        }


        JSONObject obj=new JSONObject();
        obj.put("action", "logout");
        obj.put("id",new Integer(id));

        StringWriter out = new StringWriter();

        try
        {

            obj.writeJSONString(out);

        } catch(IOException e)

        {
            e.printStackTrace();
        }


        String jsonText=out.toString();



        isWaitingResponse=true;
        sendMessage(jsonText);

        Log.d(TAG,"Logout message sent: "+jsonText);

        //wait for response;
        if(await(5, TimeUnit.SECONDS,msgResponseLatch)==false)
        {
            Log.e(TAG,"Logout message sent, but no response from server "+serverAddress);
            return false;
        }




        signalConnection.close();
        //wait for response;
        if(await(5, TimeUnit.SECONDS,closeLatch)==false)
        {
            Log.e(TAG,"Close message sent, but no response from server "+serverAddress);
            return false;
        }


        return true;




    }


    @Override
    public void onClose(int statusCode, String reason) {



        Log.d(TAG,"on close is called inside");

        if(closeLatch!=null)
        {
            closeLatch.countDown();
            Log.d(TAG,"close latch is counted down");
        }

    }

    @Override
    public void onConnect() {
        if(connectionLatch!=null)
            connectionLatch.countDown();

    }

    @Override
    public void onMessage(String msg) {

        processMessage(msg);

    }

    @Override
    public void onStartConnect() {

        //set a latch here
        connectionLatch=new CountDownLatch(1);

    }

    @Override
    public void onMessageSent(String msg) {

        if(!isWaitingResponse)
            return;

        msgResponseLatch=new CountDownLatch(1);

    }

    @Override
    public void onClosing() {

        closeLatch=new CountDownLatch(1);

    }


    public boolean await(int duration, TimeUnit unit,CountDownLatch latch) {
        try {


            return latch.await(duration, unit);

        } catch (InterruptedException ex) {
            Logger.getLogger(WebRTCClient.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }


    }

    public void sendMessage(String msg)
    {
        signalConnection.sendMessage(msg);

        Log.d(TAG,"message is sent: "+msg);
    }

    public void processMessage(String msg)
    {
        JSONParser parser=new JSONParser();

        Object obj=null;
        try{
            obj=parser.parse(msg);
        }catch(ParseException e)
        {
            e.printStackTrace();
        }

        if(obj==null)
            return;


        Log.d(TAG,"going to parse message");

        JSONObject json=(JSONObject)obj;
        String action=(String)json.get("action");

        Log.d(TAG,"got action: "+action);
        if(action.equals("login"))
        {

            String sid=(String)json.get("id");
            this.id=Integer.parseInt(sid);
            status=Peers.Status.STATUS_IDLE;

            Log.d(TAG,"successfully logged in");


            if(currentHeartBeatTask!=null)
            {
                if(!currentHeartBeatTask.isCancelled() &&  !currentHeartBeatTask.isDone())
                    currentHeartBeatTask.cancel(true);
            }


            //start the heartbeat sending
            //to run after 5 s
            currentHeartBeatTask=heartbeatService.schedule(new HeartbeatTask(), 5000, TimeUnit.MILLISECONDS);


            if(!isWaitingResponse) return;

            if(msgResponseLatch==null) return;

            msgResponseLatch.countDown();
            isWaitingResponse=false;

            return;

        }

        else if(action.equals("logout"))
        {

            this.id=-1;
            this.status=Peers.Status.STATUS_IDLE;
            this.peers.clear();


            Log.d(TAG,"Successfully logged out");

            if(!isWaitingResponse) return;

            if(msgResponseLatch==null) return;

            msgResponseLatch.countDown();
            isWaitingResponse=false;

            return;




        }
        else if(action.equals("peer_login"))
        {

            addPeer(json);

        }
        else if(action.equals("peer_logout"))
        {
            removePeer(json);
        }
        else if(action.equals("peer_status_update"))
        {
            updatePeerStatus(json);
        }
        else if(action.equals("peer_online"))
        {

            //current treat online peer and peer login as the same
            addPeer(json);
        }
        else if(action.equals("peer_msg"))
        {
            processPeerMsg(json);
        }
        else
        {
            Log.d(TAG,"unknown action: ignored");

        }



    }


    public void processPeerMsg(JSONObject json)
    {

        //first check the message is really to itself
        int destPeerID=Integer.parseInt((String)json.get("to"));
        if(destPeerID!=id)
            return; //message is ignored


        int srcPeerID=Integer.parseInt((String)json.get("from"));
        Peers peer=peers.get(srcPeerID);
        //check msg
        String msg=(String)json.get("msg");
    }


    public void declinePeer(int peerID,String reason)
    {
        JSONObject obj=new JSONObject();
        obj.put("action", "peer_msg");
        obj.put("from",Integer.toString(id));
        obj.put("to", Integer.toString(peerID));
        obj.put("msg","invite_decline");
        obj.put("reason",reason);



        StringWriter out = new StringWriter();

        try
        {

            obj.writeJSONString(out);

        } catch(IOException e)

        {
            e.printStackTrace();
        }


        String jsonText=out.toString();
        sendMessage(jsonText);

    }

    public boolean addPeer(JSONObject json)
    {


        int peer_id=Integer.parseInt((String)json.get("id"));




        //see if peer already exists
        if(peers.containsKey(peer_id))
        {
            Log.e(TAG,"peer with id "+peer_id+ " already exists!");
            return false;
        }

        //parse various information about this peer
        Peers peer=new Peers();
        peer.setPeerId(peer_id);

        //get name
        String name=(String)json.get("name");
        peer.setName(name);

        //get status
        String peer_status=(String)json.get("status");
        if(peer_status.equalsIgnoreCase("idle"))
            peer.setStatus(Peers.Status.STATUS_IDLE);
        else
            peer.setStatus(Peers.Status.STATUS_BUSY);

        //get platorm
        String platform=(String)json.get("platform");
        peer.setPlatform(platform);

        //get udp enabled or not
        String udp=(String)json.get("udp");
        if(udp.equalsIgnoreCase("true"))
            peer.setUdp(true);
        else
            peer.setUdp(false);

        peers.put(peer_id, peer);

        if(guicallback!=null)
        {
            guicallback.AddPeer(peer);
        }

        return true;
    }

    public boolean removePeer(JSONObject json)
    {
        int peer_id=Integer.parseInt((String)json.get("id"));

        if(!peers.containsKey(peer_id))
        {
            Log.e(TAG,"peer with id "+peer_id +" is not existent, cannot be deleted!");
            return false;
        }


        if(guicallback!=null)
        {
            guicallback.RemovePeer(peers.get(peer_id));
        }

        peers.remove(peer_id);
        return true;



    }

    public boolean updatePeerStatus(JSONObject json)
    {
        int peer_id=Integer.parseInt((String)json.get("id"));

        if(!peers.containsKey(peer_id))
        {
            Log.e(TAG,"peer with id "+peer_id +" is not existent, cannot be updated!");
            return false;
        }

        Peers peer=peers.get(peer_id);

        String new_status=(String)json.get("status");
        if(new_status.equalsIgnoreCase("idle"))
            peer.setStatus(Peers.Status.STATUS_IDLE);
        else
            peer.setStatus(Peers.Status.STATUS_BUSY);

        if(guicallback!=null)
        {
            guicallback.UpdatePeer(peer);
        }

        return true;


    }


    private class HeartbeatTask implements Runnable{

        @Override
        public void run() {
            JSONObject obj=new JSONObject();
            obj.put("action", "heartbeat");
            obj.put("id",Integer.toString(id));


            StringWriter out = new StringWriter();

            try
            {

                obj.writeJSONString(out);

            } catch(IOException e)

            {
                e.printStackTrace();
            }


            String jsonText=out.toString();
            sendMessage(jsonText);

            //schedule running after 10 seconds
            currentHeartBeatTask=heartbeatService.schedule(new HeartbeatTask(), 10000, TimeUnit.MILLISECONDS);

        }

    }

}

