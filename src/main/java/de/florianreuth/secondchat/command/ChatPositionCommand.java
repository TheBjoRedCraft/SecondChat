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

package de.florianreuth.secondchat.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.florianreuth.secondchat.ChatPosition;
import de.florianreuth.secondchat.SecondChat;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public class ChatPositionCommand {
    
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommandManager.literal("chatpos")
                .then(ClientCommandManager.literal("set")
                    .then(ClientCommandManager.argument("chatId", IntegerArgumentType.integer(1))
                        .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                            .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
                                .executes(ChatPositionCommand::setPosition)
                            )
                        )
                    )
                )
                .then(ClientCommandManager.literal("reset")
                    .then(ClientCommandManager.argument("chatId", IntegerArgumentType.integer(1))
                        .executes(ChatPositionCommand::resetPosition)
                    )
                )
                .then(ClientCommandManager.literal("list")
                    .executes(ChatPositionCommand::listPositions)
                )
        );
    }
    
    private static int setPosition(CommandContext<FabricClientCommandSource> context) {
        int chatId = IntegerArgumentType.getInteger(context, "chatId");
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        
        ChatPosition position = new ChatPosition(chatId, x, y);
        SecondChat.instance().setChatPosition(position);
        
        context.getSource().sendFeedback(Component.literal(
            String.format("Set position for Chat #%d to x=%d, y=%d", chatId, x, y)
        ));
        context.getSource().sendFeedback(Component.literal(
            "Note: Negative x values are from the right edge of the screen"
        ));
        
        return 1;
    }
    
    private static int resetPosition(CommandContext<FabricClientCommandSource> context) {
        int chatId = IntegerArgumentType.getInteger(context, "chatId");
        
        SecondChat.instance().setChatPosition(new ChatPosition(chatId, 0, 0));
        context.getSource().sendFeedback(Component.literal(
            String.format("Reset position for Chat #%d to default", chatId)
        ));
        
        return 1;
    }
    
    private static int listPositions(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.literal("Chat Positions:"));
        
        int maxChatId = SecondChat.instance().getMaxChatId();
        if (maxChatId < 1) {
            context.getSource().sendFeedback(Component.literal("No active chats configured"));
            return 1;
        }
        
        for (int i = 1; i <= maxChatId; i++) {
            ChatPosition pos = SecondChat.instance().getChatPosition(i);
            if (pos != null) {
                context.getSource().sendFeedback(Component.literal(
                    String.format("  Chat #%d: x=%d, y=%d", i, pos.x(), pos.y())
                ));
            } else {
                context.getSource().sendFeedback(Component.literal(
                    String.format("  Chat #%d: default position (stacked)", i)
                ));
            }
        }
        
        context.getSource().sendFeedback(Component.literal(""));
        context.getSource().sendFeedback(Component.literal("Usage: /chatpos set <chatId> <x> <y>"));
        context.getSource().sendFeedback(Component.literal("       /chatpos reset <chatId>"));
        context.getSource().sendFeedback(Component.literal("Negative x = from right edge"));
        
        return 1;
    }
}
