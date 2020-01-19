package com.ee305.aafi.ghosthunters.ui_components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.ee305.aafi.ghosthunters.GhostHunters;

public class Label extends UIEntity {


    private String text;
    private BitmapFont font;
    private int containtMinHeight;
    private int containtMinWidth;
    private int textX;
    private int textY;

    public void setBackgroundColor(Color backgroundColor) {
        Pixmap px=new Pixmap(this.width,this.height, Pixmap.Format.RGBA8888);
        px.setColor(backgroundColor);
        px.fill();
        texture=new Texture(px);
        px.dispose();

    }

    public void setTextColor(Color textColor) {
        font.setColor(textColor);
    }


    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public Label(int x, int y, int width, int height, String id, WorldEntityContext entityContext, String text, int size, int fontNum) {
        super(x,y,width,height,id,entityContext);


        String path=null;
        if (fontNum==1){
            path="kimberley bl.ttf";
        }else if (fontNum==2){
            path="lunchds.ttf";
        }else if (fontNum==3){
            path="yoster.ttf";
        }else if(fontNum==4){
            path="CONSOLA.TTF";
        }

        FreeTypeFontGenerator generator=new FreeTypeFontGenerator(Gdx.files.internal(path));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter=new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size=size;
        font=generator.generateFont(parameter);

        this.text=text;

        GlyphLayout layout = new GlyphLayout();
        layout.setText(font,text);
        containtMinWidth = (int)layout.width;// contains the width of the current set text
        containtMinHeight= (int)layout.height; // contains the height of the current set text

        if (width<containtMinWidth){
            this.width=containtMinWidth;
        }

        setBackgroundColor(Color.BLACK);

        font.setColor(Color.WHITE);
        centerElement();

        generator.dispose();

    }



    public boolean isVisible=true;
    public void centerElement(){
        int displacementFromTop=(height-containtMinHeight)/2;
        textX=getX()+(width/2)-containtMinWidth/2;
        textY=getY()+height-displacementFromTop;
    }


    @Override
    public void tick(float deltaTIme) {

        if(isVisible) {
            SpriteBatch b = GhostHunters.getGameInstance().getBatch();
            b.draw(texture, getX(), getY(), width, height);
            font.draw(b, text, textX, textY);
        }

    }

    @Override
    public WorldEntityContext getContext() {
        return entityContext;
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
