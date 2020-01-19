package com.ee305.aafi.ghosthunters.ui_components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.ee305.aafi.ghosthunters.GhostHunters;


public class Console extends TextInput {
    public interface CommandSubmit{

        void textSubmit(String command);
    }


    Texture cursor;
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        if (isFocused){
            if (isInBoundaries(screenX,screenY)){
                isFocused=false;
                isAlted=false;
            }
        }else{
            if (isInBoundaries(screenX,screenY)){
                isFocused=true;
                isAlted=false;
            }else{
                isFocused=false;
                isAlted=false;
            }
        }



        return false;
    }

    public boolean isInBoundaries(int testX, int testY){


        if (getX()<=testX && testX<=getX()+width){
            if (getY()+(isFocused?-700:0)<=testY && testY<=getY()+(isFocused?focusedTexture.getHeight():height)){
                return true;
            }
        }

        return false;
    }

    Label l;
    public Console(int x, int y, int width, int height, String id, WorldEntityContext entityContext, int textLimit, CommandSubmit sub) {
        super(x,y,width,height,id,entityContext,textLimit,createFont(),makeBackground(width,height),makeFocusedBackground(width,height));
        this.sub=sub;
        l=new Label(1300,120,100,100,"ConsoleLabel",WorldEntityContext.ALL,"View only",20,4);
        l.setBackgroundColor(Color.CLEAR);
        l.setTextColor(Color.GREEN);
        Pixmap px= new Pixmap(2,10, Pixmap.Format.RGBA8888);
        px.setColor(Color.GOLD);
        px.fill();
        cursor=new Texture(px);
        px.dispose();
        currentText="";

    }
    CommandSubmit sub;

    static Texture makeBackground(int width, int height){
        Pixmap px=new Pixmap(width,height, Pixmap.Format.RGBA8888);
        px.setColor(Color.valueOf("111E6C"));
        px.fill();
        Texture texture=new Texture(px);
        px.dispose();
        return texture;
    }

    static Texture makeFocusedBackground(int width, int height){
        Pixmap px=new Pixmap(width,900, Pixmap.Format.RGBA8888);
        Color c=Color.valueOf("4c516d");
        c.a=0.5f;
        px.setColor(c);
        px.fill();
        drawBorders(px,Color.valueOf("0bb1b0"),width,900);
        Texture texture=new Texture(px);
        px.dispose();
        return texture;
    }

    static void drawBorders(Pixmap px,Color fillColor,int width,int height){
        int borderSize=4;
        px.setColor(fillColor);
        px.fillRectangle(0,0,width,borderSize);
        px.fillRectangle(0,height-borderSize,width,height);
        px.fillRectangle(0,0,borderSize,height);
        px.fillRectangle(width-borderSize,0,width,height);

    }
    float flickerSum =0.5f;
    float deltaSum=0.0f;


    private String buffer="";
    @Override
    public void tick(float deltaTime) {

        SpriteBatch b= GhostHunters.getGameInstance().getBatch();
        flickerSum = flickerSum +deltaTime;



        if (isFocused){
            b.draw(focusedTexture, getX(), getY()-700);
        }else {
            b.draw(texture, getX(), getY());
        }

        if (isAlted){
            l.tick(deltaTime);
        }

        String allText=currentText+"\n"+buffer;

        if (buffer.isEmpty()){

        }


        GlyphLayout layout=new GlyphLayout(font,buffer);

        GlyphLayout layout2=new GlyphLayout();

        layout2.setText(font,allText,Color.WHITE,width-20,10,true);
        float currentHeight=layout2.height;
        int maxHeight=60;
        if (isFocused) {
            maxHeight=750;
        }

        while (layout2.height>maxHeight){
            allText=allText.substring(allText.indexOf("\n")+1);

            layout2.setText(font,allText,Color.WHITE,width-20,10,true);
        }

        GlyphLayout layout1=font.draw(b, allText, getX() + 8, getY() +height+10, 0, allText.length(),width-20 ,10,true);

        if (flickerSum >1) {
            flickerSum = 0;
        }else if (cursor!=null && flickerSum >0.5){
            b.draw(cursor, getX() + 10 + layout.width, getY() + height+10  - layout1.height , cursor.getWidth(), cursor.getHeight());
        }

        if (!isFocused || !isEditable || isAlted) return;
        if (currentCommand== Command.REPLICATE) {
            accumulateHoldTime = accumulateHoldTime + deltaTime;
            deltaSum=deltaTime+deltaSum;
            if (accumulateHoldTime > 0.500 && deltaSum>=0.02) {
                if (letter=="" || letter=="del") {
                    truncateText();
                }else if (letter!="\n")
                {
                    addLetter();
                }
                deltaSum=0.0f;

            }
        }

    }

    static BitmapFont createFont(){
        FreeTypeFontGenerator generator=new FreeTypeFontGenerator(Gdx.files.internal("CONSOLA.TTF"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter=new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size=13;
        BitmapFont f=generator.generateFont(parameter);
        f.setColor(Color.WHITE);
        return f;
    }

    boolean isAlted=false;

    @Override
    public boolean onKeyPressed(int key) {

        accumulateHoldTime =0;

        if (key == Input.Keys.ALT_RIGHT) {
            isFocused=!isFocused;
            if (!isFocused){
                isAlted=false;
            }else {
                isAlted=!isAlted;
            }
            return false;
        }

        if (!isFocused || isAlted) return false;


        if (key == Input.Keys.ENTER) {
            letter="\n";
            currentText=currentText+"\n"+buffer;
            sub.textSubmit(buffer);

            buffer="";
        }else if (key == Input.Keys.SPACE) {
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

    public void log(String str){

        if (currentText.length()>=3000){
            currentText="";
        }
        currentText=currentText+"\n"+str;

    }

    @Override
    public boolean onKeyReleased(int key) {
        currentCommand=Command.NOTHING;

        return false;
    }

    protected void truncateText(){
        if (buffer.length()==0){
            return;
        }
        buffer= buffer.substring(0,buffer.length()-1);
    }

    private void addLetter() {
        if (buffer.length()==54){
            currentText=currentText+"\n"+buffer;
            buffer="";
        }else {
            buffer = buffer + letter;
        }
    }



}
