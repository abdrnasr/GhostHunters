package com.ee305.aafi.ghosthunters.player_related;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.ee305.aafi.ghosthunters.DynamicEntity;
import com.ee305.aafi.ghosthunters.GhostHunters;
import com.ee305.aafi.ghosthunters.network_related.State;

public class Ghost extends Player {

    public boolean isVulnerable = true;

    public Ghost(int x, int y, String id, WorldEntityContext entityContext, Texture entityTexture, float scale, int pID,String name) {
        super(x, y, id, entityContext, entityTexture, scale, pID,true,name);
        movementSpeed = 200;
    }
    float damagePerSecond=20;
    private boolean isVisible=false;


    private boolean shouldGetDamaged=false;

    @Override
    public void updateState(State.PlayerState playerState) {
        if (GhostHunters.getGameInstance().currentControlledPlayer!=this) {
            shiftPressed = playerState.shiftPressed;
        }
        super.updateState(playerState);
    }

    @Override
    public void updateByInput(com.ee305.aafi.ghosthunters.player_related.Input input) {
        super.updateByInput(input);

    }

    public boolean onKeyPressed(int key) {
        super.onKeyPressed(key);
        switch(key){
            case Input.Keys.SHIFT_LEFT:
                shiftPressed=true;
        }
        return false;
    }


    public boolean onKeyReleased(int key) {
        super.onKeyReleased(key);
        switch(key){
            case Input.Keys.SHIFT_LEFT:
                shiftPressed=false;
        }
        return false;
    }

    public void getDamaged(float deltaTime){

        shouldGetDamaged=true;


    }

    public void tick(float deltaTime) {
        super.tick(deltaTime);
        GhostHunters gh = GhostHunters.getGameInstance();

        if (this.hp==0){
            sprite.setRotation(90);
        }

        if (isVisible ||gh.currentControlledPlayer==this){
            sprite.draw(gh.getBatch());
        }

        if (shouldGetDamaged){
            if(this.hp>0){
                this.hp =this.hp- damagePerSecond * deltaTime;
                movementSpeed = 40;
            } else if(this.hp == 0) {
                //set to dead
                movementSpeed = 0;
            }
            else if(this.hp < 0){
                this.hp = 0;
            }
        }else{
            movementSpeed = 200;
        }

        if(hp != 0) {

            if (shiftPressed) {
                isVulnerable = false;
                sprite.setColor(Color.GRAY);
            }

            if (isGhostFarFromHunters() && !shiftPressed) {
                isVulnerable = true;
                sprite.setColor(Color.WHITE);
            }

            isVisible=false;
            DynamicEntity collide = gh.isCollidingPlayerEntity(this);
            if (collide instanceof Hunter) {
                isVisible=true;
                if (isVulnerable) {
                    ((Hunter) collide).getDamaged(deltaTime);
                }
            }


            shouldGetDamaged=false;
        }



    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    boolean isGhostFarFromHunters(){
        GhostHunters gh=GhostHunters.getGameInstance();
        for (Player e: gh.playersEntities){
            if(e instanceof Hunter)
            {
                if( Math.abs(e.sprite.getBoundingRectangle().getCenter(new Vector2()).sub(this.sprite.getBoundingRectangle().getCenter(new Vector2())).len()) < 150)
                {
                    return false;
                }
            }
        }

        return true;
    }
}
