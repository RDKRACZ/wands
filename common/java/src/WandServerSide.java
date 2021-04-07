package net.nicguzzo.common;

import java.util.Vector;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.nicguzzo.WandsMod;
import java.util.HashMap;

public class WandServerSide {
	//private static final Logger LOGGER = LogManager.getLogger();
	private static final int MAX_UNDO = 2048;

	private static HashMap<PlayerEntity, CircularBuffer> player_undo = new HashMap<PlayerEntity, CircularBuffer>();

	public int slt;
	public PlayerEntity player;
	public Vector<Integer> slots;
	public BlockState state;
	public BlockPos pos0;
	public BlockPos pos1;
	public WandItem.PaletteMode palatte_mode;
	public boolean isCreative;
	public float experienceProgress;
	public ItemStack wand_stack;
	public ItemStack shulker = null;
	
	public int mode;
	public int plane;
	public World world;
	float BLOCKS_PER_XP = WandsMod.config.blocks_per_xp;

	public WandServerSide(World world,PlayerEntity player, BlockPos pos_state, BlockPos pos0, BlockPos pos1, int palatte_mode,
			boolean isCreative, float experienceProgress, ItemStack wand_stack, int mode, int plane) {
		this.slots = new Vector<Integer>();;
		this.player = player;
		this.world = world;
		this.pos0 = pos0;
		this.pos1 = pos1;
		this.palatte_mode = WandItem.PaletteMode.values()[palatte_mode];
		this.state = world.getBlockState(pos_state);
		this.isCreative = isCreative;
		this.experienceProgress = experienceProgress;
		this.wand_stack = wand_stack;
		this.mode = mode;
		this.plane = plane;

		ItemStack offhand = WandsMod.compat.get_player_offhand_stack(player.getInventory());
		if (WandsMod.compat.is_shulker(player, offhand)) {
			shulker = offhand;
		}
		
		if (this.mode<2 || this.palatte_mode == WandItem.PaletteMode.SAME) {
			if (shulker != null) {
				int sl=WandsMod.compat.in_shulker_slot(player, shulker, state);
				if (sl != -1) {					
					slots.add(sl);
				}
			}
		} else if (this.palatte_mode == WandItem.PaletteMode.RANDOM
				|| this.palatte_mode == WandItem.PaletteMode.ROUND_ROBIN) {
			if (shulker != null) {
				slots=WandsMod.compat.shulker_slots(player, shulker);
			} else {
				for (int i = 0; i < WandsMod.compat.get_main_inventory_size(player.getInventory()); ++i) {
					ItemStack stack2 = WandsMod.compat.get_player_main_stack(player.getInventory(), i);
					Block blk = WandsMod.compat.block_from_item(stack2.getItem());
					if (blk != null && blk != Blocks.AIR && !(blk instanceof ShulkerBoxBlock) && !WandsMod.compat.has_tag(stack2)) {
						slots.add(i);
					}
				}
			}
		}
	}

	public void placeBlock() {

		if (pos0.equals(pos1)) {
			place(pos0);
		} else {
			int xs, ys, zs, xe, ye, ze;
			if (pos0.getX() >= pos1.getX()) {
				xs = pos1.getX() + 1;
				xe = pos0.getX() + 1;
			} else {
				xs = pos0.getX();
				xe = pos1.getX();
			}
			if (pos0.getY() >= pos1.getY()) {
				ys = pos1.getY() + 1;
				ye = pos0.getY() + 1;
			} else {
				ys = pos0.getY();
				ye = pos1.getY();
			}
			if (pos0.getZ() >= pos1.getZ()) {
				zs = pos1.getZ() + 1;
				ze = pos0.getZ() + 1;
			} else {
				zs = pos0.getZ();
				ze = pos1.getZ();
			}
			if (mode == 4) {// line
				// System.out.println("Line! pos0 "+pos0+" pos1 "+pos1);
				line();
			} else if (mode == 5) {// circle
				// System.out.println("circle! pos0 "+pos0+" pos1 "+pos1);
				circle();
			} else {// box
				for (int z = zs; z < ze; z++) {
					for (int y = ys; y < ye; y++) {
						for (int x = xs; x < xe; x++) {
							if (place(new BlockPos(x, y, z))) {
								// count++;
							}
						}
					}
				}
			}
		}
		slots = null;
	}

