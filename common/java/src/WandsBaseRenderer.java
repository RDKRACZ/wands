package net.nicguzzo.common;

import net.minecraft.world.World;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PaneBlock;

import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.nicguzzo.WandsMod;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.GameRenderer;

public class WandsBaseRenderer {

	
	public static final float p_o = -0.001f;// preview offset
	public static float BLOCKS_PER_XP = 0;
	private static long t0 = 0;
	private static long t1 = 0;
	private static boolean prnt = false;

	public static void render(World world, PlayerEntity player, BlockPos pos, BlockState block_state, double camX,
			double camY, double camZ, int lim, boolean isCreative, boolean isDoubleSlab, boolean isSlabTop,
			float experienceProgress, MyDir side, boolean isFullCube, double hit_x, double hit_y, double hit_z) {
		WandItem wand = WandsMod.compat.get_player_wand(player);		
		if (wand == null) {
			return;
		}
		prnt = false;
		t1 = System.currentTimeMillis();
		if (t1 - t0 > 1000) {
			t0 = System.currentTimeMillis();
			prnt = true;			
		}

		Block block = block_state.getBlock();

		int d = 1;
		boolean is_slab = false;
		SlabBlock slab = null;
		if (block instanceof SlabBlock) {
			is_slab = true;
			slab = (SlabBlock) block;
			if (isDoubleSlab) {
				d = 2;
			}
		}
		ItemStack item_stack = null;

		int it_count = 0;
		boolean line_circle=(WandItem.getMode() == 4 || WandItem.getMode() == 5);

		if (line_circle && WandItem.fill1_state != null) {
			item_stack = new ItemStack(WandItem.fill1_state.getBlock());
		} else {
			item_stack = new ItemStack(block);
		}

		ItemStack offhand = WandsMod.compat.get_player_offhand_stack(player);

		boolean destroy =WandsMod.compat.can_destroy(block_state, offhand, isCreative);
		
		if (prnt) {
			//System.out.println("destroy: "+destroy + " offhand "+offhand+" block_state: "+block_state+" block: "+block);
			//WandsMod.compat.send_message_to_player("destroy");
		}

		it_count = WandsMod.compat.in_inventory(player,item_stack);
		
		int in_shulker=0;

		if(!destroy){
			in_shulker =WandsMod.compat.in_shulker(player, item_stack);
		}
		
		boolean mm = WandItem.getPaletteMode()!=WandItem.PaletteMode.SAME && (WandItem.getMode() == 2 || WandItem.getMode() == 4|| WandItem.getMode() == 5 );

		if (!destroy && !mm && !isCreative && it_count < d && in_shulker<d && WandItem.fill_pos1 == null) {
			if (prnt) {
				WandsMod.compat.send_message_to_player("no blocks");
			}
		}
		boolean notags=WandsMod.compat.has_tag(item_stack);
		if (it_count >= d || isCreative || mm || in_shulker >= d||WandItem.fill_pos1 != null || notags ||destroy) {
			boolean is_pane = false;
			boolean is_fence = false;
			boolean is_fence_gate = false;
			boolean is_stairs = false;
			boolean is_leaves = false;
			if (!is_slab) {
				if (block instanceof PaneBlock) {
					is_pane = true;
				} else if (block instanceof FenceBlock) {
					is_fence = true;
				} else if (block instanceof FenceGateBlock) {
					is_fence_gate = true;
				} else if (block instanceof StairsBlock) {
					is_stairs = true;
				} else if (block instanceof LeavesBlock) {
					is_leaves = true;
				}
			}
			float xp = 0.0f;
			int max_xp_blocks = 0;
			// if (prnt) {
			// System.out.println("BLOCKS_PER_XP :" + BLOCKS_PER_XP);
			// }
			if (!isCreative && BLOCKS_PER_XP > 0) {
				xp = WandItem.calc_xp(player.experienceLevel, experienceProgress);
				max_xp_blocks = (int) (xp * BLOCKS_PER_XP);
				if (max_xp_blocks < it_count) {
					if (prnt) {
						WandsMod.compat.send_message_to_player("not enough xp");
					}
				}
			}
			
			boolean allowed=false;
			boolean denied=false;
			
			if(WandsConfig.allowed.contains(block)){
				allowed=true;
			}
			if(WandsConfig.denied.contains(block)){
				denied=true;
			}


			if (!denied && (max_xp_blocks > 0 || BLOCKS_PER_XP == 0 || isCreative)
					&& (isFullCube || is_slab || is_pane || is_fence || is_fence_gate || is_stairs||mm || allowed ||destroy)) {
				float h = 1.0f;
				float y0 = 0.0f;
				if (slab != null) {
					if (!isSlabTop) {
						h = 0.5f;
					} else {
						y0 = 0.5f;
						h = 0.5f;
					}
					//System.out.println("shulker "+in_shulker);
				}
				MinecraftClient client=MinecraftClient.getInstance();
				Camera camera = client.gameRenderer.getCamera();
				if (camera.isReady()) 
				{			
					Vec3d c = camera.getPos().negate();
					//Box box = (new Box(pos1, pos2)).offset(vec3d);
					
					Tessellator tessellator = Tessellator.getInstance();
					BufferBuilder bufferBuilder = tessellator.getBuffer();
					RenderSystem.setShader(GameRenderer::getPositionColorShader);
					bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
					//RenderSystem.pushMatrix();
					//RenderSystem.translated(-camX, -camY, -camZ);
					// RenderSystem.enableDepthTest();
					RenderSystem.disableDepthTest();
					RenderSystem.enableBlend();
					RenderSystem.defaultBlendFunc();
					RenderSystem.disableTexture();
					RenderSystem.lineWidth(3.0f);
					
					switch (WandItem.getMode()) {
					case 0: {
						if (is_pane || is_fence || is_fence_gate) {
							WandItem.valid = false;
							break;
						}
						mode0(bufferBuilder,c,wand, pos, y0, h, side, world, block_state, player, lim, hit_x, hit_y, hit_z,destroy);
					}
						break;
					case 1: {
						mode1(bufferBuilder,c,wand, pos, y0, h, side, hit_x, hit_y, hit_z, world, block_state, player, lim, max_xp_blocks,
								item_stack, (is_pane || is_fence || is_fence_gate || is_stairs || is_leaves), isCreative,destroy);
					}
						break;
					case 2: {
						// player.sendMessage(new LiteralText("m use 2"),true);
						WandItem.valid = true;
						if (WandItem.fill_pos1 != null) {
							float x1 = WandItem.fill_pos1.getX();
							float y1 = WandItem.fill_pos1.getY();
							float z1 = WandItem.fill_pos1.getZ();
							float x2 = pos.getX();
							float y2 = pos.getY();
							float z2 = pos.getZ();
							if (!WandItem.fill_pos1.equals(pos)) {
								if (x1 >= x2) {
									x1 += 1;
								} else {
									x2 += 1;
								}
								if (y1 >= y2) {
									y1 += 1;
								} else {
									y2 += 1;
								}
								if (z1 >= z2) {
									z1 += 1;
								} else {
									z2 += 1;
								}
							} else {
								x2 = x1 + 1;
								y2 = y1 + 1;
								z2 = z1 + 1;
							}
							if (Math.abs(x2 - x1) <= lim && Math.abs(y2 - y1) <= lim && Math.abs(z2 - z1) <= lim) {
								preview(bufferBuilder,c.x+x1, c.y+y1, c.z+z1, c.x+x2, c.y+y2, c.z+z2);
							}
						}
					}
						break;
					case 3: {
						mode3(bufferBuilder,c,wand, pos, block_state, world, side,destroy);
					}
						break;
					case 4: {
						WandItem.valid = true;
						if (WandItem.fill_pos1 != null) {
							float x1 = WandItem.fill_pos1.getX();
							float y1 = WandItem.fill_pos1.getY();
							float z1 = WandItem.fill_pos1.getZ();
							float x2 = pos.getX();
							float y2 = pos.getY();
							float z2 = pos.getZ();
							float xx = x2 - x1;
							float yy = y2 - y1;
							float zz = z2 - z1;
							int nbl = (int) Math.ceil(Math.PI * Math.sqrt(xx * xx + yy * yy + zz * zz));
							if (!isCreative && nbl > lim) {
								WandItem.valid = false;
								if (prnt) {
									WandsMod.compat.send_message_to_player("wand limit reached");
								}
							} else {
								bufferBuilder.vertex(c.x+x1+0.5, c.y+y1+0.5, c.z+z1+0.5).color(255,255,255,255).next();
								bufferBuilder.vertex(c.x+x2+0.5, c.y+y2+0.5, c.z+z2+0.5).color(255,255,255,255).next();
								line(bufferBuilder,c,WandItem.fill_pos1, pos);
							}
						}
					}
						break;
					case 5: {
						WandItem.valid = true;
						
						if (WandItem.fill_pos1 != null) {
							float x1 = WandItem.fill_pos1.getX();
							float y1 = WandItem.fill_pos1.getY();
							float z1 = WandItem.fill_pos1.getZ();
							float x2 = pos.getX();
							float y2 = y1;// pos.getY();
							float z2 = pos.getZ();
							float xx = x2 - x1;
							float yy = y2 - y1;
							float zz = z2 - z1;
							int radius = (int) (Math.sqrt(xx * xx + yy * yy + zz * zz));
							int cir = (int) (Math.PI * radius);
							if (!isCreative && cir > lim) {
								WandItem.valid = false;
								if (prnt) {
									WandsMod.compat.send_message_to_player("wand limit reached");
								}
							} else {
								if (prnt) {
									WandsMod.compat.send_message_to_player("diam: " + ((radius * 2) + 1));
								}
								bufferBuilder.vertex(c.x+x1+0.5, c.y+y1+0.5, c.z+z1+0.5).color(255,255,255,255).next();
								bufferBuilder.vertex(c.x+x2+0.5, c.y+y2+0.5, c.z+z2+0.5).color(255,255,255,255).next();								
								circle(bufferBuilder,c,WandItem.fill_pos1, pos, WandItem.getPlane().ordinal());
							}
						}
					}
						break;
					default: {
						WandItem.valid = false;
					}
						break;
					}
					tessellator.draw();
				}
			}
		}
	}

