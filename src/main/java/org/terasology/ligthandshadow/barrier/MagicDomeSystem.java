/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.ligthandshadow.barrier;

import org.joml.Math;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.audio.events.PlaySoundEvent;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.itemRendering.components.AnimateRotationComponent;
import org.terasology.logic.characters.CharacterImpulseEvent;
import org.terasology.logic.characters.CharacterMoveInputEvent;
import org.terasology.logic.console.commandSystem.annotations.Command;
import org.terasology.logic.location.LocationComponent;
import org.terasology.registry.In;
import org.terasology.utilities.random.FastRandom;
import org.terasology.world.RelevanceRegionComponent;

@RegisterSystem
public class MagicDomeSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final Logger logger = LoggerFactory.getLogger(MagicDomeSystem.class);

    private static final int WORLD_RADIUS = 500;

    @In
    private EntityManager entityManager;

    private Vector3f lastPos = new Vector3f();
    private EntityRef magicDomeEntity = EntityRef.NULL;
    private float updateDelta;
    private FastRandom random = new FastRandom();

    @Override
    public void postBegin() {
        //toggleDome();
    }

    @Command(shortDescription = "Activate/Deactivate dome barrier", helpText = "Activates or deactivates the dome barrier around the world", runOnServer = true)
    public String dome() {
        toggleDome();
        return "Toggled dome.";
    }

    public void toggleDome() {

        if (!entityManager.getEntitiesWith(MagicDome.class).iterator().hasNext()) {

            magicDomeEntity = entityManager.create("lightAndShadowResources:magicDome", new Vector3f());
            LocationComponent loc = magicDomeEntity.getComponent(LocationComponent.class);
            loc.setWorldScale(2 * WORLD_RADIUS * 1.01f);
            magicDomeEntity.addOrSaveComponent(loc);
            magicDomeEntity.addOrSaveComponent(new RelevanceRegionComponent());
            magicDomeEntity.setAlwaysRelevant(true);

            AnimateRotationComponent rotationComponent = new AnimateRotationComponent();
            rotationComponent.rollSpeed = 0.01f;
            rotationComponent.pitchSpeed = 0.01f;
            rotationComponent.yawSpeed = 0.01f;
            magicDomeEntity.addOrSaveComponent(rotationComponent);
        } else {
            magicDomeEntity.destroy();
        }
    }

    @ReceiveEvent(components = {LocationComponent.class})
    public void onCharacterMovement(CharacterMoveInputEvent moveInputEvent, EntityRef player, LocationComponent loc) {
        Vector3f pos = new Vector3f(loc.getWorldPosition(new Vector3f()));

        float distance = pos.length();

        float deltaDistance = Math.abs(pos.distance(lastPos));

        for (EntityRef domeEntity : entityManager.getEntitiesWith(MagicDome.class, LocationComponent.class)) {
            LocationComponent domeLocationComponent = domeEntity.getComponent(LocationComponent.class);
            Vector3f domeCenter = domeLocationComponent.getWorldPosition(new Vector3f());
            Vector3f domeVerticalTop = domeCenter.add(0, WORLD_RADIUS, 0);

            MagicDome dome = domeEntity.getComponent(MagicDome.class);

            if (deltaDistance > 0.2f) {
//                logger.info("CharacerMoveInputEvent: position: {} - distance from O: {}, delta: {}", pos, distance, deltaDistance);

                if (lastPos.length() < WORLD_RADIUS && lastPos.length() < pos.length() && distance > WORLD_RADIUS) {
//                    logger.info("Sending player back inside!");
                    Vector3f impulse = new Vector3f(pos).normalize().negate();
                    impulse.mul(64.0f);
                    impulse.y = 6;

                    player.send(new CharacterImpulseEvent(impulse));
                    player.send(new PlaySoundEvent(magicDomeEntity.getComponent(MagicDome.class).hitSound, 2f));
                }

                if (lastPos.length() > WORLD_RADIUS && lastPos.length() > pos.length() && distance < WORLD_RADIUS) {
//                    logger.info("Sending player back outside");
                    Vector3f impulse = new Vector3f(pos).normalize();
                    float verticalDiff = Math.sqrt(Math.abs(Math.sqrt(lastPos.y()) - Math.sqrt(domeVerticalTop.y())));
                    float impulseY = (Math.abs(verticalDiff) / (float) WORLD_RADIUS) * 3f;

                    impulse.mul(64);
                    impulse.y += impulseY;
                    player.send(new CharacterImpulseEvent(impulse));

                    player.send(new PlaySoundEvent(magicDomeEntity.getComponent(MagicDome.class).hitSound, 2f));

                }
                lastPos.set(pos);
            }
        }
    }

    @Override
    public void update(float delta) {
        updateDelta += delta;
        if (updateDelta > 2.0f) {
            for (EntityRef entity : entityManager.getEntitiesWith(MagicDome.class, AnimateRotationComponent.class)) {
                AnimateRotationComponent rotationComponent = entity.getComponent(AnimateRotationComponent.class);

                rotationComponent.yawSpeed = Math.clamp(-0.01f, 0.01f, rotationComponent.yawSpeed + random.nextFloat(-0.01f, 0.01f));
                rotationComponent.pitchSpeed = Math.clamp(-0.01f, 0.01f, rotationComponent.pitchSpeed + random.nextFloat(-0.01f, 0.01f));
                rotationComponent.rollSpeed = Math.clamp(-0.01f, 0.01f, rotationComponent.rollSpeed + random.nextFloat(-0.01f, 0.01f));

                entity.saveComponent(rotationComponent);
            }
            updateDelta = 0;
        }
    }
}
