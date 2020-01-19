package com.ee305.aafi.ghosthunters.network_related;

import java.net.SocketAddress;

public class Client {

    public int getClientNumber() {
        return cNum;
    }

    private int cNum;
    private int lastReceivedSequenceNumber;
    private long lastReceivedMessageTime;
    private int port;
    private String name;

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public int getLastReceivedSequenceNumber() {
        return lastReceivedSequenceNumber;
    }

    public void setLastReceivedSequenceNumber(int lastReceivedSequenceNumber) {
        this.lastReceivedSequenceNumber = lastReceivedSequenceNumber;
    }

    public long getLastRecievedMessageTime() {
        return lastReceivedMessageTime;
    }

    public void setLastRecievedMessageTime(long lastReceivedMessageTime) {
        this.lastReceivedMessageTime = lastReceivedMessageTime;
    }

    public SocketAddress getSocketAddress() {
        return address;
    }

    private SocketAddress address;


    private Client(SocketAddress address,int clientNumber,int port,String name){
        this.address=address;
        this.cNum=clientNumber;
        this.port=port;
        this.name=name;

    }

    public static Client createNewClient(SocketAddress address,int clientNumber,int clientPortNumber,String name)  {

        return new Client(address,clientNumber,clientPortNumber,name);

    }




}

 class FullServerException extends Exception{

    FullServerException(String message){
        super(message);
    }

}