package com.ee305.aafi.ghosthunters.ui_components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.ee305.aafi.ghosthunters.GhostHunters;

public class ProgressBar extends UIEntity {

    private float progress=0;
    private int padding=3;
    private Texture progressBar;
    int  innerBoxHeight;
    int innerBoxWidth;

    public ProgressBar(int x, int y, int width, int height, String id, WorldEntityContext entityContext, float initialProgress, Color progressColor) {
        super(x, y, width, height, id, entityContext);
        progress=initialProgress;
        innerBoxHeight=height-2*padding;
        innerBoxWidth=width-2*padding;
        Pixmap px=new Pixmap(innerBoxWidth,innerBoxHeight,Pixmap.Format.RGBA8888);
        px.setColor(progressColor);
        px.fill();
        progressBar=new Texture(px);
        px.dispose();

        px=new Pixmap(width,height,Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        texture=new Texture(px);
        px.dispose();
    }


    public void setProgress(float newProgress){
        progress=newProgress;
    }

    @Override
    public void tick(float deltaTIme) {

        SpriteBatch b=GhostHunters.getGameInstance().getBatch();

        b.draw(texture,getX(),getY(),width,height);
        b.draw(progressBar,getX()+padding,getY()+padding,progress*innerBoxWidth,innerBoxHeight);
    }

    @Override
    public WorldEntityContext getContext() {
        return entityContext;
    }

    @Override
    public void dispose() {
        texture.dispose();
        progressBar.dispose();
    }
}
