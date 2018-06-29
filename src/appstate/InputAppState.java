package appstate;

import characters.MyGameCharacterControl;
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.InputManager;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.scene.Geometry;
import java.util.ArrayList;
import java.util.List;

public class InputAppState extends AbstractAppState implements AnalogListener, ActionListener {
    
     private Application app;
    private InputManager inputManager;
    private MyGameCharacterControl character; //The Custom Character Control
    private float sensitivity = 5000;

    List<Geometry> targets = new ArrayList<Geometry>();
    
    public enum InputMapping{
        
        LeanLeft,
        LeanRight,
        LeanFree,
        RotateLeft,
        RotateRight,
        LookUp,
        LookDown,
        StrafeLeft,
        StrafeRight,
        MoveForward,
        MoveBackward,
        Fire;
    }
    
    private String[] mappingNames = new String[]{InputMapping.LeanFree.name(), InputMapping.LeanLeft.name(), InputMapping.LeanRight.name(), InputMapping.RotateLeft.name(), InputMapping.RotateRight.name(), InputMapping.LookUp.name(), InputMapping.LookDown.name(), InputMapping.StrafeLeft.name(), InputMapping.StrafeRight.name(), InputMapping.MoveForward.name(), InputMapping.MoveBackward.name()};
    
    
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = app;
        this.inputManager = app.getInputManager();
        addInputMappings();
        assignJoysticks();
    }
    
    private void addInputMappings(){
        inputManager.addMapping(InputMapping.LeanFree.name(), new KeyTrigger(KeyInput.KEY_V));
        inputManager.addMapping(InputMapping.LeanLeft.name(), new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping(InputMapping.LeanRight.name(), new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping(InputMapping.RotateLeft.name(), new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping(InputMapping.RotateRight.name(), new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping(InputMapping.LookUp.name(), new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping(InputMapping.LookDown.name(), new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping(InputMapping.StrafeLeft.name(), new KeyTrigger(KeyInput.KEY_H), new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping(InputMapping.StrafeRight.name(), new KeyTrigger(KeyInput.KEY_K), new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping(InputMapping.MoveForward.name(), new KeyTrigger(KeyInput.KEY_U), new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping(InputMapping.MoveBackward.name(), new KeyTrigger(KeyInput.KEY_J), new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addListener(this, mappingNames);
        
        inputManager.addMapping(InputMapping.Fire.name(), new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    }
    /**
    * 
    */
    private void assignJoysticks(){
        Joystick[] joysticks = inputManager.getJoysticks();
        if (joysticks != null){
            for( Joystick j : joysticks ) {
                for(JoystickAxis axis : j.getAxes()){
                    if(axis == j.getXAxis()){
                        axis.assignAxis(InputMapping.StrafeRight.name(), InputMapping.StrafeLeft.name());
                    } else if ( axis == j.getYAxis()){
                        axis.assignAxis(InputMapping.MoveBackward.name(), InputMapping.MoveForward.name());
                    } else if (axis.getLogicalId().equals("ry")){
                        axis.assignAxis(InputMapping.LookDown.name(), InputMapping.LookUp.name());
                    } else if(axis.getLogicalId().equals("rx")){
                        axis.assignAxis(InputMapping.RotateRight.name(), InputMapping.RotateLeft.name());
                    }
                }
                
                for(JoystickButton button : j.getButtons()){
                    button.assignButton("Fire");
                }
            }

            
        }
    }
    /**
    * 
    */
    @Override
    public void cleanup() {
        super.cleanup();
        for (InputMapping i : InputMapping.values()) {
            if (inputManager.hasMapping(i.name())) {
                inputManager.deleteMapping(i.name());
            }
        }
        inputManager.removeListener(this);
    }
    
    public void onAnalog(String action, float value, float tpf) {
        if(character != null){
            character.onAnalog(action, value * sensitivity, tpf);
        }
    }

    public void onAction(String name, boolean isPressed, float tpf) {
        if(character != null){
            
           if (name.equals("Fire")) {
               if (isPressed && character.getCooldown() == 0f){
                   fire();
               }
           } else {
           
               character.onAction(name, isPressed, tpf);
           }
        }
    }
    
    public void setCharacter(MyGameCharacterControl character){
        this.character = character;
    }
    
    public void setTargets(List<Geometry> targets){
        this.targets = targets;
    }
    
    public List<Geometry>  getTargets(){
        return targets;
    }
    
    public void fire(){
     
       
        if(character != null){
            Ray r = new Ray(app.getCamera().getLocation(), app.getCamera().getDirection());
         
            CollisionResults collRes = new CollisionResults();
            for(Geometry g: targets){
                g.collideWith(r, collRes);
            }
            if(collRes.size() > 0){
                System.out.println( "HIT!");
            }
            character.onFire();
        }
       
    }
    
    
}

