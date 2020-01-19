package com.ee305.aafi.ghosthunters.ui_components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.ee305.aafi.ghosthunters.GhostHunters;
import com.ee305.aafi.ghosthunters.KeyboardInputReceiver;

public class TextInput extends UIEntity implements KeyboardInputReceiver {


    BitmapFont font;
    protected volatile String currentText="...";
    int textLimit;
    protected String letter;
    Command currentCommand=Command.NOTHING;
    float accumulateHoldTime =0.0f;
    Texture focusedTexture;

    public void setCurrentText(String currentText) {
        this.currentText = currentText;
    }

    public boolean isEditable=true;


    enum Command{
        NOTHING,REPLICATE
    }



    public TextInput(int x, int y,int width,int height, String id, WorldEntityContext entityContext, int textLimit) {
        super(x, y, width,height, id, entityContext);
        font=new BitmapFont(Gdx.files.internal("GameText.fnt"));
        this.textLimit=textLimit;
        font.setColor(Color.BLACK);

        Pixmap px=new Pixmap(width,height, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        drawBorders(5,px,Color.GRAY);
        texture=new Texture(px);
        px.dispose();
        px=new Pixmap(width,height, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        drawBorders(5,px,Color.GOLD);
        focusedTexture=new Texture(px);
        px.dispose();
    }

    public TextInput(int x, int y,int width,int height, String id, WorldEntityContext entityContext, int textLimit,
                     BitmapFont font,Texture texture, Texture focusedTexture) {
        super(x, y, width,height, id, entityContext);

        this.font=font;
        this.textLimit=textLimit;
        this.texture=texture;
        this.focusedTexture=focusedTexture;

    }

      void drawBorders(int borderSize,Pixmap px,Color fillColor){
        px.setColor(fillColor);
        px.fillRectangle(0,0,width,borderSize);
        px.fillRectangle(0,height-borderSize,width,height);
        px.fillRectangle(0,0,borderSize,height);
        px.fillRectangle(width-borderSize,0,width,height);

    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        if (isInBoundaries(screenX,screenY)){
            isFocused=true;

        }else{
            isFocused=false;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return super.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean onKeyPressed(int key) {

        if(!isEditable){
            return false;
        }

        accumulateHoldTime =0;
        if (!isFocused) return false;


        if (key == Input.Keys.SPACE) {
            letter= " ";
            addLetter();
        }else if (key == Input.Keys.BACKSPACE) {
            letter="";
            truncateText();
        }else {
            String iLetter= Input.Keys.toString(key);
            if (iLetter.length()==1){
                letter=iLetter;
                addLetter();
            }
        }
        currentCommand=Command.REPLICATE;

        return false;
    }

    private boolean topLeft=false;
    @Override
    public boolean onKeyReleased(int key) {
        currentCommand=Command.NOTHING;

        return false;
    }

    protected void truncateText(){
        if (currentText.length()==0){
            return;
        }
        currentText= currentText.substring(0,currentText.length()-1);
    }

    private void addLetter() {
        if (textLimit == currentText.length()) {
            return;
        }
            currentText = currentText + letter;

    }

    float deltaSum=0.0f;
    @Override
    public void tick(float deltaTime) {

        SpriteBatch b= GhostHunters.getGameInstance().getBatch();

        font.draw(b, currentText, getX() + 8, getY() + font.getLineHeight());
        if (isFocused){
            b.draw(focusedTexture, getX(), getY());
        }else{
            b.draw(texture,getX(),getY());
        }
        font.draw(b,currentText,getX()+5,getY()+font.getLineHeight());

        if (!isFocused || !isEditable) return;
        if (currentCommand== Command.REPLICATE) {
            accumulateHoldTime = accumulateHoldTime + deltaTime;
            deltaSum=deltaTime+deltaSum;
            if (accumulateHoldTime > 0.500 && deltaSum>=0.02) {
                if (letter=="" || letter=="del") {
                    truncateText();
                }else
                {
                    addLetter();
                }
                deltaSum=0.0f;

            }
        }

    }


    @Override
    public WorldEntityContext getContext() {
        return entityContext;
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
    }

    public String getCurrentText() {
        return currentText;
    }
}
