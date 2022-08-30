package su.plo.voice.api.server.audio.source;

import org.jetbrains.annotations.NotNull;
import su.plo.voice.api.audio.source.AudioSource;
import su.plo.voice.api.server.player.VoicePlayer;
import su.plo.voice.api.server.pos.ServerPos3d;
import su.plo.voice.proto.packets.Packet;
import su.plo.voice.proto.packets.udp.cllientbound.SourceAudioPacket;

import java.util.UUID;
import java.util.function.Predicate;

public interface ServerAudioSource extends AudioSource {

    @NotNull UUID getId();

    int getState();

    @NotNull ServerPos3d getPosition();

    void setAngle(int angle);

    void setIconVisible(boolean visible);

    boolean isIconVisible();

    void addFilter(Predicate<VoicePlayer> filter);

    void removeFilter(Predicate<VoicePlayer> filter);

    void sendAudioPacket(SourceAudioPacket packet, short distance);

    void sendPacket(Packet<?> packet, short distance);
}
