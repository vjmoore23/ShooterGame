package mygame;

import AI.AIControl;

import animations.AIAnimationControl;
import animations.AdvAnimationManagerControl;
import animations.CharacterInputAnimationAppState;
import appstate.InputAppState;
import characters.AICharacterControl;
import characters.ChaseCamCharacter;
import characters.MyGameCharacterControl;
import characters.NavMeshNavigationControl;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import physics.PhysicsTestHelper;


public class Main extends SimpleApplication implements PhysicsCollisionListener{
    //Class variables
    protected BulletAppState bulletAppState;//Pyhsics object
    private Vector3f normalGravity = new Vector3f(0, -9.81f, 9);//Gravity vector
    private Application app;
    private InputAppState jaimeAppState;
    private MyGameCharacterControl jaimeControl;
    private CameraNode camNode;
    public static Material lineMat;
    private Vector3f camLocation = new Vector3f(0,-20f,0);
    private Vector3f lookAtDirection = new Vector3f(0,-0.8f,-0.2f);
    private float camDistance = 30f;
    private AdvAnimationManagerControl animControl;
     NavMeshNavigationControl navMesh; 
    
    
    //create a new file for storing position and velocity data
    public static java.io.File file = new java.io.File("positions.txt");
    public static java.io.PrintWriter output;
    public static java.io.File file2 = new java.io.File("collision_points.txt");
    public static java.io.PrintWriter output2;
    
    // initiate the objects for generating random numbers
    private Random positionGenerator = new Random();
    private Random velGenerator = new Random();
    private Random posNegGenerator = new Random();
    private Random gravGenerator = new Random();
    
    private Vector3f pt;    // 3D vector position of ball on collision
    private String hit;     // which the object ball hits
    
    // intiate the variable for storing time
    long startTime;
    long startGravTime;
    long estimatedTime;
    
    // variable determines whether gravity is universal
    //  or seperate for the balls
    boolean randGrav = false;
    
    private BitmapText gravText;

    // Set the number of generated balls
    final int NUMBALLS = 3;
    
    // Set the initial velocity in XYZ
    final int IVX = 5;
    final int IVY = 5;
    final int IVZ = 5;
    
    private RigidBodyControl[] ball_physics = new RigidBodyControl[NUMBALLS];
    SphereObj[] sphereObjects = new SphereObj[NUMBALLS];
    
    BallTextDisplay[] ballDisplays = new BallTextDisplay[NUMBALLS];

