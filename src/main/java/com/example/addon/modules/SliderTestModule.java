package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class SliderTestModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> placeRange = sgGeneral.add(new IntSetting.Builder()
        .name("place-range")
        .description("Width of the walkable highway (railings are added on sides)")
        .defaultValue(5)
        .range(3, 5)
        .build()
    );

    private final Setting<Integer> opacity = sgGeneral.add(new IntSetting.Builder()
        .name("opacity")
        .description("Opacity of the highway render (0-255)")
        .defaultValue(40)
        .range(0, 255)
        .sliderRange(0, 255)
        .build()
    );

    private final Setting<Boolean> placeRailings = sgGeneral.add(new BoolSetting.Builder()
        .name("place-railings")
        .description("Enable placing railings on sides of highway")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> mineAboveRailings = sgGeneral.add(new BoolSetting.Builder()
        .name("mine-above-railings")
        .description("Enable mining blocks above railings")
        .defaultValue(false)
        .build()
    );

    public SliderTestModule() {
        super(AddonTemplate.CATEGORY, "highway-builder", "Highway building tool with railings and mining capabilities.");
    }

    @Override
    public void onActivate() {
        info("Highway Builder activated! Highway width: " + placeRange.get() +
            " blocks, Opacity: " + opacity.get() +
            ", Railings: " + placeRailings.get() +
            ", Mine above: " + mineAboveRailings.get());
    }

    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.player == null) return;

        // Get player position and facing direction
        Vec3d playerPos = mc.player.getPos();
        float yaw = mc.player.getYaw();

        // Get integer player block position
        int playerBlockX = (int) Math.floor(playerPos.x);
        int playerBlockZ = (int) Math.floor(playerPos.z);
        int playerBlockY = (int) Math.floor(playerPos.y) - 1; // One block below player feet

        // Calculate highway width
        int walkableWidth = placeRange.get(); // This is the walkable part
        int halfWidth = walkableWidth / 2; // How many blocks on each side of center

        // Calculate direction vectors based on player yaw (using integers for block positioning)
        // Forward direction (where player is facing)
        double forwardRadians = Math.toRadians(-yaw); // Negative for correct direction
        double forwardX = Math.sin(forwardRadians);
        double forwardZ = Math.cos(forwardRadians);

        // Perpendicular direction for highway width
        double perpX = Math.cos(forwardRadians);
        double perpZ = -Math.sin(forwardRadians);

        // Create color with adjustable opacity
        Color renderColor = new Color(0, 0, 139, opacity.get()); // Dark blue with custom opacity

        // Use a Set to track rendered blocks to avoid duplicates
        java.util.Set<BlockPos> renderedBlocks = new java.util.HashSet<>();

        // Render highway blocks (extending forward for 10 blocks as example)
        for (int i = 0; i < 10; i++) {
            // Calculate forward position using integer block coordinates
            int forwardBlockX = playerBlockX + (int) Math.round(forwardX * i);
            int forwardBlockZ = playerBlockZ + (int) Math.round(forwardZ * i);

            // Render walkable highway blocks centered on player
            // For walkableWidth=5: positions -2, -1, 0, 1, 2
            for (int w = -halfWidth; w <= halfWidth; w++) {
                // Skip if we're beyond the walkable width (for even numbers)
                if (walkableWidth % 2 == 0 && w == halfWidth) continue;

                // Calculate block position using integer coordinates
                int blockX = forwardBlockX + (int) Math.round(perpX * w);
                int blockZ = forwardBlockZ + (int) Math.round(perpZ * w);

                BlockPos blockPos = new BlockPos(blockX, playerBlockY, blockZ);

                // Only render if we haven't already rendered this block
                if (renderedBlocks.add(blockPos)) {
                    Box blockBox = new Box(blockPos);
                    event.renderer.box(blockBox, renderColor, renderColor, ShapeMode.Both, 0);
                }
            }

            // Render railings if enabled
            if (placeRailings.get()) {
                // Left railing (one block to the left of walkable area)
                int leftRailingPos = -halfWidth - 1;
                if (walkableWidth % 2 == 0) leftRailingPos = -halfWidth;

                int leftRailingX = forwardBlockX + (int) Math.round(perpX * leftRailingPos);
                int leftRailingZ = forwardBlockZ + (int) Math.round(perpZ * leftRailingPos);

                BlockPos leftRailing = new BlockPos(leftRailingX, playerBlockY + 1, leftRailingZ);
                if (renderedBlocks.add(leftRailing)) {
                    Box leftRailingBox = new Box(leftRailing);
                    event.renderer.box(leftRailingBox, renderColor, renderColor, ShapeMode.Both, 0);
                }

                // Right railing (one block to the right of walkable area)
                int rightRailingPos = halfWidth + 1;

                int rightRailingX = forwardBlockX + (int) Math.round(perpX * rightRailingPos);
                int rightRailingZ = forwardBlockZ + (int) Math.round(perpZ * rightRailingPos);

                BlockPos rightRailing = new BlockPos(rightRailingX, playerBlockY + 1, rightRailingZ);
                if (renderedBlocks.add(rightRailing)) {
                    Box rightRailingBox = new Box(rightRailing);
                    event.renderer.box(rightRailingBox, renderColor, renderColor, ShapeMode.Both, 0);
                }
            }
        }
    }
}
