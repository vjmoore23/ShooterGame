package com.jme3.monkeyzone;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import java.util.Random;

public class Target {

    private BulletAppState bulletAppState;    
    private Node rootNode;
    private Node targets;
    private AssetManager assetManager;
    public Geometry ballOne;
    public Geometry ballTwo;
    public Geometry ballThree;
    public Geometry ballFour;
    public RigidBodyControl ballOneColl;
    public RigidBodyControl ballTwoCol;
    public RigidBodyControl ballThreeColl;
    public RigidBodyControl ballFourColl;;
    
    public Target (AssetManager assetManager, Node rootNode, Node targets, BulletAppState bulletAppState) {
        
        this.bulletAppState = bulletAppState;
        this.rootNode = rootNode;
        this.targets = targets;
        this.assetManager = assetManager;
        rootNode.attachChild(targets);

        // Creates balls
        createBallOne();
        createBallTwo();
        createBallThree();
        createBallFour();
    }

    public void createBallOne() {
        Random random = new Random();
        Sphere createBall = new Sphere(32, 32, 0.8f);
        ballOne = new Geometry("Ball 1", createBall);
        createBall.setTextureMode(Sphere.TextureMode.Projected);
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.randomColor());
        ballOne.setMaterial(mat);                
        ballOne.setLocalTranslation((random.nextFloat() * 3f), (random.nextFloat() * 3f), (random.nextFloat() * 3f));
        targets.attachChild(ballOne);
        ballOneColl = new RigidBodyControl(1f);
        ballOne.addControl(ballOneColl);
        bulletAppState.getPhysicsSpace().add(ballOneColl);
        ballOneColl.setMass(1);
        ballOneColl.setFriction(0);
        ballOneColl.setRestitution(1.0f);
    }

    public void createBallTwo() {
        Random random = new Random();
        Sphere createBall = new Sphere(32, 32, 0.8f);
        ballTwo = new Geometry("Ball 2", createBall);
        createBall.setTextureMode(Sphere.TextureMode.Projected);
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.randomColor());
        ballTwo.setMaterial(mat);
        ballTwo.setLocalTranslation((random.nextFloat() * 2f), (random.nextFloat() * 2f), (random.nextFloat() * 2f));
        targets.attachChild(ballTwo);
        ballTwoCol = new RigidBodyControl(1f);
        ballTwo.addControl(ballTwoCol);
        bulletAppState.getPhysicsSpace().add(ballTwoCol);
        ballTwoCol.setMass(1);
        ballTwoCol.setFriction(0);
        ballTwoCol.setRestitution(1.0f);
    }

    public void createBallThree() {
        Random random = new Random();
        Sphere createBall = new Sphere(32, 32, 0.8f);
        ballThree = new Geometry("Ball 3", createBall);
        createBall.setTextureMode(Sphere.TextureMode.Projected);
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.randomColor());
        ballThree.setMaterial(mat);
        ballThree.setLocalTranslation((random.nextFloat() * 1f), (random.nextFloat() * 1f), (random.nextFloat() * 1f));
        targets.attachChild(ballThree);
        ballThreeColl = new RigidBodyControl(1f);
        ballThree.addControl(ballThreeColl);
        bulletAppState.getPhysicsSpace().add(ballThreeColl);
        ballThreeColl.setMass(1);
        ballThreeColl.setFriction(0);
        ballThreeColl.setRestitution(1.0f);
    }

    public void createBallFour() {
        Random random = new Random();
        Sphere createBall = new Sphere(32, 32, 0.2f);
        ballFour = new Geometry("Ball 4", createBall);
        createBall.setTextureMode(Sphere.TextureMode.Projected);
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.randomColor());
        ballFour.setMaterial(mat);
        ballFour.setLocalTranslation((random.nextFloat() * 4f), (random.nextFloat() * 4f), (random.nextFloat() * 4f));
        targets.attachChild(ballFour);
        ballFourColl = new RigidBodyControl(1f);
        ballFour.addControl(ballFourColl);
        bulletAppState.getPhysicsSpace().add(ballFourColl);
        ballFourColl.setMass(1);
        ballFourColl.setFriction(0);
        ballFourColl.setRestitution(1.0f);
    }          
}
