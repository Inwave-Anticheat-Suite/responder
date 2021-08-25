package de.inwave.responder;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Responder extends JavaPlugin implements Listener {

    public static final Function<Player, User> MAPPING_FUNCTION = player -> new User();
    private final Map<Player, User> userMap = new ConcurrentHashMap<>();
    private PacketHandler packetHandler;

    public static Responder getInstance() {
        return JavaPlugin.getPlugin(Responder.class);
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        ProtocolLibrary.getProtocolManager().addPacketListener(this.packetHandler = new PacketHandler());
    }

    @Override
    public void onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListener(this.packetHandler);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.userMap.remove(event.getPlayer());
    }

    public void queue(Player player, Runnable runnable) {
        this.userMap.computeIfAbsent(player, MAPPING_FUNCTION).enqueue(player, runnable);
    }

    class PacketHandler extends PacketAdapter {

        public PacketHandler() {
            super(
                    Responder.this,
                    ListenerPriority.MONITOR,
                    PacketType.Play.Client.TRANSACTION
            );
        }

        @Override
        public void onPacketReceiving(PacketEvent event) {
            Runnable runnable = Responder.this.userMap.computeIfAbsent(event.getPlayer(), MAPPING_FUNCTION).dequeue(event.getPacket().getShorts().readSafely(0));
            if (runnable != null)
                runnable.run();
        }

    }

}
