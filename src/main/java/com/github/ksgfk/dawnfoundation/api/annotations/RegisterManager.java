package com.github.ksgfk.dawnfoundation.api.annotations;

import com.github.ksgfk.dawnfoundation.DawnFoundation;
import com.github.ksgfk.dawnfoundation.api.utility.Action;
import com.github.ksgfk.dawnfoundation.api.utility.Action3;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
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
    private List<Pair<Field, Object>> oreDictElements = new LinkedList<>();
    private List<Class<?>> entityRenderers = new LinkedList<>();
    private List<Class<?>> tesrRenderers = new LinkedList<>();
    private List<Pair<Object, ModContainer>> guiHandlers = new LinkedList<>();
    private List<Pair<Field, Object>> smeltables = new LinkedList<>();

    private Map<String, List<Item>> itemMap = new HashMap<>();
    private Map<String, List<Block>> blockMap = new HashMap<>();
    private Map<String, List<Pair<EntityEntryBuilder<Entity>, EntityRegistry>>> entityMap = new HashMap<>();
    private Map<String, List<Pair<Class<? extends TileEntity>, TileEntityRegistry>>> tileEntityMap = new HashMap<>();
    private Map<String, List<Enchantment>> enchantMap = new HashMap<>();

    private long registeredItemCount = 0;
    private long registeredBlockCount = 0;
    private long registeredEntityCount = 0;
    private long registeredTileEntityCount = 0;
    private long registeredOreDictCount = 0;
    private long registeredSmeltCount = 0;
    private long registeredEntityRenderCount = 0;
    private long registeredTESRCount = 0;
    private long registeredGuiHandlerCount = 0;
    private long registeredEnchantCount = 0;

    private RegisterManager() {
        registerBehavior.add(((domain, field, o) -> {
            if (o instanceof Item) {
                addToMap(domain, (Item) o, itemMap);
            } else if (o instanceof Block) {
                addToMap(domain, (Block) o, blockMap);
            } else if (o instanceof Enchantment) {
                addToMap(domain, (Enchantment) o, enchantMap);
            } else {
                DawnFoundation.getLogger().warn("Type {} is not supported auto register.Ignore", o.getClass().getName());
            }
        }));
        registerBehavior.add((domain, field, o) -> {
            if (!field.isAnnotationPresent(OreDict.class)) {
                return;
            }
            oreDictElements.add(ImmutablePair.of(field, o));
        });
        registerBehavior.add((domain, field, o) -> {
            if (!field.isAnnotationPresent(Smeltable.class)) {
                return;
            }
            smeltables.add(ImmutablePair.of(field, o));
        });
    }

    private static <T> void addToMap(String key, T value, Map<String, List<T>> map) {
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
    public void processRegistries(ASMDataTable asmDataTable) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
        normalRegistries(asmDataTable);
        entitiesRegistries(asmDataTable);
        tileEntityRegistries(asmDataTable);
        processGuiHandler(asmDataTable);
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

    private void tileEntityRegistries(ASMDataTable asmDataTable) throws ClassNotFoundException {
        for (ASMDataTable.ASMData asmData : asmDataTable.getAll(TileEntityRegistry.class.getName())) {
            Class<?> clz = Class.forName(asmData.getClassName());
            Class<? extends TileEntity> e = clz.asSubclass(TileEntity.class);
            TileEntityRegistry anno = clz.getAnnotation(TileEntityRegistry.class);
            addToMap(anno.modId(), ImmutablePair.of(e, anno), tileEntityMap);
        }
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            for (ASMDataTable.ASMData asmData : asmDataTable.getAll(TESRRegistry.class.getName())) {
                Class<?> clz = Class.forName(asmData.getClassName());
                tesrRenderers.add(clz);
            }
        }
    }

    /**
     * 不需要手动调用该方法，在 {@link net.minecraftforge.client.event.ModelRegistryEvent;} 自动注册
     */
    public void registerItemModel() {
        for (Map.Entry<String, List<Item>> pair : itemMap.entrySet()) {
            registerItemModelList(pair.getValue());
        }
    }

    /**
     * 不需要手动调用该方法，在 {@link net.minecraftforge.client.event.ModelRegistryEvent;} 自动注册
     */
    public void registerBlockModel() {
        for (Map.Entry<String, List<Block>> pair : blockMap.entrySet()) {
            registerItemModelList(pair
                    .getValue()
                    .stream()
                    .map(Item::getItemFromBlock)
                    .collect(Collectors.toList()));
        }
    }

    private static void registerItemModelList(List<Item> items) {
        for (Item item : items) {
            if (item instanceof IModelRegistry) {
                ((IModelRegistry) item).registerModel();
            } else {
                ModelLoader.setCustomModelResourceLocation(item,
                        0,
                        new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), "inventory"));
            }
        }
    }

    /**
     * 不需要手动调用该方法，在 {@link net.minecraftforge.fml.common.event.FMLPreInitializationEvent} 自动注册
     */
    public void registerOreDict() {
        for (Pair<Field, Object> p : oreDictElements) {
            OreDict oreDict = p.getLeft().getAnnotation(OreDict.class);
            Object o = p.getRight();
            if (o instanceof Item) {
                OreDictionary.registerOre(oreDict.key(), (Item) o);
                registeredOreDictCount += 1;
            } else if (o instanceof Block) {
                OreDictionary.registerOre(oreDict.key(), (Block) o);
                registeredOreDictCount += 1;
            } else {
                DawnFoundation.getLogger().error("Type {} is not supported register ore dict.Ignore", o.getClass().getName());
            }
        }
    }

    /**
     * 不需要手动调用该方法，已统一注册
     */
    @SuppressWarnings("unchecked")
    public void bindEntityModel() {
        for (Class<?> renderer : entityRenderers) {
            EntityRenderer annotation = renderer.getAnnotation(EntityRenderer.class);
            RenderingRegistry.registerEntityRenderingHandler(annotation.entityClass(), manager -> {
                Render<? super Entity> r;
                try {
                    r = (Render<? super Entity>) renderer.getConstructor(RenderManager.class).newInstance(manager);
                    registeredEntityRenderCount += 1;
                    return r;
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new IllegalArgumentException("Can't construct renderer:" + renderer.getName(), e);
                }
            });
        }
    }

    /**
     * @deprecated 多种元素在同一类内注册没啥用
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    private static <T extends IForgeRegistryEntry.Impl<T>> void registerMultiElements(T instance, RegistryEvent.Register<T> event, Action action) {
        if (instance instanceof IDomainName) {
            if (instance instanceof IMultiRegisters) {
                List<IForgeRegistryEntry> o = ((IMultiRegisters) instance).getRegisters();
                for (IForgeRegistryEntry i : o) {
                    i.setRegistryName(((IDomainName) i).getDomainName());
                    event.getRegistry().register((T) i);
                    if (action != null) {
                        action.invoke();
                    }
                }
            } else {
                instance.setRegistryName(((IDomainName) instance).getDomainName());
                event.getRegistry().register(instance);
                if (action != null) {
                    action.invoke();
                }
            }
        } else {
            DawnFoundation.getLogger().warn("Type {} unimplemented interface IDomainName.Ignore", instance.getClass().getName());
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
            registerEntry(item, event, () -> registeredItemCount += 1);
        }
        for (Block b : blockMap.get(modId)) {
            if (b instanceof IDomainName) {
                event.getRegistry().register(new ItemBlock(b).setRegistryName(((IDomainName) b).getDomainName()));
            } else {
                DawnFoundation.getLogger().error("Block type {} unimplemented interface IDomainName.Ignore", b.getClass().getName());

            }
        }
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
            registerEntry(block, event, () -> registeredBlockCount += 1);
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
        for (Pair<EntityEntryBuilder<Entity>, EntityRegistry> entityEntry : entityMap.get(modId)) {
            event.getRegistry().register(entityEntry.getLeft().build());
            registeredEntityCount += 1;
        }
    }

    /**
     * 在 {@link net.minecraftforge.event.RegistryEvent.Register<Block>} 阶段调用该方法
     *
     * @param modId 注册物品的modid
     * @param event 注册事件
     */
    public void registerTileEntities(String modId, RegistryEvent.Register<Block> event) {
        checkMap(modId, tileEntityMap);
        for (Pair<Class<? extends TileEntity>, TileEntityRegistry> tile : tileEntityMap.get(modId)) {
            TileEntityRegistry anno = tile.getRight();
            GameRegistry.registerTileEntity(tile.getLeft(), new ResourceLocation(anno.modId(), anno.name()));
            registeredTileEntityCount += 1;
        }
    }

    /**
     * 在 {@link net.minecraftforge.event.RegistryEvent.Register<Enchantment>} 阶段调用该方法
     *
     * @param modId 注册物品的modid
     * @param event 注册事件
     */
    public void registerEnchantments(String modId, RegistryEvent.Register<Enchantment> event) {
        checkMap(modId, enchantMap);
        for (Enchantment enchantment : enchantMap.get(modId)) {
            registerEntry(enchantment, event, () -> registeredEnchantCount += 1);
        }
    }

    private static <T extends IForgeRegistryEntry.Impl<T>> void registerEntry(T entry, RegistryEvent.Register<T> event, @Nullable Action action) {
        if (entry instanceof IDomainName) {
            entry.setRegistryName(((IDomainName) entry).getDomainName());
            event.getRegistry().register(entry);
            if (action != null) {
                action.invoke();
            }
        } else {
            DawnFoundation.getLogger().error("Type {} unimplemented interface IDomainName.Ignore", entry.getClass().getName());
        }
    }

    /**
     * 不需要手动调用该方法，在 {@link net.minecraftforge.fml.common.event.FMLInitializationEvent} 自动注册
     */
    @SuppressWarnings("unchecked")
    public void registerTESR() {
        for (Class<?> tesr : tesrRenderers) {
            TESRRegistry annotation = tesr.getAnnotation(TESRRegistry.class);
            TileEntitySpecialRenderer<? super TileEntity> instance;
            try {
                instance = (TileEntitySpecialRenderer<? super TileEntity>) tesr.getConstructor().newInstance();
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                DawnFoundation.getLogger().error("Can't construct TESR Instance:{}", tesr.getName());
                continue;
            }
            ClientRegistry.bindTileEntitySpecialRenderer(annotation.tileEntity(), instance);
            registeredTESRCount += 1;
        }
    }

    private void processGuiHandler(ASMDataTable asmDataTable) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Map<String, ModContainer> indexedModList = Loader.instance().getIndexedModList();
        for (ASMDataTable.ASMData asmData : asmDataTable.getAll(GuiHandler.class.getName())) {
            Class<?> clz = Class.forName(asmData.getClassName());
            Field field = clz.getDeclaredField(asmData.getObjectName());
            field.setAccessible(true);
            Object ins = field.get(null);
            if (ins.getClass().isAssignableFrom(IGuiHandler.class)) {
                DawnFoundation.getLogger().error("Class {} doesn't extend from IGuiHandler", clz.getName());
                continue;
            }
            GuiHandler handler = field.getAnnotation(GuiHandler.class);
            if (Loader.isModLoaded(handler.modId())) {
                ModContainer c = indexedModList.get(handler.modId());
                guiHandlers.add(ImmutablePair.of(ins, c));
            } else {
                DawnFoundation.getLogger().error("Mod {} wasn't loaded", handler.modId());
            }
        }
    }

    /**
     * 不需要手动调用该方法，在 {@link net.minecraftforge.fml.common.event.FMLInitializationEvent} 自动注册
     */
    public void registerGuiHandlers() {
        for (Pair<Object, ModContainer> t : guiHandlers) {
            NetworkRegistry.INSTANCE.registerGuiHandler(t.getRight().getMod(), (IGuiHandler) t.getLeft());
            registeredGuiHandlerCount += 1;
        }
    }

    /**
     * 不需要手动调用该方法，在 {@link net.minecraftforge.fml.common.event.FMLInitializationEvent} 自动注册
     */
    public void registerSmeltable() {
        for (Pair<Field, Object> p : smeltables) {
            ItemStack input;
            if (p.getRight() instanceof Item) {
                input = new ItemStack((Item) p.getRight());
            } else if (p.getRight() instanceof Block) {
                input = new ItemStack((Block) p.getRight());
            } else {
                DawnFoundation.getLogger().error("Field {} isn't Item or Block.Can't add smelting.Ignore", p.getLeft().getName());
                continue;
            }
            Smeltable smeltable = p.getLeft().getAnnotation(Smeltable.class);
            Item output = Item.getByNameOrId(smeltable.result());
            if (output == null) {
                DawnFoundation.getLogger().error("Can't find Item {},ignore", smeltable.result());
                continue;
            }
            GameRegistry.addSmelting(input, new ItemStack(output, smeltable.resultCount()), smeltable.exp());
            registeredSmeltCount += 1;
        }
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

    public void statistics() {
        DawnFoundation.getLogger().info("------Register Info------");
        DawnFoundation.getLogger().info("Item:\t\t\t{}", registeredItemCount);
        DawnFoundation.getLogger().info("Block:\t\t{}", registeredBlockCount);
        DawnFoundation.getLogger().info("Entity:\t\t{}", registeredEntityCount);
        DawnFoundation.getLogger().info("OreDict:\t\t{}", registeredOreDictCount);
        DawnFoundation.getLogger().info("TileEntity:\t{}", registeredTileEntityCount);
        DawnFoundation.getLogger().info("Smelt:\t\t{}", registeredSmeltCount);
        DawnFoundation.getLogger().info("EntityRender:\t{}", registeredEntityRenderCount);
        DawnFoundation.getLogger().info("TESR:\t\t\t{}", registeredTESRCount);
        DawnFoundation.getLogger().info("GuiHandler:\t{}", registeredGuiHandlerCount);
        DawnFoundation.getLogger().info("Enchant:\t\t{}", registeredEnchantCount);
        DawnFoundation.getLogger().info("-------------------------");
    }
}
