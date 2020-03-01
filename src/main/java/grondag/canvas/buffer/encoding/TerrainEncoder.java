package grondag.canvas.buffer.encoding;

import net.minecraft.client.util.math.Matrix4f;
import net.minecraft.client.util.math.Vector4f;

import grondag.canvas.apiimpl.mesh.MutableQuadViewImpl;
import grondag.canvas.apiimpl.rendercontext.AbstractRenderContext;
import grondag.canvas.apiimpl.util.ColorHelper;
import grondag.canvas.buffer.packing.VertexCollectorImpl;
import grondag.canvas.material.MaterialVertexFormats;
import grondag.canvas.mixinterface.Matrix3fExt;

public class TerrainEncoder extends VertexEncoder {
	TerrainEncoder() {
		super(MaterialVertexFormats.VANILLA_BLOCKS_AND_ITEMS);
	}

	static final TerrainEncoder INSTANCE = new TerrainEncoder();

	@Override
	protected void bufferQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {
		final Matrix4f matrix = context.matrix();
		final Matrix3fExt normalMatrix = context.normalMatrix();
		final VertexCollectorImpl buff = (VertexCollectorImpl) context.consumer(quad);
		final int[] appendData = buff.appendData;

		int packedNormal = 0;
		int transformedNormal = 0;
		final boolean useNormals = quad.hasVertexNormals();

		if (useNormals) {
			quad.populateMissingNormals();
		} else {
			packedNormal = quad.packedFaceNormal();
			transformedNormal = normalMatrix.canvas_transform(packedNormal);
		}

		int k = 0;
		for (int i = 0; i < 4; i++) {
			// PERF: this is BS
			final Vector4f vector4f = new Vector4f(quad.x(i), quad.y(i), quad.z(i), 1.0F);
			vector4f.transform(matrix);
			appendData[k++] = Float.floatToRawIntBits(vector4f.getX());
			appendData[k++] = Float.floatToRawIntBits(vector4f.getY());
			appendData[k++] = Float.floatToRawIntBits(vector4f.getZ());

			appendData[k++] = quad.spriteColor(i, 0);

			appendData[k++] = Float.floatToRawIntBits(quad.spriteU(i, 0));
			appendData[k++] = Float.floatToRawIntBits(quad.spriteV(i, 0));

			appendData[k++] = quad.lightmap(i);

			if (useNormals) {
				final int p = quad.packedNormal(i);

				if (p != packedNormal) {
					packedNormal = p;
					transformedNormal = normalMatrix.canvas_transform(packedNormal);
				}
			}

			appendData[k++] = transformedNormal;
		}

		buff.add(appendData, k);
	}

	/** handles block color and red-blue swizzle, common to all renders. */
	@Override
	protected void colorizeQuad(MutableQuadViewImpl quad, AbstractRenderContext context) {

		final int colorIndex = quad.colorIndex();

		// TODO: handle layers

		if (colorIndex == -1 || quad.material().disableColorIndex(0)) {
			for (int i = 0; i < 4; i++) {
				quad.spriteColor(i, 0, ColorHelper.swapRedBlueIfNeeded(quad.spriteColor(i, 0)));
			}
		} else {
			final int indexedColor = context.indexedColor(colorIndex);

			for (int i = 0; i < 4; i++) {
				quad.spriteColor(i, 0, ColorHelper.swapRedBlueIfNeeded(ColorHelper.multiplyColor(indexedColor, quad.spriteColor(i, 0))));
			}
		}
	}
}
