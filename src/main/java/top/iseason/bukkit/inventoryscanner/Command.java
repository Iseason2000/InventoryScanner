package top.iseason.bukkit.inventoryscanner;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

public class Command implements CommandExecutor, TabCompleter {

    private static final List<String> tabList = new ArrayList<>();

    static {
        tabList.add("stop");
        tabList.add("start");
        tabList.add("save");
        tabList.add("edit");
        tabList.add("reload");
        tabList.add("get");
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!sender.isOp()) return true;
        if (args.length < 1) return true;
        if ("edit".equals(args[0])) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "该命令只能玩家使用");
                return true;
            }
            EditListener listener = InventoryScanner.getListener();
            if (listener != null) {
                sender.sendMessage(ChatColor.YELLOW + "当前已处于编辑模式，已放弃之前的修复。");
                HandlerList.unregisterAll(listener);
                InventoryScanner.setListener(null);
            }
            Player player = (Player) sender;
            EditListener editListener = new EditListener(player);
            Bukkit.getServer().getPluginManager().registerEvents(editListener, InventoryScanner.getInstance());
            InventoryScanner.setListener(editListener);
            sender.sendMessage(ChatColor.GREEN + "编辑模式已开启，拿着物品右键选择, 左键移除");
            return true;
        }
        if ("save".equals(args[0])) {
            EditListener listener = InventoryScanner.getListener();
            if (listener == null) {
                sender.sendMessage(ChatColor.YELLOW + "当前不处于编辑模式，无法保存");
            }
            ConfigManager.runAsync(() -> ConfigManager.saveEdit(listener));
            HandlerList.unregisterAll(listener);
            InventoryScanner.setListener(null);
            ConfigManager.loadConfigAsync();
            ConfigManager.runAsync(() -> {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                ConfigManager.loadConfig();
            });
            sender.sendMessage(ChatColor.GREEN + "已保存设置。");
            return true;
        }
        if ("stop".equals(args[0])) {
            ConfigManager.stop();
            sender.sendMessage(ChatColor.YELLOW + "已停止扫描线程。");
            return true;
        }
        if ("start".equals(args[0])) {
            ConfigManager.start();
            sender.sendMessage(ChatColor.GREEN + "已开启扫描线程。");
            return true;
        }
        if ("reload".equals(args[0])) {
            ConfigManager.loadConfigAsync();
            sender.sendMessage(ChatColor.GREEN + "配置已重载。");
            return true;
        }
        if ("get".equals(args[0])) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "请输入玩家名称");
                return true;
            }
            Player player = Bukkit.getPlayerExact(args[1]);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "玩家不存在或不在线");
                return true;
            }
            if (args.length != 3) {
                sender.sendMessage(ChatColor.YELLOW + "请输入物品键");
                return true;
            }
            ItemStack itemFromKey = ConfigManager.getItemFromKey(args[2]);
            if (itemFromKey == null) {
                sender.sendMessage(ChatColor.YELLOW + "物品键不存在");
                return true;
            }
            HashMap<Integer, ItemStack> m = player.getInventory().addItem(itemFromKey);
            if (!m.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "物品已经满");
            } else
                sender.sendMessage(ChatColor.GREEN + "已从物品键 " + ChatColor.YELLOW + args[1] + ChatColor.GREEN + " 获取物品!");
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        if (!sender.isOp()) return null;
        if (args.length != 1) {
            return null;
        }
        return tabList.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
    }
}
