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

package de.florianreuth.secondchat;

/**
 * Represents the position of a chat window on the screen.
 * Coordinates are relative to the bottom-left corner for consistency with chat rendering.
 */
public record ChatPosition(int chatId, int x, int y) {
    
    /**
     * Creates a default position for a chat based on its ID.
     * Default positions stack chats from right to left at the bottom of the screen.
     * 
     * @param chatId The ID of the chat
     * @param chatWidth The width of the chat component
     * @return A default ChatPosition
     */
    public static ChatPosition defaultPosition(int chatId, int chatWidth) {
        // Default: stack from right, using cumulative width like before
        // X is negative, meaning from the right edge
        return new ChatPosition(chatId, -chatWidth * chatId, 0);
    }
}
