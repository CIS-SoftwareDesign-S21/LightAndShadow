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
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.Event;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.ligthandshadow.componentsystem.LASUtils;
import org.terasology.ligthandshadow.componentsystem.components.LASTeamComponent;
import org.terasology.ligthandshadow.componentsystem.components.SetTeamOnActivateComponent;
import org.terasology.ligthandshadow.componentsystem.events.AddPlayerSkinToPlayerEvent;
import org.terasology.ligthandshadow.componentsystem.events.SetPlayerHealthHUDEvent;
import org.terasology.logic.characters.CharacterTeleportEvent;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.network.ClientComponent;
import org.terasology.registry.In;

/**
 * Teleports players to play arena once they chose their team.
 * It also sends events to change players skins and hud based on team they have chosen.
 *
 * @see ClientSkinSystem
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class TeleporterSystem extends BaseComponentSystem {
    private static final Logger logger = LoggerFactory.getLogger(TeleporterSystem.class);

    @In
    InventoryManager inventoryManager;
    @In
    EntityManager entityManager;

    /**
     * Depending on which teleporter the player chooses, they are set to that team
     * and teleported to that base
     *
     * @param event
     * @param entity
     */
    @ReceiveEvent(components = {SetTeamOnActivateComponent.class})
    public void onActivate(ActivateEvent event, EntityRef entity) {
        EntityRef player = event.getInstigator();
        String team = setPlayerTeamToTeleporterTeam(player, entity);
        handlePlayerTeleport(player, team);
    }

    private String setPlayerTeamToTeleporterTeam(EntityRef player, EntityRef teleporter) {
        LASTeamComponent teleporterTeamComponent = teleporter.getComponent(LASTeamComponent.class);
        LASTeamComponent playerTeamComponent = player.getComponent(LASTeamComponent.class);
        playerTeamComponent.team = teleporterTeamComponent.team;
        player.saveComponent(playerTeamComponent);
        return playerTeamComponent.team;
    }

    private void handlePlayerTeleport(EntityRef player, String team) {
        player.send(new CharacterTeleportEvent(LASUtils.getTeleportDestination(team)));
        inventoryManager.giveItem(player, EntityRef.NULL, entityManager.create(LASUtils.MAGIC_STAFF_URI));
        setPlayerSkin(player, team);
        setPlayerHud(player, team);
    }

    private void setPlayerSkin(EntityRef player, String team) {
        sendEventToClients(new AddPlayerSkinToPlayerEvent(player, team));
    }

    private void setPlayerHud(EntityRef player, String team) {
        sendEventToClients(new SetPlayerHealthHUDEvent(player, team));
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
