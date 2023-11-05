package knkiss.cn;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AntiLoop extends JavaPlugin implements Listener {
    static FileConfiguration config;
    static List<Material> itemID = new ArrayList<>();
    static Logger log;
    List<Group> itemGroup = new ArrayList<>();

    @Override
    public void onEnable() {
        log = this.getLogger();
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("AntiLoop").setExecutor(this);
        config = this.getConfig();
        config.getKeys(false).forEach(path -> itemGroup.add(new Group(path)));
        getLogger().info(" _              _     _   _                             ");
        getLogger().info("/ \\     _ __   | |_  (_) | |       ___     ___    _ __  ");
        getLogger().info("/ _ \\   | '_ \\  | __| | | | |      / _ \\   / _ \\  | '_ \\ ");
        getLogger().info("/ ___ \\  | | | | | |_  | | | |___  | (_) | | (_) | | |_) |");
        getLogger().info("/_/   \\_\\ |_| |_|  \\__| |_| |_____|  \\___/   \\___/  | .__/ ");
        getLogger().info("                                                    |_|    ");
        getLogger().info("AntLoop已加载 - 版本 - 1.0");
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e) {
        if (e.getPlayer().hasPermission("AntiLoop.bypass")) return;
        if (!itemID.contains(e.getBlock().getType())) return;
        itemGroup.forEach(group -> {
            if (group.check(e.getBlock(), e.getPlayer())) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("摆放回路受限");
            }
        });
    }

    @EventHandler
    public void onBreakBlock(BlockBreakEvent e) {
        if (!e.getPlayer().hasPermission("AntiLoop.check")) return;
        if (!e.getPlayer().getItemInHand().getType().equals(Material.BOOK)) return;
        e.getPlayer().sendMessage("方块ID:" + e.getBlock().getType());
        e.setCancelled(true);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("antiloop")) {
            if (!sender.hasPermission("AntiLoop.check")) {
                sender.sendMessage("[AntiLoop] 你没有[AntiLoop.check]权限");
                return true;
            }

            if (args.length == 0) {
                itemGroup.forEach(group -> {
                    sender.sendMessage("----------------------------------------");
                    sender.sendMessage("组名：" + group.path);
                    sender.sendMessage("是否开启（配置无误）: " + group.enable);
                    sender.sendMessage("是否替换: " + group.isReplace);
                    for (Material item : group.itemOld) {
                        sender.sendMessage("oldID:" + item);
                    }
                    if (group.isReplace) {
                        sender.sendMessage("newID:" + group.itemNew);
                    }
                    sender.sendMessage("----------------------------------------");
                });
            } else {
                if (args[0].equalsIgnoreCase("reload")) {
                    itemGroup.clear();
                    itemID.clear();
                    this.reloadConfig();
                    config = this.getConfig();
                    config.getKeys(false).forEach(path -> itemGroup.add(new Group(path)));
                    this.getLogger().info("配置文件重载完毕");
                    if (!(sender instanceof Player)) sender.sendMessage("[AntiLoop] 配置文件重载完毕");
                }
            }
        }
        return true;
    }
}
