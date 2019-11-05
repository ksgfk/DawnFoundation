package com.github.ksgfk.dawnfoundation;

import com.github.ksgfk.dawnfoundation.api.annotations.RegisterManager;
import com.github.ksgfk.dawnfoundation.api.utility.GithubReleaseUpdateCheck;
import com.github.ksgfk.dawnfoundation.common.CommonProxy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author KSGFK create in 2019/11/1
 */
@Mod(
        modid = DawnFoundation.MOD_ID,
        name = DawnFoundation.MOD_NAME,
        version = DawnFoundation.VERSION,
        updateJSON = DawnFoundation.RELEASE
)
public enum DawnFoundation {
    INSTANCE;

    public static final String MOD_ID = "dawnfoundation";
    public static final String MOD_NAME = "DawnFoundation";
    public static final String VERSION = "@version@";
    public static final String CLIENT = "com.github.ksgfk.dawnfoundation.client.ClientProxy";
    public static final String COMMON = "com.github.ksgfk.dawnfoundation.common.CommonProxy";
    public static final String UPDATE = "https://api.github.com/repos/ksgfk/DawnFoundation/releases";
    public static final String RELEASE = "https://github.com/ksgfk/DawnFoundation/blob/master/update.json";

    private static Logger logger = LogManager.getLogger(MOD_ID);

    @SidedProxy(clientSide = DawnFoundation.CLIENT, serverSide = DawnFoundation.COMMON, modId = DawnFoundation.MOD_ID)
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) throws IllegalAccessException, ClassNotFoundException, NoSuchFieldException {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void gameStart(FMLLoadCompleteEvent event) {
        RegisterManager.getInstance().statistics();
        RegisterManager.clean();
        GithubReleaseUpdateCheck.getInstance().startCheck(MOD_ID, UPDATE, VERSION, true, logger, null);
    }

    @Mod.InstanceFactory
    public static DawnFoundation getInstance() {
        return INSTANCE;
    }

    public static Logger getLogger() {
        return logger;
    }
}
