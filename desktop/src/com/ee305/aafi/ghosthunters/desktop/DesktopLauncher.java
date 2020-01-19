package com.ee305.aafi.ghosthunters.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import com.ee305.aafi.ghosthunters.GhostHunters;
public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title="Ghost hunters game";
		config.height=900;
		config.width=1600;
		new LwjglApplication(new GhostHunters(), config);
	}
}
