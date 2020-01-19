package com.ee305.aafi.ghosthunters.ui_components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.ee305.aafi.ghosthunters.GhostHunters;
import com.ee305.aafi.ghosthunters.player_related.Ghost;
import com.ee305.aafi.ghosthunters.player_related.Hunter;
import com.ee305.aafi.ghosthunters.player_related.Player;

public class PlayerDetails extends UIEntity {

    BitmapFont drawer;
    ProgressBar healthBar;
    ProgressBar batteryBar;
    Texture background;
    private int topY;
    private int rightX;
    Player p;
    Texture headImg;

    public PlayerDetails(int x, int y, Player associatedPlayer,WorldEntityContext context) {
        super(x, y,200,900-825, null, context);
        headImg = GhostHunters.getGameInstance().playerHeadsTextures.get(associatedPlayer.getPlayerID());

        FreeTypeFontGenerator generator=new FreeTypeFontGenerator(Gdx.files.internal("CONSOLA.TTF"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter=new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size=15;
        drawer=generator.generateFont(parameter);
        generator.dispose();
        rightX= width+x;
        topY=height+y;
        p=associatedPlayer;

        drawer.setColor(Color.WHITE);
        Pixmap px=new Pixmap(width,height,Pixmap.Format.RGB888);
        px.setColor(Color.valueOf("4d80e4"));
        px.fill();
        background=new Texture(px);

        healthBar=new ProgressBar(x+80,y+20,100,15,associatedPlayer!=null?associatedPlayer.getPlayerID()+"Details":"laterSet",context,0,Color.RED);
        batteryBar=new ProgressBar(x+80,y,100,15,associatedPlayer!=null?associatedPlayer.getPlayerID()+"Details":"laterSet",context,0,Color.GREEN);

    }

    public void setPlayer(Player p) {
        this.p = p;

        batteryBar.identifier=p.getPlayerID()+"BatteryBar";
        headImg=GhostHunters.getGameInstance().playerHeadsTextures.get(p.getPlayerID());
        healthBar.identifier= p.getPlayerID()+"HealthBar";
        identifier=p.getPlayerID()+"Details";

    }

    @Override
    public void tick(float deltaTime) {
        GhostHunters gh=GhostHunters.getGameInstance();

        if (p !=null) {
            gh.getBatch().draw(background, getX(), getY(), width, height);
            drawer.draw(gh.getBatch(), p.getName(), getX() + 80, topY - 13);
            drawer.draw(gh.getBatch(), "Health", getX() + 10, topY - 40);
            healthBar.setProgress(p.getHp()/100);
            healthBar.tick(deltaTime);



            if (p instanceof Hunter ) {
                if (p.getHp()==0) {
                    gh.getBatch().draw(gh.playerHeadsTextures.get(6), getX() + 20, topY - 5 - gh.playerHeadsTextures.get(6).getHeight() * 1.5f,
                            gh.playerHeadsTextures.get(6).getWidth() * 1.5f,
                            gh.playerHeadsTextures.get(6).getHeight() * 1.5f);
                }else{
                    gh.getBatch().draw(headImg, getX() + 20, topY - 5 - headImg.getHeight() * 1.5f, headImg.getWidth() * 1.5f,
                            headImg.getHeight() * 1.5f);
                }
                drawer.draw(gh.getBatch(), "Battery", getX() + 10, topY - 60);
                batteryBar.setProgress(p.getBatteryLevel()/100);
                batteryBar.tick(deltaTime);

            }else if (p instanceof Ghost){

                gh.getBatch().draw(gh.playerHeadsTextures.get(5), getX() + 20, topY - 5 - gh.playerHeadsTextures.get(5).getHeight() * 1.5f,
                        gh.playerHeadsTextures.get(5).getWidth() * 1.5f,
                        gh.playerHeadsTextures.get(5).getHeight() * 1.5f);

            }

        }


    }

    @Override
    public WorldEntityContext getContext() {
        return entityContext;
    }

    @Override
    public void dispose() {
        batteryBar.dispose();
        healthBar.dispose();
        drawer.dispose();
        background.dispose();
    }
}