	private static void mode0(BufferBuilder bufferBuilder,Vec3d c,WandItem wand,BlockPos pos, float y0, float h, MyDir side, 
		World world, BlockState block_state, PlayerEntity player, int lim,double hit_x,double hit_y,double hit_z,boolean destroy) {
		
		double x = pos.getX();
		double y = pos.getY();
		double z = pos.getZ();
		double o = 0.01f;
		switch (side) {
		case UP:
			y += h + o;
			break;
		case DOWN:
			y -= o;
			break;
		case SOUTH:
			z += 1 + o;
			break;
		case NORTH:
			z -= o;
			break;
		case EAST:
			x += 1 + o;
			break;
		case WEST:
			x -= o;
			break;
		}

		grid(bufferBuilder,side, c.x+x,c.y+ y + y0, c.z+z,  h);
		MyDir dirs[] = getMyDirMode0(side, y0, h,hit_x, hit_y, hit_z);
		MyDir d1 = dirs[0];
		MyDir d2 = dirs[1];

		if (d1 != null) {
			BlockPos pv = null;
			if (d2 != null) {
				pv = find_next_diag(world, block_state, d1, d2, pos, wand,destroy);
			} else {
				pv = find_next_pos(world, block_state, d1, pos, wand,destroy);
			}
			if (pv != null) {
				int x1 = pv.getX();
				int y1 = pv.getY();
				int z1 = pv.getZ();
				int x2 = x1 + 1;
				int y2 = y1 + 1;
				int z2 = z1 + 1;

				if (WandsMod.compat.interescts_player_bb(player, x1, y1, z1, x2, y2, z2)) {
					WandItem.valid = false;
				} else {
					WandItem.valid = true;
					// WandItem.mode2_dir = d1;
					WandItem.x1 = x1;
					WandItem.y1 = y1;
					WandItem.z1 = z1;
					WandItem.x2 = x2;
					WandItem.y2 = y2;
					WandItem.z2 = z2;
					
					preview(bufferBuilder, c.x+x1 , c.y+y1 , c.z+z1 , c.x+x2 , c.y+y2 ,c.z+ z2 );
				}
			} else {
				WandItem.valid = false;
			}
		}
	}

