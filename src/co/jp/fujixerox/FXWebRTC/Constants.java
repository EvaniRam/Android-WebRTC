package co.jp.fujixerox.FXWebRTC;

import java.util.Locale;

/**
 * Created with IntelliJ IDEA.
 * User: lapmore
 * Date: 10/24/13
 * Time: 10:33 PM
 * To change this template use File | Settings | File Templates.
 */
public final class Constants {

    // Set to true to turn on verbose logging
    public static final boolean LOGV = false;

    // Set to true to turn on debug logging
    public static final boolean LOGD = true;

    // Custom actions

    public static final String ACTION_VIEW_IMAGE =
            "jp.co.fujixerox.FXWebRTC.ACTION_VIEW_IMAGE";

    public static final String ACTION_ZOOM_IMAGE =
            "jp.co.fujixerox.FXWebRTC.ACTION_ZOOM_IMAGE";

    // Defines a custom Intent action
    public static final String BROADCAST_ACTION = "jp.co.fujixerox.FXWebRTC.BROADCAST_CLIENT_STATUS";

    // Fragment tags
    public static final String PHOTO_FRAGMENT_TAG =
            "jp.co.fujixerox.FXWebRTC.PHOTO_FRAGMENT_TAG";

    public static final String THUMBNAIL_FRAGMENT_TAG =
            "jp.co.fujixerox.FXWebRTC.THUMBNAIL_FRAGMENT_TAG";

    // Defines the key for the status "extra" in an Intent
    public static final String EXTENDED_DATA_STATUS = "jp.co.fujixerox.FXWebRTC.STATUS";

    public static final String EXTENDED_DATA_ID = "jp.co.fujixerox.FXWebRTC.ID";

    public static final String EXTENDED_DATA_MESSAGE = "jp.co.fujixerox.FXWebRTC.MESSAGE";

    // Defines the key for the log "extra" in an Intent
    public static final String EXTENDED_STATUS_LOG = "jp.co.fujixerox.FXWebRTC.LOG";

    // Defines the key for storing fullscreen state
    public static final String EXTENDED_FULLSCREEN =
            "jp.co.fujixerox.FXWebRTC.EXTENDED_FULLSCREEN";

    /*
     * A user-agent string that's sent to the HTTP site. It includes information about the device
     * and the build that the device is running.
     */
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; U; Android "
            + android.os.Build.VERSION.RELEASE + ";"
            + Locale.getDefault().toString() + "; " + android.os.Build.DEVICE
            + "/" + android.os.Build.ID + ")";

    // Status values to broadcast to the Activity



    //connection state;

    // the control socket is connected
    public static final int CONTROL_SOCKET_CONNECTED = 0;

    // receive some data from control socket
    public static final int CONTROL_SOCKET_READABLE=1;

    // the hanging get socket is connected
    public static final int HANGING_GET_SOCKET_CONNECTED = 2;

    // receive some data from hanging get socket
    public static final int HANGING_GET_SOCKET_READABLE=3;

    // control socket is closed
    public static final int CONTROL_SOCKET_CLOSED=4;

    // hanging get socket is closed
    public static final int HANGING_GET_SOCKET_CLOSED =5;




    public static final int CLIENT_SIGNED_IN=10;

    public static final int CLIENT_DISCONNECTED=11;

    public static final int PEER_CONNECTED=12;

    public static final int PEER_DISCONNECTED=13;

    public static final int MESSAGE_FROM_PEER=14;

    public static final int MESSAGE_SENT=15;

    public static final int SERVER_CONNECTION_FAILURE=16;

    public static final int CLIENT_STATE_NULL=20;

}