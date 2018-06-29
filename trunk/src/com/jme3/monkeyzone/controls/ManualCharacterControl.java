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
package com.jme3.monkeyzone.controls;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.monkeyzone.Globals;
import com.jme3.monkeyzone.messages.ActionMessage;
import com.jme3.network.Client;
import com.jme3.network.physicssync.PhysicsSyncManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;

/**
 * Manual character control, implements ManualControl interface and controls
 * a character if available on the Spatial.
 * @author normenhansen
 */
public class ManualCharacterControl extends NetworkedManualControl {

    private Spatial spatial;
    private CharacterControl characterControl;
    private Vector3f walkDirection = new Vector3f(Vector3f.ZERO);
    private Vector3f viewDirection = new Vector3f(Vector3f.UNIT_Z);
    private Vector3f directionLeft = new Vector3f(Vector3f.UNIT_X);
    private Quaternion directionQuat = new Quaternion();
    private Quaternion ROTATE_90 = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
    private float rotAmountX = 0;
    private float rotAmountY = 0;
    private float walkAmount = 0;
    private float strafeAmount = 0;
    private float speed = 10f * Globals.PHYSICS_FPS;
    private Vector3f tempVec = new Vector3f();

    public ManualCharacterControl() {
    }

    public ManualCharacterControl(Client client, long entityId) {
        super(client, entityId);
    }

    @Override
    public void doSteerX(float amount) {
        rotAmountX = amount;
    }

    @Override
    public void doSteerY(float amount) {
        rotAmountY = amount;
    }

    @Override
    public void doMoveX(float amount) {
        strafeAmount = amount;
    }

    @Override
    public void doMoveY(float amount) {
    }

    @Override
    public void doMoveZ(float amount) {
        walkAmount = amount;
    }

    @Override
    public void doPerformAction(int button, boolean pressed) {
        if (pressed && button == ActionMessage.JUMP_ACTION) {
            characterControl.jump();
        }
    }

    public Vector3f getAimDirection() {
        return viewDirection;
    }

    public Vector3f getLocation() {
        return characterControl.getPhysicsLocation(tempVec);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
        if (spatial == null) {
            return;
        }
        this.characterControl = spatial.getControl(CharacterControl.class);
        if (this.characterControl == null) {
            throw new IllegalStateException("Cannot add ManualCharacterControl to Spatial without CharacterControl");
        }
        Float spatialSpeed = (Float) spatial.getUserData("Speed");
        if (spatialSpeed != null) {
            speed = spatialSpeed * Globals.PHYSICS_FPS;
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void update(float tpf) {
        if (!enabled) {
            return;
        }

        //update if sync changed the directions
        if (!characterControl.getWalkDirection().equals(walkDirection) || !characterControl.getViewDirection().equals(viewDirection)) {
            walkDirection.set(characterControl.getWalkDirection());
            viewDirection.set(characterControl.getViewDirection()).normalizeLocal();
            directionLeft.set(viewDirection).normalizeLocal();
            ROTATE_90.multLocal(directionLeft);
        }

        walkDirection.set(viewDirection).multLocal(speed * walkAmount);
        walkDirection.addLocal(directionLeft.mult(speed * strafeAmount));

        if (rotAmountX != 0) {
            //rotate all vectors around the rotation amount
            directionQuat.fromAngleAxis((FastMath.PI) * tpf * rotAmountX, Vector3f.UNIT_Y);
            directionQuat.multLocal(walkDirection);
            directionQuat.multLocal(viewDirection);
            directionQuat.multLocal(directionLeft);
        }
        if (rotAmountY != 0) {
            directionQuat.fromAngleAxis((FastMath.PI) * tpf * rotAmountY, directionLeft);
            directionQuat.multLocal(viewDirection);
            if (viewDirection.getY() > 0.3f || viewDirection.getY() < -0.3f) {
                //rotate all vectors around the rotation amount
                directionQuat.fromAngleAxis((FastMath.PI) * tpf * -rotAmountY, directionLeft);
                directionQuat.multLocal(viewDirection);
            }
        }
        characterControl.setWalkDirection(walkDirection);
        characterControl.setViewDirection(viewDirection);
        //TODO: setting spatial rotation to avoid tilting
        spatial.getLocalRotation().lookAt(tempVec.set(viewDirection).multLocal(1, 0, 1), Vector3f.UNIT_Y);
        spatial.setLocalRotation(spatial.getLocalRotation());
    }

    public void render(RenderManager rm, ViewPort vp) {
    }
}
