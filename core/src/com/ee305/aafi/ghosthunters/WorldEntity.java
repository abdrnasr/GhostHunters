package com.ee305.aafi.ghosthunters;

public abstract class WorldEntity {

    public enum WorldEntityContext{
        MAIN_MENU,
        CLIENT_HOST_MENU,
        MATCH_MAKING,
        IN_GAME,
        ALL

    }

    protected boolean isFocused=false;
    public String identifier;
    protected WorldEntityContext entityContext;
    private int x;
    private int y;
    protected int width;
    protected int height;

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public WorldEntity(int x, int y, int width, int height, String id, WorldEntityContext entityContext) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.identifier =id;
        this.entityContext=entityContext;

    }

    public boolean isInBoundaries(int testX, int testY){

        if (getX()<=testX && testX<=getX()+width){
            if (getY()<=testY && testY<=getY()+height){
                return true;
            }
        }

        return false;
    }

    public abstract void dispose();

    public void resize(int newScreenX,int newScreenY){


    }
}

