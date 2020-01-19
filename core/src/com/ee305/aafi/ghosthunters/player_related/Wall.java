package com.ee305.aafi.ghosthunters.player_related;

import com.badlogic.gdx.graphics.Texture;
import com.ee305.aafi.ghosthunters.DynamicEntity;
import com.ee305.aafi.ghosthunters.GhostHunters;

public class Wall extends DynamicEntity {
    public Wall(int x, int y, String id, WorldEntityContext entityContext, Texture entityTexture){
        super(x, y, id, entityContext, entityTexture);
    }

    public void tick(float deltaTime){
        sprite.draw(GhostHunters.getGameInstance().getBatch());
    }

    public boolean onKeyPressed(int key){
        return false;
    }

    public boolean onKeyReleased(int key){
        return false;
    }

    public WorldEntityContext getContext() {
        return entityContext;
    }

}
