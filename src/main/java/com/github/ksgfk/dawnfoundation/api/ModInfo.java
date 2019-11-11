package com.github.ksgfk.dawnfoundation.api;

import com.github.ksgfk.dawnfoundation.DawnFoundation;
import com.github.ksgfk.dawnfoundation.api.annotations.*;
import com.github.ksgfk.dawnfoundation.api.utility.BoolFunction1;
import com.github.ksgfk.dawnfoundation.api.utility.CommonUtility;
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
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author KSGFK create in 2019/11/6
 */
public class ModInfo {
    private String modId;
    private boolean isClient;
    private Map<Class<?>, List<IForgeRegistryEntry>> entries = new HashMap<>();
    private List<Pair<OreDict, Object>> oreDicts = new LinkedList<>();
    private List<Pair<Smeltable, Object>> smeltables = new LinkedList<>();
    private List<Class<? extends TileEntity>> tileEntities = null;
    //Client
    private List<Object> guiHandlers = null;
    private List<KeyBinding> keyBindings = null;

    private ModInfo() {
    }

    public String getModId() {
        return modId;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends IForgeRegistryEntry.Impl<T>> List<T> getElements(Class<T> clazz) {
        List<IForgeRegistryEntry> l = entries.get(clazz);
        return l == null ? null : l.stream().map((e) -> (T) e).collect(Collectors.toList());
    }

    public Map<Class<?>, List<IForgeRegistryEntry>> getEntries() {
        return entries;
    }

    public List<Item> getItems() {
        return getElements(Item.class);
    }

    public List<Block> getBlocks() {
        return getElements(Block.class);
    }

    public List<Enchantment> getEnchantments() {
        return getElements(Enchantment.class);
    }

    public List<Potion> getPotions() {
        return getElements(Potion.class);
    }

    public List<PotionType> getPotionTypes() {
        return getElements(PotionType.class);
    }

    public List<Pair<OreDict, Object>> getOreDicts() {
        return oreDicts;
    }

    public List<Pair<Smeltable, Object>> getSmeltables() {
        return smeltables;
    }

    public List<EntityEntry> getEntities() {
        return getElements(EntityEntry.class);
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
        return getElements(VillagerRegistry.VillagerProfession.class);
    }

    public List<Biome> getBiomes() {
        return getElements(Biome.class);
    }

    public List<SoundEvent> getSounds() {
        return getElements(SoundEvent.class);
    }

    public boolean isClient() {
        return isClient;
    }

    public static Builder create() {
        return new Builder();
    }

    public static final class Builder {
        private List<BoolFunction1<IForgeRegistryEntry>> freResponsibleChain = new ArrayList<>();
        private List<BiConsumer<Field, IForgeRegistryEntry>> registerBehavior = new ArrayList<>();
        private ModInfo info;
        private String modId;

        private static boolean filterRegistry(IForgeRegistryEntry obj, Class<?> clazz, Map<Class<?>, List<IForgeRegistryEntry>> map) {
            if (clazz.isInstance(obj)) {
                CommonUtility.addNoRepeatListVToMapKV(clazz, obj, map, LinkedList::new);
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

            freResponsibleChain.add(o -> filterRegistry(o, Item.class, info.entries));
            freResponsibleChain.add(o -> filterRegistry(o, Block.class, info.entries));
            freResponsibleChain.add(o -> filterRegistry(o, Enchantment.class, info.entries));
            freResponsibleChain.add(o -> filterRegistry(o, Potion.class, info.entries));
            freResponsibleChain.add(o -> filterRegistry(o, PotionType.class, info.entries));
            freResponsibleChain.add(o -> filterRegistry(o, VillagerRegistry.VillagerProfession.class, info.entries));
            freResponsibleChain.add(o -> filterRegistry(o, Biome.class, info.entries));
            freResponsibleChain.add(o -> filterRegistry(o, SoundEvent.class, info.entries));

            registerBehavior.add(((field, o) -> {
                for (BoolFunction1<IForgeRegistryEntry> func : freResponsibleChain) {
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

        public Builder addRegisterBehavior(@Nonnull BiConsumer<Field, IForgeRegistryEntry> act) {
            registerBehavior.add(act);
            return this;
        }

        public Builder setModId(@Nonnull String modId) {
            this.modId = modId;
            return this;
        }

        public Builder addResponsibleChain(BoolFunction1<IForgeRegistryEntry> func) {
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
                    for (BiConsumer<Field, IForgeRegistryEntry> act : registerBehavior) {
                        if (elementInstance instanceof IForgeRegistryEntry) {
                            act.accept(element, (IForgeRegistryEntry) elementInstance);
                        } else {
                            DawnFoundation.getLogger().error("Field {}(Type:{}) isn't implement IForgeRegistryEntry.Maybe it's a bug!", element.getName(), elementInstance.getClass().getName());
                        }
                    }
                }
            }
        }

        private void getThisModEntityRegistriesFromManager() {
            List<Class<? extends Entity>> registries = Optional.ofNullable(RegisterManager.getInstance().getEntityRegistries(modId)).orElseThrow(IllegalArgumentException::new);
            List<IForgeRegistryEntry> entry = registries.stream()
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
            info.entries.put(EntityEntry.class, entry);
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