	private static void mode1(BufferBuilder bufferBuilder,Vec3d c,WandItem wand,BlockPos pos, float y0, float h, MyDir side,double hit_x,double hit_y,double hit_z,
			World world, BlockState block_state, PlayerEntity player, int lim,
			int max_xp_blocks, ItemStack item_stack, boolean dont_check_state,boolean isCreative,boolean destroy) {
		MyDir dir = MyDir.EAST;
		BlockPos pos_m = WandsMod.compat.pos_offset(pos, side, 1);
		BlockState state = world.getBlockState(pos_m);

		if (state.isAir() || WandsMod.compat.is_fluid(state, wand)) {
			BlockPos pos0 = pos;
			BlockPos pos1 = pos_m;
			BlockPos pos2 = pos;
			BlockPos pos3 = pos_m;
			int offx = 0;
			int offy = 0;
			int offz = 0;
			switch (side) {
			case UP:
			case DOWN:
				switch (WandItem.getOrientation()) {
				case HORIZONTAL:
					dir = MyDir.SOUTH;
					offz = -1;
					break;
				case VERTICAL:
					dir = MyDir.EAST;
					offx = -1;
					break;
				}
				break;
			case SOUTH:
			case NORTH:
				switch (WandItem.getOrientation()) {
				case HORIZONTAL:
					dir = MyDir.EAST;
					offx = -1;
					break;
				case VERTICAL:
					dir = MyDir.UP;
					offy = -1;
					break;
				}
				break;
			case EAST:
			case WEST:
				switch (WandItem.getOrientation()) {
				case HORIZONTAL:
					dir = MyDir.SOUTH;
					offz = -1;
					break;
				case VERTICAL:
					dir = MyDir.UP;
					offy = -1;
					break;
				}
				break;
			}

			MyDir op = dir.getOpposite();
			int i = lim - 1;
			int k = 0;
			boolean stop1 = false;
			boolean stop2 = false;
			boolean intersects = false;
			if (!isCreative) {
				int n = WandsMod.compat.in_inventory(player,item_stack);
				if (n < i) {
					i = n - 1;
				}
				if (BLOCKS_PER_XP != 0 && max_xp_blocks < i) {
					i = max_xp_blocks - 1;
				}
			}
			// boolean is_fluid;
			boolean eq = false;
			while (k < 81 && i > 0) {
				if (!stop1 && i > 0) {
					BlockState bs0 = world.getBlockState(WandsMod.compat.pos_offset(pos0, dir, 1));
					BlockState bs1 = world.getBlockState(WandsMod.compat.pos_offset(pos1, dir, 1));
					if (dont_check_state) {
						eq = bs0.getBlock().equals(block_state.getBlock());
					} else {
						eq = bs0.equals(block_state);
					}
					// is_fluid =
					// bs1.getFluidState().isIn(FluidTags.WATER)||bs1.getFluidState().isIn(FluidTags.LAVA);
					if (eq && (bs1.isAir() || WandsMod.compat.is_fluid(bs1, wand))) {
						pos0 = WandsMod.compat.pos_offset(pos0, dir, 1);
						pos1 = WandsMod.compat.pos_offset(pos1, dir, 1);
						i--;
					} else {
						stop1 = true;
					}
				}
				if (!stop2 && i > 0) {
					BlockState bs2 = world.getBlockState(WandsMod.compat.pos_offset(pos2, op, 1));
					BlockState bs3 = world.getBlockState(WandsMod.compat.pos_offset(pos3, op, 1));
					if (dont_check_state) {
						eq = bs2.getBlock().equals(block_state.getBlock());
					} else {
						eq = bs2.equals(block_state);
					}
					// is_fluid =
					// bs3.getFluidState().isIn(FluidTags.WATER)||bs3.getFluidState().isIn(FluidTags.LAVA);
					if (eq && (bs3.isAir() || WandsMod.compat.is_fluid(bs3, wand))) {
						pos2 = WandsMod.compat.pos_offset(pos2, op, 1);
						pos3 = WandsMod.compat.pos_offset(pos3, op, 1);
						i--;
					} else {
						stop2 = true;
					}
				}
				if(!destroy){
					if (WandsMod.compat.interescts_player_bb(player, pos1.getX(), pos1.getY(), pos1.getZ(), pos1.getX() + 1,
							pos1.getY() + 1, pos1.getZ() + 1)) {
						intersects = true;
						break;
					}
					if (WandsMod.compat.interescts_player_bb(player, pos3.getX(), pos3.getY(), pos3.getZ(), pos3.getX() + 1,
							pos3.getY() + 1, pos3.getZ() + 1)) {
						intersects = true;
						break;
					}
				}
				k++;
				if (stop1 && stop2) {
					k = 1000;
				}
			}

			
			if(destroy){
				pos1=WandsMod.compat.pos_offset(pos1, side.getOpposite(), 1);
				pos3=WandsMod.compat.pos_offset(pos3, side.getOpposite(), 1);
			}

			int x1 = pos1.getX() - offx;
			int y1 = pos1.getY() - offy;
			int z1 = pos1.getZ() - offz;
			int x2 = pos3.getX() + 1 + offx;
			int y2 = pos3.getY() + 1 + offy;
			int z2 = pos3.getZ() + 1 + offz;

			if (intersects) {
				WandItem.valid = false;
			} else {
				WandItem.valid = true;
				// WandItem.mode2_dir = dir.getOpposite();
				WandItem.x1 = x1 + offx;
				WandItem.y1 = y1 + offy;
				WandItem.z1 = z1 + offz;
				WandItem.x2 = x2 + offx;
				WandItem.y2 = y2 + offy;
				WandItem.z2 = z2 + offz;
				
				preview(bufferBuilder, c.x+x1 , c.y+y1 , c.z+z1 ,  c.x+x2 , c.y+y2 , c.z+z2 );
			}
		} else {
			WandItem.valid = false;
		}
	}

	static private void mode3(BufferBuilder bufferBuilder,Vec3d c,WandItem wand,BlockPos pos,BlockState block_state,World world,MyDir side,boolean destroy){
		
		wand.block_buffer_length=0;
		add_neighbour(wand, pos, block_state, world, side);
		int i = 0;		
		
		while (i < wand.getLimit()) {
			if (i < wand.block_buffer_length) {
				BlockPos p = WandsMod.compat.pos_offset(wand.block_buffer[i], side, -1);
				find_neighbours(wand, p, block_state, world, side);
			}
			i++;
		}
		for (int a = 0; a < wand.block_buffer_length; a++) {			
			BlockPos p=wand.block_buffer[a];
			if(destroy){
				wand.block_buffer[a]=WandsMod.compat.pos_offset(wand.block_buffer[a], side, -1);
				p=wand.block_buffer[a];
			}
			int x1 = p.getX();
			int y1 = p.getY();
			int z1 = p.getZ();

			preview(bufferBuilder,c.x+ x1 ,c.y+ y1 ,c.z+ z1 , c.x+x1+1 ,c.y+ y1+1 ,c.z+ z1+1 );
		}
		WandItem.valid = (wand.block_buffer_length > 0);
	}

