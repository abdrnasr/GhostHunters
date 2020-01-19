package com.ee305.aafi.ghosthunters.ui_components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.ee305.aafi.ghosthunters.GhostHunters;
import com.ee305.aafi.ghosthunters.MouseInputReceiver;
import com.ee305.aafi.ghosthunters.ui_components.UIEntity;

public class PressableText extends UIEntity implements MouseInputReceiver {


    private BitmapFont font;
    private String text;
    private Color textCurrentColor;
    private boolean isBeingPressed=false;
    private Runnable onClickSuccessful;
    private int containtMinHeight;
    private int containtMinWidth;
    private int textX;
    private int textY;
    private boolean centerAlignText;

    public PressableText(int x, int y, int width, int height, String id, WorldEntityContext entityContext,String text,Runnable onClickSuccessful ,boolean centerAlignText) {
        super(x, y, width, height, id, entityContext);
        this.centerAlignText=centerAlignText;
        this.text=text;
        font=new BitmapFont(Gdx.files.internal("GameText.fnt"));
        font.setColor(Color.BLACK);
        this.onClickSuccessful=onClickSuccessful;

        Pixmap px=new Pixmap(width,height, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        texture=new Texture(px);


        if (centerAlignText){
            GlyphLayout layout = new GlyphLayout();
            layout.setText(font,text);
            containtMinWidth= (int)layout.width;// contains the width of the current set text
            containtMinHeight = (int)layout.height;
            centerElement();
        }else{
            textX=getX()+8;
            textY=getY()+(int)font.getLineHeight();
        }



        px.dispose();

    }

    public void setText(String text) {
        this.text = text;

        if (centerAlignText){
            GlyphLayout layout = new GlyphLayout();
            layout.setText(font,text);
            containtMinWidth= (int)layout.width;// contains the width of the current set text
            containtMinHeight = (int)layout.height;
            centerElement();
        }else{
            textX=getX()+8;
            textY=getY()+(int)font.getLineHeight();
        }


    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {

        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (isInBoundaries(screenX,screenY)) {
            isBeingPressed=true;
            font.setColor(Color.GOLD);
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (isBeingPressed){
            if (onClickSuccessful!=null) {
                onClickSuccessful.run();
            }
            isBeingPressed=false;
        }
        font.setColor(Color.BLACK);
        return false;
    }
    @Override
    public void tick(float deltaTime){

        SpriteBatch b= GhostHunters.getGameInstance().getBatch();
        b.draw(texture,getX(),getY());
        font.draw(b,text,textX,textY);
    }

    @Override
    public void resize(int newScreenX, int newScreenY) {

        centerElement();
    }

    private void centerElement(){
        int displacementFromTop=(height-containtMinHeight)/2;
        textX=getX()+(width/2)-containtMinWidth/2;
        textY=getY()+height-displacementFromTop;
    }

    @Override
    public WorldEntityContext getContext() {
        return this.entityContext;
    }

    @Override
    public void dispose() {
        texture.dispose();
        font.dispose();
    }
}
