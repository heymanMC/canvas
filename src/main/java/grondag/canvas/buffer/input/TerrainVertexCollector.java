/*
 * Copyright © Original Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.buffer.input;

import java.nio.IntBuffer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import io.vram.frex.api.model.util.FaceUtil;

import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.terrain.TerrainSectorMap.RegionRenderSector;

public class TerrainVertexCollector extends BaseVertexCollector {
	protected final DrawableVertexCollector[] collectors;

	public TerrainVertexCollector(RenderState renderState, int[] target) {
		super(renderState, target);

		collectors = new DrawableVertexCollector[FaceUtil.FACE_INDEX_COUNT];

		for (int i = 0; i < FaceUtil.FACE_INDEX_COUNT; ++i) {
			collectors[i] = createCollector(renderState, target);
		}
	}

	protected DrawableVertexCollector createCollector(RenderState renderState, int[] target) {
		return new SimpleVertexCollector(renderState, target);
	}

	@Override
	public void commit(int effectiveFaceIndex, boolean castShadow) {
		collectors[effectiveFaceIndex].commit(castShadow);
		integerSize += quadStrideInts;
	}

	@Override
	public final void clear() {
		integerSize = 0;

		for (int i = 0; i < FaceUtil.FACE_INDEX_COUNT; ++i) {
			collectors[i].clear();
		}
	}

	@Override
	public FaceBucket[] faceBuckets() {
		final FaceBucket[] result = new FaceBucket[FaceUtil.FACE_INDEX_COUNT];
		int index = 0;

		for (int i = 0; i < FaceUtil.FACE_INDEX_COUNT; ++i) {
			result[i] = collectors[i].faceBucket(index);
			index += collectors[i].vertexCount();
		}

		return result;
	}

	@Override
	public void commit(int size) {
		throw new UnsupportedOperationException("Commit on compound collector must provide faceIndex and castShadow");
	}

	@Override
	public void commit(boolean castShadow) {
		throw new UnsupportedOperationException("Commit on compound collector must provide faceIndex and castShadow");
	}

	@Override
	public void toBuffer(IntBuffer intBuffer) {
		throw new UnsupportedOperationException("Terrain buffering should always use transfer buffers.");
	}

	@Override
	public void toBuffer(TransferBuffer targetBuffer, int bufferTargetIndex) {
		for (int i = 0; i < FaceUtil.FACE_INDEX_COUNT; ++i) {
			final var c = collectors[i];

			if (!c.isEmpty()) {
				collectors[i].toBuffer(targetBuffer, bufferTargetIndex);
				bufferTargetIndex += collectors[i].integerSize();
			}
		}
	}

	@Override
	public void sortIfNeeded() {
		// NOOP
	}

	@Override
	public boolean sorted() {
		return false;
	}

	@Override
	public boolean sortTerrainQuads(Vec3 sortPos, RegionRenderSector sector) {
		throw new UnsupportedOperationException("Compound vertex collector does not support sortTerrainQuads.");
	}

	@Override
	public @Nullable int[] saveState(@Nullable int[] translucentState) {
		throw new UnsupportedOperationException("Compound vertex collector does not support saveState.");
	}

	@Override
	public void loadState(int[] state) {
		throw new UnsupportedOperationException("Compound vertex collector does not support loadState");
	}

	@Override
	public FaceBucket faceBucket(int index) {
		throw new UnsupportedOperationException("Compound vertex collector does not support faceBucket");
	}
}
