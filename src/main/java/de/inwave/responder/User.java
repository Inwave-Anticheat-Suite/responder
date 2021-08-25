package de.inwave.responder;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class User {

    private final Map<Short, Runnable> action = new ConcurrentHashMap<>();

    private short transactionID; //  ++this.transactionID = mc code; --this.transactionID = responder code

    public void enqueue(Player target, Runnable runnable) {
        this.transactionID--;
        action.put(this.transactionID, runnable);

        try {
            PacketContainer container = new PacketContainer(PacketType.Play.Server.TRANSACTION);
            container.getBooleans().write(0, false);
            container.getIntegers().write(0, 0);
            container.getShorts().write(0, this.transactionID);
            ProtocolLibrary.getProtocolManager().sendServerPacket(target, container);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            action.remove(this.transactionID);
        }

        if (this.transactionID == Short.MIN_VALUE) { // reset
            this.transactionID = 0;
        }
    }

    public Runnable dequeue(short transactionID) {
        return action.remove(transactionID);
    }

}
