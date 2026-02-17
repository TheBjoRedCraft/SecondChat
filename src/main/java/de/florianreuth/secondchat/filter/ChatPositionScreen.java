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

package de.florianreuth.secondchat.filter;

import de.florianreuth.secondchat.ChatPosition;
import de.florianreuth.secondchat.SecondChat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;

public final class ChatPositionScreen extends Screen {
    private static final int PADDING = 3;
    private static final int LABEL_WIDTH = 80;
    private static final int INPUT_WIDTH = 60;
    private static final int BUTTON_WIDTH = 50;

    private final Screen parent;
    private PositionList positionList;

    public ChatPositionScreen(final Screen parent) {
        super(Component.literal("Chat Positions"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        
        // Create list of chat positions
        this.positionList = new PositionList(
            this.minecraft,
            width,
            height,
            PADDING + PADDING + (font.lineHeight + 2) * PADDING,
            30,
            font.lineHeight + PositionEntry.INNER_PADDING * 4 + 20
        );
        this.addRenderableWidget(positionList);

        // Add back button
        final int y = height - Button.DEFAULT_HEIGHT - PADDING - 1;
        addRenderableWidget(Button
            .builder(Component.literal("<-"), button -> minecraft.setScreen(parent))
            .pos(PADDING, y)
            .size(Button.DEFAULT_HEIGHT, Button.DEFAULT_HEIGHT)
            .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, PADDING + font.lineHeight, 0xFFFFFF);
    }

    private final class PositionList extends ObjectSelectionList<PositionEntry> {
        public PositionList(final net.minecraft.client.Minecraft minecraft, final int width, final int height, final int y0, final int y1, final int itemHeight) {
            super(minecraft, width, height, y0, y1);
            
            // Add entries for all active chats
            final int maxChatId = SecondChat.instance().getMaxChatId();
            for (int i = 1; i <= maxChatId; i++) {
                this.addEntry(new PositionEntry(i));
            }
            
            // If no chats exist, show a message
            if (maxChatId < 1) {
                this.addEntry(new PositionEntry(0)); // 0 = placeholder
            }
        }

        @Override
        public int getRowWidth() {
            return Math.min(600, width - 40);
        }
    }

    private final class PositionEntry extends ObjectSelectionList.Entry<PositionEntry> {
        public static final int INNER_PADDING = 3;
        
        private final int chatId;
        private EditBox xInput;
        private EditBox yInput;
        private Button setButton;
        private Button resetButton;

        public PositionEntry(final int chatId) {
            this.chatId = chatId;
        }

        @Override
        public @NotNull Component getNarration() {
            if (chatId == 0) {
                return Component.literal("No active chats configured. Add filter rules first.");
            }
            return Component.literal("Chat #" + chatId + " position settings");
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent mouseButtonEvent, final boolean bl) {
            return super.mouseClicked(mouseButtonEvent, bl);
        }

        @Override
        public void renderContent(final GuiGraphics guiGraphics, final int i, final int j, final boolean bl, final float f) {
            if (chatId == 0) {
                // Placeholder message
                guiGraphics.drawString(font, 
                    "No active chats configured. Add filter rules in the main config screen first.",
                    getContentX() + INNER_PADDING, 
                    getContentY() + INNER_PADDING, 
                    ChatFormatting.GRAY.getColor());
                return;
            }
            
            final Matrix3x2fStack pose = guiGraphics.pose();
            pose.pushMatrix();
            pose.translate(getContentX(), getContentY());

            // Chat label
            guiGraphics.drawString(font, "Chat #" + chatId, INNER_PADDING, INNER_PADDING + 5, 0xFFFFFF);

            // Get current position
            ChatPosition currentPos = SecondChat.instance().getChatPosition(chatId);
            int currentX = currentPos != null ? currentPos.x() : 0;
            int currentY = currentPos != null ? currentPos.y() : 0;

            // X input
            if (xInput == null) {
                xInput = new EditBox(font, LABEL_WIDTH + INNER_PADDING, INNER_PADDING, INPUT_WIDTH, 20, Component.literal("X"));
                xInput.setValue(String.valueOf(currentX));
                xInput.setMaxLength(6);
            }
            guiGraphics.drawString(font, "X:", LABEL_WIDTH, INNER_PADDING + 5, ChatFormatting.AQUA.getColor());
            xInput.render(guiGraphics, i, j, f);

            // Y input
            if (yInput == null) {
                yInput = new EditBox(font, LABEL_WIDTH + INPUT_WIDTH + PADDING + INNER_PADDING, INNER_PADDING, INPUT_WIDTH, 20, Component.literal("Y"));
                yInput.setValue(String.valueOf(currentY));
                yInput.setMaxLength(6);
            }
            guiGraphics.drawString(font, "Y:", LABEL_WIDTH + INPUT_WIDTH + PADDING, INNER_PADDING + 5, ChatFormatting.AQUA.getColor());
            yInput.render(guiGraphics, i, j, f);

            // Set button
            int buttonX = LABEL_WIDTH + INPUT_WIDTH * 2 + PADDING * 2 + INNER_PADDING;
            if (setButton == null) {
                setButton = Button.builder(Component.literal("Set"), button -> {
                    try {
                        int x = Integer.parseInt(xInput.getValue());
                        int y = Integer.parseInt(yInput.getValue());
                        SecondChat.instance().setChatPosition(new ChatPosition(chatId, x, y));
                    } catch (NumberFormatException e) {
                        // Invalid input, ignore
                    }
                }).pos(buttonX, INNER_PADDING).size(BUTTON_WIDTH, 20).build();
            }
            setButton.setX(buttonX);
            setButton.setY(INNER_PADDING);
            setButton.render(guiGraphics, i, j, f);

            // Reset button
            int resetButtonX = buttonX + BUTTON_WIDTH + PADDING;
            if (resetButton == null) {
                resetButton = Button.builder(Component.literal("Reset"), button -> {
                    SecondChat.instance().removeChatPosition(chatId);
                    xInput.setValue("0");
                    yInput.setValue("0");
                }).pos(resetButtonX, INNER_PADDING).size(BUTTON_WIDTH, 20).build();
            }
            resetButton.setX(resetButtonX);
            resetButton.setY(INNER_PADDING);
            resetButton.render(guiGraphics, i, j, f);

            pose.popMatrix();
        }
    }
}
