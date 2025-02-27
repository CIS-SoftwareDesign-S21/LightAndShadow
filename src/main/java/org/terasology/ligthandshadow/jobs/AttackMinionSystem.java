/*
 * Copyright 2014 MovingBlocks
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
package org.terasology.ligthandshadow.jobs;

import com.google.common.collect.Lists;
import org.terasology.engine.core.SimpleUri;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.registry.In;
import org.terasology.minion.work.Work;
import org.terasology.minion.work.WorkFactory;
import org.terasology.navgraph.WalkableBlock;
import org.terasology.pathfinding.componentSystem.PathfinderSystem;

import java.util.List;

/**
 * Created by synopia on 01.02.14.
 */
@RegisterSystem
public class AttackMinionSystem extends BaseComponentSystem {
    @In
    private PathfinderSystem pathfinderSystem;
    @In
    private WorkFactory workFactory;

    public AttackMinionSystem() {
    }

    @Override
    public void initialise() {
        workFactory.register(new AttackMinionWork("LightAndShadow:attackMinionBlack"));
        workFactory.register(new AttackMinionWork("LightAndShadow:attackMinionRed"));
    }

    @Override
    public void shutdown() {
    }


    public class AttackMinionWork implements Work {
        private final SimpleUri uri;
        private float duration = 5;

        public AttackMinionWork(String uri) {
            this.uri = new SimpleUri(uri);
        }

        @Override
        public List<WalkableBlock> getTargetPositions(EntityRef enemy) {
            List<WalkableBlock> targetPositions = Lists.newArrayList();
            WalkableBlock walkableBlock = pathfinderSystem.getBlock(enemy);
            if (walkableBlock != null) {
                targetPositions.add(walkableBlock);
            }

            return targetPositions;
        }

        @Override
        public boolean canMinionWork(EntityRef enemy, EntityRef minion) {
            WalkableBlock actualBlock = pathfinderSystem.getBlock(minion);
            WalkableBlock expectedBlock = pathfinderSystem.getBlock(enemy);

            return actualBlock == expectedBlock;
        }

        @Override
        public boolean isAssignable(EntityRef enemy) {
            WalkableBlock walkableBlock = pathfinderSystem.getBlock(enemy);
            return walkableBlock != null;
        }

        @Override
        public void letMinionWork(EntityRef block, EntityRef minion) {
        }

        @Override
        public float cooldownTime() {
            return duration;
        }

        @Override
        public boolean isRequestable(EntityRef block) {
            return true;
        }

        @Override
        public SimpleUri getUri() {
            return uri;
        }
    }
}
