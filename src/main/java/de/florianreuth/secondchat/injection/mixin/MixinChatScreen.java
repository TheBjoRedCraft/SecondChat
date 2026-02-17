/*
 * This file is part of SecondChat - https://github.com/florianreuth/SecondChat
 * Copyright (C) 2025-2026 Florian Reuth <git@florianreuth.de> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.florianreuth.secondchat.injection.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import de.florianreuth.secondchat.injection.access.IGui;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen extends Screen {

    @Unique
    private int secondChat$focusedChatIndex = -1;

    protected MixinChatScreen(Component title) {
        super(title);
    }

    @Shadow
    protected abstract boolean insertionClickMode();

    @WrapOperation(method = {"keyPressed", "mouseScrolled"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;scrollChat(I)V"))
    private void scrollSecondChat(ChatComponent instance, int posInc, Operation<Void> original) {
        if (secondChat$focusedChatIndex == -1) {
            original.call(instance, posInc);
        } else {
            secondChat$getChatHud(secondChat$focusedChatIndex).scrollChat(posInc);
        }
    }

    @WrapOperation(method = {"mouseClicked"}, at = @At(value = "NEW", target = "(Lnet/minecraft/client/gui/Font;II)Lnet/minecraft/client/gui/ActiveTextCollector$ClickableStyleFinder;"))
    private ActiveTextCollector.ClickableStyleFinder clickSecondChat(Font font, int mouseX, int mouseY, Operation<ActiveTextCollector.ClickableStyleFinder> original) {
        if (secondChat$focusedChatIndex >= 0) {
            mouseX = secondChat$fixMouseX(mouseX, secondChat$focusedChatIndex);
        }

        return original.call(font, mouseX, mouseY);
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void decideFocusedChat(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        final List<ChatComponent> chatComponents = ((IGui) minecraft.gui).secondChat$getChatComponents();
        
        // Pre-calculate cumulative widths for default positions (optimization)
        final int[] cumulativeWidths = new int[chatComponents.size() + 1];
        for (int i = 0; i < chatComponents.size(); i++) {
            cumulativeWidths[i + 1] = cumulativeWidths[i] + chatComponents.get(i).getWidth();
        }
        
        // Determine which chat is focused based on mouse position
        secondChat$focusedChatIndex = -1;
        
        // Calculate positions for each chat (considering custom positions)
        for (int i = 0; i < chatComponents.size(); i++) {
            final int chatId = i + 1;
            final ChatComponent chatComponent = chatComponents.get(i);
            
            // Get the actual position where this chat will be rendered
            final int[] position = secondChat$getChatRenderPosition(chatId, guiGraphics.guiWidth(), cumulativeWidths[chatId]);
            final int chatX = position[0];
            final int chatWidth = chatComponent.getWidth();
            
            // Check if mouse is within this chat's horizontal bounds
            if (mouseX >= chatX && mouseX < chatX + chatWidth) {
                secondChat$focusedChatIndex = i;
                break;
            }
        }

        final Matrix3x2fStack pose = guiGraphics.pose();
        
        // Render all additional chat components
        for (int i = 0; i < chatComponents.size(); i++) {
            final int chatId = i + 1;
            final ChatComponent chatComponent = chatComponents.get(i);
            
            pose.pushMatrix();
            
            // Use the same position calculation
            final int[] position = secondChat$getChatRenderPosition(chatId, guiGraphics.guiWidth(), cumulativeWidths[chatId]);
            pose.translate(position[0], position[1]);
            
            chatComponent.render(guiGraphics, font, minecraft.gui.getGuiTicks(), mouseX, mouseY, true, insertionClickMode());
            pose.popMatrix();
        }
    }
    
    @Unique
    private int[] secondChat$getChatRenderPosition(int chatId, int guiWidth, int cumulativeWidth) {
        de.florianreuth.secondchat.ChatPosition customPos = 
            de.florianreuth.secondchat.SecondChat.instance().getChatPosition(chatId);
        
        if (customPos != null) {
            // Use custom position
            int x = customPos.x() < 0 ? guiWidth + customPos.x() : customPos.x();
            return new int[]{x, customPos.y()};
        } else {
            // Use default position (stacked from right) using pre-calculated cumulative width
            return new int[]{guiWidth - cumulativeWidth, 0};
        }
    }

    @Unique
    private int secondChat$fixMouseX(final int mouseX, final int chatIndex) {
        final int chatId = chatIndex + 1;
        
        // Calculate cumulative width for this chat
        final List<ChatComponent> components = ((IGui) Minecraft.getInstance().gui).secondChat$getChatComponents();
        int cumulativeWidth = 0;
        for (int i = 0; i <= chatIndex && i < components.size(); i++) {
            cumulativeWidth += components.get(i).getWidth();
        }
        
        // Get the actual position where this chat is rendered
        final int[] position = secondChat$getChatRenderPosition(chatId, minecraft.getWindow().getGuiScaledWidth(), cumulativeWidth);
        final int chatX = position[0];
        
        // Convert screen coordinates to chat-local coordinates
        return mouseX - chatX;
    }

    @Unique
    private ChatComponent secondChat$getChatHud(int chatIndex) {
        final Gui gui = Minecraft.getInstance().gui;
        final List<ChatComponent> components = ((IGui) gui).secondChat$getChatComponents();
        if (chatIndex >= 0 && chatIndex < components.size()) {
            return components.get(chatIndex);
        }
        return components.get(0);
    }

}
