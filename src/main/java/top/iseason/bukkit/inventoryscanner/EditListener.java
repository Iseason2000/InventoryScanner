package top.iseason.bukkit.inventoryscanner;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class EditListener implements Listener {
    private final Player player;
    private String select = null;

    @Getter
    private final HashMap<String, Threshold> itemMap = new HashMap<>();
    @Getter
    private final HashMap<String, String> keyMap = new HashMap<>();

    public EditListener(Player player) {
        this.player = player;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getPlayer() != player) return;
        ItemStack itemInMainHand = player.getEquipment().getItemInMainHand();
        if (itemInMainHand == null) return;
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            select = InventoryScanner.getItemStringID(itemInMainHand);
            keyMap.put(select, InventoryScanner.serialize(itemInMainHand));
            player.sendMessage(ChatColor.GREEN + "已选择物品: " + ChatColor.YELLOW + select + ChatColor.GREEN + " , 在聊天中输入数量: 警告阈值/没收阈值 或者 没收阈值");
        } else if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) {
            if (select != null) {
                player.sendMessage(ChatColor.YELLOW + "已取消物品: " + ChatColor.GREEN + select + ChatColor.YELLOW + " 的限制!");
                itemMap.put(select, null);
                keyMap.put(select, null);
                select = null;
            } else player.sendMessage(ChatColor.YELLOW + "没有选择一个物品!");
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerChatEvent(AsyncPlayerChatEvent event) {
        if (event.getPlayer() != player) {
            return;
        }
        if (select == null) {
            player.sendMessage(ChatColor.YELLOW + "请先选择一个物品");
        }
        Threshold threshold = Threshold.fromString(event.getMessage());
        if (threshold == null) {
            player.sendMessage(ChatColor.YELLOW + "错误的格式，应该为 警告阈值/没收阈值 或者 没收阈值");
        }
        itemMap.put(select, threshold);
        player.sendMessage(ChatColor.GREEN + "物品: " + ChatColor.YELLOW + select + ChatColor.GREEN + " 的限制数量为: " + ChatColor.YELLOW + threshold);
        event.setCancelled(true);
    }
}