	int nextSlot() {
		if (palatte_mode == WandItem.PaletteMode.RANDOM) {
			slt = WandsMod.compat.get_next_int_random(player, slots.size());
		} else if (palatte_mode == WandItem.PaletteMode.ROUND_ROBIN) {
			slt = (slt + 1) % slots.size();
		}
		return slt;
	}

	static public void undo(PlayerEntity player, int n) {
		CircularBuffer u = player_undo.get(player);
		if (u != null) {
			for (int i = 0; i < n && i < u.size(); i++) {
				BlockPos p = u.pop();
				if (p != null) {
					WandsMod.compat.setBlockState(WandsMod.compat.world(player),p, WandsMod.compat.getDefaultBlockState( Blocks.AIR));
				}
			}
			// u.print();
		}
	}

	static public void redo(PlayerEntity player, int n) {
		CircularBuffer u = player_undo.get(player);
		if (u != null) {
			// System.out.println("redo");
			for (int i = 0; i < n && u.can_go_forward(); i++) {
				// System.out.println("redo "+i);
				u.forward();
				CircularBuffer.P p = u.peek();
				if (p != null && p.pos != null && p.state != null) {
					WandsMod.compat.setBlockState(WandsMod.compat.world(player),p.pos, p.state);
				}
			}
			// u.print();
		}
	}

