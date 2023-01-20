package su.plo.lib.mod.server;

import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import su.plo.lib.api.chat.MinecraftTextConverter;
import su.plo.lib.api.server.MinecraftServerLib;
import su.plo.lib.api.server.entity.MinecraftServerEntity;
import su.plo.lib.api.server.entity.MinecraftServerPlayerEntity;
import su.plo.lib.api.server.permission.PermissionsManager;
import su.plo.lib.api.server.world.MinecraftServerWorld;
import su.plo.lib.mod.client.texture.ResourceCache;
import su.plo.lib.mod.server.command.ModCommandManager;
import su.plo.lib.mod.server.entity.ModServerEntity;
import su.plo.lib.mod.server.entity.ModServerPlayer;
import su.plo.lib.mod.server.world.ModServerWorld;
import su.plo.voice.api.event.EventSubscribe;
import su.plo.voice.api.server.event.player.PlayerJoinEvent;
import su.plo.voice.api.server.event.player.PlayerQuitEvent;
import su.plo.voice.mod.chat.ComponentTextConverter;
import su.plo.voice.proto.data.player.MinecraftGameProfile;
import su.plo.voice.server.player.PermissionSupplier;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
public final class ModServerLib implements MinecraftServerLib {

    public static ModServerLib INSTANCE;

    private final Map<ServerLevel, MinecraftServerWorld> worldByInstance = Maps.newConcurrentMap();
    private final Map<UUID, MinecraftServerPlayerEntity> playerById = Maps.newConcurrentMap();

    @Setter
    private MinecraftServer server;
    @Setter
    private PermissionSupplier permissions;

    private final MinecraftTextConverter<Component> textConverter = new ComponentTextConverter();
    private final ResourceCache resources = new ResourceCache();

    @Getter
    private final ModCommandManager commandManager = new ModCommandManager(this, textConverter);
    @Getter
    private final PermissionsManager permissionsManager = new PermissionsManager();

    @Override
    public void onInitialize() {
        INSTANCE = this;
    }

    @Override
    public void onShutdown() {
        this.server = null;
        this.permissions = null;
        commandManager.clear();
        permissionsManager.clear();
    }

    @Override
    public void executeInMainThread(@NotNull Runnable runnable) {
        server.execute(runnable);
    }

    @Override
    public @NotNull MinecraftServerWorld getWorld(@NotNull Object instance) {
        if (!(instance instanceof ServerLevel))
            throw new IllegalArgumentException("instance is not " + ServerLevel.class);

        return worldByInstance.computeIfAbsent(
                ((ServerLevel) instance),
                (level) -> new ModServerWorld(level.dimension().location(), level)
        );
    }

    @Override
    public Collection<MinecraftServerWorld> getWorlds() {
        if (server.levelKeys().size() == worldByInstance.size()) {
            return worldByInstance.values();
        }

        return StreamSupport.stream(server.getAllLevels().spliterator(), false)
                .map(this::getWorld)
                .toList();
    }

    @Override
    public @NotNull MinecraftServerPlayerEntity getPlayerByInstance(@NotNull Object instance) {
        if (!(instance instanceof ServerPlayer serverPlayer))
            throw new IllegalArgumentException("instance is not " + ServerPlayer.class);

        return playerById.computeIfAbsent(
                serverPlayer.getUUID(),
                (playerId) -> new ModServerPlayer(
                        this,
                        textConverter,
                        permissions,
                        resources,
                        serverPlayer
                )
        );
    }

    @Override
    public Optional<MinecraftServerPlayerEntity> getPlayerByName(@NotNull String name) {
        ServerPlayer player = server.getPlayerList().getPlayerByName(name);
        if (player == null) return Optional.empty();

        return Optional.of(getPlayerByInstance(player));
    }

    @Override
    public Optional<MinecraftServerPlayerEntity> getPlayerById(@NotNull UUID playerId) {
        MinecraftServerPlayerEntity serverPlayer = playerById.get(playerId);
        if (serverPlayer != null) return Optional.of(serverPlayer);

        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) return Optional.empty();

        return Optional.of(getPlayerByInstance(player));
    }

    @Override
    public Optional<MinecraftGameProfile> getGameProfile(@NotNull UUID playerId) {
        return server.getProfileCache().get(playerId)
                .map(this::getGameProfile);
    }

    @Override
    public Optional<MinecraftGameProfile> getGameProfile(@NotNull String name) {
        return server.getProfileCache().get(name)
                .map(this::getGameProfile);
    }

    private MinecraftGameProfile getGameProfile(@NotNull GameProfile gameProfile) {
        return new MinecraftGameProfile(
                gameProfile.getId(),
                gameProfile.getName(),
                gameProfile.getProperties().values().stream()
                        .map((property) -> new MinecraftGameProfile.Property(
                                property.getName(),
                                property.getValue(),
                                property.getSignature()
                        ))
                        .collect(Collectors.toList())
        );
    }

    @Override
    public @NotNull Collection<MinecraftServerPlayerEntity> getPlayers() {
        return playerById.values();
    }

    @Override
    public @NotNull MinecraftServerEntity getEntity(@NotNull Object instance) {
        if (!(instance instanceof Entity))
            throw new IllegalArgumentException("instance is not " + Entity.class);

        return new ModServerEntity(
                this,
                ((Entity) instance)
        );
    }

    @Override
    public int getPort() {
        return server.getPort();
    }

    @EventSubscribe
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        getPlayerByInstance(event.getPlayer());
    }

    @EventSubscribe
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        playerById.remove(event.getPlayerId());
    }
}
