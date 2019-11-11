package com.github.ksgfk.dawnfoundation.api;

import com.github.ksgfk.dawnfoundation.DawnFoundation;
import com.github.ksgfk.dawnfoundation.api.annotations.*;
import com.github.ksgfk.dawnfoundation.api.utility.BoolFunction1;
import net.minecraft.block.Block;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author KSGFK create in 2019/11/6
 */
public class ModInfo {
    private String modId;
    private boolean isClient;
    private List<Item> items = new LinkedList<>();
    private List<Block> blocks = new LinkedList<>();
    private List<Enchantment> enchants = new LinkedList<>();
    private List<Potion> potions = new LinkedList<>();
    private List<PotionType> potionTypes = new LinkedList<>();
    private List<Pair<OreDict, Object>> oreDicts = new LinkedList<>();
    private List<Pair<Smeltable, Object>> smeltables = new LinkedList<>();
    private List<EntityEntry> entities = null;
    private List<Class<? extends TileEntity>> tileEntities = null;
    private List<VillagerRegistry.VillagerProfession> villager = new LinkedList<>();
    private List<Biome> biomes = new LinkedList<>();
    private List<SoundEvent> sounds = new LinkedList<>();
    //Client
    private List<Object> guiHandlers = null;
    private List<KeyBinding> keyBindings = null;

    private ModInfo() {
    }

    public String getModId() {
        return modId;
    }

