package id.naturalsmp.naturaldungeon.integration;

import id.naturalsmp.naturaldungeon.NaturalDungeon;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private final NaturalDungeon plugin;
    private Economy economy;
    private boolean enabled;

    public VaultHook(NaturalDungeon plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            enabled = false;
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            enabled = false;
            return;
        }
        economy = rsp.getProvider();
        enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getBalance(Player player) {
        return enabled ? economy.getBalance(player) : 0;
    }

    public boolean withdraw(Player player, double amount) {
        return enabled && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        return enabled && economy.depositPlayer(player, amount).transactionSuccess();
    }

    public boolean has(Player player, double amount) {
        return enabled && economy.has(player, amount);
    }
}
