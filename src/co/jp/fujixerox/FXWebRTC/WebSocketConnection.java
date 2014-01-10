package co.jp.fujixerox.FXWebRTC;

/**
 * Created by haiyang on 1/7/14.
 */

import android.os.Looper;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;


public class WebSocketConnection {

    private static final String TAG="WebSocketConnection";

    @SuppressWarnings("unused")
    //private Session session;
    private MessageHandler appmessagehandler;

    private FXWebSocketClient socketClient;

    private String serverAddress;



    public WebSocketConnection()
    {

    }

    public interface MessageHandler
    {

        public void onClose(int statusCode, String reason);
        public void onConnect();
        public void onMessage(String msg);
        public void onStartConnect();
        public void onMessageSent(String msg);
        public void onClosing();


    }

    public void setMessageHandler(MessageHandler handler)
    {
        appmessagehandler=handler;
    }

    public MessageHandler getMessageHandler()
    {
        return appmessagehandler;
    }



    public boolean isConnected()
    {
        return socketClient!=null && socketClient.isOpen();
    }

    public synchronized void close()
    {
        if(socketClient!=null)
        {

            if(appmessagehandler!=null)
                appmessagehandler.onClosing();

            socketClient.close();



        }
    }








    public synchronized void connect(String serverAddress) {


        this.serverAddress=serverAddress;

        URI serverURI;
        try{
        serverURI=new URI(serverAddress);
        }catch (URISyntaxException e)
        {
            Log.e(TAG,"Error in URI Syntax: "+serverAddress);
            return;
        }

        socketClient=new FXWebSocketClient(serverURI,new Draft_10());

        Log.d(TAG, "Status: Connecting to " + serverAddress);

        socketClient.connect();


        if(appmessagehandler!=null)
            appmessagehandler.onStartConnect();

    }



    public synchronized boolean sendMessage(String msg) {


        if(socketClient==null)
            return false;

        if(socketClient.isClosed())
            return false;


        try{
        socketClient.send(msg);
        }catch (NotYetConnectedException e)
        {
            Log.e(TAG,"connection to the server is lost!");
            return false;
        }



        if(appmessagehandler!=null)
            appmessagehandler.onMessageSent(msg);

        return true;

    }

    private class FXWebSocketClient extends WebSocketClient
    {

        private URI serverURI;

        public FXWebSocketClient(URI serverURI, Draft draft)
        {

            super(serverURI,draft);
            this.serverURI=serverURI;
        }

        public FXWebSocketClient(URI serverURI)
        {
            super(serverURI);
            this.serverURI=serverURI;
        }


        @Override
        public void onOpen(ServerHandshake serverHandshake) {

            Log.d(TAG, "Status: Connected to " + serverURI);

            if(appmessagehandler!=null)
                appmessagehandler.onConnect();

        }

        @Override
        public void onMessage(String msg) {

            Log.d(TAG,"Got msg: "+ msg);

            if(appmessagehandler!=null)
                appmessagehandler.onMessage(msg);

        }

        @Override
        public void onClose(int code, String reason, boolean remote) {

            Log.d(TAG, "Connection closed: "+code+ "---" + reason);


            socketClient=null;

            if(appmessagehandler!=null)
            {
                appmessagehandler.onClose(code, reason);

            }

        }

        @Override
        public void onError(Exception e) {

            Log.e(TAG,"Error in websocket connection!");
            Log.e(TAG,e.toString());

        }
    }




}
