package com.ee305.aafi.ghosthunters;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Timer;
import com.ee305.aafi.ghosthunters.network_related.ClientNetworkNode;
import com.ee305.aafi.ghosthunters.network_related.EndGameObject;
import com.ee305.aafi.ghosthunters.network_related.ServerNetworkNode;
import com.ee305.aafi.ghosthunters.network_related.State;
import com.ee305.aafi.ghosthunters.player_related.Ghost;
import com.ee305.aafi.ghosthunters.player_related.Hunter;
import com.ee305.aafi.ghosthunters.player_related.Input;
import com.ee305.aafi.ghosthunters.player_related.Player;
import com.ee305.aafi.ghosthunters.player_related.Wall;
import com.ee305.aafi.ghosthunters.ui_components.Button;
import com.ee305.aafi.ghosthunters.ui_components.Console;
import com.ee305.aafi.ghosthunters.ui_components.Label;
import com.ee305.aafi.ghosthunters.ui_components.PlayerDetails;
import com.ee305.aafi.ghosthunters.ui_components.PressableText;
import com.ee305.aafi.ghosthunters.ui_components.TextInput;
import com.ee305.aafi.ghosthunters.ui_components.UIEntity;
import com.ee305.aafi.ghosthunters.utils.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GhostHunters extends ApplicationAdapter {


	enum CommandType{
		SERVER,CLIENT,BOTH
	}

	HashMap<Integer, Pair<Integer,Integer>> matchmakingLocations =new HashMap<>();
	HashMap<Integer, Pair<Integer,Integer>> playerStartLocations =new HashMap<>();
	HashMap<Integer, Pair<Integer,Integer>> healthBarlocations =new HashMap<>();

	static int screenSizeX=1600;
	static int screenSizeY=900;


	private SpriteBatch batch;

	public SpriteBatch getBatch() {
		return batch;
	}

	private Texture mainBackground;
	private Texture hostJoinTexture;
	private Texture inGameBackground;

	public OrthographicCamera cam;
	public ShapeRenderer renderer;

	private Label errorLabel;

	public ServerNetworkNode server;
	public ClientNetworkNode client;

	public ArrayList<KeyboardInputReceiver> registeredKeyboardEntities =new ArrayList<>();
	public ArrayList<WorldEntity> entities=new ArrayList<>();
	public ArrayList<DynamicEntity> dynamicEntities=new ArrayList<>();
	public ArrayList<Player> playersEntities=new ArrayList<>();

	public ArrayList<UnControlledObject> matchmakingQueue=new ArrayList<>(5);
	public ArrayList<MouseInputReceiver> registeredMouseEntities=new ArrayList<>();
	private ArrayList<PlayerDetails> pdArray =new ArrayList<>();


	private Input getCurrentPlayerInput(float deltaTime){

		return new com.ee305.aafi.ghosthunters.player_related.Input(currentControlledPlayer,deltaTime);

	}
	public Player currentControlledPlayer;


	public void reportError(String error){

		errorLabel.setText("Error: "+error);
	}

	public void reportInfo(String info){

		errorLabel.setText("Info: "+info);
	}

	private <T extends WorldEntity> T queryWorldEntityByID(WorldEntity.WorldEntityContext entityContext
			, String id, Class<? extends T> t){

		for (WorldEntity e: entities){
			if (e.identifier.equalsIgnoreCase(id) && t.isInstance(e) && entityContext.equals(e.entityContext)){
				return (T)e;
			}
		}
		return null;
	}

	private <T extends WorldEntity> T getFirstOverlappingWorldEntityByContext(WorldEntity.WorldEntityContext entityContext
			, int x, int y, Class<? extends T> t){

		for (WorldEntity e: entities){
			if (e.isInBoundaries(x,y) && t.isInstance(e) && entityContext.equals(e.entityContext)){
				return (T)e;
			}
		}
		return null;
	}

	private static volatile AppState appState = AppState.MAIN_MENU;
	private static volatile WorldEntity.WorldEntityContext worldContext = WorldEntity.WorldEntityContext.MAIN_MENU;
	private static GhostHunters gameInstance;

	public ArrayList<Texture> playersTexture=new ArrayList<>();
	public ArrayList<Texture> playerHeadsTextures=new ArrayList<>();


	public static GhostHunters getGameInstance() {
		return gameInstance;
	}

	public enum  AppState{
		MAIN_MENU,
		MATCH_MAKING,
		IN_GAME
	}


	private boolean isHost;

	private State getGameState(){

		return new State(playersEntities);
	}

	public volatile Console con;

	private int joinCounts=0;

	//-----------------------------------------------
	//COMMAND LINE OPTIONS FLAGS AND VALUES
	public volatile AtomicBoolean showSentPackets=new AtomicBoolean(false);
	public volatile AtomicBoolean showRecievedPackets=new AtomicBoolean(false);
	public volatile AtomicBoolean blockServerUpdates=new AtomicBoolean(false);
	public volatile AtomicInteger minPlayers =new AtomicInteger(2);
	public volatile AtomicInteger serverPort=new AtomicInteger(2305);
	public volatile AtomicInteger clientPort=new AtomicInteger(0);
	public volatile AtomicBoolean sendInput=new AtomicBoolean(true);

	//-----------------------------------------------



	private Label gameFinishLabel;
	@Override
	public void create () {
		gameInstance=this;
		cam = new OrthographicCamera(1600,900);
		cam.translate(800,450);
		cam.update();
		renderer=new ShapeRenderer();
		batch=new SpriteBatch();

		mainBackground =new Texture(Gdx.files.internal("Main_Menu_V3.png"));
		hostJoinTexture=new Texture(Gdx.files.internal("Host_or_Join_game_page.png"));
		inGameBackground=new Texture(Gdx.files.internal("stage_outline_with_spaces.png"));

		matchmakingLocations.put(0,new Pair<>(775,660));
		matchmakingLocations.put(1,new Pair<>(485,510));
		matchmakingLocations.put(2,new Pair<>(550,300));
		matchmakingLocations.put(3,new Pair<>(990,300));
		matchmakingLocations.put(4,new Pair<>(1065,510));

		playerStartLocations.put(0,new Pair<>(840,380));
		playerStartLocations.put(1,new Pair<>(160,100));
		playerStartLocations.put(2,new Pair<>(1540,100));
		playerStartLocations.put(3,new Pair<>(300,700));
		playerStartLocations.put(4,new Pair<>(1540,700));

		playersTexture.add(new Texture("GhostHunter_Screen.png"));//THE SERVER CHARACTER
		playersTexture.add(new Texture("GhostHunter_Orange_Screen.png"));//Client 1 CHARACTER
		playersTexture.add(new Texture("GhostHunter_Pink_Screen.png"));//Client 2 CHARACTER
		playersTexture.add(new Texture("GhostHunter_Green_Screen.png"));//Client 3 CHARACTER
		playersTexture.add(new Texture("GhostHunter_Blue_Screen.png"));//Client 4 CHARACTER
		playersTexture.add(new Texture("Ghost.png"));//Ghost CHARACTER
		playersTexture.add(new Texture("GhostHunter_Gray_Screen.png"));//Dead CHARACTER

		//walls
		createWalls();

		playerHeadsTextures.add(new Texture("GhostHunter_Screen_Head.png"));
        playerHeadsTextures.add(new Texture("GhostHunter_Orange_Head.png"));
        playerHeadsTextures.add(new Texture("GhostHunter_Pink_Head.png"));
        playerHeadsTextures.add(new Texture("GhostHunter_Green_Head.png"));
        playerHeadsTextures.add(new Texture("GhostHunter_Blue_Head.png"));
        playerHeadsTextures.add(new Texture("Ghost_Head.png"));
        playerHeadsTextures.add(new Texture("GhostHunter_Gray_Head.png"));

		PressableText startDiscGameButton=new PressableText(700,0,200,60,"startDiscGameButton",
				WorldEntity.WorldEntityContext.MATCH_MAKING,"Start Game", ()->{
			if (isHost && server !=null) {
				if (joinCounts + 1 == matchmakingQueue.size()){
					server.interruptSenderThreadAndPostInstruction(ServerNetworkNode.ServerInstruction.START_GAME, "");
				}else{
					reportError("Please enter your name");
				}
			}else if(!isHost && client!=null){
				if (client.isConnected.get()){
					client.interruptAndSend(ClientNetworkNode.ClientInstruction.DISCONNECT,null);
					entities.removeAll(matchmakingQueue);
					matchmakingQueue.clear();
				}
			}
		},true);



		Button playButton=new Button(620,300,200,100, "PlayButton",WorldEntity.WorldEntityContext.MAIN_MENU,"Play game",true);
		Button exitButton=new Button(720,115,200,80, "ExitButton",WorldEntity.WorldEntityContext.MAIN_MENU,"Exit",true);
		Button hostButton=new Button(600,300,200,150,"HostButton", WorldEntity.WorldEntityContext.CLIENT_HOST_MENU,"Host game",true);
		Button joinButton=new Button(600,120,100,60,"JoinButton", WorldEntity.WorldEntityContext.CLIENT_HOST_MENU,"Join game",true);

		playButton.setOnClickFinish(()-> {
			worldContext=WorldEntity.WorldEntityContext.CLIENT_HOST_MENU;
		});

		exitButton.setOnClickFinish(()-> {
			Gdx.app.exit();
		});

		hostButton.setOnClickFinish(()->{
			startDiscGameButton.setText("Start Game");
			isHost=true;
			worldContext= WorldEntity.WorldEntityContext.MATCH_MAKING;
			appState=AppState.MATCH_MAKING;
			server=new ServerNetworkNode();

			server.setOnPlayerDisconnect( (name,id) -> {
				joinCounts--;
				for (UnControlledObject object:matchmakingQueue) {
					if (object.getPlayerID()==id){
						matchmakingQueue.remove(object);
						entities.remove(object);
						reportInfo(name+" has joined the match ("+(joinCounts+1)+"/5)");
						break;
					}
				}

			} );
			server.setOnGameStart(new ServerNetworkNode.GameStartCallback() {
				@Override
				public void gameStart(int ghostPlayerID) {

					int i=1;
					int counter=0;
					for (UnControlledObject object: matchmakingQueue) {
						int id=object.getPlayerID();
						Player p;
						Pair<Integer,Integer> xy= playerStartLocations.get(i);
						if (ghostPlayerID==id){
							p=new Ghost(playerStartLocations.get(0).getObj1(),playerStartLocations.get(0).getObj2(),"Ghost_Replicated_"+id,
									WorldEntity.WorldEntityContext.IN_GAME,playersTexture.get(5),1.5f,id,object.getText());
						}else{
							p=new Hunter(xy.getObj1(),xy.getObj2(),"Hunter_Replicated_"+id, WorldEntity.WorldEntityContext.IN_GAME,playersTexture.get(id),1.5f,id,object.getText());
							i++;
						}

						PlayerDetails playerDetails=new PlayerDetails(100+counter*200,825,p, WorldEntity.WorldEntityContext.IN_GAME);
						pdArray.add(playerDetails);
						entities.add(playerDetails);

						if (id==0){
							//The server
							registeredKeyboardEntities.add(p);
							currentControlledPlayer=p;
						}
						entities.add(p);
						playersEntities.add(p);
						dynamicEntities.add(p);
						counter++;
					}

					entities.removeAll(matchmakingQueue);
					matchmakingQueue.clear();
					worldContext= WorldEntity.WorldEntityContext.IN_GAME;

				}

				@Override
				public void gameStartFail(String reason) {
					reportError(reason);
				}
			});

			server.setOnPlayerJoin((String name,int id) -> {
				joinCounts++;
				reportInfo(name+" has joined the match ("+(joinCounts+1)+"/5)");
				Pair<Integer,Integer> p= matchmakingLocations.get(id);
				String newName=name.toUpperCase();
				UnControlledObject waitObj=new UnControlledObject(p.getObj1(),p.getObj2(),"Client"+id,
				WorldEntity.WorldEntityContext.MATCH_MAKING,playersTexture.get(id),newName,id);
				matchmakingQueue.add(waitObj);
				entities.add(waitObj);

			});

			server.setOnReceivePlayerInput(( userInput,clientNumber) -> {

				for (Player p:playersEntities) {
					if (p.getPlayerID()==clientNumber){
						p.updateByInput(userInput);
					}
				}
			});

			try {
				InetAddress INet = InetAddress.getLocalHost();
				TextInput tex=queryWorldEntityByID(worldContext,"inputIP",TextInput.class);
				tex.isEditable=false;
				tex.setCurrentText(INet.getHostAddress());
			}catch (UnknownHostException e){

			}
		});


		joinButton.setOnClickFinish(()->{
			isHost=false;
			worldContext= WorldEntity.WorldEntityContext.MATCH_MAKING;
			appState=AppState.MATCH_MAKING;
			TextInput tex=queryWorldEntityByID(worldContext,"inputIP",TextInput.class);
			tex.isEditable=true;
			tex.setCurrentText("...");
			startDiscGameButton.setText("Disconnect");
		});

		entities.add(playButton);
		entities.add(exitButton);
		entities.add(hostButton);
		entities.add(joinButton);
		registeredMouseEntities.add(playButton);
		registeredMouseEntities.add(exitButton);
		registeredMouseEntities.add(hostButton);
		registeredMouseEntities.add(joinButton);
		InputMultiplexer inputMultiplexer = new InputMultiplexer();

		//for mouse and touch events
		inputMultiplexer.addProcessor(new InputAdapter(){

			@Override
			public boolean mouseMoved(int screenX, int screenY) {
				Vector3 vec=cam.unproject(new Vector3(screenX,screenY, 0));
				int newScreenY=Math.round(vec.y);
				screenX=Math.round(vec.x);

				for (MouseInputReceiver ent: registeredMouseEntities) {
					if (ent.getContext().equals(WorldEntity.WorldEntityContext.ALL)|| ent.getContext().equals(worldContext)){
						ent.mouseMoved(screenX,newScreenY);
					}
				}
				return true;
			}

			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				Vector3 vec=cam.unproject(new Vector3(screenX,screenY, 0));
				int newScreenY=Math.round(vec.y);
				screenX=Math.round(vec.x);

				for (MouseInputReceiver ent: registeredMouseEntities) {
					if (ent.getContext().equals(WorldEntity.WorldEntityContext.ALL)|| ent.getContext().equals(worldContext)){
						ent.touchDown(screenX,newScreenY,pointer,button);
					}
				}
				return true;
			}


			@Override
			public boolean touchUp(int screenX, int screenY, int pointer, int button) {
				Vector3 vec=cam.unproject(new Vector3(screenX,screenY, 0));
				int newScreenY=Math.round(vec.y);
				screenX=Math.round(vec.x);

				for (MouseInputReceiver ent: registeredMouseEntities) {
					if (ent.getContext().equals(WorldEntity.WorldEntityContext.ALL)|| ent.getContext().equals(worldContext)){
						ent.touchUp(screenX,newScreenY,pointer,button);
					}
				}

				return true;
			}
		});




		TextInput nameInput=new TextInput(650,770,300,60,"inputName",WorldEntity.WorldEntityContext.MATCH_MAKING,15);
		TextInput ipInput=new TextInput(650,830,300,60,"inputIP",WorldEntity.WorldEntityContext.MATCH_MAKING,15);

		PressableText enterButton=new PressableText(960,830,150,60,"enterButton",
				WorldEntity.WorldEntityContext.MATCH_MAKING,"Enter",()->{
			if (isHost){

				for (UnControlledObject ent:matchmakingQueue ) {
					if (ent.identifier.equalsIgnoreCase("0ReplicationWait")) {
						matchmakingQueue.remove(ent);
						entities.remove(ent);
						break;
					}
				}
				Pair<Integer,Integer> p= matchmakingLocations.get(0);
				UnControlledObject waitObj=new UnControlledObject(p.getObj1(),p.getObj2(),"0ReplicationWait",
						WorldEntity.WorldEntityContext.MATCH_MAKING,playersTexture.get(0),nameInput.getCurrentText(),0);
				matchmakingQueue.add(waitObj);
				entities.add(waitObj);
				server.setServerName(nameInput.getCurrentText());

			}else {
				if (client==null) {
					client = new ClientNetworkNode(new ClientNetworkNode.OnNewStateRecieved() {
						@Override
						public void onMatchmakingStateRecieved(ArrayList<String> list) {
							{
								entities.removeAll(matchmakingQueue);

								int prev=matchmakingQueue.size();
								matchmakingQueue.clear();

								if (prev==0){
									reportInfo("You have joined the server successfully("+list.size()+"/5)");
								}else if (prev!=list.size()){
									if (list.size()>prev){
										//new player joined
										reportInfo("A new player has joined the server("+list.size()+"/5)");
									}else{
										//player disconnected
										reportInfo("Someone has left the server("+list.size()+"/5)");
									}
								}
								int id=0;
								String name=null;
								for (String s: list){
									if (s.contains("Server")){
										id=0;
										name=s.split(":")[1];

									}else if (s.contains("Client")){
										id=Integer.parseInt(s.substring(6,7));
										name=s.split(":")[1];
									}
									Pair<Integer,Integer> xy= matchmakingLocations.get(id);

									UnControlledObject unControlledObject=new UnControlledObject(xy.getObj1(),xy.getObj2()
											,id+"ReplicationWait", WorldEntity.WorldEntityContext.MATCH_MAKING,playersTexture.get(id),name,id);

									matchmakingQueue.add(unControlledObject);
									entities.add(unControlledObject);

								}
							}
						}

						@Override
						public void onGameStart(int ghostID,int currentClientId) {


							int i=1;
							int counter=0;
							for (UnControlledObject object: matchmakingQueue) {

								int id=object.getPlayerID();
								Player p;
								Pair<Integer,Integer> xy= playerStartLocations.get(i);
								if (ghostID==id){
									p=new Ghost(playerStartLocations.get(0).getObj1(),playerStartLocations.get(0).getObj2(),"Ghost_Replicated_"+id,
											WorldEntity.WorldEntityContext.IN_GAME,playersTexture.get(5),1.5f,id,object.getText());
								}else{
									p=new Hunter(xy.getObj1(),xy.getObj2(),"Hunter_Replicated_"+id, WorldEntity.WorldEntityContext.IN_GAME,playersTexture.get(id),1.5f,id,object.getText());
									i++;
								}
								PlayerDetails playerDetails=new PlayerDetails(100+counter*200,825,p, WorldEntity.WorldEntityContext.IN_GAME);
								pdArray.add(playerDetails);
								entities.add(playerDetails);

								if (id==currentClientId){
									//The server
									registeredKeyboardEntities.add(p);
									currentControlledPlayer=p;
								}
								entities.add(p);
								playersEntities.add(p);
								dynamicEntities.add(p);
								counter++;
							}
							entities.removeAll(matchmakingQueue);
							matchmakingQueue.clear();

							setAppState(AppState.IN_GAME);
							worldContext= WorldEntity.WorldEntityContext.IN_GAME;

						}

						@Override
						public void onStateRecieved(State state) {
							for (Player s:playersEntities){

								for (State.PlayerState playerState:state.objectsState) {
									if (s.getPlayerID()==playerState.id){
										s.updateState(playerState);
									}
								}
							}

						}

						@Override
						public void endGameRecieved(boolean ghostWinner,boolean playAgain){

							announceWinner(ghostWinner,playAgain);
						}

					}
							, () -> {
						reportError("wrong ip");
					}
							, () -> {
						reportError("socket fail");
					}, ipInput.getCurrentText(), nameInput.getCurrentText()
					);
				}else{

					client.retryConnection(ipInput.getCurrentText(),nameInput.getCurrentText());

				}

			}
			  },true);

		Label labelIP=new Label(300,830,340,60,"IPlabel", WorldEntity.WorldEntityContext.MATCH_MAKING,"IP address:",50,1);
		Label labelName=new Label(300,770,340,60,"nameLabel", WorldEntity.WorldEntityContext.MATCH_MAKING,"Name:",50,1);
		Label labelErrorbar=new Label(20,20,100,30,"errorLabel", WorldEntity.WorldEntityContext.MATCH_MAKING,"Error:",15,4);
		labelErrorbar.setBackgroundColor(Color.CLEAR);

		Label gameFinishLabel=new Label(570,620,200,100,"GameFinish", WorldEntity.WorldEntityContext.IN_GAME,"",70,4);
		gameFinishLabel.setBackgroundColor(Color.CLEAR);
		gameFinishLabel.isVisible=true;

		errorLabel=labelErrorbar;
		this.gameFinishLabel=gameFinishLabel;


		entities.add(errorLabel);
		entities.add(gameFinishLabel);
		entities.add(startDiscGameButton);
		entities.add(ipInput);
		entities.add(labelIP);
		entities.add(labelName);
		entities.add(labelErrorbar);


		registeredMouseEntities.add(startDiscGameButton);

		registeredKeyboardEntities.add(ipInput);
		registeredMouseEntities.add(ipInput);

		entities.add(nameInput);
		registeredKeyboardEntities.add(nameInput);
		registeredMouseEntities.add(nameInput);


		entities.add(enterButton);
		registeredMouseEntities.add(enterButton);

		HashMap<String,CommandType> clientSeverCommand=new HashMap<>();
		clientSeverCommand.put("HEAL.ALL",CommandType.SERVER);
		clientSeverCommand.put("SHOW.SENT.PACKETS",CommandType.BOTH);
		clientSeverCommand.put("SHOW.REC.PACKETS",CommandType.BOTH);
		clientSeverCommand.put("BLOCK.SERVER.UPDATE",CommandType.CLIENT);
		clientSeverCommand.put("BLOCK.INPUT.SENDING",CommandType.CLIENT);
		clientSeverCommand.put("SET.MIN.PLAYERS",CommandType.SERVER);
		clientSeverCommand.put("DUMP",CommandType.BOTH);
		clientSeverCommand.put("PAUSE",CommandType.BOTH);
		clientSeverCommand.put("RESET",CommandType.BOTH);
		clientSeverCommand.put("SET.SERVER.PORT",CommandType.BOTH);
		clientSeverCommand.put("SET.CLIENT.PORT",CommandType.BOTH);
		clientSeverCommand.put("LIST",CommandType.BOTH);
		Console c=new Console(1150,850,400,50,"console", WorldEntity.WorldEntityContext.ALL,10000,
				(text)->{
					//TODO read commands from here;
					String [] args=text.split(" ");
					text=args[0];

					CommandType commandType=clientSeverCommand.get(text);

					if (commandType==null){
						con.log("Command '"+text+"' does not exist.");
						return;
					}

					if (text.equalsIgnoreCase("LIST") ){
						con.log("List of commands: ");
						for(Map.Entry<String, CommandType>  obj: clientSeverCommand.entrySet()){

							con.log(String.format("%-20s : %-15s",obj.getKey()
									,obj.getValue().toString()));

						}
						return;
					}

					if (text.equalsIgnoreCase("Heal.all") &&worldContext== WorldEntity.WorldEntityContext.IN_GAME){
						if (getRole().equals(commandType)){
							for (Player p:playersEntities){
									p.setHp(100);
							}
							con.log("All players were healed.");
						}else {
							con.log("Command '"+text+"' does not exist.");
						}
						return;
					}

					if (text.equalsIgnoreCase("BLOCK.SERVER.UPDATE") &&(worldContext== WorldEntity.WorldEntityContext.IN_GAME )){

						if (getRole().equals(commandType)){
							con.log("Blocking server updates...");
						}else{
							con.log("You cannot execute this command as "+getRole());
							return;
						}
						blockServerUpdates.set(true);
						return;
					}

					if (text.equalsIgnoreCase("SHOW.REC.PACKETS") &&(worldContext== WorldEntity.WorldEntityContext.IN_GAME ||
							worldContext== WorldEntity.WorldEntityContext.MATCH_MAKING)){
						con.log("Now showing recieved packets...");
						showRecievedPackets.set(true);
						return;
					}

					if (text.equalsIgnoreCase("SHOW.SENT.PACKETS") &&(worldContext== WorldEntity.WorldEntityContext.IN_GAME ||
							worldContext== WorldEntity.WorldEntityContext.MATCH_MAKING)){

						con.log("Now showing sent packets...");
						showSentPackets.set(true);
						return;
					}
					if (text.equalsIgnoreCase("DUMP")){

						showSentPackets.set(false);
						showRecievedPackets.set(false);

						Timer.schedule(new Timer.Task() {
							@Override
							public void run() {

								File f=new File("GhostHuntersLogFile.txt");
								if (f.exists()){
									f.delete();
								}
								try {
									f.createNewFile();
									FileWriter fileWriter = new FileWriter(f);
									PrintWriter printWriter = new PrintWriter(fileWriter);
									printWriter.println(con.getCurrentText());
									Gdx.app.postRunnable(()->{con.log("Created the log file"); });
									printWriter.close();
									fileWriter.close();
								}catch (IOException e){
									Gdx.app.postRunnable(()->{con.log("An error occurred please try again."); });
									return;
								}



							}
						},1);

						return;
					}
					if (text.equalsIgnoreCase("PAUSE")){
						con.log("Paused Recieved/Sent packets.");
						showSentPackets.set(false);
						showRecievedPackets.set(false);
						return;
					}

					if (text.equalsIgnoreCase("BLOCK.INPUT.SENDING")){
						con.log("Input packets are now disabled");
						sendInput.set(false);
						return;
					}


					if (text.equalsIgnoreCase("RESET")){
						con.log("Resat to default settings.");
						showSentPackets.set(false);
						showRecievedPackets.set(false);
						blockServerUpdates.set(false);
						sendInput.set(true);
						minPlayers.set(2);
						serverPort.set(2305);
						clientPort.set(0);
						return;
					}

					if (text.equalsIgnoreCase("SET.MIN.PLAYERS") ){

						if (args.length!=2){
							con.log("Choose the number of minimum players.");
							return;
						}
						int n;
						try {

							n=Integer.parseInt(args[1]);

						}catch (NumberFormatException e){
							con.log(args[1]+" is not a number.");
							return;
						}

						if (n<1 || 5<n){
							con.log("Choose a number of players that is between 1 and 5.");
							return;
						}
						minPlayers.set(n);

						con.log("Minimum players set to "+ n);
						return;
					}

					if (text.equalsIgnoreCase("SET.SERVER.PORT")){

						if (args.length!=2){
							con.log("Please specify the new server port.");
							return;
						}
						int n;
						try {

							n=Integer.parseInt(args[1]);

						}catch (NumberFormatException e){
							con.log(args[1]+" is not a number.");
							return;
						}
						serverPort.set(n);

						con.log("Server port was changed to "+serverPort);
						return;
					}

					if (text.equalsIgnoreCase("SET.CLIENT.PORT")){

						if (args.length!=2){
							con.log("Please specify the new server port.");
							return;
						}
						int n;
						try {

							n=Integer.parseInt(args[1]);

						}catch (NumberFormatException e){
							con.log(args[1]+" is not a number.");
							return;
						}
						clientPort.set(n);

						con.log("Client port was changed to "+clientPort);
						return;
					}

					con.log("Command '"+text+"' can not be used here.");
				});
		con=c;
		entities.add(c);
		registeredKeyboardEntities.add(c);
		registeredMouseEntities.add(c);
		//for keyboard events
		inputMultiplexer.addProcessor(new InputAdapter(){

			@Override
			public boolean keyDown(int keycode) {

				boolean flag=false;

				for (KeyboardInputReceiver kir: registeredKeyboardEntities){

					if (kir.getContext()==worldContext || kir.getContext()== WorldEntity.WorldEntityContext.ALL){
						if (kir.onKeyPressed(keycode)) {
							flag = true;
						}
					}

				}

				return flag;
			}

			@Override
			public boolean keyUp(int keycode) {

				boolean flag=false;

				for (KeyboardInputReceiver kir: registeredKeyboardEntities){
					if (kir.getContext()==worldContext || kir.getContext()== WorldEntity.WorldEntityContext.ALL) {
						if (kir.onKeyReleased(keycode)) {
							flag = true;
						}
					}
				}

				return flag;
			}

			@Override
			public boolean keyTyped(char character) {

				return false;
			}
		});

		Gdx.input.setInputProcessor(inputMultiplexer);
	}

	private CommandType getRole(){
		if (isHost){
			return CommandType.SERVER;
		}
		return CommandType.CLIENT;
	}

	private void announceWinner(boolean ghostWinner, boolean playAgain) {
		String message="Hunters Won";
		gameFinishLabel.setTextColor(Color.YELLOW);
		if (ghostWinner){
			message="Ghost Won";
			gameFinishLabel.setTextColor(Color.GRAY);
		}
		gameFinishLabel.setText(message);
		gameFinishLabel.isVisible=true;

		Timer.Task task=new Timer.Task() {
			@Override
			public void run() {
				Gdx.app.postRunnable(() ->{

					gameFinishLabel.isVisible=false;
					isEnded=false;


					for (Player p:playersEntities) {
						Pair<Integer,Integer> xy=matchmakingLocations.get(p.getPlayerID());
						int id=p.getPlayerID();
						UnControlledObject obj=new UnControlledObject(xy.getObj1(),xy.getObj2(),id+"ReplicationWait",
								WorldEntity.WorldEntityContext.MATCH_MAKING,playersTexture.get(id),p.getName(),id);
						matchmakingQueue.add(obj);
						entities.add(obj);
					}

					for (PlayerDetails pd:pdArray){
						pd.dispose();
					}
					entities.removeAll(pdArray);
					pdArray.clear();

					dynamicEntities.removeAll(playersEntities);
					registeredKeyboardEntities.removeAll(playersEntities);
					entities.removeAll(playersEntities);
					playersEntities.clear();

					if (playAgain){
						worldContext= WorldEntity.WorldEntityContext.MATCH_MAKING;
						setAppState(AppState.MATCH_MAKING);
					}else {
						worldContext= WorldEntity.WorldEntityContext.MAIN_MENU;
						setAppState(AppState.MAIN_MENU);
					}});


			}
		};
		isEnded=true;

		Timer.schedule(task,10);


	}


	float cummulativeDeltaTime=0;
	float endGamePrevention=0.0f;

	@Override
	public void render () {
		batch.setProjectionMatrix(cam.combined);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		if (appState.equals(AppState.MAIN_MENU)) {
			endGamePrevention=0;
			batch.begin();
			batch.draw(mainBackground,0,0,1600,900);

			for (WorldEntity ent: entities) {
				if (ent.entityContext==worldContext || ent.entityContext== WorldEntity.WorldEntityContext.ALL){
					if (ent instanceof UIEntity){

						((UIEntity) ent).tick(Gdx.graphics.getDeltaTime());
					}
				}
			}
			batch.end();


		}else if (appState==AppState.MATCH_MAKING){
			endGamePrevention=0;
			batch.begin();
			batch.draw(hostJoinTexture,0,0,1600,900);
			for (WorldEntity e:entities ) {

				if (e.entityContext.equals(WorldEntity.WorldEntityContext.MATCH_MAKING)
				|| e.entityContext == WorldEntity.WorldEntityContext.ALL){
					//TODO render

					if (e instanceof DynamicEntity){
						DynamicEntity dynamicEntity=(DynamicEntity)e ;
						dynamicEntity.tick(Gdx.graphics.getDeltaTime());
					}else if (e instanceof UIEntity){
						UIEntity uiEntity=(UIEntity)e;
						uiEntity.tick(Gdx.graphics.getDeltaTime());
					}
				}

			}
			batch.end();

		}else if (appState==AppState.IN_GAME){
			endGamePrevention=endGamePrevention+Gdx.graphics.getDeltaTime();
			//Render the game UI here
			batch.begin();
			batch.draw(inGameBackground,0,0,1600,900);
			for (WorldEntity e:entities ){

				if (e.entityContext== WorldEntity.WorldEntityContext.IN_GAME
						|| e.entityContext == WorldEntity.WorldEntityContext.ALL){
					if (e instanceof Player){
						((Player) e).tick(Gdx.graphics.getDeltaTime());
					}else if (e instanceof  Label){
						((Label) e).tick(Gdx.graphics.getDeltaTime());
					}else if(e instanceof  PlayerDetails){
						((PlayerDetails) e).tick(Gdx.graphics.getDeltaTime());
					}else if (e instanceof TextInput){
						((TextInput) e).tick(Gdx.graphics.getDeltaTime());
					}

				}

			}

			cummulativeDeltaTime=cummulativeDeltaTime+Gdx.graphics.getDeltaTime();

			if (!isHost){
				if (sendInput.get()) {
					client.interruptAndSend(ClientNetworkNode.ClientInstruction.SEND_INPUT, getCurrentPlayerInput(Gdx.graphics.getDeltaTime()));
				}

			}else  {
				if (cummulativeDeltaTime >= 0.067) {
					server.interruptSenderThreadAndPostInstruction(ServerNetworkNode.ServerInstruction.STATE, getGameState());
				}
				if (!isEnded && allHuntersDead() && endGamePrevention>=5){
					server.interruptSenderThreadAndPostInstruction(ServerNetworkNode.ServerInstruction.END_GAME,new EndGameObject(true,true));
					announceWinner(true,true);
				}
				if (!isEnded &&isGhostDead()&& endGamePrevention>=5){
					server.interruptSenderThreadAndPostInstruction(ServerNetworkNode.ServerInstruction.END_GAME,new EndGameObject(false,true));
					announceWinner(false,true);
				}

			}

			batch.end();
		}
	}


	boolean isEnded=false;

	private boolean allHuntersDead(){
		for (Player p:playersEntities){
			if (p instanceof Hunter){
				if (p.getHp()!=0){
					return false;
				}
			}
		}
		return true;
	}

	private boolean isGhostDead(){
		for (Player p:playersEntities){
			if (p instanceof Ghost){
				if (p.getHp()==0){
					return true;
				}
			}
		}
		return false;
	}


	@Override
	public void dispose () {

		batch.dispose();
		mainBackground.dispose();
		hostJoinTexture.dispose();

	}

	@Override
	public void resize(int width, int height) {


		for (WorldEntity e:entities) {
			e.resize(width,height);
		}
		screenSizeX=width;
		screenSizeY=height;
		cam.update();
	}



	public boolean isCollidingDynamicEntity(DynamicEntity de){
		for (DynamicEntity e: dynamicEntities){
			if(!e.identifier.equalsIgnoreCase(de.identifier) && de.isOverlapping(e) && !((e instanceof Hunter) || (e instanceof Ghost))){
				System.out.println(e+" colliding "+ de);
				return true;
			}else{

			}
		}
		return false;
	}

	public DynamicEntity isCollidingPlayerEntity(DynamicEntity de){
		for (DynamicEntity e: playersEntities){
			if(!e.identifier.equalsIgnoreCase(de.identifier) && de.isOverlapping(e)){
				return e;
			}else{

			}
		}
		return null;
	}

	public synchronized static void setAppState(AppState appState) {
		GhostHunters.appState = appState;
	}

	public synchronized static AppState getAppState() {

		return appState;
	}

	public void createWalls(){
		Pixmap wall1 = new Pixmap(5, 812, Pixmap.Format.RGBA8888);
		wall1.setColor(Color.CLEAR);
		wall1.fill();
		Texture w1 = new Texture(wall1);
		Wall wa = new Wall(1588, 10, "Right border", WorldEntity.WorldEntityContext.IN_GAME, w1);
		entities.add(wa);
		dynamicEntities.add(wa);

		Wall wb = new Wall(126, 10, "Left border", WorldEntity.WorldEntityContext.IN_GAME, w1);
		entities.add(wb);
		dynamicEntities.add(wb);

		Pixmap wall2 = new Pixmap(1467, 5, Pixmap.Format.RGBA8888);
		wall2.setColor(Color.CLEAR);
		wall2.fill();
		Texture w2 = new Texture(wall2);
		Wall wc = new Wall(126, 822, "Top border", WorldEntity.WorldEntityContext.IN_GAME, w2);
		entities.add(wc);
		dynamicEntities.add(wc);

		Wall wd = new Wall(126, 9, "Bottom border", WorldEntity.WorldEntityContext.IN_GAME, w2);
		entities.add(wd);
		dynamicEntities.add(wd);

		Pixmap wall3 = new Pixmap(5, 216, Pixmap.Format.RGBA8888);
		wall3.setColor(Color.CLEAR);
		wall3.fill();
		Texture w3 = new Texture(wall3);
		Wall we = new Wall(202, 528, "Room1", WorldEntity.WorldEntityContext.IN_GAME, w3);
		entities.add(we);
		dynamicEntities.add(we);

		Pixmap wall7 = new Pixmap(130, 5, Pixmap.Format.RGBA8888);
		wall7.setColor(Color.CLEAR);
		wall7.fill();
		Texture w7 = new Texture(wall7);
		Wall wk = new Wall(202, 528, "Room1", WorldEntity.WorldEntityContext.IN_GAME, w7);
		entities.add(wk);
		dynamicEntities.add(wk);

		Pixmap wall8 = new Pixmap(5, 65, Pixmap.Format.RGBA8888);
		wall8.setColor(Color.CLEAR);
		wall8.fill();
		Texture w8 = new Texture(wall8);
		Wall wl = new Wall(332, 468, "Room1", WorldEntity.WorldEntityContext.IN_GAME, w8);
		entities.add(wl);
		dynamicEntities.add(wl);

		Pixmap wall9 = new Pixmap(45, 5, Pixmap.Format.RGBA8888);
		wall9.setColor(Color.CLEAR);
		wall9.fill();
		Texture w9 = new Texture(wall9);
		Wall wm = new Wall(332, 468, "Room1", WorldEntity.WorldEntityContext.IN_GAME, w9);
		entities.add(wm);
		dynamicEntities.add(wm);

		Pixmap wall10 = new Pixmap(48, 5, Pixmap.Format.RGBA8888);
		wall10.setColor(Color.CLEAR);
		wall10.fill();
		Texture w10 = new Texture(wall10);
		Wall wn = new Wall(452, 470, "Room1", WorldEntity.WorldEntityContext.IN_GAME, w10);
		entities.add(wn);
		dynamicEntities.add(wn);

		Pixmap wall11 = new Pixmap(5, 234, Pixmap.Format.RGBA8888);
		wall11.setColor(Color.CLEAR);
		wall11.fill();
		Texture w11 = new Texture(wall11);
		Wall wo = new Wall(495, 470, "Room1", WorldEntity.WorldEntityContext.IN_GAME, w11);
		entities.add(wo);
		dynamicEntities.add(wo);

		Pixmap wall12 = new Pixmap(5, 45, Pixmap.Format.RGBA8888);
		wall12.setColor(Color.CLEAR);
		wall12.fill();
		Texture w12 = new Texture(wall12);
		Wall wp = new Wall(495, 780, "Room1", WorldEntity.WorldEntityContext.IN_GAME, w12);
		entities.add(wp);
		dynamicEntities.add(wp);

		Pixmap wall4 = new Pixmap(5, 144, Pixmap.Format.RGBA8888);
		wall4.setColor(Color.CLEAR);
		wall4.fill();
		Texture w4 = new Texture(wall4);
		Wall wf = new Wall(203, 90, "Room2", WorldEntity.WorldEntityContext.IN_GAME, w4);
		entities.add(wf);
		dynamicEntities.add(wf);

		Pixmap wall13 = new Pixmap(113, 5, Pixmap.Format.RGBA8888);
		wall13.setColor(Color.CLEAR);
		wall13.fill();
		Texture w13 = new Texture(wall13);
		Wall wq = new Wall(203, 90, "Room2", WorldEntity.WorldEntityContext.IN_GAME, w13);
		entities.add(wq);
		dynamicEntities.add(wq);

		Pixmap wall14 = new Pixmap(101, 5, Pixmap.Format.RGBA8888);
		wall14.setColor(Color.CLEAR);
		wall14.fill();
		Texture w14 = new Texture(wall14);
		Wall wr = new Wall(392, 90, "Room2", WorldEntity.WorldEntityContext.IN_GAME, w14);
		entities.add(wr);
		dynamicEntities.add(wr);

		Pixmap wall15 = new Pixmap(5, 160, Pixmap.Format.RGBA8888);
		wall15.setColor(Color.CLEAR);
		wall15.fill();
		Texture w15 = new Texture(wall15);
		Wall ws = new Wall(493, 9, "Room2", WorldEntity.WorldEntityContext.IN_GAME, w15);
		entities.add(ws);
		dynamicEntities.add(ws);

		Pixmap wall16 = new Pixmap(175, 5, Pixmap.Format.RGBA8888);
		wall16.setColor(Color.CLEAR);
		wall16.fill();
		Texture w16 = new Texture(wall16);
		Wall wt = new Wall(203, 230, "Room2", WorldEntity.WorldEntityContext.IN_GAME, w16);
		entities.add(wt);
		dynamicEntities.add(wt);

		Pixmap wall17 = new Pixmap(5, 80, Pixmap.Format.RGBA8888);
		wall17.setColor(Color.CLEAR);
		wall17.fill();
		Texture w17 = new Texture(wall17);
		Wall wu = new Wall(289, 152, "Room2", WorldEntity.WorldEntityContext.IN_GAME, w17);
		entities.add(wu);
		dynamicEntities.add(wu);

		Pixmap wall18 = new Pixmap(5, 85, Pixmap.Format.RGBA8888);
		wall18.setColor(Color.CLEAR);
		wall18.fill();
		Texture w18 = new Texture(wall18);
		Wall wv = new Wall(333, 232, "Room2", WorldEntity.WorldEntityContext.IN_GAME, w18);
		entities.add(wv);
		dynamicEntities.add(wv);

		Wall ww = new Wall(494, 232, "Room2", WorldEntity.WorldEntityContext.IN_GAME, w18);
		entities.add(ww);
		dynamicEntities.add(ww);

		Pixmap wall19 = new Pixmap(35, 5, Pixmap.Format.RGBA8888);
		wall19.setColor(Color.CLEAR);
		wall19.fill();
		Texture w19 = new Texture(wall19);
		Wall wx = new Wall(459, 232, "Room2", WorldEntity.WorldEntityContext.IN_GAME, w19);
		entities.add(wx);
		dynamicEntities.add(wx);

		Pixmap wall20 = new Pixmap(166, 5, Pixmap.Format.RGBA8888);
		wall20.setColor(Color.CLEAR);
		wall20.fill();
		Texture w20 = new Texture(wall20);
		Wall wy = new Wall(333, 313, "Room2", WorldEntity.WorldEntityContext.IN_GAME, w20);
		entities.add(wy);
		dynamicEntities.add(wy);

		Pixmap wall5 = new Pixmap(67, 82, Pixmap.Format.RGBA8888);
		wall5.setColor(Color.CLEAR);
		wall5.fill();
		Texture w5 = new Texture(wall5);
		Wall wg = new Wall(204, 342, "Box1", WorldEntity.WorldEntityContext.IN_GAME, w5);
		entities.add(wg);
		dynamicEntities.add(wg);

		Pixmap wall21 = new Pixmap(5, 150, Pixmap.Format.RGBA8888);
		wall21.setColor(Color.CLEAR);
		wall21.fill();
		Texture w21 = new Texture(wall21);
		Wall wz = new Wall(590, 675, "Room3", WorldEntity.WorldEntityContext.IN_GAME, w21);
		entities.add(wz);
		dynamicEntities.add(wz);

		Pixmap wall22 = new Pixmap(5, 241, Pixmap.Format.RGBA8888);
		wall22.setColor(Color.CLEAR);
		wall22.fill();
		Texture w22 = new Texture(wall22);
		Wall waa = new Wall(588, 364, "Room3", WorldEntity.WorldEntityContext.IN_GAME, w22);
		entities.add(waa);
		dynamicEntities.add(waa);

		Pixmap wall23 = new Pixmap(212, 5, Pixmap.Format.RGBA8888);
		wall23.setColor(Color.CLEAR);
		wall23.fill();
		Texture w23 = new Texture(wall23);
		Wall wab = new Wall(588, 503, "Room3", WorldEntity.WorldEntityContext.IN_GAME, w23);
		entities.add(wab);
		dynamicEntities.add(wab);

		Pixmap wall24 = new Pixmap(234, 5, Pixmap.Format.RGBA8888);
		wall24.setColor(Color.CLEAR);
		wall24.fill();
		Texture w24 = new Texture(wall24);
		Wall wac = new Wall(892, 503, "Room3", WorldEntity.WorldEntityContext.IN_GAME, w24);
		entities.add(wac);
		dynamicEntities.add(wac);

		Wall wad = new Wall(1126, 675, "Room3", WorldEntity.WorldEntityContext.IN_GAME, w21);
		entities.add(wad);
		dynamicEntities.add(wad);

		Pixmap wall25 = new Pixmap(5, 155, Pixmap.Format.RGBA8888);
		wall25.setColor(Color.CLEAR);
		wall25.fill();
		Texture w25 = new Texture(wall25);
		Wall wae = new Wall(1125, 451, "Room3", WorldEntity.WorldEntityContext.IN_GAME, w25);
		entities.add(wae);
		dynamicEntities.add(wae);

		Pixmap wall26 = new Pixmap(5, 180, Pixmap.Format.RGBA8888);
		wall26.setColor(Color.CLEAR);
		wall26.fill();
		Texture w26 = new Texture(wall26);
		Wall waf = new Wall(588, 90, "Room4", WorldEntity.WorldEntityContext.IN_GAME, w26);
		entities.add(waf);
		dynamicEntities.add(waf);

		Wall wag = new Wall(588, 88, "Room4", WorldEntity.WorldEntityContext.IN_GAME, w23);
		entities.add(wag);
		dynamicEntities.add(wag);

		Wall wah = new Wall(588, 267, "Room4", WorldEntity.WorldEntityContext.IN_GAME, w23);
		entities.add(wah);
		dynamicEntities.add(wah);

		Wall wai = new Wall(892, 88, "Room4", WorldEntity.WorldEntityContext.IN_GAME, w24);
		entities.add(wai);
		dynamicEntities.add(wai);

		Wall waj = new Wall(892, 267, "Room4", WorldEntity.WorldEntityContext.IN_GAME, w24);
		entities.add(waj);
		dynamicEntities.add(waj);

		Pixmap wall27 = new Pixmap(5, 331, Pixmap.Format.RGBA8888);
		wall27.setColor(Color.CLEAR);
		wall27.fill();
		Texture w27 = new Texture(wall27);
		Wall wak = new Wall(1124, 9, "Room4", WorldEntity.WorldEntityContext.IN_GAME, w27);
		entities.add(wak);
		dynamicEntities.add(wak);

		Pixmap wall28 = new Pixmap(5, 226, Pixmap.Format.RGBA8888);
		wall28.setColor(Color.CLEAR);
		wall28.fill();
		Texture w28 = new Texture(wall28);
		Wall wal = new Wall(1208, 451, "Room5", WorldEntity.WorldEntityContext.IN_GAME, w28);
		entities.add(wal);
		dynamicEntities.add(wal);

		Pixmap wall29 = new Pixmap(315, 5, Pixmap.Format.RGBA8888);
		wall29.setColor(Color.CLEAR);
		wall29.fill();
		Texture w29 = new Texture(wall29);
		Wall wam = new Wall(1208, 450, "Room5", WorldEntity.WorldEntityContext.IN_GAME, w29);
		entities.add(wam);
		dynamicEntities.add(wam);

		Pixmap wall30 = new Pixmap(235, 5, Pixmap.Format.RGBA8888);
		wall30.setColor(Color.CLEAR);
		wall30.fill();
		Texture w30 = new Texture(wall30);
		Wall wan = new Wall(1289, 673, "Room5", WorldEntity.WorldEntityContext.IN_GAME, w30);
		entities.add(wan);
		dynamicEntities.add(wan);

		Pixmap wall31 = new Pixmap(5, 160, Pixmap.Format.RGBA8888);
		wall31.setColor(Color.CLEAR);
		wall31.fill();
		Texture w31 = new Texture(wall31);
		Wall wao = new Wall(1520, 518, "Room5", WorldEntity.WorldEntityContext.IN_GAME, w31);
		entities.add(wao);
		dynamicEntities.add(wao);

		Pixmap wall32 = new Pixmap(140, 5, Pixmap.Format.RGBA8888);
		wall32.setColor(Color.CLEAR);
		wall32.fill();
		Texture w32 = new Texture(wall32);
		Wall wap = new Wall(1385, 517, "Room5", WorldEntity.WorldEntityContext.IN_GAME, w32);
		entities.add(wap);
		dynamicEntities.add(wap);

		Pixmap wall33 = new Pixmap(145, 5, Pixmap.Format.RGBA8888);
		wall33.setColor(Color.CLEAR);
		wall33.fill();
		Texture w33 = new Texture(wall33);
		Wall waq = new Wall(1289, 742, "Room5", WorldEntity.WorldEntityContext.IN_GAME, w33);
		entities.add(waq);
		dynamicEntities.add(waq);

		Pixmap wall34 = new Pixmap(71, 5, Pixmap.Format.RGBA8888);
		wall34.setColor(Color.CLEAR);
		wall34.fill();
		Texture w34 = new Texture(wall34);
		Wall war = new Wall(1519, 742, "Room5", WorldEntity.WorldEntityContext.IN_GAME, w34);
		entities.add(war);
		dynamicEntities.add(war);

		Wall was = new Wall(1209, 334, "Room6", WorldEntity.WorldEntityContext.IN_GAME, w29);
		entities.add(was);
		dynamicEntities.add(was);

		Pixmap wall35 = new Pixmap(5, 162, Pixmap.Format.RGBA8888);
		wall35.setColor(Color.CLEAR);
		wall35.fill();
		Texture w35 = new Texture(wall35);
		Wall wat = new Wall(1523, 177, "Room6", WorldEntity.WorldEntityContext.IN_GAME, w35);
		entities.add(wat);
		dynamicEntities.add(wat);

		Pixmap wall36 = new Pixmap(5, 80, Pixmap.Format.RGBA8888);
		wall36.setColor(Color.CLEAR);
		wall36.fill();
		Texture w36 = new Texture(wall36);
		Wall wau = new Wall(1523, 9, "Room6", WorldEntity.WorldEntityContext.IN_GAME, w36);
		entities.add(wau);
		dynamicEntities.add(wau);

		Pixmap wall37 = new Pixmap(230, 50, Pixmap.Format.RGBA8888);
		wall37.setColor(Color.CLEAR);
		wall37.fill();
		Texture w37 = new Texture(wall37);
		Wall wav = new Wall(1226, 214, "Box2", WorldEntity.WorldEntityContext.IN_GAME, w37);
		entities.add(wav);
		dynamicEntities.add(wav);

		Pixmap wall38 = new Pixmap(227, 49, Pixmap.Format.RGBA8888);
		wall38.setColor(Color.CLEAR);
		wall38.fill();
		Texture w38 = new Texture(wall38);
		Wall waw = new Wall(1226, 83, "Box3", WorldEntity.WorldEntityContext.IN_GAME, w38);
		entities.add(waw);
		dynamicEntities.add(waw);
	}



}
