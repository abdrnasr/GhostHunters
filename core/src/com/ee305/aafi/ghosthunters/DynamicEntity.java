package com.ee305.aafi.ghosthunters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

public abstract class DynamicEntity extends WorldEntity implements KeyboardInputReceiver {

    public Sprite sprite;
    boolean isCollidable=true;
    public DynamicEntity(int x, int y, String id, WorldEntityContext entityContext, Texture entityTexture) {
        super(x, y, entityTexture.getWidth(), entityTexture.getHeight(), id, entityContext);
        this.sprite=new Sprite(entityTexture,width,height);
        sprite.setBounds(x,y,width,height);
        sprite.setX(x);
        sprite.setY(y);

    }

    public DynamicEntity(int x, int y, String id, WorldEntityContext entityContext, Texture entityTexture,float scale) {
        super(x, y, entityTexture.getWidth(), entityTexture.getHeight(), id, entityContext);
        this.sprite=new Sprite(entityTexture);
        sprite.setOrigin(width/2,height/2);
        sprite.setX(x);
        sprite.setY(y);
        sprite.setScale(scale);

    }

    @Override
    public void setX(int x) {
        sprite.setX(x);
    }

    @Override
    public void setY(int y) {
        sprite.setY(y);
    }

    @Override
    public int getX() {
        return (int)this.sprite.getX();
    }

    @Override
    public int getY() {
        return (int)this.sprite.getY();
    }

    public boolean isOverlapping(DynamicEntity otherEntity){
        if (!isCollidable || !otherEntity.isCollidable){
            return false;
        }

        return this.sprite.getBoundingRectangle().overlaps(otherEntity.sprite.getBoundingRectangle());
    }

    @Override
    public void dispose() {
       //TODO
    }

    @Override
    public abstract void tick(float deltaTime);

    @Override
    public abstract boolean onKeyPressed(int key);

    @Override
    public abstract boolean onKeyReleased(int key);
}
