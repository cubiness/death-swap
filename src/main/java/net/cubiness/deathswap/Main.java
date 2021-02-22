package net.cubiness.deathswap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.cubiness.colachampionship.ColaCore;
import net.cubiness.colachampionship.minigame.MinigameManager;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

  public DeathSwap minigame;

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
    MinigameManager manager = ((ColaCore) Bukkit.getPluginManager().getPlugin("ColaCore")).getMinigames();
    minigame = new DeathSwap(this, manager);
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent e) {
    minigame.onPlayerDeath(e.getEntity());
  }
}
