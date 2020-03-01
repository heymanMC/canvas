/*******************************************************************************
 * Copyright 2019, 2020 grondag
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


package grondag.canvas.apiimpl.rendercontext;

import java.util.Random;
import java.util.function.Consumer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.buffer.packing.VertexCollectorList;
import grondag.canvas.material.MaterialContext;
import grondag.canvas.mixinterface.Matrix3fExt;

public abstract class AbstractRenderContext implements RenderContext {
	public final VertexCollectorList collectors = new VertexCollectorList();
	private final ObjectArrayList<QuadTransform> transformStack = new ObjectArrayList<>();
	private static final QuadTransform NO_TRANSFORM = (q) -> true;
	protected Matrix4f matrix;
	protected Matrix3fExt normalMatrix;
	protected int overlay;

	private final QuadTransform stackTransform = (q) -> {
		int i = transformStack.size() - 1;

		while (i >= 0) {
			if (!transformStack.get(i--).transform(q)) {
				return false;
			}
		}

		return true;
	};

	private QuadTransform activeTransform = NO_TRANSFORM;

	protected final boolean transform(MutableQuadView q) {
		return activeTransform.transform(q);
	}

	protected boolean hasTransform() {
		return activeTransform != NO_TRANSFORM;
	}

	@Override
	public void pushTransform(QuadTransform transform) {
		if (transform == null) {
			throw new NullPointerException("Renderer received null QuadTransform.");
		}

		transformStack.push(transform);

		if (transformStack.size() == 1) {
			activeTransform = transform;
		} else if (transformStack.size() == 2) {
			activeTransform = stackTransform;
		}
	}

	@Override
	public void popTransform() {
		transformStack.pop();

		if (transformStack.size() == 0) {
			activeTransform = NO_TRANSFORM;
		} else if (transformStack.size() == 1) {
			activeTransform = transformStack.get(0);
		}
	}

	protected final MeshConsumer meshConsumer = new MeshConsumer(this);

	@Override
	public final Consumer<Mesh> meshConsumer() {
		return meshConsumer;
	}

	protected final FallbackConsumer fallbackConsumer = new FallbackConsumer(this);

	@Override
	public final Consumer<BakedModel> fallbackConsumer() {
		return fallbackConsumer;
	}

	@Override
	public final QuadEmitter getEmitter() {
		return meshConsumer.getEmitter();
	}

	protected abstract boolean cullTest(Direction face);

	protected abstract Random random();

	protected abstract boolean defaultAo();

	protected abstract BlockState blockState();

	public abstract MaterialContext materialContext();

	public abstract Matrix4f matrix();

	public abstract Matrix3fExt normalMatrix();

	public abstract int overlay();

	public abstract VertexConsumer consumer(MutableQuadViewImpl quad);

	public abstract int indexedColor(int colorIndex);

	public abstract void applyLighting(MutableQuadViewImpl quad);

	public abstract void computeLighting(MutableQuadViewImpl quad);
}
