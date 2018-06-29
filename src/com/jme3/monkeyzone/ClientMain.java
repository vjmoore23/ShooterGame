/*
 * Copyright (c) 2009-2011 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.monkeyzone;

import AI.AIControl;
import animations.AIAnimationControl;
import animations.AdvAnimationManagerControl;
import animations.CharacterInputAnimationAppState;
import characters.AICharacterControl;
import characters.ChaseCamCharacter;
import characters.NavMeshNavigationControl;
import com.jme3.monkeyzone.controls.UserCommandControl;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.monkeyzone.controls.DefaultHUDControl;
import com.jme3.monkeyzone.controls.UserInputControl;
import com.jme3.monkeyzone.messages.ActionMessage;
import com.jme3.monkeyzone.messages.AutoControlMessage;
import com.jme3.monkeyzone.messages.ChatMessage;
import com.jme3.monkeyzone.messages.ManualControlMessage;
import com.jme3.monkeyzone.messages.ServerAddEntityMessage;
import com.jme3.monkeyzone.messages.ServerAddPlayerMessage;
import com.jme3.monkeyzone.messages.ServerDisableEntityMessage;
import com.jme3.monkeyzone.messages.ServerEffectMessage;
import com.jme3.monkeyzone.messages.ServerEnableEntityMessage;
import com.jme3.monkeyzone.messages.ServerEnterEntityMessage;
import com.jme3.monkeyzone.messages.ServerEntityDataMessage;
import com.jme3.monkeyzone.messages.ServerRemoveEntityMessage;
import com.jme3.monkeyzone.messages.ServerRemovePlayerMessage;
import com.jme3.monkeyzone.messages.StartGameMessage;
import com.jme3.network.Network;
import com.jme3.network.NetworkClient;
import com.jme3.network.physicssync.PhysicsSyncManager;
import com.jme3.network.physicssync.SyncCharacterMessage;
import com.jme3.network.physicssync.SyncRigidBodyMessage;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.dynamic.TextCreator;
import de.lessvoid.nifty.controls.textfield.TextFieldControl;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import physics.PhysicsTestHelper;

/**
 * The client Main class, also the screen controller for most parts of the
 * login and lobby GUI
 * @author normenhansen
 */
