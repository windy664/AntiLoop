package org.windy.antiloop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.Set;

public class LoopChecker {

    // 主方法，用于开始回路检查
    public static boolean checkLoop(Level level, BlockPos startPos, Set<Block> blocks) {
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> currentPath = new HashSet<>(); // 用于记录当前路径
        return isLoop(level, startPos, null, startPos, currentPath, visited, blocks);
    }

    // 递归检查回路，考虑3D方向
    private static boolean isLoop(Level level, BlockPos current, BlockPos prevLocation, BlockPos originalLocation,
                                  Set<BlockPos> currentPath, Set<BlockPos> visited, Set<Block> blocks) {
        // 如果当前方块已经在路径中，说明形成了回路
        if (currentPath.contains(current)) {
          //  System.out.println("发现回路：" + current);  // 打印检测到回路的位置
            return true; // 形成回路
        }

        // 如果当前方块已被访问过且不在路径中，跳过
        if (visited.contains(current)) {
            return false;
        }

        // 在访问当前方块之前，先标记当前方块
        visited.add(current);
        currentPath.add(current);

        // 遍历所有六个方向，检查相邻方块
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = current.relative(dir);

            // 跳过上一个方块，避免循环
            if (neighbor.equals(prevLocation)) continue;

            // 仅当邻居是我们关心的方块时，继续
            if (!level.getBlockState(neighbor).isAir() && blocks.contains(level.getBlockState(neighbor).getBlock())) {
                // 递归检查下一个方块
                if (isLoop(level, neighbor, current, originalLocation, currentPath, visited, blocks)) {
                    return true; // 如果找到回路，则返回
                }
            }
        }

        // 如果没有找到回路，回溯并去掉当前方块
        currentPath.remove(current);
        return false; // 没有形成回路
    }
}
