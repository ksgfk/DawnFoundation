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
    private Map<Class<? extends IForgeRegistryEntry>, List<IForgeRegistryEntry>> entries = new HashMap<>();
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
    public <T extends IForgeRegistryEntry<T>> List<T> getElements(Class<T> clazz) {
        List<IForgeRegistryEntry> l = entries.get(clazz);
        return l == null ? null : l.stream().map((e) -> (T) e).collect(Collectors.toList());
    }

    public Map<Class<? extends IForgeRegistryEntry>, List<IForgeRegistryEntry>> getEntries() {
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
        private List<Pair<BoolFunction1<IForgeRegistryEntry>, Class<? extends IForgeRegistryEntry>>> freResponsibleChain = new ArrayList<>();
        private List<BiConsumer<Field, IForgeRegistryEntry>> registerBehavior = new ArrayList<>();
        private ModInfo info;
        private String modId;

        private static boolean filterRegistry(IForgeRegistryEntry obj, Class<? extends IForgeRegistryEntry> clazz) {
            return clazz.isInstance(obj);
        }

        private Builder() {
            info = new ModInfo();
            info.isClient = FMLCommonHandler.instance().getEffectiveSide().isClient();
            if (info.isClient) {
                info.keyBindings = new LinkedList<>();
            }

            addResponsibleChain(Item.class);
            addResponsibleChain(Block.class);
            addResponsibleChain(Enchantment.class);
            addResponsibleChain(Potion.class);
            addResponsibleChain(PotionType.class);
            addResponsibleChain(VillagerRegistry.VillagerProfession.class);
            addResponsibleChain(Biome.class);
            addResponsibleChain(SoundEvent.class);

            registerBehavior.add(((field, o) -> {
                for (Pair<BoolFunction1<IForgeRegistryEntry>, Class<? extends IForgeRegistryEntry>> func : freResponsibleChain) {
                    if (func.getLeft().invoke(o)) {
                        if (func.getRight() == null) {
                            continue;
                        }
                        CommonUtility.addNoRepeatListVToMapKV(func.getRight(), o, info.entries, LinkedList::new);
                        break;
                    }
                }
            }));
            addRegisterBehavior((field, o) -> {
                if (!field.isAnnotationPresent(OreDict.class)) {
                    return;
                }
                info.oreDicts.add(ImmutablePair.of(field.getAnnotation(OreDict.class), o));
            });
            addRegisterBehavior((field, o) -> {
                if (!field.isAnnotationPresent(Smeltable.class)) {
                    return;
                }
                info.smeltables.add(ImmutablePair.of(field.getAnnotation(Smeltable.class), o));
            });
            addRegisterBehavior((field, o) -> {
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

        /**
         * 向责任链末尾添加新责任，自定义责任规则
         *
         * @param clazz 实现 {@link IForgeRegistryEntry} 的类，作为索引
         * @param func  责任规则
         */
        public Builder addResponsibleChain(@Nullable Class<? extends IForgeRegistryEntry> clazz, @Nonnull BoolFunction1<IForgeRegistryEntry> func) {
            freResponsibleChain.add(ImmutablePair.of(func, clazz));
            return this;
        }

        /**
         * 向责任链末尾添加新责任
         *
         * @param clazz 实现 {@link IForgeRegistryEntry} 的类，作为索引
         */
        public Builder addResponsibleChain(Class<? extends IForgeRegistryEntry> clazz) {
            addResponsibleChain(clazz, o -> filterRegistry(o, clazz));
            return this;
        }

        public ModInfo build() {
            addResponsibleChain(null, o -> {
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
            List<Class<?>> registries = Optional.ofNullable(RegisterManager.getInstance().getRegistries(modId)).orElseGet(() -> {
                DawnFoundation.getLogger().warn("Mod {} can't find Registry Annotation", modId);
                return new LinkedList<>();
            });
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
            List<Class<? extends Entity>> registries = Optional.ofNullable(RegisterManager.getInstance().getEntityRegistries(modId)).orElseGet(() -> {
                DawnFoundation.getLogger().warn("Mod {} can't find EntityRegistry Annotation", modId);
                return new LinkedList<>();
            });
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
