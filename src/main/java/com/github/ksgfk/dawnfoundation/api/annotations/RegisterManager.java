package com.github.ksgfk.dawnfoundation.api.annotations;

import com.github.ksgfk.dawnfoundation.DawnFoundation;
import com.github.ksgfk.dawnfoundation.api.utility.Action3;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 注册管理者，生命周期到 {@link net.minecraftforge.fml.common.event.FMLLoadCompleteEvent} 事件触发时结束
 * 超出生命周期时再获取单例将会返回Null
 *
 * @author KSGFK create in 2019/10/21
 */
public class RegisterManager {
    private static RegisterManager instance = new RegisterManager();

    private List<Action3<String, Field, Object>> registerBehavior = new ArrayList<>();
    private List<Pair<Field, Object>> oreDictElements = new ArrayList<>();
    private List<Class<?>> entityRenderers = new ArrayList<>();

    private Map<String, List<Item>> itemMap = new HashMap<>();
    private Map<String, List<Block>> blockMap = new HashMap<>();
    private Map<String, List<Pair<EntityEntryBuilder<Entity>, EntityRegistry>>> entityMap = new HashMap<>();

    private RegisterManager() {
        registerBehavior.add(((domain, field, o) -> {
            if (o instanceof Item) {
                addToMap(domain, (Item) o, itemMap);
            } else if (o instanceof Block) {
                addToMap(domain, (Block) o, blockMap);
            } else {
                DawnFoundation.getLogger().warn("Type {} is not supported auto register,ignore", o.getClass().getName());
            }
        }));
        registerBehavior.add(((domain, field, o) -> {
            if (field.isAnnotationPresent(OreDict.class)) {
                oreDictElements.add(ImmutablePair.of(field, o));
            }
        }));
    }

