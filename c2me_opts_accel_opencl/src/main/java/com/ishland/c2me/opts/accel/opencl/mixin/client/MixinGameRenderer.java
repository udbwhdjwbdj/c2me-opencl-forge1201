package com.ishland.c2me.opts.accel.opencl.mixin.client;

import com.ishland.c2me.opts.accel.opencl.common.progress.GlobalProgressStash;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public class MixinGameRenderer {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"
      )}
   )
   private void onRenderSubtitles(CallbackInfo ci, @Local GuiGraphics drawContext) {
      String progressText = GlobalProgressStash.PROGRESS_TEXT;
      if (progressText != null) {
         int width = this.minecraft.getWindow().getGuiScaledWidth() - 20;
         Component text = Component.nullToEmpty(progressText);
         c2me$drawWrappedText(
            drawContext,
            this.minecraft.font,
            text,
            10,
            this.minecraft.getWindow().getGuiScaledHeight() - this.minecraft.font.wordWrapHeight(text, width) - 10,
            width,
            -2039584,
            true
         );
      }
   }

   @Unique
   private static void c2me$drawWrappedText(GuiGraphics context, Font textRenderer, FormattedText text, int x, int y, int width, int color, boolean shadow) {
      for (FormattedCharSequence line : textRenderer.split(text, width)) {
         context.drawString(textRenderer, line, x, y, color, shadow);
         y += 9;
      }
   }
}