	private boolean place(BlockPos pos) {

		boolean placed = false;
		ItemStack item_stack = null;
		Block blk = null;
		if (slots.size() > 0) {
			if (mode != 0) {
				this.nextSlot();
			}
			if (shulker!=null) {
				item_stack =WandsMod.compat.item_from_shulker(shulker,slots.get(slt));
			} else {
				item_stack = WandsMod.compat.get_player_main_stack(player.getInventory(), slots.get(slt));
			}
		}
		if (item_stack == null){
			item_stack = new ItemStack(state.getBlock());
		}
		blk = WandsMod.compat.block_from_item(item_stack.getItem());
		if (blk != null) {
			if ((blk instanceof SnowBlock)) {
				// disabled for now
			} else {
				if (palatte_mode == WandItem.PaletteMode.RANDOM)
					state = WandsMod.compat.random_rotate(WandsMod.compat.getDefaultBlockState(blk), world);
				else
					state = WandsMod.compat.getDefaultBlockState(blk);
			}
		}
		// LOGGER.info("state " + state);

		Block block = state.getBlock();
		BlockState state2 = world.getBlockState(pos);
		int d = 1;
		WandItem wand = WandsMod.compat.get_player_wand(player);
		if (WandsBaseRenderer.can_place(state2, wand, world, pos)) {
			int slot = -1;

			if ((block instanceof PaneBlock) || (block instanceof FenceBlock)) {
				state = WandsMod.compat.getDefaultBlockState(state.getBlock());
			} else if (block instanceof SlabBlock) {
				if (WandsMod.compat.is_double_slab(state)) {
					d = 2;// should consume 2 if its a double slab
				}
			}
			if (palatte_mode == WandItem.PaletteMode.RANDOM && (block instanceof SnowBlock)) {
				d = WandsMod.compat.get_next_int_random(player, 7) + 1;
				
				state = WandsMod.compat.with_snow_layers(block, d);
			}

			if (isCreative) {
				
				if (player_undo.get(player) == null) {
					player_undo.put(player, new CircularBuffer(MAX_UNDO));
				}
				CircularBuffer u = player_undo.get(player);
				u.put(pos, state);
				// u.print();				
				WandsMod.compat.setBlockState(world,pos,state);
			} else {
				float xp = WandItem.calc_xp(player.experienceLevel, experienceProgress);
				float dec = 0.0f;
				// System.out.println("BLOCKS_PER_XP "+BLOCKS_PER_XP);
				// LOGGER.info("BLOCKS_PER_XP "+BLOCKS_PER_XP);
				if (BLOCKS_PER_XP != 0) {
					dec = (1.0f / BLOCKS_PER_XP);
				}
				if (BLOCKS_PER_XP == 0 || (xp - dec) > 0) {

					//ItemStack item_stack = new ItemStack(state.getBlock());
					if (shulker != null && slots.size() > 0) {						
						placed = WandsMod.compat.setBlockState(world,pos,state);
						if (placed) {
							WandsMod.compat.remove_item_from_shulker(shulker,  slots.get(slt), d);
						}						
					} else {
						ItemStack off_hand_stack = WandsMod.compat.get_player_offhand_stack(player.getInventory());
						if (!off_hand_stack.isEmpty() && item_stack.getItem() == off_hand_stack.getItem()
								&& off_hand_stack.getCount() >= d) {
							placed = WandsMod.compat.setBlockState(world,pos,state);
							if (placed)
								WandsMod.compat.player_offhand_stack_dec(player.getInventory(), d);
						} else {
							for (int i = 0; i < WandsMod.compat.get_main_inventory_size(player.getInventory()); ++i) {
								ItemStack stack2 = WandsMod.compat.get_player_main_stack(player.getInventory(), i);
								if (stack2!=null && item_stack!=null 
									&& !stack2.isEmpty() && item_stack.getItem() == stack2.getItem()
										&& stack2.getCount() >= d) {
									slot = i;
								}
							}
							if (slot > -1) {
								placed = WandsMod.compat.setBlockState(world,pos,state);
								if (placed) {
									WandsMod.compat.player_stack_dec(player.getInventory(), slot, d);
								}
							}
						}
					}
					// LOGGER.info("placed"+placed);
					if (placed) {
						WandsMod.compat.inc_wand_damage(player, wand_stack, 1);

						if (BLOCKS_PER_XP != 0) {
							float diff = WandItem.calc_xp_to_next_level(player.experienceLevel);
							float prog = experienceProgress;
							if (diff > 0 && BLOCKS_PER_XP != 0.0f) {
								float a = (1.0f / diff) / BLOCKS_PER_XP;
								if (prog - a > 0) {
									prog = prog - a;
								} else {
									if (prog > 0.0f) {
										// TODO: dirty solution....
										prog = 1.0f + (a - prog);
									} else {
										prog = 1.0f;
									}
									if (player.experienceLevel > 0) {
										player.experienceLevel--;
										diff = WandItem.calc_xp_to_next_level(player.experienceLevel);
										a = (1.0f / diff) / BLOCKS_PER_XP;
										if (prog - a > 0) {
											prog = prog - a;
										}
									}
									WandsMod.compat.send_xp_to_player(player);									
								}
							}
						}
					}
				}
			}
		}
		return placed;
	}

	// bresenham 3d from
	// https://www.geeksforgeeks.org/bresenhams-algorithm-for-3-d-line-drawing/
	private void line() {

		int x1 = pos0.getX();
		int y1 = pos0.getY();
		int z1 = pos0.getZ();
		int x2 = pos1.getX();
		int y2 = pos1.getY();
		int z2 = pos1.getZ();
		int dx, dy, dz, xs, ys, zs, p1, p2;
		dx = Math.abs(x2 - x1);
		dy = Math.abs(y2 - y1);
		dz = Math.abs(z2 - z1);
		if (x2 > x1) {
			xs = 1;
		} else {
			xs = -1;
		}
		if (y2 > y1) {
			ys = 1;
		} else {
			ys = -1;
		}
		if (z2 > z1) {
			zs = 1;
		} else {
			zs = -1;
		}

		// X
		if (dx >= dy && dx >= dz) {
			p1 = 2 * dy - dx;
			p2 = 2 * dz - dx;
			while (x1 != x2) {
				x1 += xs;
				if (p1 >= 0) {
					y1 += ys;
					p1 -= 2 * dx;
				}
				if (p2 >= 0) {
					z1 += zs;
					p2 -= 2 * dx;
				}
				p1 += 2 * dy;
				p2 += 2 * dz;

				place(new BlockPos(x1, y1, z1));
			}
		} else if (dy >= dx && dy >= dz) {
			p1 = 2 * dx - dy;
			p2 = 2 * dz - dy;
			while (y1 != y2) {
				y1 += ys;
				if (p1 >= 0) {
					x1 += xs;
					p1 -= 2 * dy;
				}
				if (p2 >= 0) {
					z1 += zs;
					p2 -= 2 * dy;
				}
				p1 += 2 * dx;
				p2 += 2 * dz;
				place(new BlockPos(x1, y1, z1));
			}
		} else {
			p1 = 2 * dy - dz;
			p2 = 2 * dx - dz;
			while (z1 != z2) {
				z1 += zs;
				if (p1 >= 0) {
					y1 += ys;
					p1 -= 2 * dz;
				}
				if (p2 >= 0) {
					x1 += xs;
					p2 -= 2 * dz;
				}
				p1 += 2 * dy;
				p2 += 2 * dx;
				place(new BlockPos(x1, y1, z1));
			}
		}
	}

