package com.ee305.aafi.ghosthunters;

public interface KeyboardInputReceiver {

    boolean onKeyPressed(int key);

    boolean onKeyReleased(int key);

    void tick(float deltaTime);

    WorldEntity.WorldEntityContext  getContext();
}
