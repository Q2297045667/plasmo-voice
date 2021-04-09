package su.plo.voice.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import su.plo.voice.client.VoiceClient;
import su.plo.voice.common.entities.MutedEntity;
import su.plo.voice.socket.SocketClientUDPQueue;

public class CustomEntityRenderer {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    public static void entityRender(PlayerEntity player, MatrixStack matrices, boolean hasLabel, VertexConsumerProvider vertexConsumers, int light) {
        if(VoiceClient.serverConfig == null) {
            renderIcon(VoiceClient.SPEAKER_WARNING, player, matrices, hasLabel, vertexConsumers, light);
            return;
        }

        if(VoiceClient.config.showIcons == 2) {
            return;
        }

        if(player.getUuid().equals(client.player.getUuid())) {
            return;
        }

        if(player.isInvisibleTo(MinecraftClient.getInstance().player) || (client.options.hudHidden && VoiceClient.config.showIcons == 0)) {
            return;
        }

        if(VoiceClient.serverConfig.clients.contains(player.getUuid())) {
            if(VoiceClient.clientMutedClients.contains(player.getUuid())) {
                renderIcon(VoiceClient.SPEAKER_MUTED, player, matrices, hasLabel, vertexConsumers, light);
            } else if (VoiceClient.serverConfig.mutedClients.containsKey(player.getUuid())) {
                MutedEntity muted = VoiceClient.serverConfig.mutedClients.get(player.getUuid());
                if(muted.to == 0 || muted.to > System.currentTimeMillis()) {
                    renderIcon(VoiceClient.SPEAKER_MUTED, player, matrices, hasLabel, vertexConsumers, light);
                } else {
                    VoiceClient.serverConfig.mutedClients.remove(muted.uuid);
                }
            } else if(SocketClientUDPQueue.talking.containsKey(player.getUuid())) {
                if(SocketClientUDPQueue.talking.get(player.getUuid())) {
                    renderIcon(VoiceClient.SPEAKER_PRIORITY, player, matrices, hasLabel, vertexConsumers, light);
                } else {
                    renderIcon(VoiceClient.SPEAKER_ICON, player, matrices, hasLabel, vertexConsumers, light);
                }
            }
        } else {
            renderIcon(VoiceClient.SPEAKER_WARNING, player, matrices, hasLabel, vertexConsumers, light);
        }
    }

    private static void renderIcon(Identifier identifier, PlayerEntity player, MatrixStack matrices, boolean hasLabel, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        if(hasLabel) {
            matrices.translate(0D, player.getHeight() + 0.8D, 0D);
        } else {
            matrices.translate(0D, player.getHeight() + 0.5D, 0D);
        }
        matrices.multiply(client.getEntityRenderDispatcher().getRotation());
        matrices.scale(-0.025F, -0.025F, 0.025F);
        matrices.translate(0D, -1D, 0D);

        float offset = -5F;

        VertexConsumer builder = vertexConsumers.getBuffer(RenderLayer.getText(identifier));
        if(player.isDescending()) {
            vertex(builder, matrices, offset, 10F, 0F, 0F, 1F, 40, light);
            vertex(builder, matrices, offset + 10F, 10F, 0F, 1F, 1F, 40, light);
            vertex(builder, matrices, offset + 10F, 0F, 0F, 1F, 0F, 40, light);
            vertex(builder, matrices, offset, 0F, 0F, 0F, 0F, 40, light);
        } else {
            vertex(builder, matrices, offset, 10F, 0F, 0F, 1F, 255, light);
            vertex(builder, matrices, offset + 10F, 10F, 0F, 1F, 1F, 255, light);
            vertex(builder, matrices, offset + 10F, 0F, 0F, 1F, 0F, 255, light);
            vertex(builder, matrices, offset, 0F, 0F, 0F, 0F, 255, light);

            VertexConsumer builderSeeThrough  = vertexConsumers.getBuffer(RenderLayer.getTextSeeThrough(identifier));
            vertex(builderSeeThrough, matrices, offset, 10F, 0F, 0F, 1F, 40, light);
            vertex(builderSeeThrough, matrices, offset + 10F, 10F, 0F, 1F, 1F, 40, light);
            vertex(builderSeeThrough, matrices, offset + 10F, 0F, 0F, 1F, 0F, 40, light);
            vertex(builderSeeThrough, matrices, offset, 0F, 0F, 0F, 0F, 40, light);
        }

        matrices.pop();
    }

    private static void vertex(VertexConsumer builder, MatrixStack matrices, float x, float y, float z, float u, float v, int alpha, int light) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f modelViewMatrix = entry.getModel();

        builder.vertex(modelViewMatrix, x, y, z);
        builder.color(255, 255, 255, alpha);
        builder.texture(u, v);
        builder.overlay(OverlayTexture.DEFAULT_UV);
        builder.light(light);
        builder.normal(0F, 0F, -1F);
        builder.next();
    }
}