public class ClientMain extends SimpleApplication implements ScreenController {
    //Class variables
    private static ClientMain app;
    private AdvAnimationManagerControl animControl;
    NavMeshNavigationControl navMesh; 
    private Vector3f normalGravity = new Vector3f(0, -9.81f, 9);//Gravity vector
    Node jaime;
    Node targets;
    Target target;
    private int gameTimeCount = 0;
    private float shotsFired = 0;
    private float shotsHit = 0;
    private int destroyedTarget = 0;
    private int points = 0;
    private int ball1 = 0;
    private int ball2 = 0;
    private int ball3 = 0;
    private int ball4 = 0;
    BitmapText timePlayed;
    BitmapText bulletsFired;
    BitmapText bulletsHit;
    BitmapText targetsDestroyed;
    BitmapText pointSum;
    
    
    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String string = args[i];
            if ("-server".equals(string)) {
                ServerMain.main(args);
                return;
            }
        }
        AppSettings settings = new AppSettings(true);
        settings.setFrameRate(Globals.SCENE_FPS);
        settings.setSettingsDialogImage("Interface/sillyFace.jpg");
        settings.setTitle("Moore's First Game");
        Util.registerSerializers();
        Util.setLogLevels(true);
        app = new ClientMain();
        app.setSettings(settings);
        app.setPauseOnLostFocus(false);
        app.start();
    }
    private WorldManager worldManager;
    private PhysicsSyncManager syncManager;
    private ClientEffectsManager effectsManager;
    private UserCommandControl commandControl;
    private Nifty nifty;
    private NiftyJmeDisplay niftyDisplay;
    private TextRenderer statusText;
    private NetworkClient client;
    private ClientNetListener listenerManager;
    private BulletAppState bulletAppState;
    private ChaseCamera chaseCam;

    @Override
    public void simpleInitApp() {
        startNifty();
        client = Network.createClient();
        bulletAppState = new BulletAppState();
        if (Globals.PHYSICS_THREADED) {
            bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        }
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().setAccuracy(Globals.PHYSICS_FPS);
        if(Globals.PHYSICS_DEBUG){
            bulletAppState.getPhysicsSpace().enableDebug(assetManager);
        }
        inputManager.setCursorVisible(true);
        flyCam.setEnabled(false);
//        chaseCam = new ChaseCamera(cam, inputManager);
//        chaseCam.setSmoothMotion(true);
//        chaseCam.setChasingSensitivity(100);
//      chaseCam.setTrailingEnabled(true);

        syncManager = new PhysicsSyncManager(app, client);
        syncManager.setMaxDelay(Globals.NETWORK_MAX_PHYSICS_DELAY);
        syncManager.setMessageTypes(AutoControlMessage.class,
                ManualControlMessage.class,
                ActionMessage.class,
                SyncCharacterMessage.class,
                SyncRigidBodyMessage.class,
                ServerEntityDataMessage.class,
                ServerEnterEntityMessage.class,
                ServerAddEntityMessage.class,
                ServerAddPlayerMessage.class,
                ServerEffectMessage.class,
                ServerEnableEntityMessage.class,
                ServerDisableEntityMessage.class,
                ServerRemoveEntityMessage.class,
                ServerRemovePlayerMessage.class);
        stateManager.attach(syncManager);

        //ai manager for controlling units
        commandControl = new UserCommandControl(nifty.getScreen("default_hud"), inputManager);
        //world manager, manages entites and server commands
        worldManager = new WorldManager(this, rootNode, commandControl);
        //adding/creating controls later attached to user controlled spatial
        worldManager.addUserControl(new UserInputControl(inputManager, cam));
        worldManager.addUserControl(commandControl);
        worldManager.addUserControl(new DefaultHUDControl(nifty.getScreen("default_hud")));
        stateManager.attach(worldManager);
        //effects manager for playing effects
        effectsManager = new ClientEffectsManager();
        stateManager.attach(effectsManager);
        //register effects manager and world manager with sync manager so that messages can apply their data
        syncManager.addObject(-2, effectsManager);
        syncManager.addObject(-1, worldManager);


        listenerManager = new ClientNetListener(this, client, worldManager, effectsManager);
        
        //Add Ball Shooter
        PhysicsTestHelper.createBallShooter(this,rootNode, bulletAppState.getPhysicsSpace());
        
        //Create the Physics World based on the Helper class
        PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());
        
        //Create 5 different moveable boxes 
        PhysicsTestHelper.createMoveableBox(rootNode, assetManager,  bulletAppState.getPhysicsSpace(), 1f, "Interface/faceX.jpg", 10, 10, 0);
        PhysicsTestHelper.createMoveableBox(rootNode, assetManager,  bulletAppState.getPhysicsSpace(), 2f, "Interface/faceHearts.jpg", 13, 15, 0);
        PhysicsTestHelper.createMoveableBox(rootNode, assetManager,  bulletAppState.getPhysicsSpace(), 1f, "Interface/faceTongue.jpg", -13, 15,0);
        PhysicsTestHelper.createMoveableBox(rootNode, assetManager,  bulletAppState.getPhysicsSpace(), 1.5f, "Interface/sillyFace.jpg", 15, 15, 0);
        PhysicsTestHelper.createMoveableBox(rootNode, assetManager,  bulletAppState.getPhysicsSpace(), 2.5f, "Interface/monsterFace.jpg", 14, 15, 0);
        PhysicsTestHelper.createMoveableBox(rootNode, assetManager,  bulletAppState.getPhysicsSpace(), 2f, "Interface/mustFace.jpg", -15, 10, 0);
        
        //Create 5 different imoveable spheres
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),10 ,1, "Interface/rain1.jpg", -10, 20,-3);
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),20 ,1, "Interface/rain2.jpg", -5, 25,-3);
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),15 ,2, "Interface/rain3.jpg", 5, 33,-10);
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),25 ,1, "Interface/rain4.jpg", 10, 15,10);
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),22 ,2, "Interface/rain5.jpg", -15, 23,-5);
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),13 ,1, "Interface/rain6.jpg", 15, 33,-3);
        
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),13 ,4, "Interface/rain1.jpg", 50, 5,-3);
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),13 ,6, "Interface/rain2.jpg", 50, 5,-50);
        
        targets = new Node("targets");
        target = new Target(assetManager, rootNode, targets, bulletAppState);
       
         //Causes program to crash when called
        //initKeys();
        
    }
    
    
     /**
      * 
      * @param scene
      * @param pos 
      */
     private Node setupCharacter(){
      
         
        // Load model, attach to character node
        Node aiCharacter = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");

        AICharacterControl physicsCharacter = new AICharacterControl(0.3f, 2.5f, 8f);
        
        aiCharacter.addControl(physicsCharacter);
        bulletAppState.getPhysicsSpace().add(physicsCharacter);
        aiCharacter.setLocalTranslation(0, 10, 0);
        aiCharacter.setLocalScale(2f);
        rootNode.attachChild(aiCharacter);
        AdvAnimationManagerControl animControl = new AdvAnimationManagerControl("animations/animations-jaime.properties");
        aiCharacter.addControl(animControl);
        
      
        aiCharacter.addControl(new AIControl());
        aiCharacter.addControl(new AIAnimationControl());
        aiCharacter.addControl(new NavMeshNavigationControl((Node) assetManager.loadModel("Scenes/newScene.j3o")));
       
       
        //rootNode.attachChild(camNode);
        //List<Spatial> targets = new ArrayList<Spatial>();
        
        //targets.add(camNode);
        //targets.add(mainPlayer);
        
        //jaime.getControl(AIControl.class).setState(AIControl.State.Follow);
       // jaime.getControl(AIControl.class).setTargetList(targets);
        //
        return aiCharacter;
    }
     
    
    /**
     * 
     * @return 
     */
     private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }
     
     /**
      * createAICharacter method
      * 
      */
    private void createAICharacter(Node target) {
        // Load model, attach to character node
        Node jaime = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        
        jaime.setLocalScale(1.50f);
        
        Node mainPlayer = (Node)assetManager.loadModel("Models/Buggy/Buggy/j3o");
        AICharacterControl AIcharacter = new AICharacterControl(0.3f, 2.5f, 8f);
        
        jaime.addControl(AIcharacter);
        getPhysicsSpace().add(AIcharacter);
        rootNode.attachChild(jaime);
        jaime.addControl(new AIControl());
        jaime.addControl(new AIAnimationControl());
        //Create camera for jaime
        //CameraNode camNode = new CameraNode("CamNode", cam);
        //camNode.setControlDir(CameraControl.ControlDirection.CameraToSpatial);
        
        //Create line of sight
        //Geometry g = new Geometry("", new Box(1,1,1));
        //g.setModelBound(new BoundingSphere(5f, Vector3f.ZERO));
        //g.updateModelBound();
        //g.setMaterial(lineMat);
        //camNode.attachChild(g);
        //camNode.addControl(new SoundEmitterControl());
        //getFlyByCamera().setMoveSpeed(25);
        //rootNode.attachChild(camNode);
        List<Spatial> targets = new ArrayList<Spatial>();
        //targets.add(camNode);
        targets.add(target);
        
        jaime.getControl(AIControl.class).setState(AIControl.State.Follow);
        //jaime.getControl(AIControl.class).setTargetList(targets);
        //jaime.getControl(AIControl.class).setTarget(camNode);
    }
     
    /**
     * 
     * @return 
     */
     private Node createPlayerCharacter() {
        
        stateManager.attach(bulletAppState);
        //stateManager.detach(stateManager.getState(FlyCamAppState.class));
        
        //Node jaimeNode = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        //jaimeNode.setLocalTranslation(12f, 0, 8f);
        //ChaseCamCharacter charControl = new ChaseCamCharacter(-4.5f, 2.5f, 0f);
        //MyGameCharacterControl charControl = new MyGameCharacterControl(0,-20f,0);
        
        //charControl.setGravity(normalGravity);
        //charControl.setCamera(cam);
        
        //ChaseCamera chaseCam = new ChaseCamera(cam, jaimeNode, inputManager);
        //chaseCam.setDragToRotate(false);
        //chaseCam.setSmoothMotion(true);
        //chaseCam.setLookAtOffset(new Vector3f(0, 1f, 0));
        //chaseCam.setDefaultDistance(7f);
        //chaseCam.setMaxDistance(8f);
        //chaseCam.setMinDistance(6f);
       
        //chaseCam.setTrailingSensitivity(50);        
        //chaseCam.setChasingSensitivity(10);
        //chaseCam.setRotationSpeed(10);
        //chaseCam.setDragToRotate(true);
        //chaseCam.setToggleRotationTrigger();

        //jaimeNode.addControl(charControl);
        //bulletAppState.getPhysicsSpace().add(charControl);

        //CharacterInputAnimationAppState appState = new CharacterInputAnimationAppState();
        
        //appState.addActionListener(charControl);
        //appState.addAnalogListener(charControl);
        //appState.setChaseCamera(chaseCam);
        //stateManager.attach(appState);
        //rootNode.attachChild(jaimeNode);
        //inputManager.setCursorVisible(false);
        
        //animControl = new AdvAnimationManagerControl("animations/animations-jaime.properties");
        //jaimeNode.addControl(animControl);
        
        //appState.addActionListener(animControl);
        //appState.addAnalogListener(animControl);
        
        //Add the Player to the world and use the customer character and input control classes
        Node jaimeNode = (Node)assetManager.loadModel("Models/Jaime/Jaime.j3o");
        
        ChaseCamCharacter charControl = new ChaseCamCharacter(0.5f, 2.5f, 8f);
        charControl.setGravity(normalGravity);
        charControl.setCamera(cam);
        
        ChaseCamera chaseCam = new ChaseCamera(cam, jaimeNode, inputManager);
        chaseCam.setDragToRotate(false);
        chaseCam.setSmoothMotion(true);
        chaseCam.setLookAtOffset(new Vector3f(0, 1f, 0));
        chaseCam.setDefaultDistance(10f);
        chaseCam.setMaxDistance(20f);
        chaseCam.setMinDistance(10f);
        
        chaseCam.setTrailingSensitivity(50);
        chaseCam.setChasingSensitivity(1);
        chaseCam.setRotationSpeed(1);
        chaseCam.setDragToRotate(true);
        chaseCam.setToggleRotationTrigger();
        
        jaimeNode.addControl(charControl);
        bulletAppState.getPhysicsSpace().add(charControl);
        
        CharacterInputAnimationAppState appState = new CharacterInputAnimationAppState();
        appState.addActionListener(charControl);
        appState.addAnalogListener(charControl);
        appState.setChaseCamera(chaseCam);
        
        stateManager.attach(appState);
        rootNode.attachChild(jaimeNode);
        inputManager.setCursorVisible(false);
        
        AdvAnimationManagerControl animControl = new AdvAnimationManagerControl("animations/animations-jaime.properties");
        jaimeNode.addControl(animControl);
        
        appState.addActionListener(animControl);
        appState.addAnalogListener(animControl);
        
        return jaimeNode;
    }

    /**
     * starts the nifty gui system
     */
    private void startNifty() {
        guiNode.detachAllChildren();
        guiNode.attachChild(fpsText);
        niftyDisplay = new NiftyJmeDisplay(assetManager,
                inputManager,
                audioRenderer,
                guiViewPort);
        nifty = niftyDisplay.getNifty();
        try {
            nifty.fromXml("Interface/ClientUI.xml", "load_game", this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        statusText = nifty.getScreen("load_game").findElementByName("layer").findElementByName("panel").findElementByName("status_text").getRenderer(TextRenderer.class);
        guiViewPort.addProcessor(niftyDisplay);
    }

    /**
     * sets the status text of the main login view, threadsafe
     * @param text
     */
    public void setStatusText(final String text) {
        enqueue(new Callable<Void>() {

            public Void call() throws Exception {
                statusText.setText(text);
                return null;
            }
        });
    }
    
    

    /**
     * updates the list of players in the lobby gui, threadsafe
     */
    public void updatePlayerData() {
        Logger.getLogger(ClientMain.class.getName()).log(Level.INFO, "Updating player data");
        enqueue(new Callable<Void>() {

            public Void call() throws Exception {
                Screen screen = nifty.getScreen("lobby");
                Element panel = screen.findElementByName("layer").findElementByName("panel").findElementByName("players_panel").findElementByName("players_list").findElementByName("panel");
                List<PlayerData> players = PlayerData.getHumanPlayers();
                for (Iterator<Element> it = new LinkedList<Element>(panel.getElements()).iterator(); it.hasNext();) {
                    Element element = it.next();
                    element.markForRemoval();//disable();
                }
                TextCreator labelCreator = new TextCreator("unknown player");
                labelCreator.setStyle("my-listbox-item-style");
                for (Iterator<PlayerData> it = players.iterator(); it.hasNext();) {
                    PlayerData data = it.next();
                    Logger.getLogger(ClientMain.class.getName()).log(Level.INFO, "List player {0}", data);
                    labelCreator.setText(data.getStringData("name"));
                    labelCreator.create(nifty, screen, panel);
                }
                return null;
            }
        });
    }

    /**
     * add text to chat window, threadsafe
     * @param text
     */
    public void addChat(final String text) {
        enqueue(new Callable<Void>() {

            public Void call() throws Exception {
                Screen screen = nifty.getScreen("lobby");
                Element panel = screen.findElementByName("layer").findElementByName("bottom_panel").findElementByName("chat_panel").findElementByName("chat_list").findElementByName("chat_list_panel");
                TextCreator labelCreator = new TextCreator(text);
                labelCreator.setStyle("my-listbox-item-style");
                labelCreator.create(nifty, screen, panel);
                return null;
            }
        });
    }

    /**
     * gets the text currently entered in the textbox and sends it as a chat message
     */
    public void sendChat() {
        Logger.getLogger(ClientMain.class.getName()).log(Level.INFO, "Send chat message");
        enqueue(new Callable<Void>() {

            public Void call() throws Exception {
                Screen screen = nifty.getScreen("lobby");
                TextFieldControl control = screen.findElementByName("layer").findElementByName("bottom_panel").findElementByName("chat_panel").findElementByName("chat_bottom_bar").findElementByName("chat_text").getControl(TextFieldControl.class);
                String text = control.getText();
                sendMessage(text);
                control.setText("");
                return null;
            }
        });
    }
    
    
    private void initKeys() {
        inputManager.addMapping("Shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "Shoot");
        
    }
    
    private ActionListener actionListener = new ActionListener() {
    
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("Shoot") && !keyPressed) {
                CollisionResults results = new CollisionResults();
                Ray ray = new Ray(cam.getLocation(), cam.getDirection());
                targets.collideWith(ray, results);

                if (results.size() > 0) {
                    for (int i = 0; i < results.size(); i++) {
                       
                        float dist = results.getCollision(i).getDistance();
                        Vector3f pt = results.getCollision(i).getContactPoint();
                        String hit = results.getCollision(i).getGeometry().getName();
                      
                        createNewTarget(hit);                                        
                        System.out.println("* Collision #" + i);
                        System.out.println("  You shot " + hit + " at " + pt + ", " + dist + " wu away.");
                        shotsHit++;
                        break;
                    }
                } else {
                   
                }
                shotsFired++;
                updateShotsFired();
            }
          
        }
    };
    
      private void createNewTarget(String hit) {

        if (hit.equals("Ball 1")) {
            ball1++;
            if (ball1 == 1) {
                targets.detachChildNamed(hit);
                target.createBallOne();
                points = points + 2;
                destroyedTarget++;
                ball1 = 0;
            }
        } else if (hit.equals("Ball 2")) {
            ball2++;
            if (ball2 == 2) {
                targets.detachChildNamed(hit);
                target.createBallTwo();
                points = points + 6;
                destroyedTarget++;
                ball2 = 0;
            }
        } else if (hit.equals("Ball 3")) {
            ball3++;
            if (ball3 == 3) {
                targets.detachChildNamed(hit);
                target.createBallThree();
                points = points + 10;
                destroyedTarget++;
                ball3 = 0;
            }
        } else if (hit.equals("Ball 4")) {
            ball4++;
            if (ball4 == 4) {
                targets.detachChildNamed(hit);
                target.createBallFour();
                points = points + 16;
                destroyedTarget++;
                ball4 = 0;
            }
        }
    }

    //FIXME: nifty cannot find sendChat() when sendChat(String text) is existing too
    public void sendMessage(String text) {
        client.send(new ChatMessage(text));
    }

    /**
     * connect to server (called from gui)
     */
    public void connect() {
        //TODO: not connect when already trying..
        final String userName = nifty.getScreen("load_game").findElementByName("layer").findElementByName("panel").findElementByName("username_text").getControl(TextFieldControl.class).getText();
        if (userName.trim().length() == 0) {
            setStatusText("Username invalid");
            return;
        }
        listenerManager.setName(userName);
        statusText.setText("Connecting..");
        try {
            client.connectToServer(Globals.DEFAULT_SERVER, Globals.DEFAULT_PORT_TCP, Globals.DEFAULT_PORT_UDP);
            client.start();
        } catch (IOException ex) {
            setStatusText(ex.getMessage());
            Logger.getLogger(ClientMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * brings up the lobby display
     */
    public void lobby() {
//        chaseCam.setDragToRotate(false);
        inputManager.setCursorVisible(true);
        nifty.gotoScreen("lobby");
    }

    /**
     * send message to start selected game
     */
    public void startGame() {
        //TODO: map selection
        client.send(new StartGameMessage("Scenes/newScene.j3o"));
    }

    /**
     * loads a level, basically does everything on a seprate thread except
     * updating the UI and attaching the level
     * @param name
     * @param modelNames
     */
    public void loadLevel(final String name, final String[] modelNames) {
        final TextRenderer statusText = nifty.getScreen("load_level").findElementByName("layer").findElementByName("panel").findElementByName("status_text").getRenderer(TextRenderer.class);
       
        if (name.equals("null")) {
            enqueue(new Callable<Void>() {

                public Void call() throws Exception {
                    worldManager.closeLevel();
                    lobby();
                    return null;
                }
            });
            return;
        }
        new Thread(new Runnable() {

            public void run() {
                try {
                    enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            nifty.gotoScreen("load_level");
                            statusText.setText("Loading Terrain..");
                            return null;
                        }
                    }).get();
                    worldManager.loadLevel(name);
                    enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            statusText.setText("Creating NavMesh..");
                            return null;
                        }
                    }).get();
                    worldManager.createNavMesh();
                    enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            statusText.setText("Loading Models..");
                            jaime = setupCharacter();
                           
                            return null;
                        }
                    }).get();
                    worldManager.preloadModels(modelNames);
                    enqueue(new Callable<Void>() {

                        public Void call() throws Exception {
                            worldManager.attachLevel();
                            statusText.setText("Done Loading!");
                            nifty.gotoScreen("default_hud");
                            inputManager.setCursorVisible(false);
                            inputManager.addMapping("Shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
                            jaime.getControl(NavMeshNavigationControl.class).moveTo(new Vector3f(cam.getLocation().x,0,cam.getLocation().z));
//                            chaseCam.setDragToRotate(false);
                            return null;
                        }
                    }).get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void bind(Nifty nifty, Screen screen) {
         // Creates Time Played Text
        timePlayed = new BitmapText(guiFont, false);
        timePlayed.setSize(guiFont.getCharSet().getRenderedSize());
        timePlayed.setLocalTranslation(0, 475, 0);
        guiNode.attachChild(timePlayed);
        
        bulletsFired = new BitmapText(guiFont, false);
        bulletsFired.setSize(guiFont.getCharSet().getRenderedSize());
        bulletsFired.setText("Bullets Fired: " + shotsFired);
        bulletsFired.setLocalTranslation(0, 450, 0);
        guiNode.attachChild(bulletsFired);
    }

    public void onStartScreen() {
    }

    public void onEndScreen() {
        
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (getTimer().getTimeInSeconds() >= 1) {
            getTimer().reset();
 
            gameTimeCount++;
            updateTime();
        }
        
           // Stops the game at 5 minutes
            if (gameTimeCount >= 300) {
                JOptionPane.showMessageDialog(null, "Two minutes is up! Program will shut down.");
                // Shows the final results
                JOptionPane.showMessageDialog(null,
                        "Bullets Fired: " + shotsFired + "\n"
                        + "Bullets Hit: " + shotsHit + "\n"
                        + "Targets Destroyed: " + destroyedTarget + "\n"
                        + "Points: " + points,
                        "Results:", JOptionPane.INFORMATION_MESSAGE);
                stop();
            }
             
        
    }
     private void updateTime() {
        timePlayed.setText("Game time: " + gameTimeCount + " seconds");
    }
     
     private void updateShotsFired() {
        bulletsFired.setText("Bullets Fired: " + shotsFired);
        bulletsHit.setText("Bullets Hit: " + shotsHit);
        targetsDestroyed.setText("Targets Destroyed: " + destroyedTarget);
        pointSum.setText("Points: " + points);
    }

    @Override
    public void simpleRender(RenderManager rm) {
        
    }

    @Override
    public void destroy() {
        try {
            client.close();
        } catch (Exception ex) {
            Logger.getLogger(ClientMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        super.destroy();
    }
}
