package com.ee305.aafi.ghosthunters.player_related;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.ee305.aafi.ghosthunters.DynamicEntity;
import com.ee305.aafi.ghosthunters.GhostHunters;
import com.ee305.aafi.ghosthunters.network_related.State;

public class Hunter extends Player {


    int rotationSpeed =300; //degree/second
    float currentAngle=40;

    float lightRadius=150;
    static int numberOfRays=25;
    float lightAngularSize=40;


    public Hunter(int x, int y, String id, WorldEntityContext entityContext, Texture entityTexture, float scale, int pID,String name) {
        super(x, y, id, entityContext, entityTexture, scale, pID,false,name);
    }

    @Override
    public void updateState(State.PlayerState playerState) {
        currentAngle=playerState.currentAngle;
        batteryLevel=playerState.battery;
        super.updateState(playerState);
    }

    public boolean onKeyPressed(int key) {
        super.onKeyPressed(key);
        switch(key){
            case Input.Keys.SHIFT_LEFT:
                if(this.shiftPressed)
                    this.shiftPressed = false;
                else
                    this.shiftPressed=true;
        }
        return false;
    }

    public float getCurrentAngle() {
        return currentAngle;
    }

    @Override
    public void tick(float deltaTime) {
        super.tick(deltaTime);
        //rotate
        float targetAngle= currentAngle;
        int numberOfKeysPressed=keysPressed();
        if (numberOfKeysPressed<=2 && numberOfKeysPressed!=0){

            if (numberOfKeysPressed==1){

                if (moveRight){
                    if (currentAngle>180){
                        targetAngle=360;
                    }else {
                        targetAngle=0;
                    }

                }else if (moveLeft){
                    targetAngle = 180;
                }
                else if (moveUp){
                    targetAngle = 90;
                }
                else if (moveDown){
                    targetAngle = 270;
                }


                float netDisplacement=
                        MathUtils.clamp(targetAngle-currentAngle ,-rotationSpeed *deltaTime, rotationSpeed *deltaTime);


                if (currentAngle<=90 && currentAngle>=0 && (targetAngle==0 || targetAngle==270)){
                    netDisplacement=-Math.abs(netDisplacement);

                }else if (currentAngle<=360 && currentAngle>=270 && (targetAngle==360 || targetAngle==90)){
                    netDisplacement=Math.abs(netDisplacement);

                }

                currentAngle=netDisplacement+currentAngle;

                if (currentAngle<0){
                    currentAngle=currentAngle+360;
                }

                if (currentAngle>=360){
                    currentAngle=currentAngle-360;
                }


            }else{

            }

        }

        drainBattery(deltaTime);
        //ray trace
        if(this.batteryLevel > 0 && this.shiftPressed && hp > 0) {
            rayTrace(deltaTime);
        }else{

            Vector2 rayReference=new Vector2(1,0);
            rayReference.setAngle(currentAngle);
            rayReference.setLength(25);
            Vector2 playerPosition=(new Vector2(getX(),getY())).add(width/2,height/2);
            GhostHunters gh=GhostHunters.getGameInstance();
            gh.getBatch().end();

            rayReference=rayReference.cpy().add(playerPosition);
            gh.renderer.begin(ShapeRenderer.ShapeType.Filled);
            gh.renderer.setProjectionMatrix(GhostHunters.getGameInstance().cam.combined);
            gh.renderer.setColor(Color.GREEN);
            gh.renderer.rectLine(playerPosition,rayReference,3);

            gh.renderer.end();
            gh.getBatch().begin();
        }
        //adjust light textures

    }

    private void rayTrace(float deltaTime){

        Vector2 rayReference=new Vector2(1,0);
        rayReference.setAngle(currentAngle);
        Vector2 playerPosition=(new Vector2(getX(),getY())).add(width/2,height/2);
        GhostHunters gh=GhostHunters.getGameInstance();
        gh.getBatch().end();

        Vector2 [] currentRays=new Vector2[numberOfRays];
        int numberOfSideRays=((numberOfRays-1)/2);
        float rayAngularDisplacement=(lightAngularSize)/numberOfSideRays;
        currentRays[0]=rayReference;
        for (int i=1;i/2 <numberOfSideRays;i=i+2){
            int factor=(i/2)+1;
            currentRays[i]=rayReference.cpy().rotate(factor*rayAngularDisplacement);
            currentRays[i+1]=rayReference.cpy().rotate(-factor*rayAngularDisplacement);
        }

        for (Vector2 vec:currentRays) {

            while (!isRayCollidingWithEntity(playerPosition.cpy().add(vec),deltaTime)){
                vec.setLength(vec.len()+1);
                if (vec.len()>=lightRadius){
                    break;
                }
            }
        }

        gh.renderer.setProjectionMatrix(GhostHunters.getGameInstance().cam.combined);
        gh.renderer.begin(ShapeRenderer.ShapeType.Filled);
        gh.renderer.setColor(1, 1, 0, 1);


        for (int i=0;i<currentRays.length;i++){
            Vector2 vec=currentRays[i];
            if (currentRays.length-1==i){
                gh.renderer.arc(playerPosition.x,playerPosition.y,vec.len(),vec.angle(),1+(rayAngularDisplacement/2) );
            }else if (currentRays.length-2==i){
                gh.renderer.arc(playerPosition.x,playerPosition.y,vec.len(),vec.angle(),-1-(rayAngularDisplacement/2));
            }else{
                gh.renderer.arc(playerPosition.x,playerPosition.y,vec.len(),vec.angle(),1+(rayAngularDisplacement/2));
                gh.renderer.arc(playerPosition.x,playerPosition.y,vec.len(),vec.angle(),-1-(rayAngularDisplacement/2));
            }

        }

        gh.renderer.end();
        gh.getBatch().begin();
    }

    boolean isRayCollidingWithEntity(Vector2 end,float deltaTime){
        GhostHunters gh=GhostHunters.getGameInstance();

        for (DynamicEntity e: gh.dynamicEntities){

            if(!e.identifier.equalsIgnoreCase(this.identifier) && e.sprite.getBoundingRectangle().contains(end))
            {
                if(e instanceof Ghost ){
                    ((Ghost) e).setVisible(true);
                    if (((Ghost) e).isVulnerable == true) {
                        ((Ghost) e).getDamaged(deltaTime);
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void getDamaged(float deltaTime){
        this.hp = 0;
        //set to dead
        this.sprite.setRotation(90);
        this.movementSpeed = 0;
        deadHunters++;
    }

    public void drainBattery(float deltaTime){
        if(this.batteryLevel > 0 && this.shiftPressed){
            this.batteryLevel = this.batteryLevel - 15 * deltaTime;
        }else if(!this.shiftPressed && this.batteryLevel < 100){
            this.batteryLevel = this.batteryLevel + 20 * deltaTime;
        }else{
            this.shiftPressed = false;
        }
    }

}