	static private void find_neighbours(WandItem wand, BlockPos pos, BlockState block_state, World world, MyDir side) {

		if (side == MyDir.UP || side == MyDir.DOWN) {
			BlockPos p0 = WandsMod.compat.pos_offset(pos, MyDir.EAST, 1);
			add_neighbour(wand, p0, block_state, world, side);

			p0 = WandsMod.compat.pos_offset(pos, MyDir.EAST, 1);
			BlockPos p1 = WandsMod.compat.pos_offset(p0, MyDir.NORTH, 1);
			add_neighbour(wand, p1, block_state, world, side);

			p0 = WandsMod.compat.pos_offset(pos, MyDir.NORTH, 1);
			add_neighbour(wand, p0, block_state, world, side);

			p0 = WandsMod.compat.pos_offset(pos, MyDir.NORTH, 1);
			p1 = WandsMod.compat.pos_offset(p0, MyDir.WEST, 1);
			add_neighbour(wand, p1, block_state, world, side);

			p0 = WandsMod.compat.pos_offset(pos, MyDir.WEST, 1);
			add_neighbour(wand, p0, block_state, world, side);

			p0 = WandsMod.compat.pos_offset(pos, MyDir.SOUTH, 1);
			p1 = WandsMod.compat.pos_offset(p0, MyDir.WEST, 1);
			add_neighbour(wand, p1, block_state, world, side);

			p0 = WandsMod.compat.pos_offset(pos, MyDir.SOUTH, 1);
			add_neighbour(wand, p0, block_state, world, side);

			p0 = WandsMod.compat.pos_offset(pos, MyDir.SOUTH, 1);
			p1 = WandsMod.compat.pos_offset(p0, MyDir.EAST, 1);
			add_neighbour(wand, p1, block_state, world, side);

		} else {
			if (side == MyDir.EAST || side == MyDir.WEST) {
				BlockPos p0 = WandsMod.compat.pos_offset(pos, MyDir.UP, 1);
				add_neighbour(wand, p0, block_state, world, side);

				p0 = WandsMod.compat.pos_offset(pos, MyDir.UP, 1);
				BlockPos p1 = WandsMod.compat.pos_offset(p0, MyDir.NORTH, 1);
				add_neighbour(wand, p1, block_state, world, side);

				p0 = WandsMod.compat.pos_offset(pos, MyDir.NORTH, 1);
				add_neighbour(wand, p0, block_state, world, side);

				p0 = WandsMod.compat.pos_offset(pos, MyDir.NORTH, 1);
				p1 = WandsMod.compat.pos_offset(p0, MyDir.DOWN, 1);
				add_neighbour(wand, p1, block_state, world, side);

				p0 = WandsMod.compat.pos_offset(pos, MyDir.DOWN, 1);
				add_neighbour(wand, p0, block_state, world, side);

				p0 = WandsMod.compat.pos_offset(pos, MyDir.SOUTH, 1);
				p1 = WandsMod.compat.pos_offset(p0, MyDir.DOWN, 1);
				add_neighbour(wand, p1, block_state, world, side);

				p0 = WandsMod.compat.pos_offset(pos, MyDir.SOUTH, 1);
				add_neighbour(wand, p0, block_state, world, side);

				p0 = WandsMod.compat.pos_offset(pos, MyDir.SOUTH, 1);
				p1 = WandsMod.compat.pos_offset(p0, MyDir.UP, 1);
				add_neighbour(wand, p1, block_state, world, side);

			} else if (side == MyDir.NORTH || side == MyDir.SOUTH) {
				BlockPos p0 = WandsMod.compat.pos_offset(pos, MyDir.EAST, 1);
				add_neighbour(wand, p0, block_state, world, side);

				p0 = WandsMod.compat.pos_offset(pos, MyDir.EAST, 1);
				BlockPos p1 = WandsMod.compat.pos_offset(p0, MyDir.UP, 1);
				add_neighbour(wand, p1, block_state, world, side);

				p0 = WandsMod.compat.pos_offset(pos, MyDir.UP, 1);
				add_neighbour(wand, p0, block_state, world, side);

				p0 = WandsMod.compat.pos_offset(pos, MyDir.UP, 1);
				p1 = WandsMod.compat.pos_offset(p0, MyDir.WEST, 1);
				add_neighbour(wand, p1, block_state, world, side);

				p0 = WandsMod.compat.pos_offset(pos, MyDir.WEST, 1);
				add_neighbour(wand, p0, block_state, world, side);

				p0 = WandsMod.compat.pos_offset(pos, MyDir.DOWN, 1);
				p1 = WandsMod.compat.pos_offset(p0, MyDir.WEST, 1);
				add_neighbour(wand, p1, block_state, world, side);

				p0 = WandsMod.compat.pos_offset(pos, MyDir.DOWN, 1);
				add_neighbour(wand, p0, block_state, world, side);

				p0 = WandsMod.compat.pos_offset(pos, MyDir.DOWN, 1);
				p1 = WandsMod.compat.pos_offset(p0, MyDir.EAST, 1);
				add_neighbour(wand, p1, block_state, world, side);

			}
		}

	}

	static public boolean can_place(BlockState state, WandItem wand, World world, BlockPos pos) {

		return (state.isAir() || WandsMod.compat.is_fluid(state, wand) || WandsMod.compat.is_plant(state));
	}

	static private void add_neighbour(WandItem wand, BlockPos pos, BlockState block_state, World world, MyDir side) {
		BlockPos pos2 = WandsMod.compat.pos_offset(pos, side, 1);
		if (!wand.in_buffer(pos2)) {
			BlockState bs1 = world.getBlockState(pos);
			BlockState bs2 = world.getBlockState(pos2);
			if (bs1.equals(block_state) && can_place(bs2, wand, world, pos2)) {
				wand.add_buffer(pos2);
			}
		}
	}

