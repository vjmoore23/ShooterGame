package characters;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.control.CameraControl;


public class MyGameCharacterControl extends BetterCharacterControl 
        implements ActionListener, AnalogListener {
    
    boolean forward = false, backward = false, leftRotate = false, rightRotate = false, leftStrafe = false, rightStrafe = false;
    protected Node head = new Node("Head");
    private float yaw = 0;
    protected float moveSpeed = 3;

    private float cooldownTime = 1f;
    private float cooldown = 0f;
    
    public MyGameCharacterControl(float radius, float height, float mass) {
        super(radius, height, mass);
        head.setLocalTranslation(0, 1.8f, 0);
    }
    
    
    public void onAction(String action, boolean isPressed, float tpf) {
        if (action.equals("StrafeLeft")) {
            leftStrafe = isPressed;
        } else if (action.equals("StrafeRight")) {
            rightStrafe = isPressed;
        } else if (action.equals("MoveForward")) {
            forward = isPressed;
        } else if (action.equals("MoveBackward")) {
            backward = isPressed;
        } else if (action.equals("Jump")) {
            jump();
        } else if (action.equals("Duck")) {
            setDucked(isPressed);
        } 
    }
    
    public void onAnalog(String name, float value, float tpf) {
        if (name.equals("RotateLeft")) {
            rotate(tpf * value);
        } else if (name.equals("RotateRight")) {
            rotate(-tpf * value);
        } else if(name.equals("LookUp")){
            lookUpDown(value * tpf);
        } else if (name.equals("LookDown")){
            lookUpDown(-value * tpf);
        } 
        /**
         * 
         */
        else if (name.equals("MoveForward") || name.equals("MoveBackward") || name.equals("StrafeLeft") || name.equals("StrafeRight")){
            moveSpeed = value * 3;
        }
        /**
         * 
         */
    }
    
    public void update(float tpf) {
        super.update(tpf);
        Vector3f modelForwardDir = spatial.getWorldRotation().mult(Vector3f.UNIT_Z);
        Vector3f modelLeftDir = spatial.getWorldRotation().mult(Vector3f.UNIT_X);
        walkDirection.set(0,0,0);
        
        if(cooldown > 0){
            cooldown -= tpf;
            cooldown = Math.max(cooldown, 0);
        }
        
        if (forward) {
            walkDirection.addLocal(modelForwardDir.mult(moveSpeed));
        } else if (backward) {
            walkDirection.addLocal(modelForwardDir.negate().multLocal(moveSpeed));
        }
        if (leftStrafe) {
            walkDirection.addLocal(modelLeftDir.mult(moveSpeed));
        } else if (rightStrafe) {
            walkDirection.addLocal(modelLeftDir.negate().multLocal(moveSpeed));
        }
        
        setWalkDirection(walkDirection);
    }
    
    public void setCamera(Camera cam){
        CameraNode camNode = new CameraNode("CamNode", cam);
        camNode.setControlDir(CameraControl.ControlDirection.SpatialToCamera);
        head.attachChild(camNode);
        /**
         * Uncomment for chasecam
         */
        camNode.setLocalTranslation(new Vector3f(0, 5, -5));
        camNode.lookAt(head.getLocalTranslation(), Vector3f.UNIT_Y);
    }
    
    protected void rotate(float value){
        Quaternion rotate = new Quaternion().fromAngleAxis(FastMath.PI * value, Vector3f.UNIT_Y);
        rotate.multLocal(viewDirection);
        setViewDirection(viewDirection);
    }
    
    protected void lookUpDown(float value){
        yaw += value;
        yaw = FastMath.clamp(yaw, -FastMath.HALF_PI, FastMath.HALF_PI);
        head.setLocalRotation(new Quaternion().fromAngles(yaw, 0, 0));
    }
    
    public float getCooldown(){
        return cooldown;
    }
    
    public void onFire(){
        cooldown = cooldownTime;
    }
    
}
