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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.florianreuth.secondchat.command.ChatPositionCommand;
import de.florianreuth.secondchat.filter.FilterRule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SecondChat implements ClientModInitializer {

    private /*final*/ static SecondChat INSTANCE;

    private final Logger logger = LogManager.getLogger("SecondChat");
    private final Path config = FabricLoader.getInstance().getConfigDir().resolve("secondchat.json");
    private final Path positionsConfig = FabricLoader.getInstance().getConfigDir().resolve("secondchat-positions.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private List<FilterRule> rules;
    private Map<Integer, ChatPosition> chatPositions = new HashMap<>();

    public static SecondChat instance() {
        return INSTANCE;
    }

    @Override
    public void onInitializeClient() {
        INSTANCE = this;

        if (Files.exists(config)) {
            try {
                final FilterRule[] rules = gson.fromJson(Files.readString(config), FilterRule[].class);
                this.rules = rules == null ? new ArrayList<>() : Arrays.stream(rules).collect(Collectors.toList());
            } catch (Exception e) {
                logger.error("Failed to read file: {}!", config.toString(), e);
            }
        } else {
            rules = new ArrayList<>(); // Needs to be modifiable
        }
        
        // Load chat positions
        if (Files.exists(positionsConfig)) {
            try {
                final ChatPosition[] positions = gson.fromJson(Files.readString(positionsConfig), ChatPosition[].class);
                if (positions != null) {
                    for (ChatPosition position : positions) {
                        chatPositions.put(position.chatId(), position);
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to read positions file: {}!", positionsConfig.toString(), e);
            }
        }
        
        // Register commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            ChatPositionCommand.register(dispatcher);
        });
    }

    public void save() {
        try {
            Files.write(config, gson.toJson(rules).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            logger.error("Failed to create file: {}!", config.toString(), e);
        }
    }

    public void add(final FilterRule rule) {
        rules.add(rule);
        save();
    }

    public void remove(final FilterRule rule) {
        rules.remove(rule);
        save();
    }

    public boolean matches(final String input) {
        return rules.stream().anyMatch(rule -> switch (rule.type()) {
            case EQUALS -> input.equals(rule.value());
            case EQUALS_IGNORE_CASE -> input.equalsIgnoreCase(rule.value());
            case STARTS_WITH -> input.startsWith(rule.value());
            case ENDS_WITH -> input.endsWith(rule.value());
            case CONTAINS -> input.contains(rule.value());
            case REGEX -> input.matches(rule.value());
        });
    }

    public int getChatId(final String input) {
        return rules.stream()
            .filter(rule -> switch (rule.type()) {
                case EQUALS -> input.equals(rule.value());
                case EQUALS_IGNORE_CASE -> input.equalsIgnoreCase(rule.value());
                case STARTS_WITH -> input.startsWith(rule.value());
                case ENDS_WITH -> input.endsWith(rule.value());
                case CONTAINS -> input.contains(rule.value());
                case REGEX -> input.matches(rule.value());
            })
            .findFirst()
            .map(FilterRule::chatId)
            .orElse(0);
    }

    public int getMaxChatId() {
        return rules.stream()
            .mapToInt(FilterRule::chatId)
            .max()
            .orElse(0);
    }

    public List<FilterRule> rules() {
        return rules;
    }
    
    /**
     * Gets the position for a chat, or returns null if no custom position is set.
     */
    public ChatPosition getChatPosition(int chatId) {
        return chatPositions.get(chatId);
    }
    
    /**
     * Sets the position for a chat and saves it to the config.
     */
    public void setChatPosition(ChatPosition position) {
        chatPositions.put(position.chatId(), position);
        savePositions();
    }
    
    /**
     * Saves chat positions to the config file.
     */
    private void savePositions() {
        try {
            final List<ChatPosition> positionsList = new ArrayList<>(chatPositions.values());
            Files.write(positionsConfig, gson.toJson(positionsList).getBytes(), 
                       StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            logger.error("Failed to save positions file: {}!", positionsConfig.toString(), e);
        }
    }

}
