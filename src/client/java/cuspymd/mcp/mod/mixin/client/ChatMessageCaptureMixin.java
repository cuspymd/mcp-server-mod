package cuspymd.mcp.mod.mixin.client;

import cuspymd.mcp.mod.command.ChatMessageCapture;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatMessageCaptureMixin {

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD")
    )
    private void onChatMessage(Component message, MessageSignature signature, GuiMessageTag indicator, CallbackInfo ci) {
        ChatMessageCapture.getInstance().captureMessage(message.getString(), classifySource(signature, indicator));
    }

    private ChatMessageCapture.MessageSource classifySource(MessageSignature signature, GuiMessageTag indicator) {
        if (indicator == GuiMessageTag.system()
            || indicator == GuiMessageTag.systemSinglePlayer()
            || indicator == GuiMessageTag.chatError()) {
            return ChatMessageCapture.MessageSource.SYSTEM;
        }

        if (signature != null || indicator != null) {
            return ChatMessageCapture.MessageSource.PLAYER_CHAT;
        }

        return ChatMessageCapture.MessageSource.UNKNOWN;
    }
}
