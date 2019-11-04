package com.github.ksgfk.dawnfoundation.common;

import com.github.ksgfk.dawnfoundation.api.annotations.RegisterManager;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;

/**
 * @author KSGFK create in 2019/11/1
 */
@Mod.EventBusSubscriber
public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
        RegisterManager.getInstance().processRegistries(event.getAsmData());
        RegisterManager.getInstance().registerOreDict();
    }

    public void init(FMLInitializationEvent event) {
        RegisterManager.getInstance().registerGuiHandlers();
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
    }
}
