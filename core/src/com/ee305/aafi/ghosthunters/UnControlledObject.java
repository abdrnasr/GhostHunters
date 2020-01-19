package com.ee305.aafi.ghosthunters;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.ee305.aafi.ghosthunters.ui_components.Label;

public class UnControlledObject extends DynamicEntity {

    private Label textureLabel;
    private int playerID;
    public UnControlledObject(int x, int y, String id, WorldEntityContext entityContext, Texture entityTexture, String label,int pID) {
        super(x, y, id, entityContext, entityTexture,3);

        textureLabel=new Label(x,y-height-25,width,height,id+"_label",entityContext,label,30,3);
        textureLabel.setBackgroundColor(Color.CLEAR);
        textureLabel.setTextColor(Color.MAGENTA);
        isCollidable=false;

        playerID=pID;

        int displacementX=(width-textureLabel.width)/2;
        x= x + displacementX;

        textureLabel.setX(x);
        textureLabel.centerElement();

    }

    public int getPlayerID() {
        return playerID;
    }

    public String getText() {
        return textureLabel.getText();
    }

    @Override
    public void tick(float deltaTime) {

        SpriteBatch b=GhostHunters.getGameInstance().getBatch();
        sprite.draw(b);
        textureLabel.tick(deltaTime);

    }

    @Override
    public WorldEntityContext getContext() {
        return getContext();
    }

    @Override
    public boolean onKeyPressed(int key) {
        return false;
    }

    @Override
    public boolean onKeyReleased(int key) {
        return false;
    }
}
