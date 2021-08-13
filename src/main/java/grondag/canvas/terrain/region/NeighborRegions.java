/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.terrain.region;

import java.util.function.Consumer;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

import grondag.canvas.apiimpl.util.FaceConstants;
import grondag.canvas.config.Configurator;
import grondag.canvas.perf.MicroTimer;
import grondag.canvas.terrain.occlusion.geometry.OcclusionResult;
import grondag.canvas.varia.BlockPosHelper;

/** Caches directly adjacent regions for fast access and provides visitor operations for terrain iteration. */
public class NeighborRegions {
	private final RenderRegion owner;
	private final boolean isBottom;
	private final boolean isTop;
	private final RenderRegion[] neighbors = new RenderRegion[6];

	NeighborRegions(RenderRegion owner) {
		this.owner = owner;
		BlockPos origin = owner.origin;
		ClientWorld world = owner.worldRenderState.getWorld();
		isBottom = origin.getY() == world.getBottomY();
		isTop = origin.getY() == world.getTopY() - 16;
	}

	void close() {
		for (int i = 0; i < 6; ++i) {
			final RenderRegion nr = neighbors[i];

			if (nr != null) {
				nr.neighbors.notifyNeighborClosed(BlockPosHelper.oppositeFaceIndex(i), owner);
			}
		}
	}

	public void forEachAvailable(Consumer<RenderRegion> operation) {
		operation.accept(getNeighbor(FaceConstants.EAST_INDEX));
		operation.accept(getNeighbor(FaceConstants.WEST_INDEX));
		operation.accept(getNeighbor(FaceConstants.NORTH_INDEX));
		operation.accept(getNeighbor(FaceConstants.SOUTH_INDEX));

		if (!isTop) {
			operation.accept(getNeighbor(FaceConstants.UP_INDEX));
		}

		if (!isBottom) {
			operation.accept(getNeighbor(FaceConstants.DOWN_INDEX));
		}
	}

	private RenderRegion getNeighbor(int faceIndex) {
		RenderRegion region = neighbors[faceIndex];

		if (region == null || region.isClosed()) {
			// this check is now done in all callers
			//if ((faceIndex == FaceConstants.UP_INDEX && isTop) || (faceIndex == FaceConstants.DOWN_INDEX && isBottom)) {
			//	return null;
			//}

			final Direction face = ModelHelper.faceFromIndex(faceIndex);
			BlockPos origin = owner.origin;
			region = owner.storage.getOrCreateRegion(origin.getX() + face.getOffsetX() * 16, origin.getY() + face.getOffsetY() * 16, origin.getZ() + face.getOffsetZ() * 16);
			neighbors[faceIndex] = region;
			region.neighbors.attachOrConfirmVisitingNeighbor(BlockPosHelper.oppositeFaceIndex(faceIndex), owner);
		}

		return region;
	}

	private void attachOrConfirmVisitingNeighbor(int visitingFaceIndex, RenderRegion visitingNeighbor) {
		assert neighbors[visitingFaceIndex] == null || neighbors[visitingFaceIndex] == visitingNeighbor
			: "Visting render region is attaching to a position that already has a non-null region";

		neighbors[visitingFaceIndex] = visitingNeighbor;
	}

	private void notifyNeighborClosed(int closedFaceIndex, RenderRegion closingNeighbor) {
		assert neighbors[closedFaceIndex] == closingNeighbor
			: "Closing neighbor render region does not match current attachment";

		neighbors[closedFaceIndex] = null;
	}

	// WIP: remove
	private static final MicroTimer timer = new MicroTimer("enqueu", 500000);

