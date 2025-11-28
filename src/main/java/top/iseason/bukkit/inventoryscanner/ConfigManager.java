package top.iseason.bukkit.inventoryscanner;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ConfigManager {
    @Getter
    private static HashMap<String, Threshold> blacklist = new HashMap<>();
    @Getter
    private static HashMap<String, String> keyMap = new HashMap<>();
    @Getter
    private static long period = 40L;

    private static Scanner scanner = null;
    public static InventoryScanner plugin = null;
    @Getter
    private static String message = "";
    @Getter
    private static String warningMessage = "";
    @Getter
    static long warningCoolDown = 200L;

    public static String logPath = null;
    public static Integer fileSize = 1024;
    public static Integer fileNum = 10;
    public static boolean consoleSilent = false;
    private static final Logger logger = Logger.getLogger(InventoryScanner.class.getName());

    public static void initLogger() throws IOException {
        new File(logPath).mkdirs();
        FileHandler fileHandler = new FileHandler(logPath + "/log-%u.log", fileSize * 1024, fileNum, true);
        fileHandler.setFormatter(new SimpleFormatter() {
            private static final String format = "[%1$tF %1$tT] %2$s %n";

            @Override
            public synchronized String format(java.util.logging.LogRecord lr) {
                return String.format(format,
                        new java.util.Date(lr.getMillis()),
                        lr.getMessage());
            }
        });
        logger.setLevel(Level.INFO);
        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false); // 不使用全局处理器
    }

    public static void loadConfig() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveDefaultConfig();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        period = config.getLong("check-period", 40L);
        blacklist = loadThresholdMap(config.getConfigurationSection("blacklist"));
        keyMap = loadStringMap(config.getConfigurationSection("key-map"));
        message = config.getString("message", "");
        warningMessage = config.getString("warningMessage", "");
        warningCoolDown = config.getLong("warningCoolDown", 10000L);
        logPath = config.getString("log-path", plugin.getDataFolder().toString() + "/logs");
        consoleSilent = config.getBoolean("console-silent");
        fileSize = config.getInt("file-size", fileSize);
        fileNum = config.getInt("file-num", fileNum);
    }

    public static void stop() {
        if (scanner != null)
            scanner.cancel();
        scanner = null;
    }

    public static void start() {
        if (scanner != null) return;
        scanner = new Scanner();
        scanner.runTaskTimerAsynchronously(plugin, 0L, period);
    }

    public static void loadConfigAsync() {
        ConfigManager.runAsync(ConfigManager::loadConfig);
    }

    public static ItemStack getItemFromKey(String key) {
        String s = keyMap.get(key);
        if (s == null) return null;
        return InventoryScanner.deserialize(s);
    }

    private static HashMap<String, Threshold> loadThresholdMap(ConfigurationSection section) {
        HashMap<String, Threshold> map = new HashMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            Threshold threshold = Threshold.fromString(section.getString(key));
            if (threshold == null) continue;
            map.put(key, threshold);
        }
        return map;
    }

    private static HashMap<String, String> loadStringMap(ConfigurationSection section) {
        HashMap<String, String> map = new HashMap<>();
        if (section == null) return map;
        for (String key : section.getKeys(false)) {
            String string = section.getString(key, null);
            if (string == null) continue;
            map.put(key, string);
        }
        return map;
    }

    public static void saveEdit(EditListener listener) {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveDefaultConfig();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection b = config.getConfigurationSection("blacklist");
        if (b == null) b = config.createSection("blacklist");
        ConfigurationSection finalB = b;
        listener.getItemMap().forEach((k, v) -> finalB.set(k, v.toString()));
        ConfigurationSection k = config.getConfigurationSection("key-map");
        if (k == null) k = config.createSection("key-map");
        listener.getKeyMap().forEach(k::set);
        config.set("log-path", logPath);
        config.set("console-silent", consoleSilent);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().info("配置报错异常");
        }
    }

    public static void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    private static class Scanner extends BukkitRunnable {
        private final HashMap<UUID, Long> coolDown = new HashMap<>();

        @Override
        public void run() {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                //用于统计黑名单里的物品的数量
                if (onlinePlayer.isOp() || onlinePlayer.hasPermission("inventoryscanner.bypass")) continue;
                HashMap<String, Integer> temp = new HashMap<>();
                HashMap<String, List<Integer>> temp2 = new HashMap<>();
//                AtomicInteger index = new AtomicInteger(-1);
                //统计数量
                InventoryView openInventory = onlinePlayer.getOpenInventory();
                for (int index = 0; index < 1000; index++) {
                    ItemStack itemStack;
                    try {
                        itemStack = openInventory.getItem(index);
                    } catch (Exception e) {
                        break;
                    }
                    if (itemStack == null) continue;
                    if (itemStack.getType() == Material.AIR) continue;
                    String itemStringID = null;
                    try {
                        itemStringID = InventoryScanner.getItemStringID(itemStack);
                    } catch (Exception ignored) {
                    }
                    if (itemStringID == null) continue;
                    Threshold threshold = getBlacklist().get(itemStringID);
                    if (threshold == null) continue;
                    Integer count = temp.get(itemStringID);
                    int amount = itemStack.getAmount();
                    count = (count != null) ? count + amount : amount;
                    temp.put(itemStringID, count);
                    //超过限额
                    if (threshold.getMax() >= 0 && count >= threshold.getMax()) {
                        //获取之前的索引
                        List<Integer> preIndexes = temp2.get(itemStringID);
                        if (preIndexes != null) {
                            for (Integer preIndex : preIndexes) {
                                openInventory.setItem(preIndex, null);
                            }
                            temp2.remove(itemStringID);
                        }
                        openInventory.setItem(index, null);
                        continue;
                    }
                    //未超标前储存索引
                    List<Integer> indexes = temp2.computeIfAbsent(itemStringID, k -> new ArrayList<>());
                    indexes.add(index);
                }
                //打印日志
                temp.forEach((K, V) -> {
                    Threshold threshold = getBlacklist().get(K);
                    int warning = threshold.getWarning();
                    int max = threshold.getMax();
                    if (warning > 0 && V >= warning && V < max) {
                        UUID uniqueId = onlinePlayer.getUniqueId();
                        Long aLong = coolDown.get(uniqueId);
                        long l = System.currentTimeMillis();
                        if (aLong != null && l - aLong < getWarningCoolDown()) return;

                        String s = "玩家 " + onlinePlayer.getName() + " 触发黑名单预警 " + K + ChatColor.RESET + " 警告阈值 " + warning + " ，背包拥有的数量为 " + V + ",位置: " + formatLocation(onlinePlayer.getLocation());
                        if (!consoleSilent) plugin.getLogger().info(s);
                        logger.info(s);
                        String m = getWarningMessage();
                        if (!m.isEmpty()) {
                            coolDown.put(uniqueId, l);
                            onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', m).replace("%item%", K).replace("%max%", String.valueOf(max)).replace("%current%", V.toString()));
                        }
                        return;
                    } else if (V < warning) return;
                    String m = getMessage();
                    if (!m.isEmpty()) {
                        onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', m).replace("%item%", K).replace("%max%", String.valueOf(max)).replace("%current%", V.toString()));
                    }
                    String s = "玩家 " + onlinePlayer.getName() + " 触发黑名单 " + K + ChatColor.RESET + " 阈值 " + max + " ，背包拥有的数量为 " + V + ",位置: " + formatLocation(onlinePlayer.getLocation());
                    if (!consoleSilent) plugin.getLogger().info(s);
                    logger.info(s);
                });
            }
        }
    }

    private static String formatLocation(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
//    private static class LogFormatter {
//        private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//        public static String format(String record) {
//            StringBuilder sb = new StringBuilder();
//            String dataFormat = sdf.format(System.currentTimeMillis());
//            sb.append("[").append(dataFormat).append("]").append(" ");
//            sb.append(record).append("\n");
//            return sb.toString();
//        }
//    }
}
