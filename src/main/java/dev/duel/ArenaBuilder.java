package dev.duel;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Lantern;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Строит мрачную круглую арену слоями (по одному слою за тик, без лагов).
 * Пол и стены — чёрный камень, плачущий обсидиан, цепи и души-фонари.
 * Снаружи и снизу/сверху — бедрок: выбраться и попасть внутрь нельзя.
 */
public class ArenaBuilder {

    public static void build(DuelPlugin plugin, Location c, final int R, final int H, final Runnable done) {
        final World w = c.getWorld();
        final int cx = c.getBlockX(), cy = c.getBlockY(), cz = c.getBlockZ();
        final int W = R + 2;
        new BukkitRunnable() {
            int dy = -4;

            @Override
            public void run() {
                if (dy > H) { cancel(); done.run(); return; }
                for (int dx = -W; dx <= W; dx++) {
                    for (int dz = -W; dz <= W; dz++) {
                        double d = Math.sqrt(dx * dx + dz * dz);
                        if (d > W + 0.5) continue;
                        Material m = pick(dx, dy, dz, d, R, H);
                        Block b = w.getBlockAt(cx + dx, cy + dy, cz + dz);
                        b.setType(m, false);
                        if (m == Material.SOUL_LANTERN && dy == H - 6) {
                            Lantern l = (Lantern) b.getBlockData();
                            l.setHanging(true);
                            b.setBlockData(l, false);
                        }
                    }
                }
                dy++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static int hash(int x, int y, int z) {
        return ((x * 73856093) ^ (y * 83492791) ^ (z * 19349663)) & 0x7fffffff;
    }

    private static Material pick(int dx, int dy, int dz, double d, int R, int H) {
        if (d > R + 1.5) return Material.BEDROCK;          // внешняя оболочка
        if (dy == -4 || dy == H) return Material.BEDROCK;  // дно и крыша
        if (dy <= -2) return Material.BLACKSTONE;
        if (d > R + 0.5) return wall(dx, dy, dz, H);       // внутренний слой стены
        if (dy == -1) return floor(dx, dz, d, R);
        return interior(dx, dy, dz, R, H);
    }

    private static Material floor(int dx, int dz, double d, int R) {
        if (d <= 2.5) return Material.CRYING_OBSIDIAN;
        if (d <= 4.5) return Material.POLISHED_BLACKSTONE_BRICKS;
        if (d >= R - 1.5) return Material.OBSIDIAN;
        if (d >= R - 3.5 && d <= R - 2.5) return Material.CRYING_OBSIDIAN;
        int h = hash(dx, 0, dz) % 10;
        if (h == 0) return Material.CRACKED_POLISHED_BLACKSTONE_BRICKS;
        return (((dx + dz) & 1) == 0) ? Material.POLISHED_BLACKSTONE : Material.POLISHED_BLACKSTONE_BRICKS;
    }

    private static Material wall(int dx, int dy, int dz, int H) {
        if (dy <= 0) return Material.OBSIDIAN;
        if (dy == H - 2) return Material.CRYING_OBSIDIAN;
        double ang = Math.toDegrees(Math.atan2(dz, dx));
        boolean pillar = ((ang + 360 + 7.5) % 30) < 6;
        if (pillar) return dy == H - 3 ? Material.CHISELED_POLISHED_BLACKSTONE : Material.POLISHED_BLACKSTONE_BRICKS;
        int h = hash(dx, dy, dz) % 100;
        if (h < 4) return Material.GILDED_BLACKSTONE;
        if (h < 25) return Material.CRACKED_POLISHED_BLACKSTONE_BRICKS;
        return Material.BLACKSTONE;
    }

    private static Material interior(int dx, int dy, int dz, int R, int H) {
        // 8 колонн-укрытий (2x2, высота 5) на середине радиуса
        int pr = R / 2;
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(22.5 + 45 * k);
            int px = (int) Math.round(pr * Math.cos(a)), pz = (int) Math.round(pr * Math.sin(a));
            if ((dx == px || dx == px + 1) && (dz == pz || dz == pz + 1)) {
                if (dy <= 3) return Material.POLISHED_BLACKSTONE_BRICKS;
                if (dy == 4) return Material.CHISELED_POLISHED_BLACKSTONE;
                if (dy == 5 && dx == px && dz == pz) return Material.SOUL_LANTERN;
                return Material.AIR;
            }
        }
        // 8 цепей с души-фонарями под крышей
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(45 * k);
            int hx = (int) Math.round((R - 4) * Math.cos(a)), hz = (int) Math.round((R - 4) * Math.sin(a));
            if (dx == hx && dz == hz) {
                if (dy >= H - 5) return Material.CHAIN;
                if (dy == H - 6) return Material.SOUL_LANTERN;
            }
        }
        return Material.AIR;
    }
}
