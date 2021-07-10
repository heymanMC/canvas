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

import grondag.canvas.buffer.VboBuffer;
import grondag.canvas.buffer.encoding.VertexCollectorList;
import grondag.canvas.render.region.DrawableRegion;
import grondag.canvas.render.region.UploadableRegion;

public class VsUploadableRegion implements UploadableRegion {
	protected final VboBuffer vboBuffer;
	protected final DrawableRegion drawable;

	public VsUploadableRegion(VertexCollectorList collectorList, boolean sorted, int bytes, long packedOriginBlockPos) {
		vboBuffer = new VboBuffer(bytes, VsFormat.VS_MATERIAL);
		drawable = VsDrawableRegion.pack(collectorList, vboBuffer, sorted, bytes, packedOriginBlockPos);
	}

	@Override
	public DrawableRegion produceDrawable() {
		vboBuffer.upload();
		return drawable;
	}
}