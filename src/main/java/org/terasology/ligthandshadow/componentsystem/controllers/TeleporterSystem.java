/*
 * Copyright 2017 MovingBlocks
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
package org.terasology.ligthandshadow.componentsystem.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.entity.EntityBuilder;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.Event;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.ligthandshadow.componentsystem.LASUtils;
import org.terasology.ligthandshadow.componentsystem.components.HasFlagComponent;
import org.terasology.ligthandshadow.componentsystem.components.LASTeamComponent;
import org.terasology.ligthandshadow.componentsystem.components.SetTeamOnActivateComponent;
import org.terasology.ligthandshadow.componentsystem.events.AddPlayerSkinToPlayerEvent;
import org.terasology.ligthandshadow.componentsystem.events.ScoreUpdateFromServerEvent;
import org.terasology.logic.characters.CharacterTeleportEvent;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3f;
import org.terasology.network.ClientComponent;
import org.terasology.registry.In;

@RegisterSystem(RegisterMode.AUTHORITY)

public class TeleporterSystem extends BaseComponentSystem {
    private static final Logger logger = LoggerFactory.getLogger(TeleporterSystem.class);
    private static final String MAGIC_STAFF_URI = "LightAndShadowResources:magicStaff";
    @In
    InventoryManager inventoryManager;
    @In
    EntityManager entityManager;

    private EntityBuilder builder;

    // The position near the team's base that player will be teleported to on choosing a team
    private static final Vector3f RED_TELEPORT_DESTINATION = new Vector3f(29, 12, 0);
    private static final Vector3f BLACK_TELEPORT_DESTINATION = new Vector3f(-29, 12, 0);

    @ReceiveEvent(components = {SetTeamOnActivateComponent.class})
    public void onActivate(ActivateEvent event, EntityRef entity) {
        LASTeamComponent teleporterTeamComponent = entity.getComponent(LASTeamComponent.class);
        EntityRef player = event.getInstigator();
        LASTeamComponent playerTeamComponent = player.getComponent(LASTeamComponent.class);

        /* Depending on which teleporter the player chooses, they are set to that team
         * and teleported to that base */
        String teleporterTeam = teleporterTeamComponent.team;
        playerTeamComponent.team = teleporterTeam;
        player.saveComponent(playerTeamComponent);
        if (teleporterTeam.equals(LASUtils.RED_TEAM)) {
            player.send(new CharacterTeleportEvent(new Vector3f(RED_TELEPORT_DESTINATION)));
            inventoryManager.giveItem(player, EntityRef.NULL, entityManager.create(MAGIC_STAFF_URI));
            setPlayerSkin(player, teleporterTeam);
            return;
        }
        if (teleporterTeam.equals(LASUtils.BLACK_TEAM)) {
            player.send(new CharacterTeleportEvent(new Vector3f(BLACK_TELEPORT_DESTINATION)));
            inventoryManager.giveItem(player, EntityRef.NULL, entityManager.create(MAGIC_STAFF_URI));
            setPlayerSkin(player, teleporterTeam);
            return;
        }
    }

    private void setPlayerSkin(EntityRef player, String team) {
        if (team.equals(LASUtils.RED_TEAM)) {
            builder = entityManager.newBuilder(LASUtils.RED_PAWN);
            builder.saveComponent(player.getComponent(LocationComponent.class));
            builder.build();
            sendEventToClients(new AddPlayerSkinToPlayerEvent(team, player));
            return;
        }
        if (team.equals(LASUtils.BLACK_TEAM)) {
            builder = entityManager.newBuilder(LASUtils.BLACK_PAWN);
            builder.saveComponent(player.getComponent(LocationComponent.class));
            builder.build();
            sendEventToClients(new AddPlayerSkinToPlayerEvent(team, player));
            return;
        }
    }

    private void sendEventToClients(Event event) {
        if (entityManager.getCountOfEntitiesWith(ClientComponent.class) != 0) {
            Iterable<EntityRef> clients = entityManager.getEntitiesWith(ClientComponent.class);
            for (EntityRef client : clients) {
                client.send(event);
            }
        }
    }
}
