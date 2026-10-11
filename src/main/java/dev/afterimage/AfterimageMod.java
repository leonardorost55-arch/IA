package dev.afterimage;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class AfterimageMod implements ModInitializer {
    private static final Logger LOGGER = Logger.getLogger("AFTERIMAGE");
    private static final long TOTAL_TICKS = 20L * 60L * 20L;
    private static final long SAVE_INTERVAL = 100L;
    private static final Map<UUID, Long> progress = new ConcurrentHashMap<>();
    private static final Set<UUID> finale = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, List<UUID>> anomaly = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() == World.OVERWORLD) loadProgress(server);
        });
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (world.getRegistryKey() == World.OVERWORLD) saveProgress(server);
        });
        ServerTickEvents.END_SERVER_TICK.register(AfterimageMod::tick);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative()) continue;
            UUID id = player.getUuid();
            if (finale.contains(id)) {
                maintainFinale(player);
                continue;
            }
            long t = progress.getOrDefault(id, 0L) + 1;
            progress.put(id, t);
            if (t % SAVE_INTERVAL == 0) saveProgress(server);
            if (t >= TOTAL_TICKS) {
                startFinale(player);
                continue;
            }

            event(player, t, 40 * 20L, "Esse som deve ter sido da caverna.");
            event(player, t, 2 * 60 * 20L, "Seus passos pareceram vir um bloco atrás.");
            event(player, t, 4 * 60 * 20L, "Não há motivo para conferir a lista de jogadores.");
            if (t == 6 * 60 * 20L) {
                play(player, SoundEvents.BLOCK_NOTE_BLOCK_BIT, SoundCategory.AMBIENT, 0.15f, 0.45f);
                whisper(player, "Uma nota apareceu onde o cursor estava.");
            }
            if (t == 8 * 60 * 20L) {
                player.sendMessage(Text.literal("[Local] não olhe para trás ainda").formatted(Formatting.DARK_GRAY), false);
            }
            if (t == 10 * 60 * 20L) {
                whisper(player, "Este mundo existe há mais tempo do que você.");
                play(player, SoundEvents.AMBIENT_CAVE, SoundCategory.AMBIENT, 0.25f, 0.6f);
            }
            if (t == 12 * 60 * 20L) spawnAnomaly(player);
            event(player, t, 14 * 60 * 20L, "Consigo ver o espaço entre os seus movimentos.");
            event(player, t, 16 * 60 * 20L, "Você pode parar a gravação quando quiser.");
            if (t == 18 * 60 * 20L) {
                whisper(player, "Seu nome não está escrito no arquivo do mundo.");
                play(player, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.AMBIENT, 0.25f, 0.55f);
            }
            if (t > 19 * 60 * 20L && t % 80 == 0) {
                player.sendMessage(Text.literal("[" + player.getName().getString() + "]: ainda aqui").formatted(Formatting.DARK_GRAY), false);
            }
            updateAnomaly(player);
        }
    }

    private static void event(ServerPlayerEntity player, long tick, long mark, String message) {
        if (tick == mark) whisper(player, message);
    }

    private static void whisper(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.literal(message).formatted(Formatting.GRAY), false);
    }

    private static void play(ServerPlayerEntity player, net.minecraft.sound.SoundEvent sound, SoundCategory category, float volume, float pitch) {
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(), sound, category, volume, pitch);
    }

    private static void spawnAnomaly(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        Vec3d behind = player.getRotationVec(1.0f).multiply(-15.0);
        Vec3d center = player.getEntityPos().add(behind);
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ArmorStandEntity stand = new ArmorStandEntity(EntityType.ARMOR_STAND, world);
            stand.refreshPositionAndAngles(center.x + (player.getRandom().nextDouble() - 0.5) * 0.7,
                    center.y + i * 0.18, center.z + (player.getRandom().nextDouble() - 0.5) * 0.7,
                    player.getRandom().nextFloat() * 360f, player.getRandom().nextFloat() * 90f);
            stand.setInvisible(true);
            stand.setNoGravity(true);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.OBSIDIAN));
            if (world.spawnEntity(stand)) ids.add(stand.getUuid());
        }
        anomaly.put(player.getUuid(), ids);
        whisper(player, "Há seis erros no formato de uma pessoa.");
    }

    private static void updateAnomaly(ServerPlayerEntity player) {
        List<UUID> ids = anomaly.get(player.getUuid());
        if (ids == null || ids.isEmpty()) return;
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        Vec3d look = player.getRotationVec(1.0f).normalize();
        for (UUID id : ids) {
            var entity = world.getEntity(id);
            if (!(entity instanceof ArmorStandEntity stand)) continue;
            Vec3d delta = stand.getEntityPos().subtract(player.getEyePos());
            double distance = delta.length();
            double dot = look.dotProduct(delta.normalize());
            if (distance < 8.0 || (distance < 24.0 && dot > 0.985)) {
                for (UUID other : ids) {
                    var e = world.getEntity(other);
                    if (e != null) e.discard();
                }
                anomaly.remove(player.getUuid());
                play(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.AMBIENT, 0.8f, 0.65f);
                play(player, SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.AMBIENT, 0.55f, 0.7f);
                whisper(player, "Você percebeu. Ele também.");
                return;
            }
            if (player.age % 2 == 0) {
                stand.setHeadYaw(player.getRandom().nextFloat() * 360f);
                stand.setYaw(player.getRandom().nextFloat() * 360f);
                stand.setPitch(player.getRandom().nextFloat() * 180f - 90f);
                stand.setBodyYaw(player.getRandom().nextFloat() * 360f);
            }
        }
    }

    private static void startFinale(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        finale.add(id);
        progress.put(id, TOTAL_TICKS);
        maintainFinale(player);
        player.sendMessage(Text.literal("VOCÊ NUNCA ESTEVE JOGANDO SOZINHO.").formatted(Formatting.RED, Formatting.BOLD), false);
        player.sendMessage(Text.literal("A gravação pode acabar. Eu não.").formatted(Formatting.DARK_RED), false);
    }

    private static void maintainFinale(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 1200, 254, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 1200, 254, false, false, true));
        player.setVelocity(Vec3d.ZERO);
    }

    private static Path cachePath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve(".opt_state_cache");
    }

    private static void saveProgress(MinecraftServer server) {
        try {
            Path path = cachePath(server);
            Files.createDirectories(path.getParent());
            List<String> lines = new ArrayList<>();
            progress.forEach((id, ticks) -> lines.add(id + "=" + ticks + (finale.contains(id) ? "=END" : "")));
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warning("Não foi possível salvar o progresso: " + e.getMessage());
        }
    }

    private static void loadProgress(MinecraftServer server) {
        Path path = cachePath(server);
        if (!Files.exists(path)) return;
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String[] parts = line.split("=");
                if (parts.length < 2) continue;
                UUID id = UUID.fromString(parts[0]);
                long ticks = Long.parseLong(parts[1]);
                progress.put(id, Math.min(TOTAL_TICKS, ticks));
                if (parts.length >= 3 && "END".equals(parts[2])) finale.add(id);
            }
        } catch (Exception e) {
            LOGGER.warning("Cache de progresso inválido; iniciando sem dados recuperáveis.");
        }
    }
}