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

public class Button extends UIEntity implements MouseInputReceiver {


    private String text;
    private BitmapFont font;

    private int textX;
    private int textY;

    private int containtMinWidth;
    private int containtMinHeight;
    public Button(int x, int y, int width, int height, String id, WorldEntityContext entityContext,String text,boolean fit) {
        super(x, y, width, height,id,entityContext);
        this.text=text;
        font=new BitmapFont(Gdx.files.internal("MainButtonsFont90.fnt"));
        font.setColor(renderTex);



        GlyphLayout layout = new GlyphLayout();
        layout.setText(font,text);
        containtMinWidth= (int)layout.width;// contains the width of the current set text
        containtMinHeight = (int)layout.height;

        if (fit){
            width=containtMinWidth+10;
            height=containtMinHeight+20;
        }


        Pixmap px=new Pixmap(width,height, Pixmap.Format.RGBA8888);
        px.setColor(defaultBackground);
        px.fill();
        texture=new Texture(px);
        this.width=width;
        this.height=height;
        px.dispose();

    }

    private void centerElement(){
        int displacementFromTop=(height-containtMinHeight)/2;
        textX=getX()+(width/2)-containtMinWidth/2;
        textY=getY()+height-displacementFromTop;
    }

    private Color renderTex=Color.WHITE;
    private Color hoverColorTex=Color.GOLD;
    private Color defaultColorTex=Color.WHITE;
    private Color clickedColorTex=Color.GOLDENROD;

    private Color hoverBackground =Color.BLACK;
    private Color defaultBackground=Color.CLEAR;
    private Color clickedBackground=Color.BLACK;

    public void setTextProperties(Color hover,Color defaultC,Color clicked){
        if (hover!=null){
            hoverColorTex=hover;
        }

        if (defaultC!=null){
            defaultColorTex=defaultC;
        }

        if (clicked!=null){
            clickedColorTex=clicked;
        }
    }


    @Override
    public boolean mouseMoved(int screenX, int screenY) {

        if (isBeingPressed){
            return false;
        }

        if (isInBoundaries(screenX,screenY)){
            if (getOnHover()!=null){
                getOnHover().run();
            }

            renderTex=hoverColorTex;
        }else {
            if (getOnHoverExit()!=null){
                getOnHoverExit().run();
            }

            renderTex=defaultColorTex;
        }

        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (isInBoundaries(screenX,screenY)){

            isBeingPressed=true;
            if (getOnClickStart()!=null){
                getOnClickStart().run();
            }
            renderTex=clickedColorTex;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button)
    {
        if (isBeingPressed){
            isBeingPressed=false;
            if (getOnClickFinish()!=null){
                getOnClickFinish().run();
            }

            renderTex=defaultColorTex;
        }

        return false;
    }

    @Override
    public void tick(float deltaTime) {

        SpriteBatch b= GhostHunters.getGameInstance().getBatch();
        b.draw(texture,getX(),getY());
        font.setColor(renderTex);
        font.draw(b,text,textX,textY);
    }
    @Override
    public WorldEntityContext getContext() {
        return this.entityContext;
    }

    @Override
    public void resize(int newScreenX, int newScreenY) {
        super.resize(newScreenX, newScreenY);
        //Todo rescale on resize
        centerElement();
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
    }
}
