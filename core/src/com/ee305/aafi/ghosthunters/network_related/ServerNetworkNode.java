package com.ee305.aafi.ghosthunters.network_related;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.ee305.aafi.ghosthunters.GhostHunters;
import com.ee305.aafi.ghosthunters.player_related.Input;
import com.ee305.aafi.ghosthunters.utils.PacketUtils;
import com.ee305.aafi.ghosthunters.utils.Pair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class ServerNetworkNode {

    private String serverName;

    public synchronized void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public synchronized String getServerName() {
        return serverName;
    }

    private Thread ServerListenerThread;
    private Thread ServerSenderThread;
    private ArrayList<Client> clients=new ArrayList<>();
    DatagramSocket socket;
    private AtomicInteger sequenceNumber=new AtomicInteger(0);

    public enum ServerInstruction{
        START_GAME,
        KICK,
        STATE,
        END_GAME
    }


    public ServerNetworkNode(){

        ServerListenerThread= new Thread(new ServerListener());
        ServerListenerThread.start();

        ServerSenderThread=new Thread(new ServerSender());
        ServerSenderThread.start();

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
            for (Client c:clients){
                if (c.getSocketAddress().equals(receivedPacket.getSocketAddress())){
                    int receivedSequence=getSequence(message);
                    if(c.getLastReceivedSequenceNumber()<receivedSequence){
                        c.setLastReceivedSequenceNumber(receivedSequence);
                    }else{
                        return new Pair<>(receivedPacket,"Not Inorder");
                    }
                }
            }

            return new Pair<>(receivedPacket,message);
        } catch (SocketTimeoutException e) {
            return new Pair<>(null,"Timeout");
        } catch (IOException e) {
            e.printStackTrace();
            return new Pair<>(null,"IOException");
        }

    }

    private int getSequence(String message) {
        HashMap<String,String> map=getMessageServerParameters(message);

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


    public interface PlayerJoin{

         void playerJoin(String name,int id);

    }

    public interface PlayerDisconnect{

         void playerDisconnect(String name,int id);

    }

    public interface PlayerInputRecieved{

         void inputRecieved(Input userInput, int clientNumber);

    }



    PlayerJoin onPlayerJoin;
    PlayerDisconnect onPlayerDisconnect;
    PlayerInputRecieved onReceivePlayerInput;

    public void setOnPlayerDisconnect(PlayerDisconnect onPlayerDisconnect) {
        this.onPlayerDisconnect = onPlayerDisconnect;
    }

    public void setOnPlayerJoin(PlayerJoin onPlayerJoin) {
        this.onPlayerJoin = onPlayerJoin;
    }

    public void setOnReceivePlayerInput(PlayerInputRecieved onReceivePlayerInput) {
        this.onReceivePlayerInput = onReceivePlayerInput;
    }


    int recSample=0;
    final class ServerListener implements Runnable{

        @Override
        public void run()
        {
            socket =null;
            try {
                socket = new DatagramSocket(GhostHunters.getGameInstance().serverPort.get());
                socket.setSoTimeout(1000);
            } catch (SocketException e) {
                e.printStackTrace();
            }

            while(true) {
                GhostHunters.AppState appState = GhostHunters.getAppState();

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
                    continue;
                }
                //Message has data


                if(GhostHunters.getGameInstance().showRecievedPackets.get()){

                    if (isPacketOfType("Input",receivedPair.getObj2())){
                        if (recSample>=100){
                            Gdx.app.postRunnable(()->PacketUtils.prettyPrint(GhostHunters.getGameInstance().con,receivedPair.getObj1()));
                            recSample=0;
                        }
                        recSample++;
                    }else{
                        Gdx.app.postRunnable(()->PacketUtils.prettyPrint(GhostHunters.getGameInstance().con,receivedPair.getObj1()));
                    }


                }



                if (appState == GhostHunters.AppState.MATCH_MAKING) {
                    //Allow handshakes in this state

                    if (isPacketOfType("Handshake",receivedPair.getObj2())){

                        DatagramPacket ackPacket=processAckPacket(receivedPair);
                        try {
                            if (ackPacket == null) continue;
                            if (GhostHunters.getGameInstance().showSentPackets.get()){
                                Gdx.app.postRunnable(()->PacketUtils.prettyPrint(GhostHunters.getGameInstance().con,ackPacket));
                            }
                            socket.send(ackPacket);
                        }catch (IOException e){
                            e.printStackTrace();
                        }

                    }else if (isPacketOfType("Disconnect",receivedPair.getObj2())){

                        String clientMessage=receivedPair.getObj2();
                        HashMap<String,String> messageParams=getMessageServerParameters(clientMessage);

                        if (messageParams.isEmpty()) {
                            System.out.println(" parameters");
                            continue;
                        }

                        for (Client c:clients){
                            if (c.getSocketAddress().equals(receivedPair.getObj1().getSocketAddress())){
                                clients.remove(c);
                                Gdx.app.postRunnable(()->{onPlayerDisconnect.playerDisconnect(c.getName(),c.getClientNumber());});

                                break;
                            }

                        }



                    }


                } else if (appState == GhostHunters.AppState.IN_GAME) {

                    if (isPacketOfType("Input",receivedPair.getObj2())){

                        String clientMessage=receivedPair.getObj2();
                        HashMap<String,String> messageParams=getMessageServerParameters(clientMessage);

                        if (messageParams.isEmpty()) {
                            System.out.println(" parameters");
                            continue;
                        }
                        if (messageParams.containsKey("Client#")){
                            String data=messageParams.get("Data");
                            if (data==null){
                                System.out.println("Null data");
                                continue;
                            }
                            int clientNo=Integer.parseInt(messageParams.get("Client#"));


                            Json json=new Json();
                            Input in=json.fromJson(Input.class,data);
                            Gdx.app.postRunnable(()->{ onReceivePlayerInput.inputRecieved(in,clientNo);});
                        }
                    }

                } else if (appState == GhostHunters.AppState.MAIN_MENU) {

                    break ; //end this thread
                }
            }

        }
    }


    private DatagramPacket processAckPacket(Pair<DatagramPacket,String> receivedPair){


        HashMap<String,String> map= getMessageServerParameters(receivedPair.getObj2());
        if (map.isEmpty()){
            return null;
        }

        String type=map.get("Type");

        if (!type.equalsIgnoreCase("Handshake"))
        {
            return null;//ignore the packet as it is not a handshake
        }

        DatagramPacket receivedPacket=receivedPair.getObj1();

        try {
            for (Client client :clients) {

                if (client.getSocketAddress().equals(receivedPacket.getSocketAddress())){

                    return PacketUtils.
                            createPacket(receivedPacket.getAddress(),receivedPacket.getPort()
                                    ,"Type","AckHandShake",
                                    "Data","Already connected",
                                    "SetClient#",client.getClientNumber()+"",
                                    "Sequence#",sequenceNumber.getAndIncrement()+"");
                }
            }
            //a new client request arrived


            String pName=map.get("Name")==null ?"Unknown":map.get("Name");
            int newSequenceNumber=map.get("Sequence#")==null ?0:Integer.parseInt(map.get("Sequence#"));
            int cNum=generateClientNumber();
            Client c=Client.createNewClient(receivedPacket.getSocketAddress(),cNum,receivedPacket.getPort(),pName);
            c.setLastReceivedSequenceNumber(newSequenceNumber);
            clients.add(c);
            System.out.println(c.getSocketAddress());

            Gdx.app.postRunnable(()->{ onPlayerJoin.playerJoin(pName,cNum);});

            System.out.println(receivedPacket.getAddress().toString()+receivedPacket.getPort());

            return  PacketUtils.
                    createPacket(receivedPacket.getAddress(),receivedPacket.getPort()
                    ,"Type","AckHandShake",
                    "SetClient#",c.getClientNumber()+"",
                            "Sequence#",sequenceNumber.getAndIncrement()+"");

        }catch (FullServerException e){

            return PacketUtils.
                    createPacket(receivedPacket.getAddress(),receivedPacket.getPort()
                    ,"Type","RefuseHandShake",
                            "Sequence#",sequenceNumber.getAndIncrement()+"",
                            "Data","Party is already full");
        }

    }


    private int generateClientNumber() throws FullServerException {

        ArrayList<Integer> nums=new ArrayList<>(4);
        nums.add(1);
        nums.add(2);
        nums.add(3);
        nums.add(4);

        for (Client c:clients) {
            Object i= c.getClientNumber();
            nums.remove(i);
        }

        if (nums.size()==0){
            throw new FullServerException("Server is full");
        }

        return nums.get((int)(nums.size()*Math.random()) );

    }

    private boolean isPacketOfType(String packetType,String message){

        HashMap<String,String> map=getMessageServerParameters(message);
        String packetActualType=map.get("Type");
        if (packetActualType!=null && packetActualType.equalsIgnoreCase(packetType)){
            return true;
        }
        return false;
    }


    private HashMap<String,String> getMessageServerParameters(String message){


        HashMap<String,String> map =new HashMap();

        for (String s: message.split("\n")) {


            String [] paramData=s.split(": ");
            String type=paramData[0];
            if (type.equalsIgnoreCase("Type") ||
                    type.equalsIgnoreCase("Client#") ||
                    type.equalsIgnoreCase("Sequence#") ||
                    type.equalsIgnoreCase("Data") ||
                    type.equalsIgnoreCase("Dupe") ||
                    type.equalsIgnoreCase("Name") ||
                    type.equalsIgnoreCase("DeltaTime"))
            {
                map.put(paramData[0],paramData[1]);
            }else{
                //unrecognized parameter TODO
                //System.out.println(type+" is an invalid parameter");
                continue;
            }

        }

        return map;
    }


    public BlockingQueue<Pair<ServerInstruction,Object>> instructionQueue=new LinkedBlockingQueue<>();


    public interface GameStartCallback{

        void gameStart(int ghostPlayerID);

        void gameStartFail(String reason);

    }
    private GameStartCallback onGameStart;

    public void setOnGameStart(GameStartCallback onGameStart) {
        this.onGameStart = onGameStart;
    }

    public void interruptSenderThreadAndPostInstruction(ServerInstruction instruction,Object value){
        instructionQueue.add(new Pair<>(instruction,value));
        ServerSenderThread.interrupt();
    }


    int sendSample=0;
    final class ServerSender implements Runnable{

        private void broadcastMessageToAll(String ...  keyVal) throws IOException{

            boolean flag=GhostHunters.getGameInstance().showSentPackets.get();

            DatagramPacket p=null;
            boolean repeat=keyVal[1].equalsIgnoreCase("StartGame") || keyVal[1].equalsIgnoreCase("EndGame");
            for (Client c:clients) {
                InetSocketAddress address= (InetSocketAddress) c.getSocketAddress();
                 p=PacketUtils.createPacket(address.getAddress(),c.getPort(),keyVal);
                socket.send(p);
            }

            if (repeat){
                if (flag && p!=null){
                    DatagramPacket s=p;
                    Gdx.app.postRunnable(()->PacketUtils.prettyPrint(GhostHunters.getGameInstance().con,s));
                }
                for (Client c:clients) {
                    InetSocketAddress address= (InetSocketAddress) c.getSocketAddress();
                    p=PacketUtils.createPacket(address.getAddress(),c.getPort(),keyVal);
                    socket.send(p);
                }

                if (flag && p!=null){
                    DatagramPacket s=p;
                    Gdx.app.postRunnable(()->PacketUtils.prettyPrint(GhostHunters.getGameInstance().con,s));
                }
            }else{

                if (flag && p!=null && sendSample>=60){
                    DatagramPacket s=p;
                    Gdx.app.postRunnable(()->PacketUtils.prettyPrint(GhostHunters.getGameInstance().con,s));
                    sendSample=0;
                }
                sendSample++;
            }

        }

        private int getNextGhost(){

            ArrayList<Integer> randomArray=new ArrayList<>();

            randomArray.add(0);
            for (Client c:clients){
                randomArray.add(c.getClientNumber());
            }

            int i=(int)(Math.random()*randomArray.size());
            return randomArray.get(i);
        }


        @Override
        public void run()
        {
            while(true) {

                GhostHunters.AppState state=GhostHunters.getAppState();

                try {

                    Pair<ServerInstruction,Object> ins=instructionQueue.poll();

                    if (state== GhostHunters.AppState.MATCH_MAKING){
                        if (ins!=null && ins.getObj1().equals(ServerInstruction.START_GAME)) {

                            if (clients.size()+1>=GhostHunters.getGameInstance().minPlayers.get()){
                                GhostHunters.setAppState(GhostHunters.AppState.IN_GAME);
                                int ghostNumber=getNextGhost();
                                Gdx.app.postRunnable(()->onGameStart.gameStart(ghostNumber));
                                broadcastMessageToAll(
                                        "Type","StartGame"
                                        ,"Sequence#",sequenceNumber.getAndIncrement()+""
                                        ,"SetNewGhost",ghostNumber+"");
                                continue;
                           }else{
                               onGameStart.gameStartFail("The game requires "+GhostHunters.getGameInstance().minPlayers +" players or more");
                           }

                        }
                        Thread.sleep(500);//half a second sleep
                        ArrayList<String> matchmakingState=new ArrayList();
                        matchmakingState.add("Server:"+getServerName());
                        for (Client c:clients){
                            matchmakingState.add("Client"+c.getClientNumber()+":"+c.getName());
                        }
                        Json json=new Json();
                        String message=json.toJson(matchmakingState).replaceAll("\\00+","");

                        if (clients.size()!=0) {
                            broadcastMessageToAll("Type", "Matchmaking",
                                    "Sequence#", sequenceNumber.getAndIncrement() + ""
                                    , "Data", message);
                        }

                    }else if (state== GhostHunters.AppState.IN_GAME){

                        if (ins!=null && ins.getObj1()==ServerInstruction.STATE){

                            Json json=new Json();
                            String message=json.toJson(ins.getObj2(),State.class).replaceAll("\\00+","");

                            if (clients.size()!=0) {
                                broadcastMessageToAll(
                                        "Type", "State",
                                        "Sequence#", sequenceNumber.getAndIncrement() + "",
                                        "Data", message);
                            }
                        }

                        if (ins!=null && ins.getObj1()==ServerInstruction.END_GAME){
                            Json json=new Json();
                            String message=json.toJson(ins.getObj2(),EndGameObject.class).replaceAll("\\00+","");
                            if (clients.size()!=0) {
                                broadcastMessageToAll(
                                        "Type", "EndGame",
                                        "Sequence#", sequenceNumber.getAndIncrement() + "",
                                        "Data", message);
                            }
                        }

                    }

                }catch (InterruptedException e){
                    System.out.println("Interrupted");
                } catch (IOException e) {
                    e.printStackTrace();
                }




            }

        }
    }







}
