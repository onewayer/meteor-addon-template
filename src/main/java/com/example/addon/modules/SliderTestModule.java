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



    private final Setting<Integer> renderDistance = sgGeneral.add(new IntSetting.Builder()
        .name("render-distance")
        .description("How far ahead to render the highway preview")
        .defaultValue(10)
        .range(5, 50)
        .sliderRange(5, 50)
        .build()
    );

    // 4-directional highway system for straight highways only
    private enum HighwayDirection {
        NORTH(0, 1, 0.0f, "North"),     // Forward in negative Z
        EAST(-1, 0, 90.0f, "East"),     // Forward in positive X
        SOUTH(0, -1, 180.0f, "South"),  // Forward in positive Z
        WEST(1, 0, 270.0f, "West");     // Forward in negative X

        public final int offsetX, offsetZ;
        public final float yaw;
        public final String displayName;

        HighwayDirection(int offsetX, int offsetZ, float yaw, String displayName) {
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
            this.yaw = yaw;
            this.displayName = displayName;
        }

        // Snap player yaw to nearest cardinal direction
        public static HighwayDirection fromPlayerYaw(float playerYaw) {
            // Normalize yaw to 0-360 range
            float normalizedYaw = ((playerYaw % 360) + 360) % 360;

            // Find closest cardinal direction (each covers 90-degree sector)
            if (normalizedYaw >= 315 || normalizedYaw < 45) {
                return NORTH;  // 315-45 degrees
            } else if (normalizedYaw >= 45 && normalizedYaw < 135) {
                return EAST;   // 45-135 degrees
            } else if (normalizedYaw >= 135 && normalizedYaw < 225) {
                return SOUTH;  // 135-225 degrees
            } else {
                return WEST;   // 225-315 degrees
            }
        }
    }

    public SliderTestModule() {
        super(AddonTemplate.CATEGORY, "highway-builder", "Highway building tool with railings and mining capabilities for straight highways only.");
    }

    @Override
    public void onActivate() {
        info("Highway Builder activated: width is " + placeRange.get() + " blocks");
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
        int walkableWidth = placeRange.get();
        int halfWidth = walkableWidth / 2;

        // Get direction vectors (always use axis snapping)
        HighwayDirection direction = HighwayDirection.fromPlayerYaw(yaw);
        double forwardX = direction.offsetX;
        double forwardZ = direction.offsetZ;

        // Calculate perpendicular for cardinal directions
        int perpX = -direction.offsetZ;
        int perpZ = direction.offsetX;

        // Create colors
        Color renderColor = new Color(0, 0, 139, opacity.get());
        Color axisColor = new Color(255, 0, 0, opacity.get()); // Red for center line

        // Track rendered blocks to avoid duplicates
        java.util.Set<BlockPos> renderedBlocks = new java.util.HashSet<>();

        // Render highway blocks
        int distance = renderDistance.get();
        for (int i = 0; i < distance; i++) {
            // Calculate forward position
            int forwardBlockX = playerBlockX + (int) Math.round(forwardX * i);
            int forwardBlockZ = playerBlockZ + (int) Math.round(forwardZ * i);

            // Render walkable highway blocks
            for (int w = -halfWidth; w <= halfWidth; w++) {
                // Skip edge for even widths
                if (walkableWidth % 2 == 0 && w == halfWidth) continue;

                // Calculate block position using perpendicular vectors
                int blockX = forwardBlockX + (perpX * w);
                int blockZ = forwardBlockZ + (perpZ * w);

                BlockPos blockPos = new BlockPos(blockX, playerBlockY, blockZ);

                if (renderedBlocks.add(blockPos)) {
                    Box blockBox = new Box(blockPos);
                    // Highlight center line in red
                    Color blockColor = (w == 0) ? axisColor : renderColor;
                    event.renderer.box(blockBox, blockColor, blockColor, ShapeMode.Both, 0);
                }
            }

            // Render railings if enabled
            if (placeRailings.get()) {
                // Left railing
                int leftRailingPos = -halfWidth - 1;
                if (walkableWidth % 2 == 0) leftRailingPos = -halfWidth;

                int leftRailingX = forwardBlockX + (perpX * leftRailingPos);
                int leftRailingZ = forwardBlockZ + (perpZ * leftRailingPos);

                BlockPos leftRailing = new BlockPos(leftRailingX, playerBlockY + 1, leftRailingZ);
                if (renderedBlocks.add(leftRailing)) {
                    Box leftRailingBox = new Box(leftRailing);
                    event.renderer.box(leftRailingBox, renderColor, renderColor, ShapeMode.Both, 0);
                }

                // Right railing
                int rightRailingPos = halfWidth + 1;

                int rightRailingX = forwardBlockX + (perpX * rightRailingPos);
                int rightRailingZ = forwardBlockZ + (perpZ * rightRailingPos);

                BlockPos rightRailing = new BlockPos(rightRailingX, playerBlockY + 1, rightRailingZ);
                if (renderedBlocks.add(rightRailing)) {
                    Box rightRailingBox = new Box(rightRailing);
                    event.renderer.box(rightRailingBox, renderColor, renderColor, ShapeMode.Both, 0);
                }
            }
        }
    }
}