    public List<Item> getItems() {
        return items;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public List<Enchantment> getEnchantments() {
        return enchants;
    }

    public List<Potion> getPotions() {
        return potions;
    }

    public List<PotionType> getPotionTypes() {
        return potionTypes;
    }

    public List<Pair<OreDict, Object>> getOreDicts() {
        return oreDicts;
    }

    public List<Pair<Smeltable, Object>> getSmeltables() {
        return smeltables;
    }

    public List<EntityEntry> getEntities() {
        return entities;
    }

    public List<Class<? extends TileEntity>> getTileEntities() {
        return tileEntities;
    }

    public List<Object> getGuiHandlers() {
        return guiHandlers;
    }

    public List<KeyBinding> getKeyBindings() {
        return keyBindings;
    }

    public List<VillagerRegistry.VillagerProfession> getVillager() {
        return villager;
    }

    public List<Biome> getBiomes() {
        return biomes;
    }

    public List<SoundEvent> getSounds() {
        return sounds;
    }

    public boolean isClient() {
        return isClient;
    }

    public static Builder create() {
        return new Builder();
    }

    public static final class Builder {
        private List<BoolFunction1<Object>> freResponsibleChain = new ArrayList<>();
        private List<BiConsumer<Field, Object>> registerBehavior = new ArrayList<>();
        private ModInfo info;
        private String modId;

        @SuppressWarnings("unchecked")
        private static <T> boolean filterRegistry(Object obj, Class<T> clazz, List<T> target) {
            if (clazz.isInstance(obj)) {
                target.add((T) obj);
                return true;
            } else {
                return false;
            }
        }

        private Builder() {
            info = new ModInfo();
            info.isClient = FMLCommonHandler.instance().getEffectiveSide().isClient();
            if (info.isClient) {
                info.keyBindings = new LinkedList<>();
            }

            freResponsibleChain.add(o -> filterRegistry(o, Item.class, info.items));
            freResponsibleChain.add(o -> filterRegistry(o, Block.class, info.blocks));
            freResponsibleChain.add(o -> filterRegistry(o, Enchantment.class, info.enchants));
            freResponsibleChain.add(o -> filterRegistry(o, Potion.class, info.potions));
            freResponsibleChain.add(o -> filterRegistry(o, PotionType.class, info.potionTypes));
            freResponsibleChain.add(o -> filterRegistry(o, VillagerRegistry.VillagerProfession.class, info.villager));
            freResponsibleChain.add(o -> filterRegistry(o, Biome.class, info.biomes));
            freResponsibleChain.add(o -> filterRegistry(o, SoundEvent.class, info.sounds));

            registerBehavior.add(((field, o) -> {
                for (BoolFunction1<Object> func : freResponsibleChain) {
                    if (func.invoke(o)) {
                        break;
                    }
                }
            }));
            registerBehavior.add((field, o) -> {
                if (!field.isAnnotationPresent(OreDict.class)) {
                    return;
                }
                info.oreDicts.add(ImmutablePair.of(field.getAnnotation(OreDict.class), o));
            });
            registerBehavior.add((field, o) -> {
                if (!field.isAnnotationPresent(Smeltable.class)) {
                    return;
                }
                info.smeltables.add(ImmutablePair.of(field.getAnnotation(Smeltable.class), o));
            });
            registerBehavior.add((field, o) -> {
                if (info.isClient) {
                    if (o instanceof KeyBinding) {
                        info.keyBindings.add((KeyBinding) o);
                    }
                }
            });
        }

        public Builder addRegisterBehavior(@Nonnull BiConsumer<Field, Object> act) {
            registerBehavior.add(act);
            return this;
        }

        public Builder setModId(@Nonnull String modId) {
            this.modId = modId;
            return this;
        }

        public Builder addResponsibleChain(BoolFunction1<Object> func) {
            freResponsibleChain.add(func);
            return this;
        }

        public ModInfo build() {
            freResponsibleChain.add(o -> {
                DawnFoundation.getLogger().warn("Type {} is not supported auto register.Ignore", o.getClass().getName());
                return false;
            });
            try {
                getThisModRegistriesFromManager();
                getThisModEntityRegistriesFromManager();
                getThisModTileEntityRegistriesFromManager();
                getThisModGuiHandlerFromManager();
                info.modId = modId;
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
            return info;
        }

        private void getThisModRegistriesFromManager() throws IllegalAccessException {
            List<Class<?>> registries = Optional.ofNullable(RegisterManager.getInstance().getRegistries(modId)).orElseThrow(IllegalArgumentException::new);
            for (Class<?> registry : registries) {
                for (Field element : registry.getFields()) {
                    if (element.isAnnotationPresent(Skip.class)) {
                        DawnFoundation.getLogger().info("Skip Field:{},Type:{}", element.getName(), element.getType().getName());
                        continue;
                    }
                    Object elementInstance = element.get(null);
                    for (BiConsumer<Field, Object> act : registerBehavior) {
                        act.accept(element, elementInstance);
                    }
                }
            }
        }

        private void getThisModEntityRegistriesFromManager() {
            List<Class<? extends Entity>> registries = Optional.ofNullable(RegisterManager.getInstance().getEntityRegistries(modId)).orElseThrow(IllegalArgumentException::new);
            info.entities = registries.stream()
                    .map(clazz -> {
                        EntityRegistry anno = clazz.getAnnotation(EntityRegistry.class);
                        if (anno.hasCustomFunction()) {
                            Method m = null;
                            for (Method method : clazz.getDeclaredMethods()) {
                                if (method.getAnnotation(EntityRegistry.Custom.class) != null) {
                                    m = method;
                                    break;
                                }
                            }
                            if (m != null) {
                                m.setAccessible(true);
                                if (Modifier.isStatic(m.getModifiers())) {
                                    if (m.getReturnType() == EntityEntry.class) {
                                        try {
                                            return (EntityEntry) m.invoke(null);
                                        } catch (IllegalAccessException | InvocationTargetException e) {
                                            throw new IllegalArgumentException(e);
                                        }
                                    } else {
                                        throw new IllegalArgumentException("Method " + m.getName() + " return type isn't EntityEntry");
                                    }
                                } else {
                                    throw new IllegalArgumentException("Method " + m.getName() + " isn't static");
                                }
                            } else {
                                throw new IllegalArgumentException("Can't find Annotation in class " + clazz.getName() + " .Ignore");
                            }
                        } else {
                            EntityEntryBuilder<Entity> builder = EntityEntryBuilder.create()
                                    .entity(clazz)
                                    .id(new ResourceLocation(anno.modId(), anno.name()), anno.id())
                                    .name(anno.modId() + "." + anno.name())
                                    .tracker(anno.updateRange(), anno.updateFrequency(), anno.isSendVelocityUpdates());
                            if (anno.eggColor1() != -1 && anno.eggColor2() != -1) {
                                builder.egg(anno.eggColor1(), anno.eggColor2());
                            }
                            return builder.build();
                        }
                    })
                    .collect(Collectors.toList());
        }

        private void getThisModTileEntityRegistriesFromManager() {
            info.tileEntities = Optional
                    .ofNullable(RegisterManager.getInstance().getTileEntityRegistries(modId))
                    .orElseGet(LinkedList::new);
        }

        private void getThisModGuiHandlerFromManager() {
            info.guiHandlers = Optional
                    .ofNullable(RegisterManager.getInstance().getGuiHandlerRegistries(modId))
                    .orElseGet(LinkedList::new);
        }
    }
}