	static public BlockPos find_next_diag(World world, BlockState block_state, MyDir dir1, MyDir dir2, BlockPos pos,
			WandItem wand,boolean destroy) {
		BlockPos p0=pos;
		for (int i = 0; i < wand.getLimit(); i++) {
			BlockPos p1 = WandsMod.compat.pos_offset(pos, dir1, 1);
			pos = WandsMod.compat.pos_offset(p1, dir2, 1);
			BlockState bs = world.getBlockState(pos);
			if (bs != null) {
				if(destroy){
					if (!bs.equals(block_state) && p0!=null)
						return p0;
				}else{
					if (can_place(bs, wand, world, pos)) {
						return pos;
					} else {
						if (!bs.equals(block_state))
							return null;
					}
				}
			}
			p0=pos;
		}
		return null;
	}

	static public BlockPos find_next_pos(World world, BlockState block_state, MyDir dir, BlockPos pos, WandItem wand,boolean destroy) {
		for (int i = 0; i < wand.getLimit(); i++) {
			BlockPos pos2 = WandsMod.compat.pos_offset(pos, dir, i + 1);
			BlockState bs = world.getBlockState(pos2);
			
			if (bs != null) {
				if (!bs.equals(block_state)) {
					if(destroy){
						return WandsMod.compat.pos_offset(pos, dir, i);
					}else{
						if (can_place(bs, wand, world, pos2)) {
							return pos2;
						} else {
							return null;
						}
					}
				}
			}
		}
		return null;
	}

	static public float unitCoord(float x) {
		float y = x - ((int) x);
		if (y < 0)
			y = 1.0f + y;
		return y;
	}

	static public MyDir[] getMyDirMode0(MyDir side, float y0, float h, double hit_x, double hit_y, double hit_z) {
		MyDir ret[] = new MyDir[2];
		ret[0] = null;
		ret[1] = null;
		// MinecraftClient client = MinecraftClient.getInstance();
		// ClientPlayerEntity player = client.player;
		float x = unitCoord((float) hit_x);
		float y = unitCoord((float) hit_y);
		float z = unitCoord((float) hit_z);
		float a = 0.25f;
		float b = 0.75f;
		float a2 = y0 + a * h;
		float b2 = y0 + b * h;
		switch (side) {
		case UP:
		case DOWN:
			if (x >= a && x <= b) {
				if (z <= a) {
					ret[0] = MyDir.NORTH;
				} else {
					if (z >= b) {
						ret[0] = MyDir.SOUTH;
					} else {
						ret[0] = side.getOpposite();
					}
				}
			} else {
				if (z >= a && z <= b) {
					if (x <= a) {
						ret[0] = MyDir.WEST;
					} else {
						if (x >= b) {
							ret[0] = MyDir.EAST;
						}
					}
				} else {
					if (x <= a && z <= a) {
						ret[0] = MyDir.WEST;
						ret[1] = MyDir.NORTH;
					}
					if (x >= b && z <= a) {
						ret[0] = MyDir.EAST;
						ret[1] = MyDir.NORTH;
					}
					if (x >= b && z >= b) {
						ret[0] = MyDir.EAST;
						ret[1] = MyDir.SOUTH;
					}
					if (x <= a && z >= b) {
						ret[0] = MyDir.WEST;
						ret[1] = MyDir.SOUTH;
					}
				}
			}
			break;
		case EAST:
		case WEST:

			if (z >= a && z <= b) {
				if (y <= a2) {
					ret[0] = MyDir.DOWN;
				} else {
					if (y >= b2) {
						ret[0] = MyDir.UP;
					} else {
						ret[0] = side.getOpposite();
					}
				}
			} else {
				if (y >= a2 && y <= b2) {
					if (z <= a) {
						ret[0] = MyDir.NORTH;
						return ret;
					} else {
						if (z >= b) {
							ret[0] = MyDir.SOUTH;
							return ret;
						}
					}
				} else {
					if (y <= a2 && z <= a) {
						ret[0] = MyDir.DOWN;
						ret[1] = MyDir.NORTH;
					}
					if (y >= b2 && z <= a) {
						ret[0] = MyDir.UP;
						ret[1] = MyDir.NORTH;
					}
					if (y >= b2 && z >= b) {
						ret[0] = MyDir.UP;
						ret[1] = MyDir.SOUTH;
					}
					if (y <= a2 && z >= b) {
						ret[0] = MyDir.DOWN;
						ret[1] = MyDir.SOUTH;
					}
				}
			}
			break;
		case NORTH:
		case SOUTH:
			if (x >= a && x <= b) {
				if (y <= a2) {
					ret[0] = MyDir.DOWN;
				} else {
					if (y >= b2) {
						ret[0] = MyDir.UP;
					} else {
						ret[0] = side.getOpposite();
					}
				}
			} else {
				if (y >= a2 && y <= b2) {
					if (x <= a) {
						ret[0] = MyDir.WEST;
					} else {
						if (x >= b2) {
							ret[0] = MyDir.EAST;
						}
					}
				} else {
					if (y <= a2 && x <= a) {
						ret[0] = MyDir.DOWN;
						ret[1] = MyDir.WEST;
					}
					if (y >= b2 && x <= a) {
						ret[0] = MyDir.UP;
						ret[1] = MyDir.WEST;
					}
					if (y >= b2 && x >= b) {
						ret[0] = MyDir.UP;
						ret[1] = MyDir.EAST;
					}
					if (y <= a2 && x >= b) {
						ret[0] = MyDir.DOWN;
						ret[1] = MyDir.EAST;
					}
				}
			}
			break;
		}
		if (WandItem.getInvert()) {
			if (ret[0] != null)
				ret[0] = ret[0].getOpposite();
			if (ret[1] != null)
				ret[1] = ret[1].getOpposite();

		}
		return ret;
	}

