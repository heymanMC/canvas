/*******************************************************************************
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.chunk;

import java.util.Arrays;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;

import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;

import grondag.canvas.apiimpl.rendercontext.TerrainRenderContext;
import grondag.canvas.chunk.ChunkPaletteCopier.PaletteCopy;
import grondag.canvas.chunk.occlusion.FastChunkOcclusionDataBuilder;

// PERF: to conserve memory, make this thread-local, residing in the terrain context
// only capture the paletteCopy, chunks and border states before prepare

public class FastRenderRegion extends AbstractRenderRegion implements RenderAttachedBlockView {
	protected final BlockPos.Mutable searchPos = new BlockPos.Mutable();

	protected final Object[] renderData = new Object[MAIN_CACHE_SIZE];
	public final BlockEntity[] blockEntities = new BlockEntity[MAIN_CACHE_SIZE];

	private final BlockState[] states = new BlockState[TOTAL_CACHE_SIZE];
	private final float[] aoCache = new float[TOTAL_CACHE_SIZE];
	private final int[] lightCache = new int[TOTAL_CACHE_SIZE];


	public final TerrainRenderContext terrainContext;

	public FastRenderRegion(TerrainRenderContext terrainContext) {
		this.terrainContext = terrainContext;
	}

	public void prepare(ProtoRenderRegion protoRegion) {
		System.arraycopy(protoRegion.chunks, 0, chunks, 0, 16);
		System.arraycopy(EMPTY_BLOCK_ENTITIES, 0, blockEntities, 0, MAIN_CACHE_SIZE);
		System.arraycopy(EMPTY_RENDER_DATA, 0, renderData, 0, MAIN_CACHE_SIZE);
		System.arraycopy(EMPTY_AO_CACHE, 0, aoCache, 0, TOTAL_CACHE_SIZE);
		System.arraycopy(EMPTY_LIGHT_CACHE, 0, lightCache, 0, TOTAL_CACHE_SIZE);

		world = protoRegion.world;

		originX = protoRegion.originX;
		originY = protoRegion.originY;
		originZ = protoRegion.originZ;

		chunkBaseX = protoRegion.chunkBaseX;
		chunkBaseY = protoRegion.chunkBaseY;
		chunkBaseZ = protoRegion.chunkBaseZ;

		final PaletteCopy pc  = protoRegion.takePaletteCopy();

		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					states[mainChunkLocalIndex(x, y, z)] = pc.apply(x | (y << 8) | (z << 4));
				}
			}
		}

		pc.release();

		System.arraycopy(protoRegion.states, 0, states, MAIN_CACHE_SIZE, BORDER_CACHE_SIZE);

		copyBeData(protoRegion);
	}

	private void copyBeData(ProtoRenderRegion protoRegion) {
		final ShortArrayList blockEntityPos = protoRegion.blockEntityPos;

		if( !blockEntityPos.isEmpty()) {
			final ObjectArrayList<BlockEntity> blockEntities = protoRegion.blockEntities;
			final int limit = blockEntityPos.size();

			for (int i = 0; i < limit; i++) {
				this.blockEntities[blockEntityPos.getShort(i)] = blockEntities.get(i);
			}
		}

		final ShortArrayList renderDataPos = protoRegion.renderDataPos;

		if( !renderDataPos.isEmpty()) {
			final ObjectArrayList<Object> renderData = protoRegion.renderData;
			final int limit = renderDataPos.size();

			for (int i = 0; i < limit; i++) {
				this.renderData[renderDataPos.getShort(i)] = renderData.get(i);
			}
		}
	}

	@Override
	public BlockState getBlockState(BlockPos pos) {
		final int i = blockIndex(pos.getX(), pos.getY(), pos.getZ());

		if (i == -1) {
			return world.getBlockState(pos);
		}

		return states[i];
	}

	public BlockState getBlockState(int x, int y, int z) {
		final int i = blockIndex(x, y, z);

		if (i == -1) {
			return world.getBlockState(searchPos.set(x, y, z));
		}

		return states[i];
	}

	/**
	 * Assumes values 0-15
	 */
	public BlockState getLocalBlockState(int x, int y, int z) {
		return states[mainChunkLocalIndex(x, y, z)];
	}

	@Override
	@Nullable
	public BlockEntity getBlockEntity(BlockPos pos) {
		return isInMainChunk(pos) ? blockEntities[mainChunkBlockIndex(pos)] : world.getBlockEntity(pos);
	}

	@Override
	public FluidState getFluidState(BlockPos pos) {
		return getBlockState(pos).getFluidState();
	}

	@Override
	public int getLightLevel(LightType type, BlockPos pos) {
		return world.getLightLevel(type, pos);
	}

	@Override
	public Object getBlockEntityRenderAttachment(BlockPos pos) {
		return isInMainChunk(pos) ? renderData[mainChunkBlockIndex(pos)] : null;
	}

	public int cachedBrightness(int x, int y, int z) {
		final int i = blockIndex(x, y, z);

		if (i == -1) {
			searchPos.set(x, y, z);
			final BlockState state = world.getBlockState(searchPos);
			return WorldRenderer.getLightmapCoordinates(world, state, searchPos);
		}

		int result = lightCache[i];

		if (result == Integer.MAX_VALUE) {
			final BlockState state = states[i];
			result = WorldRenderer.getLightmapCoordinates(world, state, searchPos.set(x, y, z));
			lightCache[i] = result;
		}

		return result;
	}

	public int directBrightness(BlockPos pos) {
		return WorldRenderer.getLightmapCoordinates(world, getBlockState(pos), pos);
	}

	public float cachedAoLevel(int x, int y, int z) {
		final int i = blockIndex(x, y, z);

		if (i == -1) {
			searchPos.set(x, y, z);
			final BlockState state = world.getBlockState(searchPos);
			return state.getLuminance() == 0 ? state.getAmbientOcclusionLightLevel(this, searchPos) : 1F;
		}

		float result = aoCache[i];

		if (result == Float.MAX_VALUE) {
			final BlockState state = states[i];
			result = state.getLuminance() == 0 ? state.getAmbientOcclusionLightLevel(this, searchPos.set(x, y, z)) : 1F;
			aoCache[i] = result;
		}

		return result;
	}

	public boolean isOpaque(int x, int y, int z, FastChunkOcclusionDataBuilder builder) {
		if (isInMainChunk(x, y, z)) {
			return builder.isClosed(x & 0xF, y & 0xF, z & 0xF);
		} else {
			final BlockState state = getBlockState(x, y, z);
			return state.isFullOpaque(this, searchPos.set(x, y, z));
		}
	}

	@Override
	public LightingProvider getLightingProvider() {
		return world.getLightingProvider();
	}

	@Override
	public int getColor(BlockPos blockPos, ColorResolver colorResolver) {
		final int x = blockPos.getX();
		final int z = blockPos.getZ();

		final int result = ChunkColorCache.get(getChunk(x >> 4, z >> 4)).getColor(x, blockPos.getY(), z, colorResolver);

		return result;
	}

	private static final float[] EMPTY_AO_CACHE = new float[TOTAL_CACHE_SIZE];
	private static final int[] EMPTY_LIGHT_CACHE = new int[TOTAL_CACHE_SIZE];
	private static final Object[] EMPTY_RENDER_DATA = new Object[MAIN_CACHE_SIZE];
	private static final BlockEntity[] EMPTY_BLOCK_ENTITIES = new BlockEntity[MAIN_CACHE_SIZE];

	static {
		Arrays.fill(EMPTY_AO_CACHE, Float.MAX_VALUE);
		Arrays.fill(EMPTY_LIGHT_CACHE, Integer.MAX_VALUE);
	}
}