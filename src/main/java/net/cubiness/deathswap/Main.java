package net.cubiness.deathswap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.cubiness.colachampionship.ColaCore;
import net.cubiness.colachampionship.minigame.Minigame;
import net.cubiness.colachampionship.minigame.MinigameAPI;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

  public DeathSwap minigame;

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
    MinigameAPI api = ((ColaCore) Bukkit.getPluginManager().getPlugin("ColaCore")).getAPI();
    minigame = new DeathSwap(this, api);
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent e) {
    minigame.onPlayerDeath(e.getEntity());
  }
}
