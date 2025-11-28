package top.iseason.bukkit.inventoryscanner;

import de.tr7zw.nbtapi.NBT;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;


public final class InventoryScanner extends JavaPlugin {
    @Getter
    @Setter
    private static EditListener listener = null;
    @Getter
    private static InventoryScanner instance = null;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        ConfigManager.plugin = this;
        ConfigManager.loadConfig();
        try {
            ConfigManager.initLogger();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ConfigManager.start();
        getServer().getPluginCommand("InventoryScanner").setExecutor(new Command());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static String getItemStringID(ItemStack itemStack) {
        StringBuilder builder = new StringBuilder(itemStack.getType().toString());
        if (itemStack.hasItemMeta()) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta.hasDisplayName())
                builder.append('_').append(itemMeta.getDisplayName());
        }
        short durability = itemStack.getDurability();
        if (durability > 0) builder.append(':').append(durability);
        return builder.toString();
    }

    public static String serialize(ItemStack itemStack) {
        return NBT.itemStackToNBT(itemStack).toString();
    }

    public static ItemStack deserialize(String string) {
        try {
            return NBT.itemStackFromNBT(NBT.parseNBT(string));
        } catch (Exception e) {
            return null;
        }
    }

}
