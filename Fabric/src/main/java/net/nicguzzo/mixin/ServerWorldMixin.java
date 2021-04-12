package net.nicguzzo.mixin;

import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.nicguzzo.common.WandServerSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerWorld.class)
public class ServerWorldMixin {
	//@Inject(at = @At("HEAD"), method = "removePlayer(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/entity/Entity/RemovalReason;)V")
	@Inject(at = @At("HEAD"), method = "removePlayer")
	public void removePlayer(ServerPlayerEntity player, RemovalReason reason,CallbackInfo info) 
	{
		//System.out.println("disconnect clean player undo history!!!");
		if (player != null) {
			System.out.println("removing undo from player: "+player.getName().asString());
			WandServerSide.player_undo.remove(player.getUuidAsString());
		}
	}

}