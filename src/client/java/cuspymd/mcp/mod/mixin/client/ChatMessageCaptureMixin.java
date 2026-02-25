package cuspymd.mcp.mod.mixin.client;

import cuspymd.mcp.mod.command.ChatMessageCapture;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public class ChatMessageCaptureMixin {

    @Inject(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD")
    )
    private void onChatMessage(Text message, MessageSignatureData signature, MessageIndicator indicator, CallbackInfo ci) {
        ChatMessageCapture.getInstance().captureMessage(message.getString(), classifySource(signature, indicator));
    }

    private ChatMessageCapture.MessageSource classifySource(MessageSignatureData signature, MessageIndicator indicator) {
        if (indicator == MessageIndicator.system()
            || indicator == MessageIndicator.singlePlayer()
            || indicator == MessageIndicator.chatError()) {
            return ChatMessageCapture.MessageSource.SYSTEM;
        }

        if (signature != null || indicator != null) {
            return ChatMessageCapture.MessageSource.PLAYER_CHAT;
        }

        return ChatMessageCapture.MessageSource.UNKNOWN;
    }
}
