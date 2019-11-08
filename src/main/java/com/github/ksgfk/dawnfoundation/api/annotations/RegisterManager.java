package com.github.ksgfk.dawnfoundation.api.annotations;

import com.github.ksgfk.dawnfoundation.DawnFoundation;
import com.github.ksgfk.dawnfoundation.api.ModInfo;
import com.github.ksgfk.dawnfoundation.api.utility.Action1;
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
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
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

    //new
    private Map<String, List<Class<?>>> registryMap = new HashMap<>();
    private Map<String, List<Class<? extends Entity>>> entityRegistryMap = new HashMap<>();
    private Map<String, List<Class<? extends TileEntity>>> tileEntityRegistryMap = new HashMap<>();
    private Map<String, List<Object>> guiHandlerMap = null;
    private List<Class<?>> entityRenders = new LinkedList<>();
    private List<Class<?>> tesrRenders = new LinkedList<>();
    private List<Class<? extends WorldProvider>> dimType = new LinkedList<>();

    private boolean isClient;

    private RegisterManager() {
        isClient = FMLCommonHandler.instance().getEffectiveSide().isClient();
        if (isClient) {
            guiHandlerMap = new HashMap<>();
        }
    }

    private static <T> void addToMap(String key, T value, Map<String, List<T>> map) {
        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            List<T> l = new LinkedList<>();
            l.add(value);
            map.put(key, l);
        }
    }

    public void processRegistries(ASMDataTable asmDataTable) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
        findASMData(asmDataTable, Registry.class.getName(), clazz -> {
            Registry info = clazz.getAnnotation(Registry.class);
            addToMap(info.modId(), clazz, registryMap);
        });
        findASMData(asmDataTable, EntityRegistry.class.getName(), clazz -> {
            Class<? extends Entity> entity = clazz.asSubclass(Entity.class);
            EntityRegistry info = clazz.getAnnotation(EntityRegistry.class);
            addToMap(info.modId(), entity, entityRegistryMap);
        });
        findASMData(asmDataTable, TileEntityRegistry.class.getName(), clazz -> {
            Class<? extends TileEntity> te = clazz.asSubclass(TileEntity.class);
            TileEntityRegistry anno = te.getAnnotation(TileEntityRegistry.class);
            addToMap(anno.modId(), te, tileEntityRegistryMap);
        });
        findASMData(asmDataTable, DimensionTypeRegistry.class.getName(), clazz -> {
            Class<? extends WorldProvider> wp = clazz.asSubclass(WorldProvider.class);
            dimType.add(wp);
        });
        if (isClient) {
            findASMData(asmDataTable, EntityRenderer.class.getName(), entityRenders::add);
            findASMData(asmDataTable, TESRRegistry.class.getName(), tesrRenders::add);
            findGuiHandler(asmDataTable);
        }
    }

    private static void findASMData(ASMDataTable asmDataTable, String annoName, Action1<Class<?>> act) throws ClassNotFoundException {
        for (ASMDataTable.ASMData asmData : asmDataTable.getAll(annoName)) {
            Class<?> clazz = Class.forName(asmData.getClassName(), false, Thread.currentThread().getContextClassLoader());
            act.invoke(clazz);
        }
    }

    private void findGuiHandler(ASMDataTable asmDataTable) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
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
                addToMap(handler.modId(), ins, guiHandlerMap);
            } else {
                DawnFoundation.getLogger().error("Mod {} wasn't loaded", handler.modId());
            }
        }
    }

    @Nullable
    public List<Class<?>> getRegistries(@Nonnull String modId) {
        return registryMap.get(modId);
    }

    @Nullable
    public List<Class<? extends Entity>> getEntityRegistries(@Nonnull String modId) {
        return entityRegistryMap.get(modId);
    }

    @Nullable
    public List<Class<? extends TileEntity>> getTileEntityRegistries(@Nonnull String modId) {
        return tileEntityRegistryMap.get(modId);
    }

    @Nullable
    public List<Object> getGuiHandlerRegistries(@Nonnull String modId) {
        return guiHandlerMap.get(modId);
    }

    /**
     * 在 {@link net.minecraftforge.client.event.ModelRegistryEvent} 阶段自动绑定实体模型
     */
    @SuppressWarnings("unchecked")
    public void bindEntityRenderer() {
        for (Class<?> renderer : entityRenders) {
            EntityRenderer annotation = renderer.getAnnotation(EntityRenderer.class);
            RenderingRegistry.registerEntityRenderingHandler(annotation.entityClass(), manager -> {
                Render<? super Entity> r;
                try {
                    r = (Render<? super Entity>) renderer.getConstructor(RenderManager.class).newInstance(manager);
                    return r;
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new IllegalArgumentException("Can't construct renderer:" + renderer.getName(), e);
                }
            });
        }
    }

    /**
     * 在 {@link net.minecraftforge.fml.common.event.FMLInitializationEvent} 阶段已自动绑定TileEntity模型
     */
    @SuppressWarnings("unchecked")
    public void registerTESR() {
        for (Class<?> tesr : tesrRenders) {
            TESRRegistry annotation = tesr.getAnnotation(TESRRegistry.class);
            TileEntitySpecialRenderer<? super TileEntity> instance;
            try {
                instance = (TileEntitySpecialRenderer<? super TileEntity>) tesr.getConstructor().newInstance();
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                DawnFoundation.getLogger().error("Can't construct TESR Instance:{}", tesr.getName());
                continue;
            }
            ClientRegistry.bindTileEntitySpecialRenderer(annotation.tileEntity(), instance);
        }
    }

    /**
     * 在 {@link net.minecraftforge.fml.common.event.FMLInitializationEvent} 阶段注册
     */
    public void registerDimensionType() {
        for (Class<? extends WorldProvider> dim : dimType) {
            DimensionTypeRegistry registry = dim.getAnnotation(DimensionTypeRegistry.class);
            DimensionType type = DimensionType.register(registry.name(), registry.suffix(), registry.id(), dim, registry.keepLoaded());
            DimensionManager.registerDimension(registry.id(), type);
        }
    }

    /**
     * 在 {@link net.minecraftforge.client.event.ModelRegistryEvent} 阶段注册
     */
    public static void registerItemModel(ModInfo info) {
        info.getItems().forEach(RegisterManager::registerItemModelList);
    }

    /**
     * 在 {@link net.minecraftforge.client.event.ModelRegistryEvent} 阶段注册
     */
    public static void registerBlockModel(ModInfo info) {
        info.getBlocks().stream().map(Item::getItemFromBlock).forEach(RegisterManager::registerItemModelList);
    }

    private static void registerItemModelList(Item item) {
        if (item instanceof IModelRegistry) {
            ((IModelRegistry) item).registerModel();
        } else {
            ModelLoader.setCustomModelResourceLocation(item,
                    0,
                    new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), "inventory"));
        }
    }

    public static <T extends IForgeRegistryEntry<T>> void registerGeneric(List<T> list, RegistryEvent.Register<T> event) {
        for (T e : list) {
            event.getRegistry().register(e);
        }
    }

    public static void registerItem(ModInfo info, RegistryEvent.Register<Item> event) {
        registerGeneric(info.getItems(), event);
        registerGeneric(info.getBlocks().stream()
                        .map(block -> new ItemBlock(block).setRegistryName(Objects.requireNonNull((block.getRegistryName()))))
                        .collect(Collectors.toList()),
                event);
    }

    public static void registerBlock(ModInfo info, RegistryEvent.Register<Block> event) {
        registerGeneric(info.getBlocks(), event);
    }

    public static void registerEnchantment(ModInfo info, RegistryEvent.Register<Enchantment> event) {
        registerGeneric(info.getEnchantments(), event);
    }

    public static void registerPotion(ModInfo info, RegistryEvent.Register<Potion> event) {
        registerGeneric(info.getPotions(), event);
    }

    public static void registerPotionType(ModInfo info, RegistryEvent.Register<PotionType> event) {
        registerGeneric(info.getPotionTypes(), event);
    }

    public static void registerEntity(ModInfo info, RegistryEvent.Register<EntityEntry> event) {
        registerGeneric(info.getEntities(), event);
    }

    public static void registerVillager(ModInfo info, RegistryEvent.Register<VillagerRegistry.VillagerProfession> event) {
        registerGeneric(info.getVillager(), event);
    }

    public static void registerBiome(ModInfo info, RegistryEvent.Register<Biome> event) {
        registerGeneric(info.getBiomes(), event);
    }

    public static void registerSoundEvent(ModInfo info, RegistryEvent.Register<SoundEvent> event) {
        registerGeneric(info.getSounds(), event);
    }

    public static void registerOreDict(ModInfo info) {
        for (Pair<OreDict, Object> o : info.getOreDicts()) {
            OreDict dict = o.getLeft();
            Object obj = o.getRight();
            if (o.getRight() instanceof Item) {
                OreDictionary.registerOre(dict.key(), (Item) obj);
            } else if (o.getRight() instanceof Block) {
                OreDictionary.registerOre(dict.key(), (Block) obj);
            } else {
                DawnFoundation.getLogger().error("Type {} is not supported register ore dict.Ignore", o.getClass().getName());
            }
        }
    }

    public static void registerSmelt(ModInfo info) {
        for (Pair<Smeltable, Object> p : info.getSmeltables()) {
            Smeltable smeltable = p.getLeft();
            Object o = p.getRight();
            ItemStack input;
            if (o instanceof Item) {
                input = new ItemStack((Item) o);
            } else if (o instanceof Block) {
                input = new ItemStack((Block) o);
            } else {
                DawnFoundation.getLogger().error("Field {} isn't Item or Block.Can't add smelting.Ignore", o.getClass().getName());
                continue;
            }
            Item output = Item.getByNameOrId(smeltable.result());
            if (output == null) {
                DawnFoundation.getLogger().error("Can't find Item {}.Ignore", smeltable.result());
                continue;
            }
            GameRegistry.addSmelting(input, new ItemStack(output, smeltable.resultCount()), smeltable.exp());
        }
    }

    public static void registerTileEntity(ModInfo info) {
        for (Class<? extends TileEntity> tile : info.getTileEntities()) {
            TileEntityRegistry anno = tile.getAnnotation(TileEntityRegistry.class);
            GameRegistry.registerTileEntity(tile, new ResourceLocation(anno.modId(), anno.name()));
        }
    }

    public static void registerGuiHandler(ModInfo info, Object mod) {
        for (Object t : info.getGuiHandlers()) {
            NetworkRegistry.INSTANCE.registerGuiHandler(mod, (IGuiHandler) t);
        }
    }

    public static void registerKeyBinding(ModInfo info) {
        info.getKeyBindings().forEach(ClientRegistry::registerKeyBinding);
    }

    public static RegisterManager getInstance() {
        return instance;
    }

    /**
     * 在 {@link net.minecraftforge.fml.common.event.FMLLoadCompleteEvent} 阶段自动释放资源
     */
    public static void clean() {
        instance = null;
    }

