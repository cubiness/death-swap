package net.cubiness.deathswap;

import net.cubiness.colachampionship.ColaCore;
import net.cubiness.colachampionship.minigame.Minigame;
import net.cubiness.colachampionship.minigame.MinigameAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener, CommandExecutor {

  @Override
  public void onEnable() {
    getServer().getPluginManager().registerEvents(this, this);
    MinigameAPI api = ((ColaCore) Bukkit.getPluginManager().getPlugin("ColaCore")).getAPI();
    Minigame minigame = new DeathSwap(this, api);
  }
}
