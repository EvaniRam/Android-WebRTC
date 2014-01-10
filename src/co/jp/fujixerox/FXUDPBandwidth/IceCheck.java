package co.jp.fujixerox.FXUDPBandwidth;

import co.jp.fujixerox.FXWebRTC.WebRTCClient;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.*;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.security.LongTermCredential;
import test.SdpUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by haiyang on 1/10/14.
 */
public class IceCheck implements Runnable{
    private WebRTCClient client;
    private long startTime;
    private Agent localAgent;
    private String localSDP;
    private String remoteSDP;
    private CountDownLatch remoteSDPLatch;

    private ServerTransportAddress stunserver;
    private TurnServerTransportAddress turnserver;
    private int localport;
    private boolean usingTurn;
    private IceStateClientListener clientListener;



    public interface IceStateClientListener
    {
        public void onICEComplete(String name,CheckList list);
        public void onICETerminate();
    }

    public IceCheck(boolean usingTurn)
    {
        localport=-1;
        startTime=-1;
        this.usingTurn=usingTurn;
    }

    public void setStunServer(String serveraddress, int port, boolean isUDP)
    {
        stunserver=new ServerTransportAddress(serveraddress, port, isUDP);
    }
    public void setTurnServer(String serveraddress, int port, boolean isUDP,
                              String username,String password)
    {
        turnserver=new TurnServerTransportAddress(serveraddress,port,
                isUDP,username,password);
    }
    public void setWebRTCClient(WebRTCClient client)
    {
        this.client=client;
       // clientListener=client;
    }

    public boolean isSettingReady()
    {
        boolean ready= localport>0 && stunserver!=null && client!=null;
        if(!usingTurn)
            return ready;
        else
            return ready && turnserver!=null;

    }

    public void setUDPPort(int port)
    {
        this.localport=port;
    }

    protected  Agent createAgent(int rtpPort) throws Throwable
    {
        return createAgent(rtpPort, false);
    }


    protected Agent createAgent(int rtpPort, boolean isTrickling)  throws Throwable
    {

        Agent agent = new Agent();

        agent.setTrickling(isTrickling);

        // STUN
        StunCandidateHarvester stunHarv = new StunCandidateHarvester(


                //    new TransportAddress("stun.jitsi.net", 3478, Transport.UDP)
                new TransportAddress(stunserver.ServerAddress,stunserver.port,
                        stunserver.isUDP? Transport.UDP:Transport.TCP)

        );


        // StunCandidateHarvester stun6Harv = new StunCandidateHarvester(
        //   new TransportAddress("stun6.jitsi.net", 3478, Transport.UDP));

        agent.addCandidateHarvester(stunHarv);
        // agent.addCandidateHarvester(stun6Harv);

        // TURN

        if(usingTurn)
        {
            String[] hostnames = new String[]
                    {
                            turnserver.ServerAddress,
                                /*"stun6.jitsi.net"*/
                    };
            int port = turnserver.port;



            //LongTermCredential longTermCredential
            //  = new LongTermCredential("guest", "anonymouspower!!");

            LongTermCredential longTermCredential
                    = new LongTermCredential(turnserver.username, turnserver.password);

            for (String hostname : hostnames)
                agent.addCandidateHarvester(
                        new TurnCandidateHarvester(
                                new TransportAddress(hostname, port,
                                        turnserver.isUDP?Transport.UDP:Transport.TCP),
                                longTermCredential));

        }

        //UPnP: adding an UPnP harvester because they are generally slow
        //which makes it more convenient to test things like trickle.
        //  agent.addCandidateHarvester( new UPNPHarvester() );

        //STREAMS
        createStream(rtpPort, "audio", agent);
        //  createStream(rtpPort + 2, "video", agent);


        long endTime = System.currentTimeMillis();
        long total = endTime - startTime;

        System.out.println("Total harvesting time: " + total + "ms.");

        return agent;
    }


    public void sendPeerSDP()
    {
       // client.sendPeerSDP(localSDP);
    }

    public void setRemoteSDP(String SDP)
    {

        System.out.println("remote SDP has been received!");
        this.remoteSDP=SDP;
        if(remoteSDPLatch!=null)
        {
            remoteSDPLatch.countDown();

        }
    }



    public IceMediaStream createStream(int rtpPort,String streamName,Agent agent) throws Throwable
    {
        IceMediaStream stream = agent.createMediaStream(streamName);

        long starttime = System.currentTimeMillis();

        //TODO: component creation should probably be part of the library. it
        //should also be started after we've defined all components to be
        //created so that we could run the harvesting for everyone of them
        //simultaneously with the others.

        //rtp
        agent.createComponent(
                stream, Transport.UDP, rtpPort, rtpPort, rtpPort + 100);

        long endTime = System.currentTimeMillis();
        System.out.println("RTP Component created in "
                + (endTime - starttime) +" ms");
        //  startTime = endTime;
        //rtcpComp


        agent.createComponent(
                stream, Transport.UDP, rtpPort + 1, rtpPort + 1, rtpPort + 101);

        endTime = System.currentTimeMillis();
        System.out.println("RTCP Component created in "
                + (endTime - startTime) +" ms");


        return stream;
    }


