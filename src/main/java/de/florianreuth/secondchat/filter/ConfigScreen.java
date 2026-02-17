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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;

public final class ConfigScreen extends Screen {
    private static final int RED_TRANSPARENT = 0x80FF0000;
    private static final int PADDING = 3;
    private static final int CHAT_ID_BUTTON_SPACING = 2;

    // Button widths
    private static final int TEXT_FIELD_WIDTH = 150;
    private static final int FILTER_BUTTON_WIDTH = 100;
    private static final int CHAT_ID_BUTTON_WIDTH = 30;
    private static final int CHAT_ID_DEC_WIDTH = 15;
    private static final int CHAT_ID_INC_WIDTH = 15;
    private static final int ADD_BUTTON_WIDTH = 30;
    
    // Total width calculation:
    // Buttons: 150 + 100 + 15 + 30 + 15 + 30 = 340
    // Spacing: PADDING between (editBox-filter), (filter-dec), (inc-add) = 3*PADDING
    //          CHAT_ID_BUTTON_SPACING between (dec-chatId), (chatId-inc) = 2*CHAT_ID_BUTTON_SPACING
    private static final int TOTAL_CONTROL_WIDTH = TEXT_FIELD_WIDTH + FILTER_BUTTON_WIDTH + 
                                                     CHAT_ID_DEC_WIDTH + CHAT_ID_BUTTON_WIDTH + 
                                                     CHAT_ID_INC_WIDTH + ADD_BUTTON_WIDTH +
                                                     3 * PADDING + 2 * CHAT_ID_BUTTON_SPACING;

    private final Screen parent;

    private EditBox editBox;
    private Button addButton;
    private Button chatIdButton; // Needs to be a field so decrement/increment buttons can update its display text
    private FilterType filterType = FilterType.CONTAINS;
    private int chatId = 1;

    private FilterRule alreadyAdded;

    public ConfigScreen(final Screen parent) {
        super(Component.translatable("secondchat.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new SlotList(
            this.minecraft,
            width,
            height,
            PADDING + PADDING + (font.lineHeight + 2) * PADDING /* title is 2 */,
            30,
            font.lineHeight + ListEntry.INNER_PADDING * 4 + 20  // Increased height for position controls
        ));

        final int y = height - Button.DEFAULT_HEIGHT - PADDING - 1;
        // Center all controls horizontally
        int x = width / 2 - TOTAL_CONTROL_WIDTH / 2;
        editBox = addRenderableWidget(new EditBox(this.font, x, y, TEXT_FIELD_WIDTH, Button.DEFAULT_HEIGHT, Component.empty()));

        x += TEXT_FIELD_WIDTH + PADDING;
        addRenderableWidget(Button
            .builder(getFilterTypeText(filterType), button -> {
                filterType = FilterType.values()[(filterType.ordinal() + 1) % FilterType.values().length];
                button.setMessage(getFilterTypeText(filterType));
            })
            .pos(x, y)
            .size(FILTER_BUTTON_WIDTH, Button.DEFAULT_HEIGHT)
            .build());

        x += FILTER_BUTTON_WIDTH + PADDING;
        // Decrement button
        addRenderableWidget(Button
            .builder(Component.literal("-"), button -> {
                if (chatId > 1) {
                    chatId--;
                    chatIdButton.setMessage(getChatIdText(chatId));
                }
            })
            .pos(x, y)
            .size(CHAT_ID_DEC_WIDTH, Button.DEFAULT_HEIGHT)
            .build());
        
        x += CHAT_ID_DEC_WIDTH + CHAT_ID_BUTTON_SPACING;
        chatIdButton = addRenderableWidget(Button
            .builder(getChatIdText(chatId), button -> {
                // Click to cycle through common values
                if (chatId < 10) {
                    chatId++;
                } else {
                    chatId = 1;
                }
                button.setMessage(getChatIdText(chatId));
            })
            .pos(x, y)
            .size(CHAT_ID_BUTTON_WIDTH, Button.DEFAULT_HEIGHT)
            .build());
        
        x += CHAT_ID_BUTTON_WIDTH + CHAT_ID_BUTTON_SPACING;
        // Increment button
        addRenderableWidget(Button
            .builder(Component.literal("+"), button -> {
                chatId++;
                chatIdButton.setMessage(getChatIdText(chatId));
            })
            .pos(x, y)
            .size(CHAT_ID_INC_WIDTH, Button.DEFAULT_HEIGHT)
            .build());

        x += CHAT_ID_INC_WIDTH + PADDING;
        addButton = addRenderableWidget(Button
            .builder(Component.translatable("secondchat.config.add"), button -> {
                SecondChat.instance().add(new FilterRule(editBox.getValue(), filterType, chatId));
                minecraft.setScreen(new ConfigScreen(parent));
            })
            .pos(x, y)
            .size(ADD_BUTTON_WIDTH, Button.DEFAULT_HEIGHT)
            .build());
        addButton.active = false;

        addRenderableWidget(Button
            .builder(Component.literal("Back"), button -> minecraft.setScreen(parent))
            .pos(PADDING, y)
            .size(50, Button.DEFAULT_HEIGHT)
            .build());
    }

