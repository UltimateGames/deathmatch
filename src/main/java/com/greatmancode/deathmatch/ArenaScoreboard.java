package com.greatmancode.deathmatch;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class ArenaScoreboard {

	Scoreboard scoreboard;
	Objective objective;
	Team team;

	public ArenaScoreboard() {
		reset();
	}

	public void addPlayer(Player p) {
		team.addPlayer(p);
	}

	public void removePlayer(Player p) {
		objective.getScore(p).setScore(0);
		scoreboard.resetScores(p);
		team.removePlayer(p);
	}

	public String[] getHighestPlayer() {
		String[] out = new String[2];
		String name = "";
		int kills = 0;
		for (OfflinePlayer p : scoreboard.getPlayers()) {
			int theKills = objective.getScore(p).getScore();
			if (kills < theKills) {
				kills = theKills;
				name = p.getName();
			}
		}
		out[0] = name;
		out[1] = String.valueOf(kills);
		return out;
	}

	public void reset() {
		if (team != null) {
			team.unregister();
			team = null;
		}
		if (objective != null) {
			objective.unregister();
			objective = null;
		}
		scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		team = scoreboard.registerNewTeam("Arena Players");
		objective = scoreboard.registerNewObjective("Kills", "playerKillCount");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
	}
}