	//[11:28:19] [Render thread/INFO] (Canvas) Avg enqueu duration = 244 ns, min = 58, max = 135614, total duration = 122, total runs = 500,000
	//[11:28:20] [Render thread/INFO] (Canvas) Avg enqueu duration = 230 ns, min = 57, max = 38613, total duration = 115, total runs = 500,000
	//[11:28:21] [Render thread/INFO] (Canvas) Avg enqueu duration = 233 ns, min = 58, max = 57270, total duration = 116, total runs = 500,000
	//[11:28:21] [Render thread/INFO] (Canvas) Avg enqueu duration = 230 ns, min = 58, max = 67271, total duration = 115, total runs = 500,000
	//[11:28:23] [Render thread/INFO] (Canvas) Avg enqueu duration = 222 ns, min = 58, max = 45777, total duration = 111, total runs = 500,000
	//[11:28:24] [Render thread/INFO] (Canvas) Avg enqueu duration = 232 ns, min = 58, max = 42968, total duration = 116, total runs = 500,000
	//[11:28:25] [Render thread/INFO] (Canvas) Avg enqueu duration = 236 ns, min = 47, max = 65222, total duration = 118, total runs = 500,000
	//[11:28:27] [Render thread/INFO] (Canvas) Avg enqueu duration = 239 ns, min = 57, max = 44127, total duration = 119, total runs = 500,000
	//[11:28:28] [Render thread/INFO] (Canvas) Avg enqueu duration = 229 ns, min = 59, max = 38971, total duration = 114, total runs = 500,000
	//[11:28:29] [Render thread/INFO] (Canvas) Avg enqueu duration = 238 ns, min = 60, max = 46974, total duration = 119, total runs = 500,000
	//[11:28:31] [Render thread/INFO] (Canvas) Avg enqueu duration = 240 ns, min = 62, max = 33578, total duration = 120, total runs = 500,000
	//[11:28:32] [Render thread/INFO] (Canvas) Avg enqueu duration = 251 ns, min = 67, max = 25229, total duration = 125, total runs = 500,000
	//[11:28:33] [Render thread/INFO] (Canvas) Avg enqueu duration = 254 ns, min = 67, max = 30776, total duration = 127, total runs = 500,000

	//[11:35:36] [Render thread/INFO] (Canvas) Avg enqueu duration = 241 ns, min = 52, max = 40739, total duration = 120, total runs = 500,000
	//[11:35:37] [Render thread/INFO] (Canvas) Avg enqueu duration = 243 ns, min = 55, max = 31537, total duration = 121, total runs = 500,000
	//[11:35:38] [Render thread/INFO] (Canvas) Avg enqueu duration = 243 ns, min = 58, max = 43481, total duration = 121, total runs = 500,000
	//[11:35:39] [Render thread/INFO] (Canvas) Avg enqueu duration = 240 ns, min = 28, max = 32471, total duration = 120, total runs = 500,000
	//[11:35:40] [Render thread/INFO] (Canvas) Avg enqueu duration = 246 ns, min = 58, max = 40397, total duration = 123, total runs = 500,000
	//[11:35:40] [Render thread/INFO] (Canvas) Avg enqueu duration = 237 ns, min = 58, max = 42871, total duration = 118, total runs = 500,000
	//[11:35:41] [Render thread/INFO] (Canvas) Avg enqueu duration = 253 ns, min = 39, max = 31407, total duration = 126, total runs = 500,000
	//[11:35:42] [Render thread/INFO] (Canvas) Avg enqueu duration = 251 ns, min = 58, max = 49151, total duration = 125, total runs = 500,000
	//[11:35:42] [Render thread/INFO] (Canvas) Avg enqueu duration = 244 ns, min = 58, max = 78472, total duration = 122, total runs = 500,000
	//[11:35:43] [Render thread/INFO] (Canvas) Avg enqueu duration = 257 ns, min = 58, max = 31279, total duration = 128, total runs = 500,000
	//[11:35:43] [Render thread/INFO] (Canvas) Avg enqueu duration = 241 ns, min = 58, max = 27262, total duration = 120, total runs = 500,000

	//[13:15:09] [Render thread/INFO] (Canvas) Avg enqueu duration = 206 ns, min = 37, max = 28088, total duration = 103, total runs = 500,000
	//[13:15:10] [Render thread/INFO] (Canvas) Avg enqueu duration = 211 ns, min = 38, max = 43385, total duration = 105, total runs = 500,000
	//[13:15:11] [Render thread/INFO] (Canvas) Avg enqueu duration = 211 ns, min = 36, max = 45818, total duration = 105, total runs = 500,000
	//[13:15:12] [Render thread/INFO] (Canvas) Avg enqueu duration = 204 ns, min = 37, max = 29404, total duration = 102, total runs = 500,000
	//[13:15:13] [Render thread/INFO] (Canvas) Avg enqueu duration = 211 ns, min = 22, max = 31342, total duration = 105, total runs = 500,000
	//[13:15:14] [Render thread/INFO] (Canvas) Avg enqueu duration = 220 ns, min = 1, max = 90447, total duration = 110, total runs = 500,000
	//[13:15:15] [Render thread/INFO] (Canvas) Avg enqueu duration = 210 ns, min = 37, max = 24938, total duration = 105, total runs = 500,000
	//[13:15:15] [Render thread/INFO] (Canvas) Avg enqueu duration = 209 ns, min = 36, max = 29978, total duration = 104, total runs = 500,000
	//[13:15:17] [Render thread/INFO] (Canvas) Avg enqueu duration = 212 ns, min = 38, max = 40256, total duration = 106, total runs = 500,000
	//[13:15:18] [Render thread/INFO] (Canvas) Avg enqueu duration = 220 ns, min = 35, max = 38265, total duration = 110, total runs = 500,000

