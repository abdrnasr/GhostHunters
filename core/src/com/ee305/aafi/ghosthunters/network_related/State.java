package com.ee305.aafi.ghosthunters.network_related;

import com.ee305.aafi.ghosthunters.player_related.Ghost;
import com.ee305.aafi.ghosthunters.player_related.Hunter;
import com.ee305.aafi.ghosthunters.player_related.Player;

import java.util.ArrayList;

public class State {


    public ArrayList<PlayerState> objectsState=new ArrayList<>();

    public State(){

    }

    public State(ArrayList<Player> players){

        for (Player player:players) {
            float angle=0;
            if (player instanceof Hunter){
                angle=((Hunter) player).getCurrentAngle();
            }
            boolean shiftPressed=player.shiftPressed;


            objectsState.add(new PlayerState(player.isMoveUp(),player.isMoveDown(),player.isMoveRight(),player.isMoveLeft(),
                    player.getHp(),player.getX(),player.getY(),player.getPlayerID(),angle,shiftPressed,player.getBatteryLevel()));

        }

    }

    public ArrayList<PlayerState> getObjectsState() {
        return objectsState;
    }

    public static class PlayerState {
        public boolean up;
        public boolean down;
        public boolean right;
        public boolean left;
        public float health;
        public int x;
        public int y;
        public int id;
        public float currentAngle;
        public boolean shiftPressed;
        public float battery;

        public PlayerState() {

        }

        public PlayerState(boolean up, boolean down, boolean right, boolean left, float health, int x, int y, int id, float currentAngle,boolean shiftPressed,float battery) {
            this.up = up;
            this.down = down;
            this.right = right;
            this.left = left;
            this.health = health;
            this.x = x;
            this.y = y;
            this.id = id;
            this.currentAngle = currentAngle;
            this.shiftPressed=shiftPressed;
            this.battery=battery;
        }
    }



}