    public static void main(String[] args) throws FileNotFoundException {
        output = new java.io.PrintWriter(file);        
        output2 = new java.io.PrintWriter(file2);
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.getPhysicsSpace().addCollisionListener(this);
        
        getFlyByCamera().setMoveSpeed(45f);
        cam.setLocation(new Vector3f(20, 20, 20));
        cam.lookAt(new Vector3f(0,0,0), Vector3f.UNIT_Y);
        
        Node scene = setupWorld();
        
//        DirectionalLight l = new DirectionalLight();
//        rootNode.addLight(l);
        
        //Create moving spheres
     
        for (int i = 0; i < NUMBALLS; i++) {
            if (i < 1) {
                
                sphereObjects[i] = new SphereObj("yellow", 5);
                
            }
            else if(i < 2) {
               
                sphereObjects[i] = new SphereObj("green", 15);
            }
            else if (i < 3) {
               
                sphereObjects[i] = new SphereObj("red", 30);
            }
           
        }
        
        // Give the balls a physical presence
        for (int i = 0; i < NUMBALLS; i++) {
            ball_physics[i] = new RigidBodyControl(1f);
        }
        // Add the balls to the scene
        for (int i = 0; i < NUMBALLS; i++) {
            sphereObjects[i].addControl(ball_physics[i]);
        }
        
        for (int i = 0; i < NUMBALLS; i++) {
            bulletAppState.getPhysicsSpace().add(ball_physics[i]);
        }
        
        // Make the balls bouncy
        for (int i = 0; i < NUMBALLS; i++) {
            ball_physics[i].setRestitution(1.5f);
        }
         // Generate the same gravity for all the balls
        Vector3f worldGrav = generateRandomGravity();
        
        // Set the gravity and random velocity to all the balls
        for (int i = 0; i < NUMBALLS; i++) {
            if (randGrav) {
                ball_physics[i].setGravity(generateRandomGravity());
            }
            else {
                ball_physics[i].setGravity(worldGrav);
            }

            estimatedTime = System.currentTimeMillis() - startTime;
            // Set the initial velocity to all the balls
            ball_physics[i].setLinearVelocity(new Vector3f(IVX , IVY , IVZ));
            if (estimatedTime > 100) {
                // Set the random velocity to all the balls
                ball_physics[i].setLinearVelocity(generateRandomVelocity());
            }
        }
         guiNode.detachAllChildren();
        setDisplayStatView(false);
        setDisplayFps(false);
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        
        // Add the text for tracking the balls' locations to the gui
        for (int i = 0; i < NUMBALLS; i++) {
            ballDisplays[i] = new BallTextDisplay(i + 1, 450, 475 - i*25, 0);
        }
        
       
             
        // Customize the keys
        initKeys();
        
        //Create the Cube box
        createBox box = new createBox(0.1f, 100f, 100f, "Box", "pink", -100f, 3f, 0);//West Wall
        createBox box2 = new createBox(100f, 0.1f, 100f, "Box", "pink", 0, -3f, 0);//Floor
        createBox box3 = new createBox(100f, 0.1f, 100f, "Box", "pink", 0, 103f, 0);//Ceiling
        createBox box4 = new createBox(100f, 100f, 0.1f, "Box", "pink", 0f, 3f, -100f);//North Wall
        createBox box5 = new createBox(0.1f, 100f, 100f, "Box", "pink", 100f, 3f, 0);//East Wall
        createBox box6 = new createBox(100f, 100f, 0.1f, "Box", "pink", 0f, 3f, 100f);//South Wall
       
        
       
       
        setupCharacter(scene);
        //cam.lookAtDirection(camLocation, Vector3f.UNIT_Y);
        //camLocation.set(cam.getDirection().mult(-camDistance));
        //cam.setLocation(camLocation);
        
        //lineMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        
         //createPlayerCharacter();
        //createAICharacter();
        

        
//        characterNode.attachChild(model);

        // Add character node to the rootNode
//        rootNode.attachChild(characterNode);
        
        //Create the Physics World based on the Helper class
        PhysicsTestHelper.createPhysicsTestWorld(rootNode, assetManager, bulletAppState.getPhysicsSpace());
        
        //Create 5 different moveable boxes 
        PhysicsTestHelper.createMoveableBox(rootNode, assetManager,  bulletAppState.getPhysicsSpace(), 1f, "Interface/faceX.jpg", 0, 3, -3);
        PhysicsTestHelper.createMoveableBox(rootNode, assetManager,  bulletAppState.getPhysicsSpace(), 2f, "Interface/faceHearts.jpg", 3, 3, -3);
        PhysicsTestHelper.createMoveableBox(rootNode, assetManager,  bulletAppState.getPhysicsSpace(), 1f, "Interface/faceTongue.jpg", -3, 1, -3);
        PhysicsTestHelper.createMoveableBox(rootNode, assetManager,  bulletAppState.getPhysicsSpace(), 1.5f, "Interface/sillyFace.jpg", 5, 5, -3);
        PhysicsTestHelper.createMoveableBox(rootNode, assetManager,  bulletAppState.getPhysicsSpace(), 2.5f, "Interface/monsterFace.jpg", 4, 5, -3);
        PhysicsTestHelper.createMoveableBox(rootNode, assetManager,  bulletAppState.getPhysicsSpace(), 2f, "Interface/mustFace.jpg", -5, 10, -3);
        
        //Create 5 different imoveable spheres
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),10 ,1, "Interface/rain1.jpg", -10, 20,-3);
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),20 ,1, "Interface/rain2.jpg", -5, 25,-3);
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),15 ,2, "Interface/rain3.jpg", 5, 33,-10);
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),25 ,1, "Interface/rain4.jpg", 10, 15,10);
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),22 ,2, "Interface/rain5.jpg", -15, 23,-5);
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),13 ,1, "Interface/rain6.jpg", 15, 33,-3);
        
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),13 ,4, "Interface/rain1.jpg", 50, 3,-3);
        PhysicsTestHelper.createNonMoveableSphere(rootNode, assetManager, bulletAppState.getPhysicsSpace(),13 ,6, "Interface/rain2.jpg", 50, 3,-50);
       
        //
        //Add the Player to the world and use the customer character and input control classes
        //Node jaimeNode = (Node)assetManager.loadModel("Models/Jaime/Jaime.j3o");
        
        //ChaseCamCharacter charControl = new ChaseCamCharacter(0.5f, 2.5f, 8f);
        //charControl.setGravity(normalGravity);
        //charControl.setCamera(cam);
        
        //ChaseCamera chaseCam = new ChaseCamera(cam, jaimeNode, inputManager);
        //chaseCam.setDragToRotate(false);
        //chaseCam.setSmoothMotion(true);
        //chaseCam.setLookAtOffset(new Vector3f(0, 1f, 0));
        //chaseCam.setDefaultDistance(10f);
        //chaseCam.setMaxDistance(20f);
        //chaseCam.setMinDistance(10f);
        
        //chaseCam.setTrailingSensitivity(50);
        //chaseCam.setChasingSensitivity(1);
        //chaseCam.setRotationSpeed(1);
        //chaseCam.setDragToRotate(true);
       // chaseCam.setToggleRotationTrigger();
        
        //jaimeNode.addControl(charControl);
        //bulletAppState.getPhysicsSpace().add(charControl);
        
        //CharacterInputAnimationAppState appState = new CharacterInputAnimationAppState();
        //appState.addActionListener(charControl);
        //appState.addAnalogListener(charControl);
        //appState.setChaseCamera(chaseCam);
        
        //stateManager.attach(appState);
        //rootNode.attachChild(jaimeNode);
        //inputManager.setCursorVisible(false);
        
        //AdvAnimationManagerControl animControl = new AdvAnimationManagerControl("animations/animations-jaime.properties");
        //jaimeNode.addControl(animControl);
        
        //appState.addActionListener(animControl);
        //appState.addAnalogListener(animControl);
        
        //Add Ball Shooter
        //PhysicsTestHelper.createBallShooter(this,rootNode,bulletAppState.getPhysicsSpace());
        
        //Add a custom font and text to the scene
        //BitmapFont myFont = assetManager.loadFont("Interface/Fonts/Arial.fnt");

        //BitmapText hudText = new BitmapText(myFont, true);
        //hudText.setText("CMSC325 Moore Project 1");
       // hudText.setColor(ColorRGBA.Cyan);
        //hudText.setSize(20);
        //hudText.setSize(guiFont.getCharSet().getRenderedSize());
        
        //Set the text in the middle of the screen
        //hudText.setLocalTranslation(settings.getWidth() /2 , settings.getHeight() / 2 + hudText.getLineHeight(), 0f); //Positions text to middle of screen
        //guiNode.attachChild(hudText);
   
        
}
     /**
      * setupWorld
      * @return 
      */
     private Node setupWorld(){
        Node scene = (Node) assetManager.loadModel("Scenes/newScene.j3o");
        
        rootNode.attachChild(scene);

        //FilterPostProcessor processor = (FilterPostProcessor) assetManager.loadAsset("Effects/Water.j3f");
        //viewPort.addProcessor(processor);
        

        //TODO: navmesh only for debug
        Geometry navGeom = new Geometry("NavMesh");
        navGeom.setMesh(((Geometry) scene.getChild("NavMesh" )).getMesh());
        Material green = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        green.setColor("Color", ColorRGBA.Green);
        green.getAdditionalRenderState().setWireframe(true);
        navGeom.setMaterial(green);

        rootNode.attachChild(navGeom); 
        
        
        Spatial terrain = scene.getChild("terrain-newScene");
        terrain.addControl(new RigidBodyControl(0));
        bulletAppState.getPhysicsSpace().addAll(terrain);
        return scene;
    }
     
     /**
      * 
      * @param scene
      * @param pos 
      */
     private void setupCharacter(Node scene){
      
         
        // Load model, attach to character node
        Node aiCharacter = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");

        AICharacterControl physicsCharacter = new AICharacterControl(0.3f, 2.5f, 8f);
        aiCharacter.addControl(physicsCharacter);
        bulletAppState.getPhysicsSpace().add(physicsCharacter);
        aiCharacter.setLocalTranslation(0, 10, 0);
        aiCharacter.setLocalScale(2f);
        scene.attachChild(aiCharacter);
        navMesh = new NavMeshNavigationControl((Node) scene);
        
        aiCharacter.addControl(navMesh);
      
       
        //aiCharacter.addControl(new NavMeshNavigationControl((Node) scene));
        
        //aiCharacter.getControl(NavMeshNavigationControl.class).moveTo(new Vector3f(50, 3,-50));
        
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
    private void createAICharacter() {
        // Load model, attach to character node
        Node jaime = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        
        jaime.setLocalScale(1.50f);
        
        Node mainPlayer = createPlayerCharacter();
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
        targets.add(mainPlayer);
        
        jaime.getControl(AIControl.class).setState(AIControl.State.Follow);
        jaime.getControl(AIControl.class).setTargetList(targets);
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
     * 
     * @param x
     * @param y
     * @param z
     * @param name
     * @param color
     * @param xTrans
     * @param yTrans
     * @param zTrans
     * @return 
     */
    public class createBox{
        Vector3f position;
        Box box; 
        Geometry geomWall; 
        Material mat;
        RigidBodyControl phy;
        createBox(float x, float y, float z, String name, String color, float xTrans, float yTrans, float zTrans){
         box = new Box(Vector3f.ZERO, x, y, z);
            box.scaleTextureCoordinates(new Vector2f(3, 6));
            geomWall = new Geometry(name, box);
            mat = new Material(assetManager,
                   "Common/MatDefs/Misc/Unshaded.j3md");  
            if (color.equalsIgnoreCase("blue")) {
                mat.setColor("Color", new ColorRGBA(0, 0, 1, 0.5f));
            } else if (color.equalsIgnoreCase("pink")) {
                mat.setColor("Color", new ColorRGBA(1f, 0.68f, 0.68f, 0.5f));  
            } else if (color.equalsIgnoreCase("orange")) {
                mat.setColor("Color", new ColorRGBA(.98f, .51f, 0, 0.5f));
            }
          
            mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            geomWall.setMaterial(mat);
            geomWall.setQueueBucket(RenderQueue.Bucket.Translucent);
            
            geomWall.setLocalTranslation(xTrans, yTrans, zTrans);
            rootNode.attachChild(geomWall);
            phy = new RigidBodyControl(0.0f);
            geomWall.addControl(phy);
            bulletAppState.getPhysicsSpace().add(phy);
            phy.setRestitution(0.5f);
            
            
        }
       
        public void addControl(RigidBodyControl wall_phy) {
            geomWall.addControl(wall_phy);
        }
        
       
    }
    
   
    
    /**
     * 
     * @return 
     */
    public Vector3f generateRandomVelocity() {
        float velx = velGenerator.nextInt(10);
        int posNeg = posNegGenerator.nextInt(2);
        if (posNeg == 0) {
            velx *= -1;
        }
        float vely = velGenerator.nextInt(10);
        posNeg = posNegGenerator.nextInt(2);
        if (posNeg == 0) {
            vely *= -1;
        }
        float velz = velGenerator.nextInt(10);
        posNeg = posNegGenerator.nextInt(2);
        if (posNeg == 0) {
            velz *= -1;
        }
        return new Vector3f( velx , vely , velz );
    }
    /**
     * 
     * @return 
     */
    public Vector3f generateRandomGravity() {
        float gravX = gravGenerator.nextInt(3);
        int posNeg = posNegGenerator.nextInt(2);
        if (posNeg == 0) {
            gravX *= -1;
        }
        float gravY = gravGenerator.nextInt(3);
        posNeg = posNegGenerator.nextInt(2);
        if (posNeg == 0) {
            gravY *= -1;
        }
        float gravZ = gravGenerator.nextInt(3);
        posNeg = posNegGenerator.nextInt(2);
        if (posNeg == 0) {
            gravZ *= -1;
        }
        return new Vector3f(gravX, gravY, gravZ);
    }
    
    /**
     * 
     * @return 
     */
    public int generateRandomPos() {
        int pos = positionGenerator.nextInt(6);
        return pos;
    }
    
     /**
      * 
      * @param geomSphere
      * @param ball_phy 
      */
        public void addControl(Geometry geomSphere, RigidBodyControl ball_phy) {
            geomSphere.addControl(ball_phy);
        }
        
    /**
     * 
     * @param event 
     */
    public void collision(PhysicsCollisionEvent event) {
        CollisionResults results = new CollisionResults();
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());
        rootNode.collideWith(ray, results);   
        if (results.size() > 0) {            
            for (int i = 0; i < results.size(); i++) {
                pt = results.getCollision(i).getContactPoint();
                hit = results.getCollision(i).getGeometry().getName();

                output2.println(hit + " " + pt);
      
                System.out.println("----- Collision # " + i + "-----");
                System.out.println("A sphere hits " + hit + " at " + pt);
            }            
        }        
    }
    
     public String getBallPosString(RigidBodyControl ball_physics) {
        String xPos = String.format("%5.2f", ball_physics.getPhysicsLocation().getX());
        float x = ball_physics.getPhysicsLocation().getX();
        float z = ball_physics.getPhysicsLocation().getZ();
        String yPos = String.format("%5.2f", ball_physics.getPhysicsLocation().getY());
        String zPos = String.format("%5.2f", ball_physics.getPhysicsLocation().getZ());
        navMesh.moveTo(new Vector3f(x,3f,z));
        return xPos + " " + yPos + " " + zPos;
       
    }
   

    @Override
    public void simpleUpdate(float tpf) {
         estimatedTime = System.currentTimeMillis() - startTime;
        long estimatedGravTime = System.currentTimeMillis() - startGravTime;
       
        int elapsedTime = 0;
  
        if(estimatedTime > 100) {

            updatePositionDisplay();   
            for (int i = 0; i < NUMBALLS; i++) {
                output.println("Sphere" + i + ": " + getBallPosString(ball_physics[i]));
                
            }
            startTime = System.currentTimeMillis();
        }
   
        else {
            updatePositionDisplay();
        }
      
        if(estimatedGravTime > 5000) {
            Vector3f worldGrav = generateRandomGravity();
            for (int i = 0; i < NUMBALLS; i++) {
                if (randGrav){
                    ball_physics[i].setGravity(generateRandomGravity());
                }
                else {
                    ball_physics[i].setGravity(worldGrav);
                }
            }
            startGravTime = System.currentTimeMillis();
        }
    }
    
    
    public void updatePositionDisplay() {        
        for (int i = 0; i < NUMBALLS; i++) {
            ballDisplays[i].setText(ball_physics[i]);
        }
        
        
    }
    
   /**
    * 
    */
    public void initKeys() {
        inputManager.addMapping("Exit",  new KeyTrigger(KeyInput.KEY_P));
        inputManager.addListener(actionListener, new String[]{"Exit"});
        inputManager.addMapping("Gravity", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addListener(actionListener, new String[]{"Gravity"});
    }
    /**
     * 
     */
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
          if (name.equals("Exit") && !keyPressed) {
              output.close();              
              output2.close();
              app.stop();
          }
          if (name.equals("Gravity") && !keyPressed) {
              randGrav = !randGrav;
          }
        }
    };

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }
    
   
    class SphereObj {
        Vector3f position;
        Sphere sphere; 
        Geometry geomSphere; 
        Material mat;
        
        SphereObj(String color, int minX) {
            
            sphere = new Sphere(32, 32, 3f); 
            geomSphere = new Geometry("Sphere", sphere); 
            mat = new Material(assetManager,
                   "Common/MatDefs/Misc/Unshaded.j3md");
             if (color.equalsIgnoreCase("yellow")) {
                mat.setColor("Color", ColorRGBA.Yellow);
            } else if (color.equalsIgnoreCase("red")) {
                mat.setColor("Color", ColorRGBA.Red);
                
            } else if (color.equalsIgnoreCase("green")) {
                mat.setColor("Color", ColorRGBA.Green);
            } else {
                mat.setColor("Color", new ColorRGBA(1, 0, 1, 0));
            }
            geomSphere.setMaterial(mat);
            rootNode.attachChild(geomSphere);
            
            int posX = minX + generateRandomPos();
            int posY = -3 + generateRandomPos();
            int posZ = -3 + generateRandomPos(); 
            
            geomSphere.move(posX, posY, posZ);
       
        }
        //adds the physics to the sphere
        public void addControl(RigidBodyControl ball_phy) {
            geomSphere.addControl(ball_phy);
            
        }
    }
    // This class displays the text to the GUI
   class BallTextDisplay {
        BitmapText ballText;
        BitmapText gravText;
        int ballNumber;
        // displays the positions of each ball separately on the GUI
        BallTextDisplay(int whichBall, float xPos, float yPos, float zPos) {
             ballText = new BitmapText(guiFont, false);
             ballText.setSize(guiFont.getCharSet().getRenderedSize());
             ballText.setText("B" + whichBall + ": ");
             ballNumber = whichBall;
             ballText.setLocalTranslation(xPos, yPos, zPos);
             guiNode.attachChild(ballText);
        }
        //displays the positions of each ball separately on the GUI
        public void setText(RigidBodyControl ball_phy) {
           String xPos = String.format("%5.2f", ball_phy.getPhysicsLocation().getX());
           String yPos = String.format("%5.2f", ball_phy.getPhysicsLocation().getY());
           String zPos = String.format("%5.2f", ball_phy.getPhysicsLocation().getZ());
           ballText.setText("S" + ballNumber + ": (" + xPos + ", " + yPos + ", " + zPos + ")");
        }
    }
}

