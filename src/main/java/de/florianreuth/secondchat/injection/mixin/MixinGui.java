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

import de.florianreuth.secondchat.injection.access.IGui;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.joml.Matrix3x2fStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinGui implements IGui {

    @Shadow
    @Final
    private ChatComponent chat;

    @Unique
    private final List<ChatComponent> secondChat$chatComponents = new ArrayList<>();

    @Unique
    private int secondChat$currentChatIndex = -1;

    @Unique
    private Minecraft secondChat$minecraft;

    @Shadow
    protected abstract void renderChat(final GuiGraphics guiGraphics, final DeltaTracker deltaTracker);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft minecraft, CallbackInfo ci) {
        secondChat$minecraft = minecraft;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderChat(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void renderSecondChat(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        final Matrix3x2fStack pose = guiGraphics.pose();
        
        // Only render chats that are actually in use (based on filter rules)
        final int maxChatId = de.florianreuth.secondchat.SecondChat.instance().getMaxChatId();
        if (maxChatId < 1) {
            return; // No additional chats to render
        }
        
        int cumulativeWidth = 0;
        for (int i = 0; i < maxChatId; i++) {
            secondChat$currentChatIndex = i;
            final ChatComponent chatComponent = secondChat$getChatComponent(i + 1);
            cumulativeWidth += chatComponent.getWidth();
            
            pose.pushMatrix();
            pose.translate(guiGraphics.guiWidth() - cumulativeWidth, 0);
            this.renderChat(guiGraphics, deltaTracker);
            pose.popMatrix();
        }

        secondChat$currentChatIndex = -1;
    }

    @Redirect(method = "renderChat", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;chat:Lnet/minecraft/client/gui/components/ChatComponent;", opcode = Opcodes.GETFIELD))
    private ChatComponent replaceChatComponent(Gui instance) {
        if (secondChat$currentChatIndex >= 0) {
            return secondChat$chatComponents.get(secondChat$currentChatIndex);
        } else {
            return chat;
        }
    }

    @Override
    public List<ChatComponent> secondChat$getChatComponents() {
        // Return only the chat components that have been created (up to maxChatId)
        final int maxChatId = de.florianreuth.secondchat.SecondChat.instance().getMaxChatId();
        if (maxChatId < 1) {
            return new ArrayList<>();
        }
        
        // Ensure all chats up to maxChatId are created
        for (int i = 1; i <= maxChatId; i++) {
            secondChat$getChatComponent(i);
        }
        
        // Return a new list to avoid ConcurrentModificationException
        // After the loop above, we're guaranteed to have at least maxChatId elements
        return new ArrayList<>(secondChat$chatComponents.subList(0, maxChatId));
    }

    @Override
    public ChatComponent secondChat$getChatComponent(int chatId) {
        // Chat IDs are 1-indexed
        // Dynamically create chat components as needed
        if (chatId < 1) {
            chatId = 1; // Fallback to chat 1 for invalid IDs
        }
        
        int index = chatId - 1;
        
        // Expand the list if necessary
        while (secondChat$chatComponents.size() <= index) {
            secondChat$chatComponents.add(new ChatComponent(secondChat$minecraft));
        }
        
        return secondChat$chatComponents.get(index);
    }

}
