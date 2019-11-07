package com.github.ksgfk.dawnfoundation.client;

import com.github.ksgfk.dawnfoundation.api.annotations.RegisterManager;
import com.github.ksgfk.dawnfoundation.common.CommonProxy;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * @author KSGFK create in 2019/11/1
 */
@Mod.EventBusSubscriber
public class ClientProxy extends CommonProxy {
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        RegisterManager.getInstance().registerTESR();
    }

    @SubscribeEvent
    public static void bindEntityRenderer(ModelRegistryEvent event) {
        RegisterManager.getInstance().bindEntityRenderer();
    }
}
