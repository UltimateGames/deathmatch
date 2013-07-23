package com.greatmancode.deathmatch;

import org.bukkit.Bukkit;
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
		scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		objective = scoreboard.registerNewObjective("Kills", "playerKillCount");
		team = scoreboard.registerNewTeam("Arena Players");
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
	}

	public void addPlayer(Player p) {
		team.addPlayer(p);
	}

	public void removePlayer(Player p) {
		team.removePlayer(p);
	}

	public void reset() {
		team.unregister();
		team = scoreboard.registerNewTeam("Arena Players");
	}
}
