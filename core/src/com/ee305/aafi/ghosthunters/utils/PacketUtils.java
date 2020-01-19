package com.ee305.aafi.ghosthunters.utils;

import com.ee305.aafi.ghosthunters.ui_components.Console;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class PacketUtils {


    public static DatagramPacket createPacket(InetAddress targetAddress,int port, String ...  keyVal ){

        StringBuilder sb=new StringBuilder();
        for (int i=0; i<keyVal.length;i=i+2){

            sb.append(keyVal[i]).
                    append(": ").
                    append(keyVal[i+1]).
                    append("\n");

        }

        byte[] data=sb.toString().getBytes(StandardCharsets.US_ASCII);

        return new DatagramPacket(data,data.length,targetAddress,port);

    }

    public static void prettyPrint(Console c,DatagramPacket dp){
        String s=new String(dp.getData());
        c.log(s.replaceAll("\00",""));

    }






}
