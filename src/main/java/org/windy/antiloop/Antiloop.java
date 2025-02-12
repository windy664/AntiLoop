package org.windy.antiloop;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Antiloop.MODID)
public class Antiloop {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "antiloop";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.

    private static final Gson GSON = new Gson();
    private static final Map<String, BlockGroup> GROUPS = new HashMap<>();
    private static final Set<Block> MONITORED_BLOCKS = new HashSet<>();

    public Antiloop(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (Antiloop) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);



        // 注册事件监听器
        NeoForge.EVENT_BUS.addListener(this::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(this::onBlockRight);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
        loadConfig();
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
    private void loadConfig() {
        File configFile = new File("config/antiloop/config.json");
        try {
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                FileUtils.copyInputStreamToFile(
                        Objects.requireNonNull(getClass().getResourceAsStream("/config/antiloop/config.json")),
                        configFile);
            }

            String jsonStr = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
            JsonObject config = JsonParser.parseString(jsonStr).getAsJsonObject();

            config.keySet().forEach(groupName -> {
                BlockGroup group = new BlockGroup(groupName, config.getAsJsonObject(groupName));
                if (group.isValid()) {
                    GROUPS.put(groupName, group);
                    MONITORED_BLOCKS.addAll(group.getBlocks());
                }
            });

            System.out.println("[AntiLoop] 配置文件加载完成，共加载 " + GROUPS.size() + " 个有效分组");
        } catch (IOException e) {
            System.err.println("[AntiLoop] 配置文件加载失败: " + e.getMessage());
        }
    }

    @SubscribeEvent
    private void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer)) return;

        ServerPlayer player = (ServerPlayer) event.getEntity();
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);

        if (!MONITORED_BLOCKS.contains(state.getBlock())) return;
        if (player.hasPermissions(2)) return; // OP 绕过

        // 遍历所有监控组
        for (BlockGroup group : GROUPS.values()) {
            // 如果方块属于当前组且检测到回路
            if (group.containsBlock(state.getBlock()) && LoopChecker.checkLoop((Level) event.getLevel(), pos, group.getBlocks())) {
                event.setCanceled(true);  // 取消放置方块
                //  player.sendSystemMessage(Component.literal(group.getMessage()));
                player.displayClientMessage(Component.literal(group.getMessage()), true);

                // 如果需要替换方块
                if (group.isReplace()) {
                    event.getLevel().setBlock(pos, group.getReplaceBlock().defaultBlockState(), 3);
                }
                break;  // 一旦检测到回路，停止遍历其他组
            }
        }
    }
    @SubscribeEvent
    private void onBlockRight(PlayerInteractEvent.RightClickBlock event) {
        // 确保在服务端处理
        if (event.getLevel().isClientSide()) return;

        // 确保事件的实体是玩家
        if (!(event.getEntity() instanceof ServerPlayer)) return;

        ServerPlayer player = (ServerPlayer) event.getEntity();
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);

        // 如果玩家手持物品（即放置方块），则返回，不处理
        if (!player.getMainHandItem().isEmpty()) {
            return; // 手持物品时不处理
        }

        // 检查是否是监控的方块
        if (!MONITORED_BLOCKS.contains(state.getBlock())) return;

        // OP 玩家绕过
        if (player.hasPermissions(2)) return;

        // 遍历所有监控组
        for (BlockGroup group : GROUPS.values()) {
            // 如果方块属于当前组且检测到回路
            if (group.containsBlock(state.getBlock()) && LoopChecker.checkLoop(event.getLevel(), pos, group.getBlocks())) {
                event.setCanceled(true);  // 取消放置方块
              //  player.sendSystemMessage(Component.literal(group.getMessage()));
                player.displayClientMessage(Component.literal(group.getMessage()), true);

                // 如果需要替换方块
                if (group.isReplace()) {
                    event.getLevel().setBlock(pos, group.getReplaceBlock().defaultBlockState(), 3);
                }
                break;  // 一旦检测到回路，停止遍历其他组
            }
        }
    }

}
