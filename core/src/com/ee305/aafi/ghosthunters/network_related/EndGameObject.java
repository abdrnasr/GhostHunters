package com.ee305.aafi.ghosthunters.network_related;

public class EndGameObject {


    public boolean ghostWinner;
    public boolean replay;

    public EndGameObject() {

    }

    public EndGameObject(boolean ghostWinner, boolean replay) {
        this.ghostWinner = ghostWinner;
        this.replay = replay;
    }
}
