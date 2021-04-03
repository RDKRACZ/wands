package net.nicguzzo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterials;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.nicguzzo.common.WandServerSide;
import net.nicguzzo.common.WandsConfig;

public class WandsMod implements ModInitializer {

	public static WandsConfig config=null;
	public static ICompatModImpl compat=new ICompatModImpl();
	public static final Identifier WAND_PACKET_ID      = new Identifier("wands", "wand");
	public static final Identifier WANDXP_PACKET_ID    = new Identifier("wands", "wandxp");
	public static final Identifier WANDCONF_PACKET_ID  = new Identifier("wands", "wandconf");
	public static final Identifier WAND_UNDO_PACKET_ID = new Identifier("wands", "wandundo");
	public static final Identifier WAND_REDO_PACKET_ID = new Identifier("wands", "wandredo");
	
	public static WandItemFabric NETHERITE_WAND_ITEM = null;
	public static WandItemFabric DIAMOND_WAND_ITEM   = null;
	public static WandItemFabric IRON_WAND_ITEM      = null;
	public static WandItemFabric STONE_WAND_ITEM     = null;
	
	public static final PaletteItem PALETTE_ITEM = new PaletteItem();
	
	@Override
	public void onInitialize() {
		config=WandsConfig.load_config(FabricLoader.getInstance().getConfigDir());
		NETHERITE_WAND_ITEM = new WandItemFabric(ToolMaterials.NETHERITE, WandsMod.config.netherite_wand_limit, 2031,  true, true);
		DIAMOND_WAND_ITEM   = new WandItemFabric(ToolMaterials.DIAMOND  , WandsMod.config.diamond_wand_limit  , 1561,  true, false);
		IRON_WAND_ITEM      = new WandItemFabric(ToolMaterials.IRON     , WandsMod.config.iron_wand_limit     ,  250,  true, false);
		STONE_WAND_ITEM     = new WandItemFabric(ToolMaterials.STONE    , WandsMod.config.stone_wand_limit    ,  131, false, false);
		Registry.register(Registry.ITEM, new Identifier("wands", "netherite_wand"), NETHERITE_WAND_ITEM);
		Registry.register(Registry.ITEM, new Identifier("wands", "diamond_wand"), DIAMOND_WAND_ITEM);
		Registry.register(Registry.ITEM, new Identifier("wands", "iron_wand"), IRON_WAND_ITEM);
		Registry.register(Registry.ITEM, new Identifier("wands", "stone_wand"), STONE_WAND_ITEM);
		Registry.register(Registry.ITEM, new Identifier("wands", "palette"), PALETTE_ITEM);

		//ServerPlayNetworking.send((ServerPlayerEntity) user, WandsMod.WANDCONF_PACKET_ID, passedData);

		ServerPlayNetworking.registerGlobalReceiver(WAND_PACKET_ID, (server, player, handler, buf, responseSender) -> {
			final BlockPos state_pos = buf.readBlockPos();
			final BlockPos pos1 = buf.readBlockPos();
			final BlockPos pos2 = buf.readBlockPos();
			final int p = buf.readInt();			
			final int mode = buf.readInt();
			final int plane = buf.readInt();
			server.execute(() -> {
				if (World.isValid(state_pos) && World.isValid(pos1) && World.isValid(pos2)) {
					ItemStack stack=player.getMainHandStack();
					if (stack.getItem() instanceof WandItemFabric) {
						WandServerSide.placeBlock(player,state_pos,pos1,pos2,p,player.getAbilities().creativeMode,player.experienceProgress,stack,mode,plane);
					}
				}
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(WAND_UNDO_PACKET_ID, (server, player, handler, buf, responseSender) -> {
			final int n = buf.readInt();
			server.execute(() -> {				
				WandServerSide.undo(player,n);
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(WAND_REDO_PACKET_ID, (server, player, handler, buf, responseSender) -> {
			final int n = buf.readInt();
			server.execute(() -> {				
				WandServerSide.redo(player,n);				
			});
		});
		/*
		ServerSidePacketRegistry.INSTANCE.register(WANDCONF_PACKET_ID, (packetContext, attachedData) -> {			
			packetContext.getTaskQueue().execute(() -> {
				final PlayerEntity player = packetContext.getPlayer();
				final PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
				passedData.writeFloat(WandsMod.config.blocks_per_xp);
				System.out.println("sending blocks_per_xp : "+WandsMod.config.blocks_per_xp);
				ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, WandsMod.WANDCONF_PACKET_ID,passedData);				
			});
		});
		ServerSidePacketRegistry.INSTANCE.register(WAND_PACKET_ID, (packetContext, attachedData) -> {
			final BlockPos state_pos = attachedData.readBlockPos();
			final BlockPos pos1 = attachedData.readBlockPos();
			final BlockPos pos2 = attachedData.readBlockPos();
			final int p = attachedData.readInt();
			
			packetContext.getTaskQueue().execute(() -> {				
				if (World.isInBuildLimit(state_pos) && World.isInBuildLimit(pos1) && World.isInBuildLimit(pos2)) {
					final PlayerEntity player = packetContext.getPlayer();
					ItemStack stack=player.getMainHandStack();					
					if (stack.getItem() instanceof WandItemFabric) {
						WandServerSide.placeBlock(player,state_pos,pos1,pos2,p,player.abilities.creativeMode,player.experienceProgress,stack);
					}
				}
			});
		});*/
	/*	ServerSidePacketRegistry.INSTANCE.register(WAND_UNDO_PACKET_ID, (packetContext, attachedData) -> {
			final BlockPos pos0 = attachedData.readBlockPos();
			packetContext.getTaskQueue().execute(() -> {
				if (World.isInBuildLimit(pos0)) {
					final PlayerEntity player = packetContext.getPlayer();
					final BlockState state = player.world.getBlockState(pos0);
					if(!state.isAir()){
						if (player.abilities.creativeMode) {
							player.world.setBlockState(pos0, Blocks.VOID_AIR.getDefaultState());
						}
					}
				}
			});
		});*/
	}
		
}