package com.ee305.aafi.ghosthunters.network_related;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.ee305.aafi.ghosthunters.GhostHunters;
import com.ee305.aafi.ghosthunters.player_related.Ghost;
import com.ee305.aafi.ghosthunters.player_related.Input;
import com.ee305.aafi.ghosthunters.utils.PacketUtils;
import com.ee305.aafi.ghosthunters.utils.Pair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientNetworkNode {


    public enum ClientInstruction{
        CONNECT,
        SEND_INPUT,
        KEEP_ALIVE,
        DISCONNECT
    }



    private Thread clientListenerThread;
    private Thread clientSenderThread;

    private AtomicInteger sequenceNumber =new AtomicInteger(0);
    private AtomicInteger lastReceivedSequenceNumber =new AtomicInteger(0);
    public AtomicBoolean isConnected=new AtomicBoolean(false);

    private DatagramSocket socket;

    private InetAddress serverAddress;

    private int clientNumber;
    private OnNewStateRecieved stateRecieved;


    public ClientNetworkNode(OnNewStateRecieved onStateUpdate,Runnable IPFail,Runnable socketFail,String textIP ,String clientName) {

        this.clientName=clientName;
        this.socketFailCallback=socketFail;
        this.IPFailCallback=IPFail;
        this.serverAddressText=textIP;
        stateRecieved=onStateUpdate;
        clientListenerThread=new Thread(new ClientListener());
        clientSenderThread=new Thread(new ClientSender());
        clientSenderThread.start();

    }
    private volatile String clientName;

    public void retryConnection(String ip,String clientName){

        this.clientName=clientName;
        if(!clientSenderThread.isAlive()){
            serverAddressText=ip;
            clientSenderThread=new Thread(new ClientSender());
            clientSenderThread.start();
        }

    }


    private static HashMap<String,String> getMessageClientParameters(String message){
        HashMap map =new HashMap<String,String>();
        for (String s: message.split("\n")) {

            String [] paramData=s.split(": ");
            String type=paramData[0];
            if (type.equalsIgnoreCase("Type") ||
                    type.equalsIgnoreCase("SetClient#") ||
                    type.equalsIgnoreCase("Sequence#") ||
                    type.equalsIgnoreCase("Data") ||
                    type.equalsIgnoreCase("Dupe") ||
                    type.equalsIgnoreCase("SetNewGhost")){
                map.put(paramData[0],paramData[1]);
            }else{
                //unrecognized parameter TODO
                //System.out.println(type+" is an invalid parameter");
                continue;
            }

        }

        if (map.isEmpty()){
            return null;
        }

        return map;
    }


    //Handshake to initialize both clients and servers
    boolean connectToServer(){

        int handshakeTry=0;
        while (handshakeTry<=3) {
            handshakeTry++;
            //building the first message
            DatagramPacket sentPacket= PacketUtils.
                    createPacket(serverAddress,GhostHunters.getGameInstance().serverPort.get()
                            , "Type","Handshake",
                            "Client#","-1",
                            "Sequence#", sequenceNumber.getAndIncrement()+"",
                            "Name",clientName);

            try {
                if (GhostHunters.getGameInstance().showSentPackets.get()){
                    Gdx.app.postRunnable(()->PacketUtils.prettyPrint(GhostHunters.getGameInstance().con,sentPacket));
                }
                socket.send(sentPacket);
                Pair<DatagramPacket,String> receivedPair = blockingReceive();
                int h=handshakeTry;
                if (receivedPair.getObj2().equalsIgnoreCase("Timeout")){
                    Gdx.app.postRunnable(()-> GhostHunters.getGameInstance().con.log("Timeout trial: "+h));
                    continue;
                }else if (receivedPair.getObj2().equalsIgnoreCase("IOException")){
                    Gdx.app.postRunnable(()-> GhostHunters.getGameInstance().con.log("IOException trial: "+h));
                    continue;
                }

                //Message has data
                HashMap<String, String> messageParams = getMessageClientParameters(receivedPair.getObj2());

                if (messageParams == null) {
                    continue;
                }
                String messageType = messageParams.get("Type");
                System.out.println();
                if (messageType.equalsIgnoreCase("AckHandshake") ) {

                    try {
                        int clientNumber = Integer.parseInt(messageParams.get("SetClient#"));
                        this.clientNumber = clientNumber;
                        if (clientListenerThread!=null && !clientListenerThread.isAlive()){
                            clientListenerThread=new Thread(new ClientListener());
                            clientListenerThread.start();
                        }

                        isConnected.set(true);
                        Gdx.app.postRunnable(()-> {GhostHunters.getGameInstance().con.log("Connected to server. ");});
                        return true;
                    } catch (NumberFormatException e) {
                        interruptAndSend(ClientInstruction.DISCONNECT,null);
                        return true;
                    }
                }

                if(messageType.equalsIgnoreCase("RefuseHandShake")){

                    String data=messageParams.get("Data");

                    if (data == null) {
                        System.out.println("null data");
                        continue;
                    }
                    Gdx.app.postRunnable(()-> {GhostHunters.getGameInstance().con.log("Connection refused: "+data);});
                    return false;
                }

            }catch (SocketException e){
                Gdx.app.postRunnable(()-> GhostHunters.getGameInstance().con.log("SocketException end "+e.getMessage()));
                return false;
            }catch (IOException e){

                Gdx.app.postRunnable(()-> GhostHunters.getGameInstance().con.log("IOException end "+e.getMessage()));
                e.printStackTrace();
            }

        }

        return false;
    }

    private int getSequence(String message) {
        HashMap<String,String> map=getMessageClientParameters(message);

        if (map.isEmpty()){
            return -1;
        }
        if (map.containsKey("Sequence#")){
            return Integer.parseInt(map.get("Sequence#"));
        }else{
            System.out.println("No sequence number found "+map.get("Type"));
        }
        return -1;
    }

    private Pair<DatagramPacket,String> blockingReceive() {

        if (socket == null) {
            System.out.println("Blocking receive should not be called if the socket is not open.");
            return new Pair<>(null,"ClosedSocket");
        }

        byte[] receivedBytes = new byte[1024];
        DatagramPacket receivedPacket = new DatagramPacket(receivedBytes, receivedBytes.length);
        try {
            socket.receive(receivedPacket);
            String message=new String(receivedPacket.getData(), StandardCharsets.US_ASCII);

            int receivedSequence=getSequence(message);
            if(lastReceivedSequenceNumber.get()<receivedSequence){
                lastReceivedSequenceNumber.set (receivedSequence);
            }else{
                return new Pair<>(receivedPacket,"Not Inorder");
            }

            return new Pair<>(receivedPacket,message);
        } catch (SocketTimeoutException e) {
            return new Pair<>(null,"Timeout");
        } catch (IOException e) {

            e.printStackTrace();
            return new Pair<>(null,"IOException");
        }

    }

    public interface OnNewStateRecieved{

        void onMatchmakingStateRecieved(ArrayList<String> joinedClients);

        void onGameStart(int ghostID,int currentClientId);

        void onStateRecieved(State state);

        void endGameRecieved(boolean ghostWinner,boolean playAgain);
    }


    private int recSample =0;

    final class ClientListener implements Runnable {


        @Override
        public void run() {

            while (true){

                if (Thread.interrupted()){
                    return;
                }
               GhostHunters.AppState state=GhostHunters.getAppState();
                Pair<DatagramPacket,String> receivedPair=blockingReceive();

                if (receivedPair.getObj2().equalsIgnoreCase("Timeout")){
                    continue;//no problems
                }else if (receivedPair.getObj2().equalsIgnoreCase("NullSocket")){
                    //TODO establish a new connection somehow
                    continue;
                }else if (receivedPair.getObj2().equalsIgnoreCase("IOException")){
                    //TODO based on the error
                    continue;
                }else if (receivedPair.getObj2().equalsIgnoreCase("Not Inorder")){
                    //TODO based on the error
                    System.out.println("out of order");

                }
                //Message has data

                if(GhostHunters.getGameInstance().showRecievedPackets.get() ){
                    HashMap<String,String> mp=getMessageClientParameters(receivedPair.getObj2());

                    if (mp==null){
                        continue;
                    }

                    if (mp.isEmpty()){
                        continue;
                    }
                    String mt=mp.get("Type");
                    System.out.println(mt);
                    if (mt.equalsIgnoreCase("StartGame") || mt.equalsIgnoreCase("EndGame")){
                        Gdx.app.postRunnable(()->PacketUtils.prettyPrint(GhostHunters.getGameInstance().con,receivedPair.getObj1()));
                    }else{
                        if (recSample>=30){
                            Gdx.app.postRunnable(()->PacketUtils.prettyPrint(GhostHunters.getGameInstance().con,receivedPair.getObj1()));
                            recSample=0;
                        }
                        recSample++;
                    }

                }


                //Matchmaking state or game start
                if (state==GhostHunters.AppState.MATCH_MAKING){

                   HashMap<String,String> messageParams=getMessageClientParameters(receivedPair.getObj2());

                   if (messageParams==null){
                       continue;
                   }

                   if (messageParams.isEmpty()){
                       System.out.println("empty state parameters");
                       continue;
                   }
                   String messageType=messageParams.get("Type");

                    if (messageType.equals("Matchmaking")) {
                        String data=messageParams.get("Data");
                        if (data==null){
                            System.out.println("null data");
                            continue;
                        }
                        Json json = new Json();
                        ArrayList<String> builtData = json.fromJson(ArrayList.class, data);
                        if (builtData==null){
                            System.out.println("problem with data");
                            continue;
                        }
                        Gdx.app.postRunnable(()-> stateRecieved.onMatchmakingStateRecieved(builtData) );
                    }else if (messageType.equals("StartGame")){
                        String data=messageParams.get("SetNewGhost");
                        if (data==null){
                            System.out.println("null data");
                            continue;
                        }
                        Gdx.app.postRunnable(()->{stateRecieved.onGameStart(Integer.parseInt(data),clientNumber);});
                    }

               }else if (state==GhostHunters.AppState.IN_GAME) {

                    if (GhostHunters.getGameInstance().blockServerUpdates.get()){
                        System.out.println("Ignoring server");
                        continue;
                    }

                    HashMap<String, String> messageParams = getMessageClientParameters(receivedPair.getObj2());

                    if (messageParams==null) {
                        System.out.println(" parameters");
                        continue;
                    }

                    if (messageParams.isEmpty()) {
                        System.out.println(" parameters");
                        continue;
                    }

                    String messageType = messageParams.get("Type");

                    if (messageType.equalsIgnoreCase("State")) {
                        String data = messageParams.get("Data");

                        if (data == null) {
                            System.out.println("null data");
                            continue;
                        }

                        Json json = new Json();
                        State recievedState = json.fromJson(State.class, data);
                        Gdx.app.postRunnable(() -> {
                            stateRecieved.onStateRecieved(recievedState);
                        });

                    }else if (messageType.equalsIgnoreCase("EndGame")){
                        String data = messageParams.get("Data");

                        if (data == null) {
                            System.out.println("null data");
                            continue;
                        }

                        Json json=new Json();
                        EndGameObject object=json.fromJson(EndGameObject.class,data);
                        Gdx.app.postRunnable(()->{ stateRecieved.endGameRecieved(object.ghostWinner,object.replay);});

                    }
                }


            }


        }


    }

    public BlockingQueue<Pair<ClientInstruction,Object>> instructionQueue=new LinkedBlockingQueue<>();
    private String serverAddressText;
    private Runnable IPFailCallback;
    private Runnable socketFailCallback;


    public void interruptAndSend(ClientInstruction instruction,Object value){
        if (!clientSenderThread.isAlive()) return;
        instructionQueue.add(new Pair<>(instruction,value));
    }

    final class ClientSender implements Runnable {


        private int sendSample =0;

        public ClientSender() {

        }

        @Override

        public void run() {

            //TODO do the connection setup

            try {
                serverAddress = InetAddress.getByName(serverAddressText);
                instructionQueue.add(new Pair<>(ClientInstruction.CONNECT,null));
            } catch (UnknownHostException e) {
                //Ask the user to try again
                Gdx.app.postRunnable(IPFailCallback);
                return;
            }

            try {
                if (socket==null){

                    if (GhostHunters.getGameInstance().clientPort.get()==0){
                        socket = new DatagramSocket();
                        socket.setSoTimeout(2000);

                    }else{
                        socket = new DatagramSocket();
                        socket.setSoTimeout(2000);
                    }
                    Gdx.app.postRunnable(()->{
                        GhostHunters.getGameInstance().con.log("On port "+GhostHunters.getGameInstance().serverPort.get()+
                                " address "+ serverAddress.getHostAddress());
                    });

                }

                //sets the initial timeout to 1s
            } catch (SocketException e) {
                Gdx.app.postRunnable(socketFailCallback);

                e.printStackTrace();
                return;
            }

            while(true){
                GhostHunters.AppState state=GhostHunters.getAppState();


                Pair<ClientInstruction,Object> currentInstruction=null;
                try {
                    currentInstruction = instructionQueue.poll(3, TimeUnit.SECONDS);

                }catch (InterruptedException e){

                }

                if (currentInstruction==null){

                    if (isConnected.get()){
                        instructionQueue.add(new Pair<>(ClientInstruction.KEEP_ALIVE,null));
                    }
                    continue;
                }


                if (state==GhostHunters.AppState.MAIN_MENU){
                    //TODO Decide whether this is appropriate
                    break;

                }else if (state==GhostHunters.AppState.MATCH_MAKING) {

                    if (currentInstruction.getObj1()==ClientInstruction.DISCONNECT) {
                        //TODO TEST some how send the disconnect packet
                        if (isConnected.get()) {

                            try {
                                DatagramPacket sentPacket=PacketUtils.createPacket(serverAddress, GhostHunters.getGameInstance().serverPort.get(),
                                        "Type", "Disconnect",
                                        "Name", clientName,
                                        "Client#", clientNumber + "",
                                        "Sequence#", sequenceNumber.getAndIncrement() + "");
                                socket.send(sentPacket);

                                if (GhostHunters.getGameInstance().showSentPackets.get()){
                                    Gdx.app.postRunnable(()->PacketUtils.prettyPrint(GhostHunters.getGameInstance().con,sentPacket));
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            sequenceNumber.set(0);
                            lastReceivedSequenceNumber.set(0);
                            serverAddress=null;
                            serverAddressText="";
                            isConnected.set(false);
                            clientListenerThread.interrupt();

                            return;
                        }
                    }


                    if (!isConnected.get() && currentInstruction.getObj1()==ClientInstruction.CONNECT) {
                        if(!connectToServer()){
                            GhostHunters.getGameInstance().reportError("Connection to server failed(No server was found).");
                            return;
                        }
                    }

                }else if (state==GhostHunters.AppState.IN_GAME){

                    if (isConnected.get() && currentInstruction.getObj1()==ClientInstruction.SEND_INPUT){
                        //TODO Test
                        Json json=new Json();
                        String message=json.toJson(currentInstruction.getObj2(), Input.class).replaceAll("\\00+","");;

                        try {
                            DatagramPacket sentPacket= PacketUtils.createPacket(serverAddress,GhostHunters.getGameInstance().serverPort.get(),
                                    "Type","Input",
                                    "Sequence#",sequenceNumber.getAndIncrement()+"",
                                    "Client#",clientNumber+"",
                                    "Data",message);

                            socket.send(sentPacket);
                            if (GhostHunters.getGameInstance().showSentPackets.get()){
                                if (sendSample>=60){
                                    Gdx.app.postRunnable(()->PacketUtils.prettyPrint(GhostHunters.getGameInstance().con,sentPacket));
                                    sendSample=0;
                                }
                                sendSample++;
                            }
                        }catch (IOException e){

                        }

                    }

                }



            }

        }
    }

}
