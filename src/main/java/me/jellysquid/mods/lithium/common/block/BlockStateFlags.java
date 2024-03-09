package me.jellysquid.mods.lithium.common.block;

import it.unimi.dsi.fastutil.objects.Reference2BooleanArrayMap;
import me.jellysquid.mods.lithium.common.ai.pathing.BlockStatePathingCache;
import me.jellysquid.mods.lithium.common.ai.pathing.PathNodeCache;
import me.jellysquid.mods.lithium.common.entity.FluidCachingEntity;
import me.jellysquid.mods.lithium.common.reflection.ReflectionUtil;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;

import java.util.ArrayList;

public class BlockStateFlags {
    public static final boolean ENABLED = BlockCountingSection.class.isAssignableFrom(ChunkSection.class);

    public static final int NUM_LISTENING_FLAGS;
    public static final ListeningBlockStatePredicate[] LISTENING_FLAGS;
    public static final int LISTENING_MASK_OR;

    //Listening Flag
    public static final ListeningBlockStatePredicate ANY;

    public static final int NUM_TRACKED_FLAGS;
    public static final TrackedBlockStatePredicate[] TRACKED_FLAGS;

    //Counting flags
    public static final TrackedBlockStatePredicate OVERSIZED_SHAPE;
    public static final TrackedBlockStatePredicate PATH_NOT_OPEN;
    public static final TrackedBlockStatePredicate ANY_FLUID;

    public static final TrackedBlockStatePredicate[] FLAGS;

    //Non counting flags
    public static final TrackedBlockStatePredicate ENTITY_TOUCHABLE;

    static {
        Reference2BooleanArrayMap<ListeningBlockStatePredicate> listeningFlags = new Reference2BooleanArrayMap<>();

        ANY = new ListeningBlockStatePredicate(listeningFlags.size()) {
            @Override
            public boolean test(BlockState operand) {
                return true;
            }
        };
        //false -> we listen to changes of all blocks that pass the predicate test.
        //true -> we only listen to changes of the predicate test result
        listeningFlags.put(ANY, false);

        NUM_LISTENING_FLAGS = listeningFlags.size();
        int listenMaskOR = 0;
        int iteration = 0;
        for (var entry : listeningFlags.reference2BooleanEntrySet()) {
            boolean listenOnlyXOR = entry.getBooleanValue();
            listenMaskOR |= listenOnlyXOR ? 0 : 1 << iteration;
        }
        LISTENING_MASK_OR = listenMaskOR;
        LISTENING_FLAGS = listeningFlags.keySet().toArray(new ListeningBlockStatePredicate[NUM_LISTENING_FLAGS]);


        ArrayList<TrackedBlockStatePredicate> countingFlags = new ArrayList<>(listeningFlags.keySet());

        OVERSIZED_SHAPE = new TrackedBlockStatePredicate(countingFlags.size()) {
            @Override
            public boolean test(BlockState operand) {
                return operand.exceedsCube();
            }
        };
        countingFlags.add(OVERSIZED_SHAPE);

        if (FluidCachingEntity.class.isAssignableFrom(Entity.class)) {
            ANY_FLUID = new TrackedBlockStatePredicate(countingFlags.size()) {
                @Override
                public boolean test(BlockState operand) {
                    return !operand.getFluidState().isEmpty();
                }
            };
            countingFlags.add(ANY_FLUID);
        } else {
            ANY_FLUID = null;
        }

        if (BlockStatePathingCache.class.isAssignableFrom(AbstractBlock.AbstractBlockState.class)) {
            PATH_NOT_OPEN = new TrackedBlockStatePredicate(countingFlags.size()) {
                @Override
                public boolean test(BlockState operand) {
                    return PathNodeCache.getNeighborPathNodeType(operand) != PathNodeType.OPEN;
                }
            };
            countingFlags.add(PATH_NOT_OPEN);
        } else {
            PATH_NOT_OPEN = null;
        }

        NUM_TRACKED_FLAGS = countingFlags.size();
        TRACKED_FLAGS = countingFlags.toArray(new TrackedBlockStatePredicate[NUM_TRACKED_FLAGS]);

        ArrayList<TrackedBlockStatePredicate> flags = new ArrayList<>(countingFlags);

        ENTITY_TOUCHABLE = new TrackedBlockStatePredicate(countingFlags.size()) {
            //How to find the remapped methods:
            //1) Run in the debugger: System.out.println(FabricLoader.getInstance().getMappingResolver().getNamespaceData("intermediary").methodNames)
            //2) Ctrl+F for the method name, in this case "onEntityCollision". Make sure to find the correct one.
            private final String remapped_onEntityCollision = ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_7892_");
            @Override
            public boolean test(BlockState operand) {
                return ReflectionUtil.hasMethodOverride(operand.getBlock().getClass(), AbstractBlock.class, true, this.remapped_onEntityCollision, BlockState.class, World.class, BlockPos.class, Entity.class);
            }
        };
        flags.add(ENTITY_TOUCHABLE);

        FLAGS = flags.toArray(new TrackedBlockStatePredicate[0]);
    }
}