    private Component getFilterTypeText(final FilterType filterType) {
        return Component.translatable("secondchat.config.filter." + filterType.name().toLowerCase()).withStyle(ChatFormatting.GOLD);
    }

    private Component getChatIdText(final int chatId) {
        return Component.translatable("secondchat.config.chatid", chatId).withStyle(ChatFormatting.AQUA);
    }

    @Override
    public void tick() {
        super.tick();
        if (addButton == null) {
            return;
        }

        addButton.active = !editBox.getValue().isEmpty();
        if (!addButton.active) {
            return;
        }

        SecondChat.instance().rules().stream()
            .filter(rule -> rule.value().equals(editBox.getValue()) && rule.type() == filterType && rule.chatId() == chatId)
            .findAny()
            .ifPresentOrElse(filterRule -> this.alreadyAdded = filterRule, () -> this.alreadyAdded = null);
        addButton.active = alreadyAdded == null;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        final Matrix3x2fStack pose = guiGraphics.pose();
        pose.pushMatrix();
        pose.scale(2.0F, 2.0F);
        guiGraphics.drawString(font, title, this.width / 4 - font.width(title) / 2, 5, -1, true);
        pose.popMatrix();
    }

    public class SlotList extends ObjectSelectionList<ListEntry> {

        public SlotList(Minecraft minecraft, int width, int height, int top, int bottom, int entryHeight) {
            super(minecraft, width, height - top - bottom, top, entryHeight);

            SecondChat.instance().rules().forEach(rule -> addEntry(new ListEntry(rule)));
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 150;
        }

        @Override
        protected void renderSelection(final GuiGraphics guiGraphics, final ListEntry entry, final int i) {
            // Remove selection box
        }
    }

    public class ListEntry extends ObjectSelectionList.Entry<ListEntry> {
        public static final int INNER_PADDING = 2;

        private final FilterRule rule;
        private EditBox xInput;
        private EditBox yInput;
        private Button setButton;
        private Button resetButton;

        public ListEntry(FilterRule rule) {
            this.rule = rule;
        }

        @Override
        public @NotNull Component getNarration() {
            return getFilterTypeText(rule.type());
        }

        @Override
        public boolean mouseClicked(final MouseButtonEvent mouseButtonEvent, final boolean bl) {
            // Check if click is on the delete area (left side) vs position controls (right side)
            int mouseX = mouseButtonEvent.mouseX();
            int contentX = getContentX();
            int contentWidth = getContentWidth();
            
            // If click is on the left 60% of the entry, delete it
            if (mouseX < contentX + contentWidth * 0.6) {
                SecondChat.instance().remove(rule);
                minecraft.setScreen(new ConfigScreen(parent));
                return true;
            }
            
            return super.mouseClicked(mouseButtonEvent, bl);
        }

