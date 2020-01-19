package com.ee305.aafi.ghosthunters;

public interface MouseInputReceiver {

    boolean mouseMoved(int screenX, int screenY) ;

    boolean touchDown(int screenX, int screenY, int pointer, int button);

    boolean touchUp(int screenX, int screenY, int pointer, int button) ;

    void tick(float deltaTime);

    WorldEntity.WorldEntityContext  getContext();

}