	/** Used in simple occlusion config. */
	public void enqueueUnvistedCameraNeighbors(final long mutalOcclusionFaceFlags) {
		assert !Configurator.advancedTerrainCulling;

		timer.start();

		final int mySquaredDist = owner.origin.squaredCameraChunkDistance();
		final int openFlags = OcclusionResult.openFacesFlag(mutalOcclusionFaceFlags, owner.cameraVisibility.entryFaceFlags());

		if ((openFlags & FaceConstants.EAST_FLAG) != 0) {
			getNeighbor(FaceConstants.EAST_INDEX).cameraVisibility.addIfFrontFacing(FaceConstants.WEST_FLAG, mySquaredDist);
		}

		if ((openFlags & FaceConstants.WEST_FLAG) != 0) {
			getNeighbor(FaceConstants.WEST_INDEX).cameraVisibility.addIfFrontFacing(FaceConstants.EAST_FLAG, mySquaredDist);
		}

		if ((openFlags & FaceConstants.NORTH_FLAG) != 0) {
			getNeighbor(FaceConstants.NORTH_INDEX).cameraVisibility.addIfFrontFacing(FaceConstants.SOUTH_FLAG, mySquaredDist);
		}

		if ((openFlags & FaceConstants.SOUTH_FLAG) != 0) {
			getNeighbor(FaceConstants.SOUTH_INDEX).cameraVisibility.addIfFrontFacing(FaceConstants.NORTH_FLAG, mySquaredDist);
		}

		if (!isTop && (openFlags & FaceConstants.UP_FLAG) != 0) {
			getNeighbor(FaceConstants.UP_INDEX).cameraVisibility.addIfFrontFacing(FaceConstants.DOWN_FLAG, mySquaredDist);
		}

		if (!isBottom && (openFlags & FaceConstants.DOWN_FLAG) != 0) {
			getNeighbor(FaceConstants.DOWN_INDEX).cameraVisibility.addIfFrontFacing(FaceConstants.UP_FLAG, mySquaredDist);
		}

		timer.stop();
	}

	/** Used in advanced occlusion config. */
	public void enqueueUnvistedCameraNeighbors() {
		assert Configurator.advancedTerrainCulling;

		final int mySquaredDist = owner.origin.squaredCameraChunkDistance();

		var region = getNeighbor(FaceConstants.EAST_INDEX);
		if (region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();
		
		region = getNeighbor(FaceConstants.WEST_INDEX);
		if (region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();
		
		region = getNeighbor(FaceConstants.NORTH_INDEX);
		if (region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();

		region = getNeighbor(FaceConstants.SOUTH_INDEX);
		if (region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();
		
		if (!isTop) {
			region = getNeighbor(FaceConstants.UP_INDEX);
			if (region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();
		}

		if (!isBottom) {
			region = getNeighbor(FaceConstants.DOWN_INDEX);
			if (region.origin.isFrontFacing(mySquaredDist)) region.cameraVisibility.addIfValid();
		}
	}

	public void enqueueUnvistedShadowNeighbors() {
		getNeighbor(FaceConstants.EAST_INDEX).shadowVisibility.addIfValid(FaceConstants.WEST_FLAG);
		getNeighbor(FaceConstants.WEST_INDEX).shadowVisibility.addIfValid(FaceConstants.EAST_FLAG);
		getNeighbor(FaceConstants.NORTH_INDEX).shadowVisibility.addIfValid(FaceConstants.SOUTH_FLAG);
		getNeighbor(FaceConstants.SOUTH_INDEX).shadowVisibility.addIfValid(FaceConstants.NORTH_FLAG);

		if (!isTop) {
			getNeighbor(FaceConstants.UP_INDEX).shadowVisibility.addIfValid(FaceConstants.DOWN_FLAG);
		}

		if (!isBottom) {
			getNeighbor(FaceConstants.DOWN_INDEX).shadowVisibility.addIfValid(FaceConstants.UP_FLAG);
		}
	}
}
