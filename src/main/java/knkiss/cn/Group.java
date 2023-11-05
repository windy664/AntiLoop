package knkiss.cn;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Group {
    String path;
    boolean enable = true;
    boolean isReplace = false;
    List<Material> itemOld = new ArrayList<>();
    Material itemNew;
    String message;

    Group(String path) {
        this.path = path;
        try {
            message = AntiLoop.config.getString(path + ".message");
        } catch (Exception e) {
            AntiLoop.log.warning(path + "的组message设置有误，无法开启");
            enable = false;
        }
        try {
            isReplace = AntiLoop.config.getBoolean(path + ".settings.isReplace");
        } catch (Exception e) {
            AntiLoop.log.warning(path + "的组是否替换有误，无法开启");
            enable = false;
        }
        try {
            if (isReplace) {
                String ID = Objects.requireNonNull(AntiLoop.config.getString(path + ".new"))
                        .toUpperCase();
                Material material = Material.getMaterial(ID);
                if (material == null) {
                    AntiLoop.log.warning("没有找到方块:" + ID);
                    enable = false;
                    return;
                }
                itemNew = material;
            }
            AntiLoop.config.getStringList(path + ".old").forEach(itemStr -> {
                String ID = itemStr.toUpperCase();
                Material material = Material.getMaterial(ID);
                if (material == null) {
                    AntiLoop.log.warning("没有找到方块:" + ID);
                    enable = false;
                    return;
                }
                itemOld.add(material);
                AntiLoop.itemID.add(material);
            });
        } catch (Exception e) {
            AntiLoop.log.warning(path + "的组方块设置有误，无法开启");
            enable = false;
        }
    }

    public boolean check(Block b, Player p) {
        if (!isLoop(b)) return false;
        p.sendMessage(message);
        if (isReplace) {
            b.setType(itemNew); // 替换方块
        }
        return !isReplace;
    }

    public boolean isLoop(Block b) {
        ArrayList<Location> list = new ArrayList<>();
        list.add(b.getLocation());
        return isLoop(b.getLocation(), b.getLocation(), b.getLocation(), list);
    }

    private boolean isLoop(Location target, Location prevLocation, Location originalLocation, ArrayList<Location> list) {
        Location[] neighbors = new Location[] {
            target.clone().add(0, 1, 0), // 上
            target.clone().add(0, -1, 0), // 下
            target.clone().add(-1, 0, 0), // 左
            target.clone().add(1, 0, 0), // 右
            target.clone().add(0, 0, -1), // 前
            target.clone().add(0, 0, 1) // 后
        };

        for (Location neighbor : neighbors) {
            if (neighbor.equals(prevLocation)) continue;
            if (!itemOld.contains(neighbor.getBlock().getType())) continue;
            if (list.contains(neighbor)) {
                if (isReplace) {
                    Block block = neighbor.getBlock();
                    block.setType(itemNew, false);
                }
                return true;
            }
            list.add(neighbor);
            if (neighbor.equals(originalLocation)) {
                return true;
            }
            if (this.isLoop(neighbor, target, originalLocation, list)) {
                return true;
            }
        }
        return false;
    }
}
