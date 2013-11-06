package com.greatmancode.deathmatch;

import java.util.HashMap;
import java.util.Map;

import me.ampayne2.ultimategames.UltimateGames;
import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.arenas.scoreboards.ArenaScoreboard;
import me.ampayne2.ultimategames.gson.Gson;
import me.ampayne2.ultimategames.webapi.WebHandler;

public class DeathmatchWebHandler implements WebHandler {

    private Arena arena;
    private UltimateGames ug;

    public DeathmatchWebHandler(UltimateGames ug, Arena arena) {
        this.arena = arena;
        this.ug = ug;
    }

    @Override
    public String sendResult() {
        Gson gson = new Gson();
        Map<String, Integer> map = new HashMap<String, Integer>();

        ArenaScoreboard scoreBoard = ug.getScoreboardManager().getArenaScoreboard(arena);
        if (scoreBoard != null) {
            if (scoreBoard.getName().equals("Kills")) {
                for (String playerName : arena.getPlayers()) {
                    map.put(playerName, scoreBoard.getScore(playerName));
                }
            }
        }

        return gson.toJson(map);
    }
}
