package com.github.ksgfk.dawnfoundation.api.utility;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

/**
 * @author KSGFK create in 2019/11/10
 */
public class CommonUtility {
    public static void addUnbreakable(ItemStack stack) {
        if (stack.getTagCompound() == null) {
            stack.setTagCompound(new NBTTagCompound());
        }
        if (!(stack.getTagCompound().hasKey("Unbreakable"))) {
            stack.setTagCompound(new NBTTagCompound().getCompoundTag("Unbreakable"));
            stack.getTagCompound().setBoolean("Unbreakable", true);
        }
    }

    public static EntityPlayer getPlayerByUUID(UUID uuid) {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(uuid);
    }

    @Nullable
    public static EntityPlayer getPlayerByName(String name) {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUsername(name);
    }

    @Nullable
    public static GameProfile getOfflinePlayerByName(String name) {
        return Arrays.stream(FMLCommonHandler.instance().getMinecraftServerInstance().getServerStatusResponse().getPlayers().getPlayers())
                .filter(player -> player.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public static GameProfile getOfflinePlayerByUUID(UUID uuid) {
        return Arrays.stream(FMLCommonHandler.instance().getMinecraftServerInstance().getServerStatusResponse().getPlayers().getPlayers())
                .filter(player -> player.getId().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    public static void playerHealWithCoolDown(EntityPlayer player, int health) {
        boolean canReturnHealth = false;
        float cooledAttackStrength = player.getCooledAttackStrength(0.5F);
        if (cooledAttackStrength >= 0.9999999F) {
            canReturnHealth = true;
        }
        if (!player.world.isRemote && canReturnHealth) {
            player.heal(health);
        }
    }

    public static <K, V> void addNoRepeatListVToMapKV(K key, V value, Map<K, List<V>> map, Supplier<List<V>> allocFunc) {
        map.computeIfAbsent(key, k -> allocFunc.get()).add(value);
    }
}