	private static void preview(BufferBuilder bufferBuilder,double fx1, double fy1, double fz1, double fx2, double fy2, double fz2) {
		fx1 += p_o;
		fy1 += p_o;
		fz1 += p_o;
		fx2 -= p_o;
		fy2 -= p_o;
		fz2 -= p_o;
		bufferBuilder.vertex(fx1, fy1, fz1).color(255,255,255,255).next();
		bufferBuilder.vertex(fx2, fy1, fz1).color(255,255,255,255).next();
		bufferBuilder.vertex(fx1, fy1, fz1).color(255,255,255,255).next();
		bufferBuilder.vertex(fx1, fy1, fz2).color(255,255,255,255).next();
		bufferBuilder.vertex(fx1, fy1, fz2).color(255,255,255,255).next();
		bufferBuilder.vertex(fx2, fy1, fz2).color(255,255,255,255).next();
		bufferBuilder.vertex(fx2, fy1, fz1).color(255,255,255,255).next();
		bufferBuilder.vertex(fx2, fy1, fz2).color(255,255,255,255).next();
		bufferBuilder.vertex(fx1, fy2, fz1).color(255,255,255,255).next();
		bufferBuilder.vertex(fx2, fy2, fz1).color(255,255,255,255).next();
		bufferBuilder.vertex(fx1, fy2, fz1).color(255,255,255,255).next();
		bufferBuilder.vertex(fx1, fy2, fz2).color(255,255,255,255).next();
		bufferBuilder.vertex(fx1, fy2, fz2).color(255,255,255,255).next();
		bufferBuilder.vertex(fx2, fy2, fz2).color(255,255,255,255).next();
		bufferBuilder.vertex(fx2, fy2, fz1).color(255,255,255,255).next();
		bufferBuilder.vertex(fx2, fy2, fz2).color(255,255,255,255).next();
		bufferBuilder.vertex(fx1, fy1, fz1).color(255,255,255,255).next();
		bufferBuilder.vertex(fx1, fy2, fz1).color(255,255,255,255).next();
		bufferBuilder.vertex(fx2, fy1, fz1).color(255,255,255,255).next();
		bufferBuilder.vertex(fx2, fy2, fz1).color(255,255,255,255).next();
		bufferBuilder.vertex(fx1, fy1, fz2).color(255,255,255,255).next();
		bufferBuilder.vertex(fx1, fy2, fz2).color(255,255,255,255).next();
		bufferBuilder.vertex(fx2, fy1, fz2).color(255,255,255,255).next();
		bufferBuilder.vertex(fx2, fy2, fz2).color(255,255,255,255).next();
	}

