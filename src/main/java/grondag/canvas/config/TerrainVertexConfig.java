package grondag.canvas.config;

import org.lwjgl.opengl.GL21;

import grondag.canvas.buffer.format.CanvasVertexFormat;
import grondag.canvas.buffer.format.CanvasVertexFormats;
import grondag.canvas.shader.GlProgram;
import grondag.canvas.texture.TextureData;
import grondag.frex.api.material.UniformRefreshFrequency;

public enum TerrainVertexConfig {
	DEFAULT(
		CanvasVertexFormats.COMPACT_MATERIAL,
		CanvasVertexFormats.COMPACT_MATERIAL.quadStrideInts,
		true
	),

	FETCH(
		CanvasVertexFormats.VF_MATERIAL,
		// VF quads use vertex stride because of indexing
		CanvasVertexFormats.VF_MATERIAL.vertexStrideInts,
		false
	) {
		@Override
		public void setupUniforms(GlProgram program) {
			program.uniformSampler("samplerBuffer", "_cvu_vfColor", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_COLOR - GL21.GL_TEXTURE0));
			program.uniformSampler("samplerBuffer", "_cvu_vfUV", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_UV - GL21.GL_TEXTURE0));
			program.uniformSampler("isamplerBuffer", "_cvu_vfVertex", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_VERTEX - GL21.GL_TEXTURE0));
			program.uniformSampler("samplerBuffer", "_cvu_vfLight", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_LIGHT - GL21.GL_TEXTURE0));
			program.uniformSampler("isamplerBuffer", "_cvu_vfQuads", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_QUADS - GL21.GL_TEXTURE0));
			program.uniformSampler("isamplerBuffer", "_cvu_vfRegions", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_REGIONS - GL21.GL_TEXTURE0));
			program.uniformSampler("usamplerBuffer", "_cvu_vfQuadRegions", UniformRefreshFrequency.ON_LOAD, u -> u.set(TextureData.VF_QUAD_REGIONS - GL21.GL_TEXTURE0));
		}
	},

	REGION(
		CanvasVertexFormats.REGION_MATERIAL,
		CanvasVertexFormats.REGION_MATERIAL.quadStrideInts,
		true
	);

	TerrainVertexConfig(
		CanvasVertexFormat vertexFormat,
		int quadStrideInts,
		boolean shouldApplyBlockPosTranslation
	) {
		this.vertexFormat = vertexFormat;
		this.quadStrideInts = quadStrideInts;
		this.shouldApplyBlockPosTranslation = shouldApplyBlockPosTranslation;
	}

	public final CanvasVertexFormat vertexFormat;

	/** Controls allocation in vertex collectors. */
	public final int quadStrideInts;

	/** If true, then vertex positions should be translated to block pos within the region. */
	public final boolean shouldApplyBlockPosTranslation;

	public void setupUniforms(GlProgram program) { }
}