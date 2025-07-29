package cuspymd.mcp.mod.mixin.client;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import cuspymd.mcp.mod.command.ChatMessageCapture;

@Mixin(ChatHud.class)
public class ChatMessageCaptureMixin {
    
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void onChatMessage(Text message, CallbackInfo ci) {
        ChatMessageCapture.getInstance().captureMessage(message.getString());
    }
}