	private static void grid(BufferBuilder bufferBuilder,MyDir side, double x, double y, double z, double h) {
		
		switch (side) {
			case UP:
			case DOWN: {				
				bufferBuilder.vertex(x, y, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 1.00f, y, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y, z + 1.00f).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 1.00f, y, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 1.00f, y, z + 1.00f).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y, z + 1.00f).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 1.00f, y, z + 1.00f).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y, z + 0.25f).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 1.00f, y, z + 0.25f).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y, z + 0.75f).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 1.00f, y, z + 0.75f).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 0.25f, y, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 0.25f, y, z + 1.00f).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 0.75f, y, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 0.75f, y, z + 1.00f).color(255,255,255,255).next();
				//RenderSystem.color4f( 178f, 0f, 0f, 255f);//r
				bufferBuilder.vertex(x + 0.40f, y, z + 0.20f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y, z + 0.05f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.60f, y, z + 0.20f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y, z + 0.05f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.40f, y, z + 0.80f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y, z + 0.95f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.60f, y, z + 0.80f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y, z + 0.95f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.20f, y, z + 0.40f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.05f, y, z + 0.50f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.20f, y, z + 0.60f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.05f, y, z + 0.50f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.80f, y, z + 0.40f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.95f, y, z + 0.50f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.80f, y, z + 0.60f).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.95f, y, z + 0.50f).color(178,0,0,255).next();
				//RenderSystem.color4f( 0f,178f,  0f, 255f);//g
				bufferBuilder.vertex(x + 0.40f, y, z + 0.50f).color(0,178,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y, z + 0.40f).color(0,178,0,255).next();
				bufferBuilder.vertex(x + 0.40f, y, z + 0.50f).color(0,178,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y, z + 0.60f).color(0,178,0,255).next();
				bufferBuilder.vertex(x + 0.60f, y, z + 0.50f).color(0,178,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y, z + 0.60f).color(0,178,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y, z + 0.40f).color(0,178,0,255).next();
				bufferBuilder.vertex(x + 0.60f, y, z + 0.50f).color(0,178,0,255).next();
				//RenderSystem.color4f( 0f, 0f,178f, 255f);//b
				bufferBuilder.vertex(x + 0.10f, y, z + 0.10f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.20f, y, z + 0.14f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.10f, y, z + 0.10f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.14f, y, z + 0.20f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.90f, y, z + 0.90f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.80f, y, z + 0.86f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.90f, y, z + 0.90f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.86f, y, z + 0.80f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.90f, y, z + 0.10f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.80f, y, z + 0.14f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.90f, y, z + 0.10f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.86f, y, z + 0.20f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.10f, y, z + 0.90f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.20f, y, z + 0.86f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.10f, y, z + 0.90f).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.14f, y, z + 0.80f).color(0,0,178,255).next();

			}
				break;
			case NORTH:
			case SOUTH: {
				//RenderSystem.color4f( 255f, 255f, 255f, 255f);//w
				bufferBuilder.vertex(x, y, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 1.00f, y, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y + 1.00f * h, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 1.00f, y, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 1.00f, y + 1.00f * h, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y + 1.00f * h, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 1.00f, y + 1.00f * h, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y + 0.25f * h, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 1.00f, y + 0.25f * h, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y + 0.75f * h, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 1.00f, y + 0.75f * h, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 0.25f, y, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 0.25f, y + 1.00f * h, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 0.75f, y, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x + 0.75f, y + 1.00f * h, z).color(255,255,255,255).next();
				//RenderSystem.color4f( 178f, 0, 0, 255f);//r
				bufferBuilder.vertex(x + 0.40f, y + 0.20f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y + 0.05f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.60f, y + 0.20f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y + 0.05f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.40f, y + 0.80f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y + 0.95f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.60f, y + 0.80f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y + 0.95f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.20f, y + 0.40f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.05f, y + 0.50f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.20f, y + 0.60f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.05f, y + 0.50f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.80f, y + 0.40f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.95f, y + 0.50f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.80f, y + 0.60f * h, z).color(178,0,0,255).next();
				bufferBuilder.vertex(x + 0.95f, y + 0.50f * h, z).color(178,0,0,255).next();
				//RenderSystem.color4f( 0f,178f,  0.0f, 255f).next();//g
				bufferBuilder.vertex(x + 0.40f, y + 0.50f * h, z).color(0,178,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y + 0.40f * h, z).color(0,178,0,255).next();
				bufferBuilder.vertex(x + 0.40f, y + 0.50f * h, z).color(0,178,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y + 0.60f * h, z).color(0,178,0,255).next();
				bufferBuilder.vertex(x + 0.60f, y + 0.50f * h, z).color(0,178,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y + 0.60f * h, z).color(0,178,0,255).next();
				bufferBuilder.vertex(x + 0.50f, y + 0.40f * h, z).color(0,178,0,255).next();
				bufferBuilder.vertex(x + 0.60f, y + 0.50f * h, z).color(0,178,0,255).next();
				//RenderSystem.color4f( 0, 0,255f, 255f).next();//b
				bufferBuilder.vertex(x + 0.10f, y + 0.10f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.20f, y + 0.14f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.10f, y + 0.10f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.14f, y + 0.20f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.90f, y + 0.90f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.80f, y + 0.86f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.90f, y + 0.90f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.86f, y + 0.80f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.90f, y + 0.10f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.80f, y + 0.14f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.90f, y + 0.10f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.86f, y + 0.20f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.10f, y + 0.90f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.20f, y + 0.86f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.10f, y + 0.90f * h, z).color(0,0,178,255).next();
				bufferBuilder.vertex(x + 0.14f, y + 0.80f * h, z).color(0,0,178,255).next();
			}
				break;
			case EAST:
			case WEST: {
				//RenderSystem.color4f( 255f, 255f, 255f, 255f).next();//w
				bufferBuilder.vertex(x, y, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y + 1.00f * h, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y, z + 1.00f).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y + 1.00f * h, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y + 1.00f * h, z + 1.00f).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y, z + 1.00f).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y + 1.00f * h, z + 1.00f).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y, z + 0.25f).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y + 1.00f * h, z + 0.25f).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y, z + 0.75f).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y + 1.00f * h, z + 0.75f).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y + 0.25f * h, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y + 0.25f * h, z + 1.00f).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y + 0.75f * h, z).color(255,255,255,255).next();
				bufferBuilder.vertex(x, y + 0.75f * h, z + 1.00f).color(255,255,255,255).next();
				//RenderSystem.color4f( 178f, 0, 0, 255f).next();//b
				bufferBuilder.vertex(x, y + 0.40f * h, z + 0.20f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.50f * h, z + 0.05f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.60f * h, z + 0.20f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.50f * h, z + 0.05f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.40f * h, z + 0.80f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.50f * h, z + 0.95f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.60f * h, z + 0.80f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.50f * h, z + 0.95f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.20f * h, z + 0.40f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.05f * h, z + 0.50f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.20f * h, z + 0.60f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.05f * h, z + 0.50f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.80f * h, z + 0.40f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.95f * h, z + 0.50f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.80f * h, z + 0.60f).color(178,0,0,255).next();
				bufferBuilder.vertex(x, y + 0.95f * h, z + 0.50f).color(178,0,0,255).next();
				//RenderSystem.color4f( 178f, 255f, 255f, 255f).next();//g
				bufferBuilder.vertex(x, y + 0.40f * h, z + 0.50f).color(0,178,0,255).next();
				bufferBuilder.vertex(x, y + 0.50f * h, z + 0.40f).color(0,178,0,255).next();
				bufferBuilder.vertex(x, y + 0.40f * h, z + 0.50f).color(0,178,0,255).next();
				bufferBuilder.vertex(x, y + 0.50f * h, z + 0.60f).color(0,178,0,255).next();
				bufferBuilder.vertex(x, y + 0.60f * h, z + 0.50f).color(0,178,0,255).next();
				bufferBuilder.vertex(x, y + 0.50f * h, z + 0.60f).color(0,178,0,255).next();
				bufferBuilder.vertex(x, y + 0.50f * h, z + 0.40f).color(0,178,0,255).next();
				bufferBuilder.vertex(x, y + 0.60f * h, z + 0.50f).color(0,178,0,255).next();
				//RenderSystem.color4f( 0, 0,255f, 255f).next();//b
				bufferBuilder.vertex(x, y + 0.10f * h, z + 0.10f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.20f * h, z + 0.14f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.10f * h, z + 0.10f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.14f * h, z + 0.20f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.90f * h, z + 0.90f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.80f * h, z + 0.86f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.90f * h, z + 0.90f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.86f * h, z + 0.80f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.90f * h, z + 0.10f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.80f * h, z + 0.14f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.90f * h, z + 0.10f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.86f * h, z + 0.20f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.10f * h, z + 0.90f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.20f * h, z + 0.86f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.10f * h, z + 0.90f).color(0,0,178,255).next();
				bufferBuilder.vertex(x, y + 0.14f * h, z + 0.80f).color(0,0,178,255).next();

		}
			break;
		}
	}
	
	static private void prev_drawCircle(BufferBuilder bufferBuilder,Vec3d c,int xc, int yc,int zc, int x, int y,int z,int plane)
	{
		switch(plane){
			case 0://XZ
				preview(bufferBuilder, c.x+ xc+x, c.y+ yc, c.z+ zc+z,   c.x+ xc+x+1, c.y+ yc+1, c.z+ zc+z+1 );
				preview(bufferBuilder, c.x+ xc-x, c.y+ yc, c.z+ zc+z,   c.x+ xc-x+1, c.y+ yc+1, c.z+ zc+z+1 );
				preview(bufferBuilder, c.x+ xc+x, c.y+ yc, c.z+ zc-z,   c.x+ xc+x+1, c.y+ yc+1, c.z+ zc-z+1 );
				preview(bufferBuilder, c.x+ xc-x, c.y+ yc, c.z+ zc-z,   c.x+ xc-x+1, c.y+ yc+1, c.z+ zc-z+1 );
				preview(bufferBuilder, c.x+ xc+z, c.y+ yc, c.z+ zc+x,   c.x+ xc+z+1, c.y+ yc+1, c.z+ zc+x+1 );
				preview(bufferBuilder, c.x+ xc-z, c.y+ yc, c.z+ zc+x,   c.x+ xc-z+1, c.y+ yc+1, c.z+ zc+x+1 );
				preview(bufferBuilder, c.x+ xc+z, c.y+ yc, c.z+ zc-x,   c.x+ xc+z+1, c.y+ yc+1, c.z+ zc-x+1 );
				preview(bufferBuilder, c.x+ xc-z, c.y+ yc, c.z+ zc-x,   c.x+ xc-z+1, c.y+ yc+1, c.z+ zc-x+1 );		
			break;
			case 1://XY
				preview(bufferBuilder, c.x+ xc+x, c.y+ yc+y, c.z+ zc,   c.x+  xc+x+1,c.y+  yc+y+1,c.z+  zc+1 );
				preview(bufferBuilder, c.x+ xc-x, c.y+ yc+y, c.z+ zc,   c.x+  xc-x+1,c.y+  yc+y+1,c.z+  zc+1 );
				preview(bufferBuilder, c.x+ xc+x, c.y+ yc-y, c.z+ zc,   c.x+  xc+x+1,c.y+  yc-y+1,c.z+  zc+1 );
				preview(bufferBuilder, c.x+ xc-x, c.y+ yc-y, c.z+ zc,   c.x+  xc-x+1,c.y+  yc-y+1,c.z+  zc+1 );
				preview(bufferBuilder, c.x+ xc+y, c.y+ yc+x, c.z+ zc,   c.x+  xc+y+1,c.y+  yc+x+1,c.z+  zc+1 );
				preview(bufferBuilder, c.x+ xc-y, c.y+ yc+x, c.z+ zc,   c.x+  xc-y+1,c.y+  yc+x+1,c.z+  zc+1 );
				preview(bufferBuilder, c.x+ xc+y, c.y+ yc-x, c.z+ zc,   c.x+  xc+y+1,c.y+  yc-x+1,c.z+  zc+1 );
				preview(bufferBuilder, c.x+ xc-y, c.y+ yc-x, c.z+ zc,   c.x+  xc-y+1,c.y+  yc-x+1,c.z+  zc+1 );
			break;
			case 2://YZ
				preview(bufferBuilder, c.x+ xc, c.y+ yc+y,c.z+  zc+z,   c.x+  xc+1, c.y+ yc+y+1, c.z+ zc+z+1  );
				preview(bufferBuilder, c.x+ xc, c.y+ yc-y,c.z+  zc+z,   c.x+  xc+1, c.y+ yc-y+1, c.z+ zc+z+1  );
				preview(bufferBuilder, c.x+ xc, c.y+ yc+y,c.z+  zc-z,   c.x+  xc+1, c.y+ yc+y+1, c.z+ zc-z+1  );
				preview(bufferBuilder, c.x+ xc, c.y+ yc-y,c.z+  zc-z,   c.x+  xc+1, c.y+ yc-y+1, c.z+ zc-z+1  );
				preview(bufferBuilder, c.x+ xc, c.y+ yc+z,c.z+  zc+y,   c.x+  xc+1, c.y+ yc+z+1, c.z+ zc+y+1  );
				preview(bufferBuilder, c.x+ xc, c.y+ yc-z,c.z+  zc+y,   c.x+  xc+1, c.y+ yc-z+1, c.z+ zc+y+1  );
				preview(bufferBuilder, c.x+ xc, c.y+ yc+z,c.z+  zc-y,   c.x+  xc+1, c.y+ yc+z+1, c.z+ zc-y+1  );
				preview(bufferBuilder, c.x+ xc, c.y+ yc-z,c.z+  zc-y,   c.x+  xc+1, c.y+ yc-z+1, c.z+ zc-y+1  );		
			break;
		}
	}
	private static void circle(BufferBuilder bufferBuilder,Vec3d c,BlockPos pos0,BlockPos pos1,int plane){
	
		int r =1;
		int xc=pos0.getX();
		int yc=pos0.getY();
		int zc=pos0.getZ();
		int px=pos1.getX()-pos0.getX();
		int py=pos1.getY()-pos0.getY();
		int pz=pos1.getZ()-pos0.getZ();
		r=(int)Math.sqrt(px*px+py*py+pz*pz );
		
		if(plane==0){//XZ;
			int x = 0, y=0, z = r;
			int d = 3 - 2 * r;
			prev_drawCircle(bufferBuilder,c,xc, yc, zc, x, y, z, plane);
			while (z >= x)
			{
				x++; 
				if (d > 0)
				{
					z--; 
					d = d + 4 * (x - z) + 10;
				} else
					d = d + 4 * x + 6;
				prev_drawCircle(bufferBuilder,c,xc, yc, zc, x, y, z, plane);
			}
		} else if (plane == 1) {// XY;
			int x = 0, y = r, z = 0;
			int d = 3 - 2 * r;
			prev_drawCircle(bufferBuilder,c,xc, yc, zc, x, y, z, plane);
			while (y >= x)
			{
				x++; 
				if (d > 0)
				{
					y--; 
					d = d + 4 * (x - y) + 10;
				} else
					d = d + 4 * x + 6;
				prev_drawCircle(bufferBuilder,c,xc, yc, zc, x, y, z, plane);
			}
		} else if (plane == 2) {// YZ;
			int x = 0, y = 0, z = r;
			int d = 3 - 2 * r;
			prev_drawCircle(bufferBuilder,c,xc, yc, zc, x, y, z, plane);
			while (z >= y)
			{
				y++; 
				if (d > 0)
				{
					z--; 
					d = d + 4 * (y - z) + 10;
				} else
					d = d + 4 * y + 6;
				prev_drawCircle(bufferBuilder,c,xc, yc, zc, x, y, z, plane);
			}
		}

	}

	private static void line(BufferBuilder bufferBuilder,Vec3d c,BlockPos pos0,BlockPos pos1)  
    {  
		
		int x1=pos0.getX();
		int y1=pos0.getY();
		int z1=pos0.getZ();
		int x2=pos1.getX();
		int y2=pos1.getY();
		int z2=pos1.getZ();
		int dx,dy,dz,xs,ys,zs,p1,p2;
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
		preview(bufferBuilder,c.x+x1, c.y+y1, c.z+z1,c.x+x1+1,c.y+y1+1,c.z+z1+1);
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
				p1 += 2 * dy ;
				p2 += 2 * dz ;
				
				preview(bufferBuilder,c.x+x1, c.y+y1, c.z+z1,c.x+x1+1,c.y+y1+1,c.z+z1+1);
				//LOGGER.info("line pos " +pos);
				
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
				p1 += 2 * dx ;
				p2 += 2 * dz ;
				preview(bufferBuilder,c.x+x1, c.y+y1, c.z+z1,c.x+x1+1,c.y+y1+1,c.z+z1+1);
				
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
				p1 += 2 * dy ;
				p2 += 2 * dx ;				
				preview(bufferBuilder,c.x+x1, c.y+y1, c.z+z1,c.x+x1+1,c.y+y1+1,c.z+z1+1);
			}
		}
	}
}