    public final class IceProcessingListener implements PropertyChangeListener {

        /*
         * @param evt the {@link PropertyChangeEvent} containing the old and new
         * states of ICE processing.
         */
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            long processingEndTime = System.currentTimeMillis();

            Object iceProcessingState = evt.getNewValue();

            System.out.println(
                    "Agent entered the " + iceProcessingState + " state.");
            if(iceProcessingState == IceProcessingState.COMPLETED)
            {
                System.out.println(
                        "Total ICE processing time: "
                                + (processingEndTime - startTime) + "ms");
                Agent agent = (Agent)evt.getSource();
                List<IceMediaStream> streams = agent.getStreams();

                for(IceMediaStream stream : streams)
                {
                    String streamName = stream.getName();
                    System.out.println(
                            "Pairs selected for stream: " + streamName);
                    List<Component> components = stream.getComponents();

                    for(Component cmp : components)
                    {
                        String cmpName = cmp.getName();
                        System.out.println(cmpName + ": "
                                + cmp.getSelectedPair());



                    }
                }

                System.out.println("Printing the completed check lists:");
                for(IceMediaStream stream : streams)
                {
                    String streamName = stream.getName();
                    System.out.println("Check list for  stream: " + streamName);
                    //uncomment for a more verbose output
                    System.out.println(stream.getCheckList());

                    if(clientListener!=null)
                        clientListener.onICEComplete(stream.getName(),stream.getCheckList());

                }



            }
            else if(iceProcessingState == IceProcessingState.TERMINATED
                    || iceProcessingState == IceProcessingState.FAILED)
            {
                /*
                 * Though the process will be instructed to die, demonstrate
                 * that Agent instances are to be explicitly prepared for
                 * garbage collection.
                 */
                ((Agent) evt.getSource()).free();

                System.out.println("ICE negotiation terminated");
                // System.exit(0);

                if(clientListener!=null)
                    clientListener.onICETerminate();
            }
        }

    }



    public void run()
    {
        // TODO code application logic here
        if(!isSettingReady())
        {
            System.out.println("Setting is not ready yet!");

        }

        startTime = System.currentTimeMillis();

        try{
            localAgent = createAgent(localport);
        }catch (Throwable e)
        {
            System.out.println("Error in creating ice agent!");
            e.printStackTrace();

        }

        localAgent.setNominationStrategy(NominationStrategy.NOMINATE_HIGHEST_PRIO);

        localAgent.addStateChangeListener(new IceProcessingListener());


       // if(client.currentUDPConnection.initiator)
       //     localAgent.setControlling(true);
      //  else
         //   localAgent.setControlling(false);


        try{
            localSDP = SdpUtils.createSDPDescription(localAgent);
        }catch (Throwable e)
        {
            System.out.println("Error creating local SDP!");
            e.printStackTrace();

        }

        //wait a bit so that the logger can stop dumping stuff:
        // Thread.sleep(500);

        System.out.println("=================== feed the following"
                +" to the remote agent ===================");


        System.out.println(localSDP);

        System.out.println("======================================"
                +"========================================\n");


        //send sdp to peer
        sendPeerSDP();

        // String sdp = readSDP();
        if(remoteSDP==null)
        {

            System.out.println("waiting for remote sdp to come");
            //wait remote sdp to come
            remoteSDPLatch=new CountDownLatch(1);

            try{
                remoteSDPLatch.await();
            }catch (InterruptedException e)
            {
                System.out.println("latch waiting is interrupted!");

            }
        }

        startTime = System.currentTimeMillis();

        try{
            SdpUtils.parseSDP(localAgent, remoteSDP);
        }catch (Throwable e)
        {
            System.out.println("Error in parsing remote SDP");
            e.printStackTrace();

        }


        //start connectivity check

        System.out.println("going to start the connectivity check!");
        localAgent.startConnectivityEstablishment();

        //Give processing enough time to finish. We'll System.exit() anyway
        //as soon as localAgent enters a final state.
        // Thread.sleep(60000);



    }






    public class ServerTransportAddress
    {
        public String ServerAddress;
        public int port;
        public boolean isUDP;

        public ServerTransportAddress(String ServerAddress, int port,boolean isUDP)
        {
            this.ServerAddress=ServerAddress;
            this.port=port;
            this.isUDP=isUDP;
        }
    }

    public class TurnServerTransportAddress extends ServerTransportAddress
    {
        public String username;
        public String password;

        public TurnServerTransportAddress(String ServerAddress, int port,boolean isUDP,
                                          String username,String password)
        {
            super(ServerAddress,port,isUDP);
            this.username=username;
            this.password=password;
        }
    }




}
