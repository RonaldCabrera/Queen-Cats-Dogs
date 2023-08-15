package net.pevori.queencats.entity.custom;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import net.minecraftforge.network.NetworkHooks;
import net.pevori.queencats.gui.menu.HumanoidAnimalMenu;
import org.jetbrains.annotations.Nullable;

public class HumanoidAnimalEntity extends TamableAnimal implements ContainerListener, HasCustomInventoryScreen, MenuProvider {
    protected static final String INVENTORY_KEY = "Humanoid_Animal_Inventory";
    protected static final String ARMOR_KEY = "Humanoid_Animal_Armor_Item";
    protected static final String SLOT_KEY = "Humanoid_Animal_Inventory_Slot";
    protected SimpleContainer inventory;
    protected Ingredient equippableArmor = Ingredient.of(Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.GOLDEN_CHESTPLATE,
            Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE);

    protected HumanoidAnimalEntity(EntityType<? extends TamableAnimal> entityType, Level world) {
        super(entityType, world);
        this.inventory = new SimpleContainer(19);

        // Allows pathfinding through doors
        ((GroundPathNavigation) this.getNavigation()).setCanOpenDoors(true);
        ((GroundPathNavigation) this.getNavigation()).setCanPassDoors(true);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        // Allows them to actually open doors to walk through them, just like villagers.
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        Level level = this.getCommandSenderWorld();
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide &&  this.isOwnedBy(player) && !this.isBaby() && this.hasArmorSlot() && this.isEquippableArmor(itemStack) && !this.hasArmorInSlot()) {
            this.equipArmor(player, itemStack);
            this.equipArmor(itemStack);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()) {
            NetworkHooks.openScreen(serverPlayer, this, buf -> {
                buf.writeInt(this.getId());
            });
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    public boolean hasArmorSlot() {
        return !isBaby();
    }

    public boolean hasArmorInSlot() {
        return this.hasItemInSlot(EquipmentSlot.CHEST);
    }

    public boolean isEquippableArmor(ItemStack itemStack){
        return equippableArmor.test(itemStack);
    }

    public void equipArmor(Player player, ItemStack stack) {
        if (this.isEquippableArmor(stack)) {
            this.inventory.setItem(0, new ItemStack(stack.getItem()));
            this.playSound(SoundEvents.HORSE_ARMOR, 0.5F, 1.0F);
            this.inventory.setChanged();

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
    }

    public void equipArmor(ItemStack stack) {
        if (this.isEquippableArmor(stack)) {
            this.setItemSlot(EquipmentSlot.CHEST, stack);
            this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        }
    }

    public void unEquipArmor(){
        this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (this.inventory != null) {
            for(int i = 0; i < this.inventory.getContainerSize(); ++i) {
                ItemStack itemStack = this.inventory.getItem(i);
                if (!itemStack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(itemStack)) {
                    this.spawnAtLocation(itemStack);
                }
            }
        }
    }

    @Override
    public void containerChanged(Container pContainer) {
        //this.inventory = pContainer;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        ListTag listTag = new ListTag();

        // Writes the armor slot (slot 0)
        if (!this.inventory.getItem(0).isEmpty()) {
            nbt.put(ARMOR_KEY, this.inventory.getItem(0).save(new CompoundTag()));
        }

        for(int i = 1; i < this.inventory.getContainerSize(); ++i) {
            ItemStack itemStack = this.inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.putByte(SLOT_KEY, (byte)i);
                itemStack.save(compoundtag);
                listTag.add(compoundtag);
            }
        }

        nbt.put(INVENTORY_KEY, listTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        ListTag nbtList = nbt.getList(INVENTORY_KEY, 10);

        // Reads the armor slot (slot 0)
        if (nbt.contains(ARMOR_KEY, 10)) {
            ItemStack itemStack = ItemStack.of(nbt.getCompound(ARMOR_KEY));

            if (!itemStack.isEmpty() && this.isEquippableArmor(itemStack)) {
                this.inventory.setItem(0, itemStack);
                this.equipArmor(itemStack);
            }
        }

        // Reads the rest of the inventory (slot 2 to 18th)
        for(int i = 0; i < nbtList.size(); ++i) {
            CompoundTag compoundtag = nbtList.getCompound(i);
            int j = compoundtag.getByte(SLOT_KEY) & 255;
            if (j >= 1 && j < this.inventory.getContainerSize()) {
                this.inventory.setItem(j, ItemStack.of(compoundtag));
            }
        }
    }

    protected void setInventory(SimpleContainer inventory){
        // Reads the rest of the inventory (slot 2 to 18th)
        for(int i = 0; i < inventory.getContainerSize(); ++i) {
            this.inventory.setItem(i, inventory.getItem(i));
            this.inventory.setChanged();
        }
    }

    @Override
    public boolean wantsToAttack(LivingEntity entity, LivingEntity owner) {
        if (!(entity instanceof Creeper) && !(entity instanceof Ghast)) {
            if (entity instanceof HumanoidAnimalEntity) {
                HumanoidAnimalEntity humanoid = (HumanoidAnimalEntity)entity;
                return !humanoid.isTame() || humanoid.getOwner() != owner;
            } else if (entity instanceof Player && owner instanceof Player && !((Player)owner).canHarmPlayer((Player)entity)) {
                return false;
            } else if (entity instanceof AbstractHorse && ((AbstractHorse)entity).isTamed()) {
                return false;
            } else {
                return !(entity instanceof TamableAnimal) || !((TamableAnimal)entity).isTame();
            }
        } else {
            return false;
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        //We provide this to the screenHandler as our class Implements Inventory
        //Only the Server has the Inventory at the start, this will be synced to the client in the ScreenHandler
        return new HumanoidAnimalMenu(syncId, playerInventory, inventory);
    }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public void openCustomInventoryScreen(Player pPlayer) {

    }
}