//    public void statistics() {
//        DawnFoundation.getLogger().info("------Register Info------");
//        DawnFoundation.getLogger().info("Item:\t\t\t{}", registeredItemCount);
//        DawnFoundation.getLogger().info("Block:\t\t{}", registeredBlockCount);
//        DawnFoundation.getLogger().info("Entity:\t\t{}", registeredEntityCount);
//        DawnFoundation.getLogger().info("OreDict:\t\t{}", registeredOreDictCount);
//        DawnFoundation.getLogger().info("TileEntity:\t{}", registeredTileEntityCount);
//        DawnFoundation.getLogger().info("Smelt:\t\t{}", registeredSmeltCount);
//        DawnFoundation.getLogger().info("Enchant:\t\t{}", registeredEnchantCount);
//        DawnFoundation.getLogger().info("Potion:\t\t{}", registeredPotionCount);
//        DawnFoundation.getLogger().info("PotionType:\t{}", registeredPotionTypeCount);
//        DawnFoundation.getLogger().info("Dim:\t\t\t{}", registeredDimCount);
//        if (isClient) {
//            DawnFoundation.getLogger().info("EntityRender:\t{}", registeredEntityRenderCount);
//            DawnFoundation.getLogger().info("TESR:\t\t\t{}", registeredTESRCount);
//            DawnFoundation.getLogger().info("GuiHandler:\t{}", registeredGuiHandlerCount);
//            DawnFoundation.getLogger().info("KeyBind:\t{}", registeredKeyBindingCount);
//        }
//        DawnFoundation.getLogger().info("-------------------------");
//    }
}
