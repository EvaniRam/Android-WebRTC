package co.jp.fujixerox.FXWebRTC;

/**
 * Created by haiyang on 1/7/14.
 */
public class Peers {

    private int peer_id;

    private Status status;

    private boolean udp;
    private String platform;
    private String name;


    public Peers()
    {

    }

    public void setPeerId(int peer_id) {
        this.peer_id = peer_id;
    }

    public void setStatus(Status status)
    {
        this.status=status;
    }



    public int getPeerId()
    {
        return peer_id;
    }



    public boolean isUdp() {
        return udp;
    }

    public void setUdp(boolean udp) {
        this.udp = udp;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus()
    {
        return status;
    }



    public enum Status
    {
        STATUS_IDLE,
        STATUS_BUSY
    }
}
