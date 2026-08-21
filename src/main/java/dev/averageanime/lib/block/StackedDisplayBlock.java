package dev.averageanime.lib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Supplier;

/**
 * A block displaying a countable stack of one item, facing the placer.
 *
 * <p>Plates of food, stacked ingots, anything set down in a pile: the block holds a count rather than
 * an inventory, so the state alone says what to draw. Adding and removing go one at a time, except with
 * a wrench, which takes the lot.
 *
 * <p>Emits a comparator signal proportional to how full it is, so a stack is readable by redstone
 * without a block entity.
 *
 * <p>What happens when the last item leaves is the subclass's business -- a plate stays as an empty
 * plate, a bare pile removes itself -- hence {@link #handleLastItemRemoved}.
 */
public abstract class StackedDisplayBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty STACK_SIZE = IntegerProperty.create("stack_size", 1, 12);

    /** Held in hand, right-click takes the whole stack rather than one at a time. */
    public static final TagKey<Item> WRENCH = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "tools/wrench")
    );

    public final int maxStackSize;
    public final Supplier<Item> displayItem;
    protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0);

    public StackedDisplayBlock(Properties properties, Supplier<Item> displayItem, int maxStackSize) {
        super(properties);
        this.displayItem = displayItem;
        this.maxStackSize = Math.clamp(maxStackSize, 1, 12);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(STACK_SIZE, this.maxStackSize));
    }
    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }
    protected ItemInteractionResult addItem(BlockState state, Level level, BlockPos pos, Player player, ItemStack heldStack) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        int currentStack = state.getValue(STACK_SIZE);
        if (currentStack >= maxStackSize) return ItemInteractionResult.FAIL;

        level.setBlock(pos, state.setValue(STACK_SIZE, currentStack + 1), 3);
        if (!player.isCreative()) heldStack.shrink(1);
        level.playSound(null, pos, getAddSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return ItemInteractionResult.SUCCESS;
    }
    protected ItemInteractionResult wrenchPickup(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        Direction direction = player.getDirection().getOpposite();
        ItemSpawns.spawnItemEntity(level, new ItemStack(this.asItem()),
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                direction.getStepX() * 0.15, 0.05, direction.getStepZ() * 0.15);
        level.removeBlock(pos, false);
        level.playSound(null, pos, getRemoveSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        return ItemInteractionResult.SUCCESS;
    }
    protected void removeItem(BlockState state, Level level, BlockPos pos, Player player) {
        int currentStack = state.getValue(STACK_SIZE);
        Direction direction = player.getDirection().getOpposite();
        ItemStack dropStack = new ItemStack(this.displayItem.get());
        ItemSpawns.spawnItemEntity(level, dropStack,
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                direction.getStepX() * 0.15, 0.05, direction.getStepZ() * 0.15);
        if (currentStack > 1) {
            level.setBlock(pos, state.setValue(STACK_SIZE, currentStack - 1), 3);
        } else {
            handleLastItemRemoved(state, level, pos);
        }
        level.playSound(null, pos, getRemoveSound(), SoundSource.BLOCKS, 0.8F, 0.8F);
    }
    protected void removeAllItems(BlockState state, Level level, BlockPos pos, Player player) {
        int currentStack = state.getValue(STACK_SIZE);
        Direction direction = player.getDirection().getOpposite();
        ItemStack dropStack = new ItemStack(this.displayItem.get(), currentStack);
        ItemSpawns.spawnItemEntity(level, dropStack,
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                direction.getStepX() * 0.15, 0.05, direction.getStepZ() * 0.15);
        handleLastItemRemoved(state, level, pos);
        level.playSound(null, pos, getRemoveSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STACK_SIZE);
    }
    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) { return true; }
    @Override
    public int getAnalogOutputSignal(BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        return state.getValue(STACK_SIZE);
    }
    protected abstract void handleLastItemRemoved(BlockState state, Level level, BlockPos pos);
    public SoundEvent getAddSound() { return SoundEvents.ITEM_FRAME_ADD_ITEM; }
    protected SoundEvent getRemoveSound() { return SoundEvents.ITEM_FRAME_REMOVE_ITEM; }
}
