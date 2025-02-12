package org.windy.antiloop;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class BlockGroup {
    private final String name;
    private boolean isReplace = true;
    private Block replaceBlock;
    private String message;
    private Set<Block> blocks = new HashSet<>();

    public BlockGroup(String name, JsonObject config) {
        this.name = name;

        try {
            // 解析配置
            this.isReplace = config.getAsJsonObject("settings").get("isReplace").getAsBoolean();
            this.message = config.get("message").getAsString();

            // 解析替换方块
            String replaceId = config.get("new").getAsString();
            this.replaceBlock = Blocks.AIR; // 默认替换方块为空气
            // 解析 ResourceLocation
            ResourceLocation replaceLocation = parseResourceLocation(replaceId);
            this.replaceBlock = BuiltInRegistries.BLOCK.get(replaceLocation); // 获取替换方块

            // 解析旧方块列表
            config.getAsJsonArray("old").forEach(element -> {
                String blockId = element.getAsString(); // 获取方块的ID
                ResourceLocation blockLocation = parseResourceLocation(blockId);
                Block block = BuiltInRegistries.BLOCK.get(blockLocation); // 获取方块
                if (block != Blocks.AIR) {
                    blocks.add(block);
                }
            });

        } catch (Exception e) {
            System.err.println("[AntiLoop] 分组 " + name + " 配置错误: " + e.getMessage());
        }
    }

    // 解析 ResourceLocation
    private ResourceLocation parseResourceLocation(String id) {
        String[] parts = id.split(":"); // 使用冒号分割命名空间和路径
        if (parts.length == 2) {
            // 直接使用 tryParse 返回的 ResourceLocation，不需要 Optional
            ResourceLocation location = ResourceLocation.tryParse(id);

            if (location != null) {
                return location; // 返回解析后的 ResourceLocation
            } else {
                throw new IllegalArgumentException("Invalid ResourceLocation format: " + id);
            }
        } else {
            throw new IllegalArgumentException("Invalid ResourceLocation format: " + id);
        }
    }





    public boolean isValid() {
        return !blocks.isEmpty() && message != null;
    }

    public boolean containsBlock(Block block) {
        return blocks.contains(block);
    }

    public Set<Block> getBlocks() {
        return blocks;
    }

    public boolean isReplace() {
        return isReplace;
    }

    public Block getReplaceBlock() {
        return replaceBlock;
    }

    public String getMessage() {
        return message;
    }
}