    private <T> void addToMap(String key, T value, Map<String, List<T>> map) {
        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            List<T> l = new ArrayList<>();
            l.add(value);
            map.put(key, l);
        }
    }

    /**
     * 不需要手动调用该方法，已统一注册
     */
    public void processRegistries(ASMDataTable asmDataTable) throws IllegalAccessException, ClassNotFoundException {
        normalRegistries(asmDataTable);
        entitiesRegistries(asmDataTable);
    }

    private void normalRegistries(ASMDataTable asmDataTable) throws ClassNotFoundException, IllegalAccessException {
        for (ASMDataTable.ASMData asmData : asmDataTable.getAll(Registry.class.getName())) {
            Class<?> clz = Class.forName(asmData.getClassName());
            Registry anno = clz.getAnnotation(Registry.class);
            for (Field registry : clz.getFields()) {
                Object registryInstance = registry.get(null);
                registerBehavior.forEach((callback) -> callback.invoke(anno.modId(), registry, registryInstance));
            }
        }
    }

    private void entitiesRegistries(ASMDataTable asmDataTable) throws ClassNotFoundException, ClassCastException {
        List<Class<? extends Entity>> entityClasses = new ArrayList<>();
        for (ASMDataTable.ASMData asmData : asmDataTable.getAll(EntityRegistry.class.getName())) {
            Class<?> clz = Class.forName(asmData.getClassName());
            Class<? extends Entity> e = clz.asSubclass(Entity.class);
            entityClasses.add(e);
        }
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            for (ASMDataTable.ASMData asmData : asmDataTable.getAll(EntityRenderer.class.getName())) {
                Class<?> clz = Class.forName(asmData.getClassName());
                entityRenderers.add(clz);
            }
        }
        entityClasses.stream()
                .map(entityClass -> {
                    EntityRegistry anno = entityClass.getAnnotation(EntityRegistry.class);
                    EntityEntryBuilder<Entity> builder = EntityEntryBuilder.create()
                            .entity(entityClass)
                            .id(new ResourceLocation(anno.modId(), anno.name()), anno.id())
                            .name(anno.modId() + "." + anno.name())
                            .tracker(anno.updateRange(), anno.updateFrequency(), anno.isSendVelocityUpdates());
                    if (anno.eggColor1() != -1 && anno.eggColor2() != -1) {
                        builder.egg(anno.eggColor1(), anno.eggColor2());
                    }
                    if (anno.canNaturalGenerate()) {
                        List<Biome> biomes = Arrays.stream(anno.biomes())
                                .map((biomeName -> {
                                    Biome biome = Biome.REGISTRY.getObject(new ResourceLocation(biomeName));
                                    if (biome == null) {
                                        throw new NullPointerException("Can't find biome " + biomeName);
                                    }
                                    return biome;
                                }))
                                .collect(Collectors.toList());
                        builder.spawn(anno.creatureType(), anno.weight(), anno.min(), anno.max(), biomes);
                    }
                    return ImmutablePair.of(builder, anno);
                })
                .collect(Collectors.toList())
                .forEach(pair -> addToMap(pair.getRight().modId(), pair, entityMap));
    }

    /**
     * 不需要手动调用该方法，已统一注册
     */
    public void registerItemModel(ModelRegistryEvent event) {
        for (Map.Entry<String, List<Item>> pair : itemMap.entrySet()) {
            registerItemModelList(pair.getValue());
        }
    }

    /**
     * 不需要手动调用该方法，已统一注册
     */
    public void registerBlockModel(ModelRegistryEvent event) {
        for (Map.Entry<String, List<Block>> pair : blockMap.entrySet()) {
            registerItemModelList(pair
                    .getValue()
                    .stream()
                    .map(Item::getItemFromBlock)
                    .collect(Collectors.toList()));
        }
    }

    private static void registerItemModelList(List<Item> items) {
        items.forEach(item -> {
            if (item instanceof IModelRegistry) {
                ((IModelRegistry) item).registerModel();
            } else {
                ModelLoader.setCustomModelResourceLocation(item,
                        0,
                        new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), "inventory"));
            }
        });
    }

    /**
     * 不需要手动调用该方法，已统一注册
     */
    public void registerOreDict() {
        for (Pair<Field, Object> p : oreDictElements) {
            OreDict oreDict = p.getLeft().getAnnotation(OreDict.class);
            Object o = p.getRight();
            if (o instanceof Item) {
                OreDictionary.registerOre(oreDict.key(), (Item) o);
            } else if (o instanceof Block) {
                OreDictionary.registerOre(oreDict.key(), (Block) o);
            } else {
                DawnFoundation.getLogger().warn("Type {} is not supported register ore dict,ignore", o.getClass().getName());
            }
        }
    }

    /**
     * 不需要手动调用该方法，已统一注册
     */
    @SuppressWarnings("unchecked")
    public void bindEntityModel(ModelRegistryEvent event) {
        entityRenderers.forEach(renderer -> {
            EntityRenderer annotation = renderer.getAnnotation(EntityRenderer.class);
            RenderingRegistry.registerEntityRenderingHandler(annotation.entityClass(), manager -> {
                try {
                    return (Render<? super Entity>) renderer.getConstructor(RenderManager.class).newInstance(manager);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new IllegalArgumentException("Can't construct renderer:" + renderer.getName(), e);
                }
            });
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends IForgeRegistryEntry.Impl<T>> void registerFunction(T instance, RegistryEvent.Register<T> event) {
        if (instance instanceof IDomainName) {
            if (instance instanceof IMultiRegisters) {
                List<IForgeRegistryEntry> o = ((IMultiRegisters) instance).getRegisters();
                for (IForgeRegistryEntry i : o) {
                    i.setRegistryName(((IDomainName) i).getDomainName());
                    event.getRegistry().register((T) i);
                }
            } else {
                instance.setRegistryName(((IDomainName) instance).getDomainName());
                event.getRegistry().register(instance);
            }
        } else {
            DawnFoundation.getLogger().warn("Type {} unimplemented interface IDomainName,ignore", instance.getClass().getName());
        }
    }

    /**
     * 在 {@link net.minecraftforge.event.RegistryEvent.Register<Item>} 阶段调用该方法
     *
     * @param modId 注册物品的modid
     * @param event 注册事件
     */
    public void registerItems(String modId, RegistryEvent.Register<Item> event) {
        checkMap(modId, itemMap);
        checkMap(modId, blockMap);
        for (Item item : itemMap.get(modId)) {
            registerFunction(item, event);
        }
        for (Block b : blockMap.get(modId)) {
            if (b instanceof IDomainName) {
                if (b instanceof IMultiRegisters) {
                    List o = ((IMultiRegisters) instance).getRegisters();
                    for (Object block : o) {
                        if (block instanceof Block) {
                            event.getRegistry().register(getItemBlock((Block) block));
                        } else {
                            DawnFoundation.getLogger().warn("Type {} isn't Block,ignore", instance.getClass().getName());
                        }
                    }
                } else {
                    event.getRegistry().register(getItemBlock(b));
                }
            } else {
                DawnFoundation.getLogger().warn("Type {} unimplemented interface IDomainName,ignore", instance.getClass().getName());
            }
        }
    }

    private static ItemBlock getItemBlock(Block block) {
        ItemBlock i = new ItemBlock(block);
        i.setRegistryName(((IDomainName) block).getDomainName());
        return i;
    }

    /**
     * 在 {@link net.minecraftforge.event.RegistryEvent.Register<Block>} 阶段调用该方法
     *
     * @param modId 注册物品的modid
     * @param event 注册事件
     */
    public void registerBlocks(String modId, RegistryEvent.Register<Block> event) {
        checkMap(modId, blockMap);
        for (Block block : blockMap.get(modId)) {
            registerFunction(block, event);
        }
    }

    /**
     * 在 {@link net.minecraftforge.event.RegistryEvent.Register<EntityEntry>} 阶段调用该方法
     *
     * @param modId 注册物品的modid
     * @param event 注册事件
     */
    public void registerEntities(String modId, RegistryEvent.Register<EntityEntry> event) {
        checkMap(modId, entityMap);
        entityMap.get(modId).forEach(entityEntry -> event.getRegistry().register(entityEntry.getLeft().build()));
    }

    private static <T> void checkMap(String modId, Map<String, T> map) {
        if (!map.containsKey(modId)) {
            throw new IllegalArgumentException("Can't find MOD ID:" + modId);
        }
    }

    public static RegisterManager getInstance() {
        return instance;
    }

    /**
     * 不需要手动调用该方法，在 {@link net.minecraftforge.fml.common.event.FMLLoadCompleteEvent} 自动释放资源
     */
    public static void clean() {
        instance = null;
    }
}
