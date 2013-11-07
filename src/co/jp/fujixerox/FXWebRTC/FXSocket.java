package co.jp.fujixerox.FXWebRTC;

import java.net.Socket;

/**
 * Created with IntelliJ IDEA.
 * User: haiyang
 * Date: 10/30/13
 * Time: 11:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class FXSocket extends Socket {



    private SocketState state=SocketState.NOT_CONNECTED;
    private SocketType type=SocketType.TYPE_NONE;
    private SocketError error=SocketError.ERROR_NONE;


    public SocketState getState(){return state;}
    public SocketType  getType(){return type;}
    public SocketError getError(){return error;}


    public FXSocket()
    {
        super();
    }

    public void setState(SocketState state)
    {
        this.state=state;
    }

    public void setType(SocketType type)
    {
        this.type=type;
    }

    public void setError(SocketError error)
    {
        this.error=error;
    }

    public enum SocketType
    {
        CONTROL_SOCKET,
        HANGING_GET_SOCKET,
        TYPE_NONE;
    }
    public enum SocketState
    {
        NOT_CONNECTED,
        CONNECTED,

    }


    public enum SocketError{
        ERROR_NONE,
        ERROR_CONNECTION
    }


}
