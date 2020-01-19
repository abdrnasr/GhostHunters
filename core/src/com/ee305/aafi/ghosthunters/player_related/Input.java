package com.ee305.aafi.ghosthunters.player_related;

    public class Input              {
        public float deltaTime;
        public boolean up;
        public boolean down;
        public boolean left;
        public boolean right;
        public boolean shiftPressed;
        public int numKeyPressed;

        public Input(Player p,float deltaTime) {
            this.deltaTime = deltaTime;
            this.up = p.moveUp;
            this.down = p.moveDown;
            this.left = p.moveLeft;
            this.right = p.moveRight;
            this.numKeyPressed=p.keysPressed();
            this.shiftPressed=p.shiftPressed;

        }

        public Input(){

        }



}
