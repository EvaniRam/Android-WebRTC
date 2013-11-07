package co.jp.fujixerox.FXWebRTC;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler.Callback;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;


/**
 * Created with IntelliJ IDEA.
 * User: haiyang
 * Date: 10/24/13
 * Time: 9:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class PeerConnectionClient {

    public static final String TAG="PeerConnectionClient";

    // This is our magical hangup signal.
    public static final String kByeMessage = "BYE";
// Delay between server connection retries, in milliseconds
    public static final int kReconnectDelay = 2000;

    public static final int DEFAULT_PORT=10812;

    public static final int DEFAULT_BUFFER_CAPACITY=4096;

    private String server_name;

    private InetAddress server_address;

    private InetSocketAddress server_socket_address;

    private int server_port;

    private FXSocket control_socket;



    private PeerConnectionClientObserver observer;

    private FXSocket hanging_get;


    private StringBuilder onconnect_data;

    private StringBuilder control_data;
    private StringBuilder notification_data;

    private Boolean control_data_to_send;
    private Boolean control_data_to_receive;

    private String client_name;

    private State state;

    private int my_id;

    private HashMap<Integer,String> peers;

    private PeerConnectionClient mhandler;

    private LinkedList<Message> messageQueue;

    public void clearStringBuilder(StringBuilder stringBuilder)
    {
           if(stringBuilder.length()!=0) {
               stringBuilder.delete(0, stringBuilder.length() - 1);
           }
    }

    public PeerConnectionClient() {

        onconnect_data=new StringBuilder(DEFAULT_BUFFER_CAPACITY);
        control_data=new StringBuilder(DEFAULT_BUFFER_CAPACITY);
        notification_data=new StringBuilder(DEFAULT_BUFFER_CAPACITY);
        peers=new HashMap<Integer, String>();
        state=State.NOT_CONNECTED;
        my_id=-1;

        messageQueue=new LinkedList<Message>();
        mhandler=this;


    }



    public void registerObserver(PeerConnectionClientObserver observer)
    {
         assert observer!=null;
          this.observer=observer;
    }



    public PeerConnectionClientObserver getObserver()
    {
        return observer;
    }




    public int getId()
    {
        return my_id;
    }

    public HashMap<Integer,String> getPeers()
    {
        return peers;
    }

    public Boolean isConnected(){
        return my_id!=-1;




    }


    public void setParam(String server, int port, String client_name)
    {
        if(state!=State.NOT_CONNECTED)
        {
            Log.w(TAG,"The client must not be connected before you call connect()");
            return;
        }

        if(server.isEmpty() || client_name.isEmpty())
        {
            Log.w(TAG,"the server or client name is empty!");
            return;
        }

        this.server_name=server;

        this.client_name=client_name;

        if(port<=0)
            port=DEFAULT_PORT;
        server_port=port;
    }

    public void initialize()
    {
        assert observer!=null;


        try {
        server_address=InetAddress.getByName(server_name);
        } catch (UnknownHostException e)
        {
            Log.e(TAG,"Unknown host!");
            e.printStackTrace();
        }

        server_socket_address=new InetSocketAddress(server_address,server_port);


        control_socket=new FXSocket();
        hanging_get=new FXSocket();

        control_socket.setType(FXSocket.SocketType.CONTROL_SOCKET);
        hanging_get.setType(FXSocket.SocketType.HANGING_GET_SOCKET);



    }


    public void connect()
    {
        assert server_name.isEmpty()==false;
        assert client_name.isEmpty()==false;
        assert server_socket_address!=null;

        if(state!=State.NOT_CONNECTED)
        {
            Log.w(TAG,"The client must not be connected before you can call Connect()");
            observer.OnServerConnectionFailure();
            return;
        }

        if(server_name.isEmpty() || client_name.isEmpty() || server_socket_address==null)
        {
            observer.OnServerConnectionFailure();
            return;
        }

        doConnect();
    }

    public void doConnect()
    {
            clearStringBuilder(onconnect_data);

            String str="GET /sign_in?"+client_name+" HTTP/1.0\r\n\r\n";

            onconnect_data.append(str);

            Boolean ret=connectControl();


            Log.d(TAG,"do connect is returned!");
            if(ret)
            {
                state=State.SIGNING_IN;
            }
            else {
                observer.OnServerConnectionFailure();
            }

            Log.d(TAG,"in do connect client state is "+state);
    }

    public Boolean sendtoPeer(int peer_id, String message)
    {
           if(state!=State.CONNECTED)
               return false;

           assert isConnected()==true;

           assert control_socket.isClosed()==true;

           if(!isConnected() || peer_id==-1)
               return false;

           clearStringBuilder(onconnect_data);


           String str="POST /message?peer_id="+my_id+"&to="+peer_id+" HTTP/1.0\r\n"+
                "Content-Length: "+message.length()+"\r\n"+
                "Content-Type: text/plain\r\n"+
                "\r\n";

           onconnect_data.append(str);
           onconnect_data.append(message);

           return connectControl();

    }

    public Boolean sendHangup(int peer_id)
    {
          return sendtoPeer(peer_id,kByeMessage);
    }

    public Boolean isSendingMessage()
    {
        if ((state == State.CONNECTED)
                && control_socket.isConnected()) return true;
        else return false;
    }

    public Boolean signOut()
    {
        if(state== State.NOT_CONNECTED || state==State.SIGNING_OUT)
            return true;

        if(!hanging_get.isClosed())
            closeHangingGet();

        if(control_socket.isClosed())
        {
            state=State.SIGNING_OUT;

            if(my_id!=-1)
            {
                 clearStringBuilder(onconnect_data);
                 String str="GET /sign_out?peer_id="+my_id+" HTTP/1.0\r\n\r\n";

                 onconnect_data.append(str);

                 return connectControl();
            }
            else
            {
                return true;
            }

        }
        else
        {
            state=State.SIGNING_OUT_WAITING;
        }

        return true;
    }

    public void close()
    {
        closeControl();
        closeHangingGet();

        clearStringBuilder(onconnect_data);



        peers.clear();

        my_id=-1;
        state=State.NOT_CONNECTED;
    }


    public  void onConnect()
    {

          //   Log.d(TAG,"onConnected is called!");


             assert onconnect_data.length()!=0;

              sendByConnectSocket(onconnect_data.toString());

              clearStringBuilder(onconnect_data);


            //  Log.d(TAG,"client state is: "+state);

            //  Log.d(TAG,"going to leave onConnect!");

    }

    public void onHangingGetConnect()
    {
            String str="GET /wait?peer_id="+my_id+ " HTTP/1.0\r\n\r\n";


            sendByHangingGetSocket(str);
    }

    public void onMessageFromPeer(int peer_id, String msg)
    {
            if(msg.equals(kByeMessage))
            {
                observer.OnPeerDisconnected(peer_id);
            }
            else
            {
                observer.OnMessageFromPeer(peer_id, msg);
            }

    }



    // Quick and dirty support for parsing HTTP header values.
    public Boolean getHeaderValue(String data, int eoh,
                                  String header_pattern, int[] value)
    {

      //  Log.d(TAG,"data is "+data);
     //   Log.d(TAG,"header pattern is "+header_pattern);

        int pos=data.indexOf(header_pattern);

        if(pos!=-1 && pos< eoh)
        {

            int end=data.indexOf("\r\n",pos+header_pattern.length());
            if(end!=-1)
            {
            value[0]=Integer.parseInt(data.substring(pos+header_pattern.length(),end));
           // Log.d(TAG,"value returned is "+value[0]);
            return true;
            }
        }

        return false;
    }

    public Boolean getHeaderValue(String data, int eoh,
                                  String header_pattern, StringBuilder value)
    {
        int pos=data.indexOf(header_pattern);

        if(pos!=-1 && pos< eoh)
        {
            int begin=pos+header_pattern.length();
            int end=data.indexOf("\r\n",begin);

            if(end==-1)
                end=eoh;

            value.insert(0,data.substring(begin,end));
            return true;

        }

        return false;
    }


    // Returns true if the whole response has been read.
    public Boolean readIntoBuffer(FXSocket socket, StringBuilder data,
                              int[] content_length)
    {
        char[] buffer=new char[DEFAULT_BUFFER_CAPACITY];






        BufferedReader reader;
        try
        {
        reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e)
        {
            Log.e(TAG,"Error getting the input stream from socket!");



            socket.setError(FXSocket.SocketError.ERROR_CONNECTION);

            if(socket.getType()== FXSocket.SocketType.CONTROL_SOCKET)
             mhandler.sendEmptyMessage(Constants.CONTROL_SOCKET_CLOSED);
            else
             mhandler.sendEmptyMessage(Constants.HANGING_GET_SOCKET_CLOSED);



            e.printStackTrace();
            return false;
        }

        int readBytes;

        clearStringBuilder(data);

        try {


        do
        {
            readBytes=reader.read(buffer,0,DEFAULT_BUFFER_CAPACITY);

            if(readBytes<=0)
                break;

            data.append(buffer,0,readBytes);

        }while(reader.ready()) ;
        } catch (IOException e)
        {
            Log.e(TAG,"Error trying to read data from socket!");



            socket.setError(FXSocket.SocketError.ERROR_CONNECTION);

               if(socket.getType()== FXSocket.SocketType.CONTROL_SOCKET)
               mhandler.sendEmptyMessage(Constants.CONTROL_SOCKET_CLOSED);
               else
               mhandler.sendEmptyMessage(Constants.HANGING_GET_SOCKET_CLOSED);


            e.printStackTrace();
            return false;
        }


        Boolean ret=false;

        int index=data.indexOf("\r\n\r\n");


        Log.d(TAG,"In readintobuffer, state is "+state);


        if (index != -1) {
            Log.i(TAG,"Headers received");

            String pattern="\r\nContent-Length: ";
            if (getHeaderValue(data.toString(), index,pattern, content_length)) {
                int total_response_size = (index + 4) + content_length[0];
                if (data.length() >= total_response_size) {
                    ret = true;
                    StringBuilder should_close=new StringBuilder(DEFAULT_BUFFER_CAPACITY);
                    String kConnection= "\r\nConnection: ";

                   // Log.d(TAG,"data is \n"+data);
                    if (getHeaderValue(data.toString(), index, kConnection, should_close) &&
                            should_close.toString().equals("close")) {

                       // Log.d(TAG,"should close string is "+should_close);

                        if(socket.getType()== FXSocket.SocketType.CONTROL_SOCKET)
                        closeControl();
                        else
                        closeHangingGet();
                        // Since we closed the socket, there was no notification delivered
                        // to us.  Compensate by letting ourselves know.
                        onClose(socket);
                    }
                } else {
                    // We haven't received everything.  Just continue to accept data.
                }
            } else {
                Log.e(TAG, "No content length field specified by the server.");
            }
        }


      //  Log.d(TAG,"Before coming out of readintobuffer, state is "+state);

        return ret;
    }



    public  void onRead()
    {


      //  Log.d(TAG,"onRead is called!");
      //  Log.d(TAG,"client state is "+state);


        int[] content_length=new int[1];
        content_length[0]=0;

        if (readIntoBuffer(control_socket, control_data, content_length)) {

          //  Log.d(TAG,"after readintobuffer, client state is "+state);

            int[] peer_id=new int[1];
            int[] eoh=new int[1];
            peer_id[0]=eoh[0]=0;
            Boolean ok = parseServerResponse(control_data.toString(), content_length, peer_id,
                    eoh);
            if (ok) {
                if (my_id == -1) {
                    // First response.  Let's store our server assigned ID.

                     try {
                          if(state != State.SIGNING_IN)

                              throw new Exception("client in wrong state! "+state);
                     }catch (Exception e)
                     {
                            e.printStackTrace();
                     }




                    my_id=peer_id[0];

                    assert my_id != -1;

                    // The body of the response will be a list of already connected peers.
                    if (content_length[0]>0) {
                        int pos = eoh[0] + 4;
                        while (pos < control_data.length()) {
                            int eol = control_data.toString().indexOf('\n', pos);
                            if (eol == -1)
                                break;
                            int id[] = new int[1];
                            id[0]=0;
                            StringBuilder name=new StringBuilder(DEFAULT_BUFFER_CAPACITY);
                            Boolean[] connected=new Boolean[1];
                            connected[0]=false;
                            if (parseEntry(control_data.substring(pos, eol), name, id,
                                    connected) && id[0] != my_id) {
                                peers.put(id[0],name.toString());
                                observer.OnPeerConnected(id[0], name.toString());
                            }
                            pos = eol + 1;
                        }
                    }
                    assert isConnected()==true;
                    observer.OnSignedIn();

                  //  Log.d(TAG,"onsignedin is called!");

                } else if (state == State.SIGNING_OUT) {
                    close();
                    observer.OnDisconnected();
                } else if (state == State.SIGNING_OUT_WAITING) {
                    signOut();
                }
            }

            clearStringBuilder(control_data);

          //  Log.d(TAG,"going here!");
          //  Log.d(TAG,"state is "+state);

            if (state == State.SIGNING_IN) {
                assert !hanging_get.isConnected();
                state = State.CONNECTED;



                connectHangingGet();
            }
        }
    }


    public  void onHangingGetRead()
    {

        int[] content_length = new int[1];
        content_length[0]=0;

        if (readIntoBuffer(hanging_get, notification_data, content_length)) {
            int[] peer_id = new int[1];
            peer_id[0]=0;
            int[] eoh = new int[1];
            eoh[0]=0;
            Boolean ok = parseServerResponse(notification_data.toString(), content_length,
                    peer_id, eoh);

            if (ok) {
                // Store the position where the body begins.
                int pos = eoh[0] + 4;

                if (my_id== peer_id[0]) {
                    // A notification about a new member or a member that just
                    // disconnected.
                    int[] id = new int[1];
                    id[0]=0;
                    StringBuilder name=new StringBuilder(DEFAULT_BUFFER_CAPACITY);
                    Boolean[] connected = new Boolean[1];
                    connected[0]=false;
                    if (parseEntry(notification_data.substring(pos), name, id,
                    connected)) {
                        if (connected[0]) {
                            peers.put(id[0],name.toString());
                            observer.OnPeerConnected(id[0], name.toString());
                        } else {
                            peers.remove(id[0]);
                            observer.OnPeerDisconnected(id[0]);
                        }
                    }
                } else {
                    onMessageFromPeer(peer_id[0],
                            notification_data.substring(pos));
                }
            }

            clearStringBuilder(notification_data);
        }

        if (hanging_get.isClosed() &&
                state == State.CONNECTED) {

            connectHangingGet();
        }
    }


    // Parses a single line entry in the form "<name>,<id>,<connected>"
    public Boolean parseEntry(String entry, StringBuilder name, int[] id,
                              Boolean[] connected)
    {

        connected[0] = false;

              // Log.d(TAG,"entry is "+entry);
              /// Log.d(TAG,"with length "+entry.length());
        int separator = entry.indexOf(',');
        if (separator != -1) {

            int end=entry.indexOf(',',separator+1);
            if(end!=-1)
            {
               // Log.d(TAG,"first to parse is "+entry.substring(separator+1));
              //  Log.d(TAG,"second to parse is "+entry.substring(separator+1,end));
              //  Log.d(TAG,"third to parse is "+entry.substring(separator+1,end+1));
            id[0] = Integer.parseInt(entry.substring(separator+1,end));
            }
            name.insert(0,entry.substring(0, separator));
            separator = entry.indexOf(',', separator + 1);
            if (separator != -1) {

             //  Log.d(TAG,"seperator is "+separator);
              // Log.d(TAG," this substring is "+entry.substring(separator+1));

                int number=Integer.parseInt(entry.substring(separator+1));
                connected[0] = (number!=0? true : false);
            }
        }
        if(name.length()==0)
            return false;
        else
            return true;

    }

    public int getResponseStatus(String response)
    {
          int status=-1;

           int pos=response.indexOf(' ');

           if(pos!=-1)
           {
               int end=response.indexOf(' ',pos+1);
              // Log.d(TAG,"end is "+end);
               if(end!=-1)
               {
              //  Log.d(TAG,"subtring is "+response.substring(pos+1,end)) ;
               status=Integer.parseInt(response.substring(pos+1,end));
               }
           }

        return status;
    }


    public Boolean parseServerResponse(String response,
                                       int content_length[],
                                       int[] peer_id,
                                       int[] eoh)
    {

       // Log.d(TAG,"the server response is "+response);
        int status = getResponseStatus(response);
        if (status != 200) {

          //  Log.e(TAG, "Received error from server");
            close();
            observer.OnDisconnected();
            return false;
        }

        eoh[0] = response.indexOf("\r\n\r\n");

        assert eoh[0]!=-1;

        if (eoh[0] == -1)
            return false;

        peer_id[0] = -1;

        // See comment in peer_channel.cc for why we use the Pragma header and
        // not e.g. "X-Peer-Id".
        getHeaderValue(response, eoh[0], "\r\nPragma: ", peer_id);

        return true;
    }

    public  void  onClose(FXSocket socket)
    {
        if(socket.getType()== FXSocket.SocketType.CONTROL_SOCKET)
            closeControl();
        else
            closeHangingGet();



        if(socket.getError()!= FXSocket.SocketError.ERROR_CONNECTION)
        {
            if (socket.getType()== FXSocket.SocketType.HANGING_GET_SOCKET) {
                if (state == State.CONNECTED) {

                    closeHangingGet();
                    connectHangingGet();

                }
                } else {
                    observer.OnMessageSent(0);


            }
        }
        else
        {
            close();
            observer.OnDisconnected();
        }
    }

    public void onMessage(String msg)
    {
        // ignore msg; there is currently only one supported message ("retry")
        doConnect();
    }

    //=====================================================================================

    public Boolean connectControl()  {


        assert control_socket.getState()== FXSocket.SocketState.NOT_CONNECTED;

        try
        {
            control_socket.connect(server_socket_address);
        } catch (IOException  e)
        {

            control_socket.setError(FXSocket.SocketError.ERROR_CONNECTION);
            Log.e(TAG,"Error in control socket connection!");
            e.printStackTrace();


            if(e instanceof  SocketException)
            {
                mhandler.sendEmptyMessage(Constants.CONTROL_SOCKET_CLOSED);
            }

            return false;
        }

        if(!control_socket.isConnected())
        {

            control_socket.setError(FXSocket.SocketError.ERROR_CONNECTION);
            Log.w(TAG,"control socket cannot connect to server!");

            return false;
        }

        Log.d(TAG,"control socket is connected!");

        control_socket.setState(FXSocket.SocketState.CONNECTED);
        mhandler.sendEmptyMessage(Constants.CONTROL_SOCKET_CONNECTED);




        return true;

    }


    public Boolean connectHangingGet()
    {


           Log.d(TAG,"hanging get connect is called!");

            assert hanging_get.getState()== FXSocket.SocketState.NOT_CONNECTED;

            try{
            hanging_get.connect(server_socket_address);
            }catch (IOException e)
            {

                hanging_get.setError(FXSocket.SocketError.ERROR_CONNECTION);
                Log.e(TAG,"Error in Hanging Get Socket Connection!");
                e.printStackTrace();

                if(e instanceof  SocketException)
                {
                    mhandler.sendEmptyMessage(Constants.HANGING_GET_SOCKET_CLOSED);
                }

                return false;
            }

            if(!hanging_get.isConnected())
            {

                hanging_get.setError(FXSocket.SocketError.ERROR_CONNECTION);
              Log.w(TAG,"hanging get socket cannot connect to server!");

                return false;
            }


           hanging_get.setState(FXSocket.SocketState.CONNECTED);
           mhandler.sendEmptyMessage(Constants.HANGING_GET_SOCKET_CONNECTED);

           return true;
    }


    public void closeControl()
    {

        assert control_socket.getState()== FXSocket.SocketState.CONNECTED;

        if(control_socket.isClosed())
            return;

        try {
            control_socket.close();
        } catch (IOException e) {


            control_socket.setError(FXSocket.SocketError.ERROR_CONNECTION);
            Log.e(TAG,"Error when closing control socket!");
            e.printStackTrace();
            return;
        }

        control_socket.setState(FXSocket.SocketState.NOT_CONNECTED);

        mhandler.sendEmptyMessage(Constants.CONTROL_SOCKET_CLOSED);
    }


    public void closeHangingGet()
    {

       assert hanging_get.getState()== FXSocket.SocketState.CONNECTED;

        if(hanging_get.isClosed())
            return;

        try {
            hanging_get.close();
        } catch (IOException e) {

            hanging_get.setError(FXSocket.SocketError.ERROR_CONNECTION);
            Log.e(TAG,"Error when closing hanging get socket!");
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return;
        }

        hanging_get.setState(FXSocket.SocketState.NOT_CONNECTED);
        mhandler.sendEmptyMessage(Constants.HANGING_GET_SOCKET_CLOSED);
    }



    public void sendByConnectSocket(String str)
    {
           assert str!=null && str.length()!=0;

           assert control_socket.getState()== FXSocket.SocketState.CONNECTED;

           BufferedWriter writer;

           try {

           writer=new BufferedWriter(new OutputStreamWriter(control_socket.getOutputStream()));
           } catch (IOException e)
           {

                   Log.e(TAG,"Error in sending through control socket!");
                   control_socket.setError(FXSocket.SocketError.ERROR_CONNECTION);
                   mhandler.sendEmptyMessage(Constants.CONTROL_SOCKET_CLOSED);



               e.printStackTrace();
               return;
           }

           try{

           writer.write(str,0,str.length());
           writer.flush();
           } catch  (IOException e)
           {

                   Log.e(TAG,"Error in sending through control socket!");
                   control_socket.setError(FXSocket.SocketError.ERROR_CONNECTION);
                   mhandler.sendEmptyMessage(Constants.CONTROL_SOCKET_CLOSED);



                   e.printStackTrace();
                    return;

           }

        Log.d(TAG,"message is sent: "+str);

    }

    public  void sendByHangingGetSocket(String str)
    {
        assert str!=null && str.length()!=0;

        assert hanging_get.getState()== FXSocket.SocketState.CONNECTED;

        BufferedWriter writer;

        try {

            writer=new BufferedWriter(new OutputStreamWriter(hanging_get.getOutputStream()));
        } catch (IOException e)
        {

                Log.e(TAG,"Error in sending through hanging get socket");
                hanging_get.setError(FXSocket.SocketError.ERROR_CONNECTION);
                mhandler.sendEmptyMessage(Constants.HANGING_GET_SOCKET_CLOSED);



                e.printStackTrace();
                return;
        }

        try{

            writer.write(str,0,str.length());
            writer.flush();
        } catch  (IOException e)
        {

                 Log.e(TAG,"Error in sending through hanging get socket");
                 hanging_get.setError(FXSocket.SocketError.ERROR_CONNECTION);
                 mhandler.sendEmptyMessage(Constants.HANGING_GET_SOCKET_CLOSED);



                e.printStackTrace();
                 return;

        }



    }

    public Boolean CheckControlData()
    {




        if(control_socket.getState()!= FXSocket.SocketState.CONNECTED)
            return false;


        InputStream in;
        try
        {
           in=control_socket.getInputStream();

        }catch (IOException e)
        {
            Log.e(TAG,"Error getting the input stream from control socket!");



            control_socket.setError(FXSocket.SocketError.ERROR_CONNECTION);



               //  Log.e(TAG,"the control socket is closed unexpectedly!");
                 mhandler.sendEmptyMessage(Constants.CONTROL_SOCKET_CLOSED);



            e.printStackTrace();
            return false;
        }

        try{

            if(in.available()>0)
            {
                return true;
            }
            else
                return false;

        }catch (IOException e)
        {
            Log.e(TAG,"Error checking the incoming data from control socket!");


            control_socket.setError(FXSocket.SocketError.ERROR_CONNECTION);




               // Log.e(TAG,"the control socket is closed unexpectedly!");
            mhandler.sendEmptyMessage(Constants.CONTROL_SOCKET_CLOSED);




            e.printStackTrace();
            return false;
        }




    }


    public Boolean CheckHangingGetData()
    {



        if(hanging_get.getState()!= FXSocket.SocketState.CONNECTED)
            return false;





        InputStream in;
        try
        {
            in=hanging_get.getInputStream();

        }catch (IOException e)
        {
            Log.e(TAG,"Error getting the input stream from hanging get socket!");




            hanging_get.setError(FXSocket.SocketError.ERROR_CONNECTION);


            if(hanging_get.isClosed())
            {

                Log.e(TAG,"the hanging get is closed unexpectedly!");
                mhandler.sendEmptyMessage(Constants.HANGING_GET_SOCKET_CLOSED);
            }



            e.printStackTrace();
            return false;
        }

        try{

            if(in.available()>0)
            {
                return true;
            }
            else
                return false;

        }catch (IOException e)
        {
            Log.e(TAG,"Error checking the incoming data from hanging get socket!");



                hanging_get.setError(FXSocket.SocketError.ERROR_CONNECTION);


                if(hanging_get.isClosed())
                {

                Log.e(TAG,"the hanging get is closed unexpectedly!");
                mhandler.sendEmptyMessage(Constants.HANGING_GET_SOCKET_CLOSED);
                }




            e.printStackTrace();
            return false;
        }




    }




    public void IncomingDataCheck()
    {
        if(CheckControlData())
        {
            mhandler.sendEmptyMessage(Constants.CONTROL_SOCKET_READABLE);
        }

        if(CheckHangingGetData())
        {
            mhandler.sendEmptyMessage(Constants.HANGING_GET_SOCKET_READABLE);
        }
    }


    public enum State
    {
        NOT_CONNECTED,
        RESOLVING,
        SIGNING_IN,
        CONNECTED,
        SIGNING_OUT_WAITING,
        SIGNING_OUT
    }





    public void sendEmptyMessage(int MSG)
    {
        Message msg=new Message();
        msg.what=MSG;

        messageQueue.addLast(msg);
    }

    public void processMessage()
    {
        while(messageQueue.size()!=0)
        {
            Message msg=messageQueue.removeFirst();
            handleMessage(msg);
        }
    }


    public void handleMessage(Message msg) {

        switch(msg.what)
        {
            case Constants.CONTROL_SOCKET_CONNECTED:
                Log.d(TAG,"CONTROL SOCKET CONNECTED");
                onConnect();
                break;
            case Constants.CONTROL_SOCKET_CLOSED:

                Log.d(TAG,"CONTROL SOCKET CLOSED");
                onClose(control_socket);
                break;
            case Constants.CONTROL_SOCKET_READABLE:
                Log.d(TAG,"CONTROL SOCKET READABLE");
                onRead();
                break;
            case Constants.HANGING_GET_SOCKET_CONNECTED:
                Log.d(TAG,"HANGING GET SOCKET CONNECTED");
                onHangingGetConnect();
                break;
            case Constants.HANGING_GET_SOCKET_CLOSED:
                Log.d(TAG,"HANGIN GET SOCKET CLOSED");

                onClose(hanging_get);
                break;
            case Constants.HANGING_GET_SOCKET_READABLE:
                Log.d(TAG,"HANGING GET SOCKET READABLE");
                onHangingGetRead();
                break;
            default:
                break;

        }




    }


}
