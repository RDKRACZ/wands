package net.nicguzzo;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterials;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.nicguzzo.common.WandServerSide;
import net.nicguzzo.common.WandsConfig;

public class WandsMod implements ModInitializer {

	public static WandsConfig config = null;
	public static ICompatModImpl compat = new ICompatModImpl();
	public static final Identifier WAND_PACKET_ID = new Identifier("wands", "wand");
	public static final Identifier WANDXP_PACKET_ID = new Identifier("wands", "wandxp");
	public static final Identifier WANDCONF_PACKET_ID = new Identifier("wands", "wandconf");
	public static final Identifier WAND_UNDO_PACKET_ID = new Identifier("wands", "wandundo");
	public static final Identifier WAND_REDO_PACKET_ID = new Identifier("wands", "wandredo");

	public static WandItemFabric NETHERITE_WAND_ITEM = null;
	public static WandItemFabric DIAMOND_WAND_ITEM = null;
	public static WandItemFabric IRON_WAND_ITEM = null;
	public static WandItemFabric STONE_WAND_ITEM = null;

	public static final PaletteItem PALETTE_ITEM = new PaletteItem();

	@Override
	public void onInitialize() {
		config = WandsConfig.load_config(FabricLoader.getInstance().getConfigDir());
		NETHERITE_WAND_ITEM = new WandItemFabric(ToolMaterials.NETHERITE, WandsMod.config.netherite_wand_limit,
				WandsMod.config.netherite_wand_durability, true, true);
		DIAMOND_WAND_ITEM = new WandItemFabric(ToolMaterials.DIAMOND, WandsMod.config.diamond_wand_limit,
				WandsMod.config.diamond_wand_durability, true, false);
		IRON_WAND_ITEM = new WandItemFabric(ToolMaterials.IRON, WandsMod.config.iron_wand_limit,
				WandsMod.config.iron_wand_durability, true, false);
		STONE_WAND_ITEM = new WandItemFabric(ToolMaterials.STONE, WandsMod.config.stone_wand_limit,
				WandsMod.config.stone_wand_durability, false, false);
		Registry.register(Registry.ITEM, new Identifier("wands", "netherite_wand"), NETHERITE_WAND_ITEM);
		Registry.register(Registry.ITEM, new Identifier("wands", "diamond_wand"), DIAMOND_WAND_ITEM);
		Registry.register(Registry.ITEM, new Identifier("wands", "iron_wand"), IRON_WAND_ITEM);
		Registry.register(Registry.ITEM, new Identifier("wands", "stone_wand"), STONE_WAND_ITEM);
		Registry.register(Registry.ITEM, new Identifier("wands", "palette"), PALETTE_ITEM);

		ServerPlayNetworking.registerGlobalReceiver(WAND_PACKET_ID, (server, player, handler, buf, responseSender) -> {
			final BlockPos state_pos = buf.readBlockPos();
			final BlockPos pos1 = buf.readBlockPos();
			final BlockPos pos2 = buf.readBlockPos();
			final int p = buf.readInt();
			final int mode = buf.readInt();
			final int plane = buf.readInt();
			server.execute(() -> {
				if (World.isValid(state_pos) && World.isValid(pos1) && World.isValid(pos2)) {
					ItemStack stack = player.getMainHandStack();
					if (stack.getItem() instanceof WandItemFabric) {
						WandServerSide srv = new WandServerSide(player.world, player, state_pos, pos1, pos2, p,
								player.getAbilities().creativeMode, player.experienceProgress, stack, mode, plane);
						srv.placeBlock();
						srv = null;
					}
				}
			});
		});
		ServerPlayNetworking.registerGlobalReceiver(WAND_UNDO_PACKET_ID,
				(server, player, handler, buf, responseSender) -> {
					final int n = buf.readInt();
					server.execute(() -> {
						WandServerSide.undo(player, n);
					});
				});
		ServerPlayNetworking.registerGlobalReceiver(WAND_REDO_PACKET_ID,
				(server, player, handler, buf, responseSender) -> {
					final int n = buf.readInt();
					server.execute(() -> {
						WandServerSide.redo(player, n);
					});
				});
		ServerPlayNetworking.registerGlobalReceiver(WANDCONF_PACKET_ID,
				(server, player, handler, buf, responseSender) -> {

					server.execute(() -> {
						PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
						passedData.writeFloat(WandsMod.config.blocks_per_xp);
						ServerPlayNetworking.send(player, WANDCONF_PACKET_ID, passedData);
					});
				});
	}

}