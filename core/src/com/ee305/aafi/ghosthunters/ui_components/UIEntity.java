package com.ee305.aafi.ghosthunters.ui_components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.ee305.aafi.ghosthunters.MouseInputReceiver;
import com.ee305.aafi.ghosthunters.WorldEntity;

public abstract class UIEntity extends WorldEntity implements MouseInputReceiver {

    Texture texture;
    private Runnable onClickFinish;
    private Runnable onHover;
    private Runnable onHoverExit;
    private Runnable onClickStart;
    boolean isBeingPressed;

    public boolean mouseMoved(int screenX, int screenY) {
        if (isInBoundaries(screenX,screenY)){
            if (onHover!=null) {
                onHover.run();
            }
        }
        return false;
    }

    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (isInBoundaries(screenX,screenY)) {
            isBeingPressed=true;
            isFocused=true;
            if (onClickStart!=null) {
                onClickStart.run();
            }
        }else {
            isFocused=false;
        }

        return false;
    }

    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (isBeingPressed){
            if (onClickFinish!=null){
                onClickFinish.run();
            }
            isBeingPressed=false;
        }
        return false;
    }

    public UIEntity(int x, int y, String id, WorldEntityContext entityContext, Texture texture) {

        super(x, y,texture.getWidth(),texture.getHeight(), id, entityContext);
        this.texture=texture;
    }
    public UIEntity(int x, int y,int width, int height, String id, WorldEntityContext entityContext) {
        super(x, y,width,height, id, entityContext);
    }

    public UIEntity(int x, int y,int width,int height, String id, WorldEntityContext entityContext, Texture texture) {

        super(x, y,width,height, id, entityContext);
        this.texture=texture;
    }

    public Runnable getOnClickFinish() {
        return onClickFinish;
    }

    public void setOnClickFinish(Runnable onClickFinish) {
        this.onClickFinish = onClickFinish;
    }

    public Runnable getOnHover() {
        return onHover;
    }

    public void setOnHover(Runnable onHover) {
        this.onHover = onHover;
    }

    public Runnable getOnClickStart() {
        return onClickStart;
    }

    public void setOnClickStart(Runnable onClickStart) {
        this.onClickStart = onClickStart;
    }
    public abstract void tick(float deltaTIme);

    public Runnable getOnHoverExit() {
        return onHoverExit;
    }

    public void setOnHoverExit(Runnable onHoverExit) {
        this.onHoverExit = onHoverExit;
    }

    @Override
    public void dispose() {
        if (texture!=null){
            texture.dispose();
        }
    }
}
