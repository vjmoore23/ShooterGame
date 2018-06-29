/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package characters;

import com.jme3.ai.navmesh.NavMesh;
import com.jme3.ai.navmesh.NavMeshPathfinder;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Prof Wireman
 */
public class NavMeshNavigationControl extends AbstractControl {

    private PathfinderThread pathfinderThread;
    private Vector3f waypointPosition = null;

    public NavMeshNavigationControl(Node world) {

        Mesh mesh = ((Geometry) world.getChild("NavMesh")).getMesh();
        NavMesh navMesh = new NavMesh(mesh);

        pathfinderThread = new PathfinderThread(navMesh);
        pathfinderThread.start();
    }

    @Override
    protected void controlUpdate(float tpf) {
        Vector3f spatialPosition = spatial.getWorldTranslation();
        if(waypointPosition != null){
            Vector2f aiPosition = new Vector2f(spatialPosition.x, spatialPosition.z);
            Vector2f waypoint2D = new Vector2f(waypointPosition.x, waypointPosition.z);
            float distance = aiPosition.distance(waypoint2D);
            if(distance > 1f){
                Vector2f direction = waypoint2D.subtract(aiPosition);
                direction.mult(tpf);
                spatial.getControl(AICharacterControl.class).setViewDirection(new Vector3f(direction.x, 0, direction.y).normalize());
                spatial.getControl(AICharacterControl.class).onAction("MoveForward", true, 1);
            } else {
                waypointPosition = null;
            }
        } else if (!pathfinderThread.isPathfinding() && pathfinderThread.pathfinder.getNextWaypoint() != null && !pathfinderThread.pathfinder.isAtGoalWaypoint() ){
            pathfinderThread.pathfinder.goToNextWaypoint();
            System.out.println("Moving to next way point...");
            waypointPosition = new Vector3f(pathfinderThread.pathfinder.getWaypointPosition());
        } else {
            spatial.getControl(AICharacterControl.class).onAction("MoveForward", false, 1);
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    public void moveTo(Vector3f target) {
        pathfinderThread.setTarget(target);
    }
    
    private class PathfinderThread extends Thread {

        private Vector3f target;
        private NavMeshPathfinder pathfinder;
        private boolean pathfinding;
        private boolean running = true;

        public PathfinderThread(NavMesh navMesh) {
            pathfinder = new NavMeshPathfinder(navMesh);
            this.setDaemon(true);
        }

        @Override
        public void run() {
            while(running){
                if (target != null) {
                    pathfinding = true;
                    pathfinder.setPosition(getSpatial().getWorldTranslation());
                    boolean success = pathfinder.computePath(target);
                    System.out.println(success);
                    if (success) {
                        target = null;
                    } 
                    pathfinding = false;
                 
                }
                try {
                    Thread.sleep(1000);
                    System.out.println("Thread sleeping...");
                } catch (InterruptedException ex) {
                    Logger.getLogger(NavMeshNavigationControl.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        public void setTarget(Vector3f target) {
            this.target = target;
        }
        
        public boolean isPathfinding(){
            return pathfinding;
        }
    };
    
}