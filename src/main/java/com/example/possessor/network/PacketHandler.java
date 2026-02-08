package com.example.possessor.network;

import com.example.possessor.PossessorMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PossessorMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++,
            PacketSyncVotes.class,
            PacketSyncVotes::toBytes,
            PacketSyncVotes::new,
            PacketSyncVotes::handle);
            
        INSTANCE.registerMessage(id++,
            PacketSyncData.class,
            PacketSyncData::toBytes,
            PacketSyncData::new,
            PacketSyncData::handle);

        INSTANCE.registerMessage(id++,
            PacketSelectPlayer.class,
            PacketSelectPlayer::toBytes,
            PacketSelectPlayer::new,
            PacketSelectPlayer::handle);
    }
}
