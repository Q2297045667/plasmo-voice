package su.plo.voice.proto.data.source;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import su.plo.voice.proto.packets.PacketSerializable;
import su.plo.voice.proto.packets.PacketUtil;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

@AllArgsConstructor
@NoArgsConstructor
@ToString
public abstract class SourceInfo implements PacketSerializable {

    public static SourceInfo of(ByteArrayDataInput in) {
        SourceInfo sourceInfo = null;
        switch (Type.valueOf(in.readUTF())) {
            case PLAYER:
                sourceInfo = new PlayerSourceInfo();
                break;
            case ENTITY:
                sourceInfo = new EntitySourceInfo();
                break;
            case STATIC:
                sourceInfo = new StaticSourceInfo();
                break;
            case DIRECT:
                return null;
        }

        if (sourceInfo == null) throw new IllegalArgumentException("Invalid source type");

        sourceInfo.deserialize(in);
        return sourceInfo;
    }

    @Getter
    protected UUID id;
    @Getter
    protected byte state;
    @Getter
    protected String codec;
    @Getter
    protected boolean iconVisible;
    @Getter
    protected int angle;

    @Override
    public void deserialize(ByteArrayDataInput in) {
        this.id = PacketUtil.readUUID(in);
        this.state = in.readByte();
        this.codec = PacketUtil.readNullableString(in);
        this.iconVisible = in.readBoolean();
        this.angle = in.readInt();
    }

    @Override
    public void serialize(ByteArrayDataOutput out) {
        out.writeUTF(getType().name());
        PacketUtil.writeUUID(out, checkNotNull(id));
        out.writeByte(state);
        PacketUtil.writeNullableString(out, codec);
        out.writeBoolean(iconVisible);
        out.writeInt(angle);
    }

    public abstract Type getType();

    public enum Type {
        PLAYER,
        ENTITY,
        STATIC,
        DIRECT
    }
}