	private void drawCircle(int xc, int yc, int zc, int x, int y, int z) {
		switch (plane) {
		case 0:
			place(new BlockPos(xc + x, yc, zc + z));
			place(new BlockPos(xc - x, yc, zc + z));
			place(new BlockPos(xc + x, yc, zc - z));
			place(new BlockPos(xc - x, yc, zc - z));
			place(new BlockPos(xc + z, yc, zc + x));
			place(new BlockPos(xc - z, yc, zc + x));
			place(new BlockPos(xc + z, yc, zc - x));
			place(new BlockPos(xc - z, yc, zc - x));

			break;
		case 1:
			place(new BlockPos(xc + x, yc + y, zc));
			place(new BlockPos(xc - x, yc + y, zc));
			place(new BlockPos(xc + x, yc - y, zc));
			place(new BlockPos(xc - x, yc - y, zc));
			place(new BlockPos(xc + y, yc + x, zc));
			place(new BlockPos(xc - y, yc + x, zc));
			place(new BlockPos(xc + y, yc - x, zc));
			place(new BlockPos(xc - y, yc - x, zc));
			break;
		case 2:
			place(new BlockPos(xc, yc - y, zc + z));
			place(new BlockPos(xc, yc + y, zc + z));
			place(new BlockPos(xc, yc + y, zc - z));
			place(new BlockPos(xc, yc - y, zc - z));
			place(new BlockPos(xc, yc + z, zc + y));
			place(new BlockPos(xc, yc - z, zc + y));
			place(new BlockPos(xc, yc + z, zc - y));
			place(new BlockPos(xc, yc - z, zc - y));
			break;
		}
	}

	void circle() {
		int r = 1;
		int xc = pos0.getX();
		int yc = pos0.getY();
		int zc = pos0.getZ();
		int px = pos1.getX() - pos0.getX();
		int py = pos1.getY() - pos0.getY();
		int pz = pos1.getZ() - pos0.getZ();
		r = (int) Math.sqrt(px * px + py * py + pz * pz);
		if (plane == 0) {// XZ;
			int x = 0, y = 0, z = r;
			int d = 3 - 2 * r;
			drawCircle(xc, yc, zc, x, y, z);
			while (z >= x) {
				x++;
				if (d > 0) {
					z--;
					d = d + 4 * (x - z) + 10;
				} else
					d = d + 4 * x + 6;
				drawCircle(xc, yc, zc, x, y, z);
			}
		} else if (plane == 1) {// XY;
			int x = 0, y = r, z = 0;
			int d = 3 - 2 * r;
			drawCircle(xc, yc, zc, x, y, z);
			while (y >= x) {
				x++;
				if (d > 0) {
					y--;
					d = d + 4 * (x - y) + 10;
				} else
					d = d + 4 * x + 6;
				drawCircle(xc, yc, zc, x, y, z);
			}
		} else if (plane == 2) {// YZ;
			int x = 0, y = 0, z = r;
			int d = 3 - 2 * r;
			drawCircle(xc, yc, zc, x, y, z);
			while (z >= y) {
				y++;
				if (d > 0) {
					z--;
					d = d + 4 * (y - z) + 10;
				} else
					d = d + 4 * y + 6;
				drawCircle(xc, yc, zc, x, y, z);
			}
		}
	}
}
