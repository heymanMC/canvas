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

package grondag.canvas.render.region.vs;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.render.VertexFormat.DrawMode;

import grondag.canvas.varia.GFX;

public class ClumpedDrawListClump {
	private final ObjectArrayList<ClumpedDrawableStorage> stores = new ObjectArrayList<>();

	public void draw(int elementType) {
		final int limit = stores.size();

		for (int regionIndex = 0; regionIndex < limit; ++regionIndex) {
			ClumpedDrawableStorage store = stores.get(regionIndex);
			GFX.drawElementsBaseVertex(DrawMode.QUADS.mode, store.triVertexCount, elementType, 0L, store.baseVertex());
		}
	}

	public void add(ClumpedDrawableStorage storage) {
		stores.add(storage);
	}

	public void bind() {
		stores.get(0).getClump().bind();
	}
}
