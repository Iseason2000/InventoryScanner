package top.iseason.bukkit.inventoryscanner;

import com.tuershen.nbtlibrary.NBTLibraryMain;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;


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
        ConfigManager.start();
        ConfigManager.initLogger();
        getServer().getPluginCommand("InventoryScanner").setExecutor(new Command());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static String getItemStringID(ItemStack itemStack) {
        StringBuilder stringBuilder = new StringBuilder();
        NBTEditor.NBTCompound id1 = NBTEditor.getNBTCompound(itemStack, "id");
        if (id1 == null) return itemStack.getType().toString().toLowerCase();
        stringBuilder.append(id1);
        NBTEditor.NBTCompound type = NBTEditor.getNBTCompound(itemStack, "tag", "type");
        if (type != null && type.toString() != null) stringBuilder.append("_").append(type);
        NBTEditor.NBTCompound damage = NBTEditor.getNBTCompound(itemStack, "Damage");
        if (damage != null && !"0s".equals(damage.toString())) stringBuilder.append("_").append(damage);
        return stringBuilder.toString().replace(":", "").replace("\"", "").replace("s", "");
    }

    public static String serialize(ItemStack itemStack) {
        return NBTLibraryMain.libraryApi.getSerializeItem().serialize(itemStack);
    }

    public static ItemStack deserialize(String string) {
        try {
            return NBTLibraryMain.libraryApi.getSerializeItem().deserialize(string);
        } catch (Exception e) {
            return null;
        }
    }

}
