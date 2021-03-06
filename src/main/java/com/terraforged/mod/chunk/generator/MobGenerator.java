/*
 * MIT License
 *
 * Copyright (c) 2020 TerraForged
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.terraforged.mod.chunk.generator;

import com.terraforged.mod.chunk.TFChunkGenerator;
import net.minecraft.entity.EntityClassification;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.MobSpawnInfo;
import net.minecraft.world.gen.WorldGenRegion;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.CatSpawner;
import net.minecraft.world.spawner.PatrolSpawner;
import net.minecraft.world.spawner.PhantomSpawner;
import net.minecraft.world.spawner.WorldEntitySpawner;
import net.minecraftforge.common.world.StructureSpawnManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobGenerator implements Generator.Mobs {

    // may be accessed cross-thread
    private static volatile boolean mobSpawning = true;

    private final CatSpawner catSpawner = new CatSpawner();
    private final PatrolSpawner patrolSpawner = new PatrolSpawner();
    private final PhantomSpawner phantomSpawner = new PhantomSpawner();
    private final TFChunkGenerator generator;

    public MobGenerator(TFChunkGenerator generator) {
        this.generator = generator;
    }

    @Override
    public final void generateMobs(WorldGenRegion region) {
        // vanilla does NOT check the mobSpawning gamerule before calling this
        if (MobGenerator.mobSpawning) {
            int chunkX = region.getMainChunkX();
            int chunkZ = region.getMainChunkZ();
            Biome biome = region.getChunk(chunkX, chunkZ).getBiomes().getNoiseBiome(0, 0, 0);
            SharedSeedRandom sharedseedrandom = new SharedSeedRandom();
            sharedseedrandom.setDecorationSeed(region.getSeed(), chunkX << 4, chunkZ << 4);
            WorldEntitySpawner.performWorldGenSpawning(region, biome, chunkX, chunkZ, sharedseedrandom);
        }
    }

    @Override
    public final void tickSpawners(ServerWorld world, boolean hostile, boolean peaceful) {
        phantomSpawner.func_230253_a_(world, hostile, peaceful);
        patrolSpawner.func_230253_a_(world, hostile, peaceful);
        catSpawner.func_230253_a_(world, hostile, peaceful);
    }

    @Override
    public List<MobSpawnInfo.Spawners> getSpawns(Biome biome, StructureManager structures, EntityClassification type, BlockPos pos) {
        List<MobSpawnInfo.Spawners> spawns = StructureSpawnManager.getStructureSpawns(structures, type, pos);
        if (spawns != null) {
            return spawns;
        }

        if (structures.getStructureStart(pos, true, Structure.SWAMP_HUT).isValid()) {
            if (type == EntityClassification.MONSTER) {
                return Structure.SWAMP_HUT.getSpawnList();
            }

            if (type == EntityClassification.CREATURE) {
                return Structure.SWAMP_HUT.getCreatureSpawnList();
            }
        }

        if (type == EntityClassification.MONSTER) {
            if (structures.getStructureStart(pos, false, Structure.PILLAGER_OUTPOST).isValid()) {
                return Structure.PILLAGER_OUTPOST.getSpawnList();
            }

            if (structures.getStructureStart(pos, false, Structure.MONUMENT).isValid()) {
                return Structure.MONUMENT.getSpawnList();
            }

            if (structures.getStructureStart(pos, true, Structure.FORTRESS).isValid()) {
                return Structure.FORTRESS.getSpawnList();
            }
        }

        return biome.getMobSpawnInfo().getSpawners(type);
    }

    @SubscribeEvent
    public static void tick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.side.isServer()) {
            mobSpawning = event.world.getGameRules().get(GameRules.DO_MOB_SPAWNING).get();
        }
    }
}
