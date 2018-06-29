/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package characters;

import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 *
 * @author Prof Wireman
 */
public class AICharacterControl extends BetterCharacterControl 
        implements ActionListener, AnalogListener {
    
    boolean forward = false, backward = false;
    protected Node head = new Node("Head");
    private float yaw = 0;
    protected float moveSpeed = 3;
    
    public AICharacterControl(float radius, float height, float mass) {
        super(radius, height, mass);
        head.setLocalTranslation(0, 1.8f, 0);
    }
    
    public void onAction(String action, boolean isPressed, float tpf) {
        if (action.equals("MoveForward")) {
            forward = isPressed;
        } else if (action.equals("MoveBackward")) {
            backward = isPressed;
        } 
    }
    
    public void onAnalog(String name, float value, float tpf) {
        
        if (name.equals("MoveForward") || name.equals("MoveBackward")){
            moveSpeed = value * 3;
        }
        
    }
    
    public void update(float tpf) {
        super.update(tpf);
        Vector3f modelForwardDir = spatial.getWorldRotation().mult(Vector3f.UNIT_Z);
        
        walkDirection.set(0,0,0);
              
        if (forward) {
            walkDirection.addLocal(modelForwardDir.mult(moveSpeed));
        } else if (backward) {
            walkDirection.addLocal(modelForwardDir.negate().multLocal(moveSpeed));
        }
        
        setWalkDirection(walkDirection);
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
    
}