        @Override
        public void renderContent(final GuiGraphics guiGraphics, final int i, final int j, final boolean bl, final float f) {
            final Matrix3x2fStack pose = guiGraphics.pose();

            final int width = getContentWidth();
            final int height = getContentHeight();

            final int color = ConfigScreen.this.alreadyAdded == rule ? RED_TRANSPARENT : Integer.MIN_VALUE;
            pose.pushMatrix();
            pose.translate(getContentX(), getContentY());
            guiGraphics.fill(0, 0, width - INNER_PADDING * 2, height, color);

            // Left side: Filter value and type
            final MutableComponent base = Component.literal(rule.value());
            guiGraphics.drawString(font, bl ? base.withStyle(ChatFormatting.ITALIC, ChatFormatting.RED) : base, INNER_PADDING, INNER_PADDING, -1);

            final Component chatIdText = getChatIdText(rule.chatId());
            final Component narration = Component.literal("").append(getNarration()).append(" ").append(chatIdText);
            final Component styledNarration = narration.copy().withStyle(ChatFormatting.GOLD);
            guiGraphics.drawString(font, styledNarration, INNER_PADDING, INNER_PADDING + font.lineHeight + 2, -1);

            // Right side: Position controls
            int rightStart = width - 300;
            
            // Get current position
            ChatPosition currentPos = SecondChat.instance().getChatPosition(rule.chatId());
            int currentX = currentPos != null ? currentPos.x() : 0;
            int currentY = currentPos != null ? currentPos.y() : 0;

            // X label and input
            guiGraphics.drawString(font, "X:", rightStart, INNER_PADDING, ChatFormatting.AQUA.getColor());
            if (xInput == null) {
                xInput = new EditBox(font, rightStart + 15, INNER_PADDING - 2, 50, 16, Component.literal("X"));
                xInput.setValue(String.valueOf(currentX));
                xInput.setMaxLength(6);
            }
            xInput.setX(rightStart + 15);
            xInput.setY(INNER_PADDING - 2);
            xInput.render(guiGraphics, i, j, f);

            // Y label and input
            guiGraphics.drawString(font, "Y:", rightStart + 70, INNER_PADDING, ChatFormatting.AQUA.getColor());
            if (yInput == null) {
                yInput = new EditBox(font, rightStart + 85, INNER_PADDING - 2, 50, 16, Component.literal("Y"));
                yInput.setValue(String.valueOf(currentY));
                yInput.setMaxLength(6);
            }
            yInput.setX(rightStart + 85);
            yInput.setY(INNER_PADDING - 2);
            yInput.render(guiGraphics, i, j, f);

            // Set button
            if (setButton == null) {
                setButton = Button.builder(Component.literal("Set"), button -> {
                    try {
                        int x = Integer.parseInt(xInput.getValue());
                        int y = Integer.parseInt(yInput.getValue());
                        SecondChat.instance().setChatPosition(new ChatPosition(rule.chatId(), x, y));
                    } catch (NumberFormatException e) {
                        // Invalid input, ignore
                    }
                }).pos(rightStart + 140, INNER_PADDING - 2).size(50, 16).build();
            }
            setButton.setX(rightStart + 140);
            setButton.setY(INNER_PADDING - 2);
            setButton.render(guiGraphics, i, j, f);

            // Reset button
            if (resetButton == null) {
                resetButton = Button.builder(Component.literal("Reset"), button -> {
                    SecondChat.instance().removeChatPosition(rule.chatId());
                    xInput.setValue("0");
                    yInput.setValue("0");
                }).pos(rightStart + 195, INNER_PADDING - 2).size(55, 16).build();
            }
            resetButton.setX(rightStart + 195);
            resetButton.setY(INNER_PADDING - 2);
            resetButton.render(guiGraphics, i, j, f);

            pose.popMatrix();
        }
    }

}
