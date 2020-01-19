package com.ee305.aafi.ghosthunters.player_related;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.ee305.aafi.ghosthunters.DynamicEntity;
import com.ee305.aafi.ghosthunters.GhostHunters;
import com.ee305.aafi.ghosthunters.network_related.State;
import com.ee305.aafi.ghosthunters.ui_components.ProgressBar;

public class Player extends DynamicEntity {


    boolean moveUp=false;
    boolean moveDown=false;
    boolean moveLeft=false;
    boolean moveRight=false;

    float hp = 100;
    float batteryLevel=100;

    public int deadHunters = 0;

    public boolean shiftPressed=false;
    public float getBatteryLevel() {
        return batteryLevel;
    }

    private ProgressBar healthBar;

    public void setHealthBar(ProgressBar healthBar) {
        this.healthBar = healthBar;
    }


    public void setHp(float hp) {
        this.hp = hp;
    }

    public boolean isMoveDown() {
        return moveDown;
    }

    public boolean isMoveUp() {
        return moveUp;
    }

    public boolean isMoveLeft() {
        return moveLeft;
    }

    public boolean isMoveRight() {
        return moveRight;
    }

    public void updateState(State.PlayerState playerState){
        setX(playerState.x);
        setY(playerState.y);
        if (GhostHunters.getGameInstance().currentControlledPlayer!=this) {
            moveUp = playerState.up;
            moveDown = playerState.down;
            moveLeft = playerState.left;
            moveRight = playerState.right;
            shiftPressed=playerState.shiftPressed;
        }

        hp=playerState.health;

    }

    private float lastRecievedInput=0.0f;

    public void updateByInput(com.ee305.aafi.ghosthunters.player_related.Input input){
        moveUp=input.up;
        moveDown=input.down;
        moveLeft=input.left;
        moveRight=input.right;
        lastRecievedInput=0.0f;
        shiftPressed=input.shiftPressed;

    }
    boolean delayRender=false;
    String name;

    public String getName() {
        return name;
    }

    public Player(int x, int y, String id, WorldEntityContext entityContext, Texture entityTexture) {
        super(x, y, id, entityContext, entityTexture);

    }

    private int playerID;
    private ProgressBar attachedHealthBar;

    private Player(int x, int y, String id, WorldEntityContext entityContext, Texture entityTexture,float scale,int pID,String name) {
        super(x, y, id, entityContext, entityTexture,scale);
        playerID=pID;
    }

    public Player(int x, int y, String id, WorldEntityContext entityContext, Texture entityTexture,float scale,int pID,boolean delayRender,String name) {
        this(x, y, id, entityContext, entityTexture,scale,pID,name);
        this.delayRender=delayRender;
        this.name=name;

    }

    public void setAttachedHealthBar(ProgressBar attachedHealthBar) {
        this.attachedHealthBar = attachedHealthBar;
    }

    public void getDamaged(float deltaTime){}

    public int getPlayerID() {
        return playerID;
    }

    int keysPressed(){
        int n=0;
        if (moveUp){
            n++;
        }
        if (moveDown){
            n++;
        }
        if (moveRight){
            n++;
        }
        if (moveLeft){
            n++;
        }

        return n;
    }

    float movementSpeed=130;
    @Override
    public boolean onKeyPressed(int key) {

        switch (key){
            case Input.Keys.W:
                moveUp= true;
                break;
            case Input.Keys.S:
                moveDown= true;
                break;
            case Input.Keys.D:
                moveRight= true;
                break;
            case Input.Keys.A:
                moveLeft= true;
                break;
        }

        return false;
    }

    private void moveBy(float xDelta,float yDelta){

        if(sprite.getX()<0){
            sprite.setX(0);
        }
        if(sprite.getX()>(1600)){
            sprite.setX(1600);
        }
        if(sprite.getY()<0){
            sprite.setY(0);
        }
        if(sprite.getY()>900){
            sprite.setY(900);
        }

        sprite.setX(sprite.getX() + xDelta);
        sprite.setY(sprite.getY() + yDelta);


        if(GhostHunters.getGameInstance().isCollidingDynamicEntity(this)){
            sprite.setX(sprite.getX() - xDelta);
            sprite.setY(sprite.getY() - yDelta);
        }

    }

    @Override
    public void tick(float deltaTime) {

        lastRecievedInput=lastRecievedInput+deltaTime;

        if (attachedHealthBar!=null){
            attachedHealthBar.setProgress(((float)hp)/100);
        }

        float newMove=deltaTime*movementSpeed;
        if (moveUp){
            moveBy(0,+newMove);
        }

        if (moveDown){
            moveBy(0,-newMove);
        }

        if (moveRight){
            moveBy(+newMove,0);
        }

        if (moveLeft){
            moveBy(-newMove,0);
        }

        if (!delayRender) {
            sprite.draw(GhostHunters.getGameInstance().getBatch());
        }
        if (healthBar!=null){
            healthBar.setProgress(hp/100);
        }

    }

    @Override
    public WorldEntityContext getContext() {
        return entityContext;
    }


    @Override
    public boolean onKeyReleased(int key) {

        switch (key){
            case Input.Keys.W:
                moveUp= false;
                break;
            case Input.Keys.S:
                moveDown= false;
                break;
            case Input.Keys.D:
                moveRight= false;
                break;
            case Input.Keys.A:
                moveLeft= false;
                break;
        }
        return false;
    }

    @Override
    public void dispose() {

    }

    public float getHp() {
        return hp;
    }
}
