/*******************************************************************************
 * Copyright 2019 grondag
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

package grondag.canvas.apiimpl;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

import grondag.canvas.shader.ShaderContext;
import grondag.canvas.shader.ShaderManager;
import grondag.fermion.bits.BitPacker64;
import grondag.fermion.bits.BitPacker64.BooleanElement;
import grondag.frex.api.material.MaterialCondition;
import grondag.frex.api.material.MaterialFinder;
import grondag.frex.api.material.MaterialShader;

public abstract class RenderMaterialImpl {
	private static final BitPacker64<RenderMaterialImpl> BITPACKER = new BitPacker64<>(RenderMaterialImpl::getBits, RenderMaterialImpl::setBits);
	public static final int MAX_SPRITE_DEPTH = 3;
	private static final BlendMode[] LAYERS = new BlendMode[4];

	static {
		final BlendMode[] layers = BlendMode.values();
		assert layers[0] == BlendMode.DEFAULT;
		assert layers.length == 5;

		for (int i = 0; i < 4; ++i) {
			LAYERS[i] = layers[i + 1];
		}
	}

	// Following are indexes into the array of boolean elements.
	// They are NOT the index of the bits themselves.  Used to
	// efficiently access flags based on sprite layer. "_START"
	// index is the flag for sprite layer 0, with additional layers
	// offset additively by sprite index.
	private static final int EMISSIVE_INDEX_START = 0;
	private static final int DIFFUSE_INDEX_START = EMISSIVE_INDEX_START + MAX_SPRITE_DEPTH;
	private static final int AO_INDEX_START = DIFFUSE_INDEX_START + MAX_SPRITE_DEPTH;
	private static final int CUTOUT_INDEX_START = AO_INDEX_START + MAX_SPRITE_DEPTH;
	private static final int UNMIPPED_INDEX_START = CUTOUT_INDEX_START + MAX_SPRITE_DEPTH;
	private static final int COLOR_DISABLE_INDEX_START = UNMIPPED_INDEX_START + MAX_SPRITE_DEPTH;

	@SuppressWarnings("unchecked")
	private static final BitPacker64<RenderMaterialImpl>.BooleanElement[] FLAGS = new BooleanElement[COLOR_DISABLE_INDEX_START + MAX_SPRITE_DEPTH];

	// PERF: reduce indirection
	@SuppressWarnings("unchecked")
	private static final BitPacker64<RenderMaterialImpl>.EnumElement<BlendMode> BLEND_MODES[] = new BitPacker64.EnumElement[MAX_SPRITE_DEPTH];

	private static final BitPacker64<RenderMaterialImpl>.IntElement SPRITE_DEPTH;

	private static final BitPacker64<RenderMaterialImpl>.IntElement SHADER;

	private static final BitPacker64<RenderMaterialImpl>.IntElement CONDITION;

	private static final long DEFAULT_BITS;

	private static final ObjectArrayList<CompositeMaterial> LIST = new ObjectArrayList<>();
	private static final Long2ObjectOpenHashMap<CompositeMaterial> MAP = new Long2ObjectOpenHashMap<>();

	public static final int SHADER_FLAGS_DISABLE_AO;

	static {
		// Beginning material bits are sent directly to the shader as control flags.
		// Currently 5 bits per layer
		// Bit order is optimized for shader usage. In particular, representation
		// of cutout and mipped handling is redundant of blend mode but is easier
		// to consume in the shader as simple on/off flags.
		FLAGS[EMISSIVE_INDEX_START + 0] = BITPACKER.createBooleanElement();
		FLAGS[DIFFUSE_INDEX_START + 0] = BITPACKER.createBooleanElement();
		FLAGS[AO_INDEX_START + 0] = BITPACKER.createBooleanElement();
		FLAGS[CUTOUT_INDEX_START + 0] = BITPACKER.createBooleanElement();
		FLAGS[UNMIPPED_INDEX_START + 0] = BITPACKER.createBooleanElement();

		FLAGS[EMISSIVE_INDEX_START + 1] = BITPACKER.createBooleanElement();
		FLAGS[DIFFUSE_INDEX_START + 1] = BITPACKER.createBooleanElement();
		FLAGS[AO_INDEX_START + 1] = BITPACKER.createBooleanElement();
		FLAGS[CUTOUT_INDEX_START + 1] = BITPACKER.createBooleanElement();
		FLAGS[UNMIPPED_INDEX_START + 1] = BITPACKER.createBooleanElement();

		FLAGS[EMISSIVE_INDEX_START + 2] = BITPACKER.createBooleanElement();
		FLAGS[DIFFUSE_INDEX_START + 2] = BITPACKER.createBooleanElement();
		FLAGS[AO_INDEX_START + 2] = BITPACKER.createBooleanElement();
		FLAGS[CUTOUT_INDEX_START + 2] = BITPACKER.createBooleanElement();
		FLAGS[UNMIPPED_INDEX_START + 2] = BITPACKER.createBooleanElement();

		// remaining elements are not part of shader control flags...

		FLAGS[COLOR_DISABLE_INDEX_START + 0] = BITPACKER.createBooleanElement();
		FLAGS[COLOR_DISABLE_INDEX_START + 1] = BITPACKER.createBooleanElement();
		FLAGS[COLOR_DISABLE_INDEX_START + 2] = BITPACKER.createBooleanElement();

		BLEND_MODES[0] = BITPACKER.createEnumElement(BlendMode.class);
		BLEND_MODES[1] = BITPACKER.createEnumElement(BlendMode.class);
		BLEND_MODES[2] = BITPACKER.createEnumElement(BlendMode.class);

		SPRITE_DEPTH = BITPACKER.createIntElement(1, MAX_SPRITE_DEPTH);
		SHADER = BITPACKER.createIntElement(ShaderManager.MAX_SHADERS);
		CONDITION = BITPACKER.createIntElement(MaterialConditionImpl.MAX_CONDITIONS);

		assert BITPACKER.bitLength() <= 64;

		long defaultBits = 0;
		defaultBits = BLEND_MODES[0].setValue(BlendMode.DEFAULT, defaultBits);
		defaultBits = BLEND_MODES[1].setValue(BlendMode.DEFAULT, defaultBits);
		defaultBits = BLEND_MODES[2].setValue(BlendMode.DEFAULT, defaultBits);
		DEFAULT_BITS = defaultBits;

		long aoDisableBits = 0;
		aoDisableBits = FLAGS[AO_INDEX_START + 0].setValue(true, aoDisableBits);
		aoDisableBits = FLAGS[AO_INDEX_START + 1].setValue(true, aoDisableBits);
		aoDisableBits = FLAGS[AO_INDEX_START + 2].setValue(true, aoDisableBits);
		SHADER_FLAGS_DISABLE_AO = (int)aoDisableBits;
	}

	public static RenderMaterialImpl.CompositeMaterial byIndex(int index) {
		assert index < LIST.size();
		assert index >= 0;

		return LIST.get(index);
	}

	protected long bits = DEFAULT_BITS;

	private long getBits() {
		return bits;
	}

	private void setBits(long bits) {
		this.bits = bits;
	}

	public BlendMode blendMode(int spriteIndex) {
		return BLEND_MODES[spriteIndex].getValue(this);
	}

	public boolean disableColorIndex(int spriteIndex) {
		return FLAGS[COLOR_DISABLE_INDEX_START + spriteIndex].getValue(this);
	}

	public int spriteDepth() {
		return SPRITE_DEPTH.getValue(this);
	}

	public boolean emissive(int spriteIndex) {
		return FLAGS[EMISSIVE_INDEX_START + spriteIndex].getValue(this);
	}

	public boolean disableDiffuse(int spriteIndex) {
		return FLAGS[DIFFUSE_INDEX_START + spriteIndex].getValue(this);
	}

	public boolean disableAo(int spriteIndex) {
		return FLAGS[AO_INDEX_START + spriteIndex].getValue(this);
	}

	public int shaderFlags() {
		return (int) (bits & 0xFFFF);
	}

	public static class CompositeMaterial extends RenderMaterialImpl implements RenderMaterial {
		private final int index;

		/**
		 * True if any texture wants AO shading. Simplifies check made by renderer at
		 * buffer-time.
		 */
		public final boolean hasAo;

		/**
		 * Determine which buffer we use - derived from base layer.
		 * If base layer is solid, then any additional sprites are handled
		 * as decals and render in solid pass.  If base layer is translucent
		 * then all sprite layers render as translucent.
		 */
		//public final BlendMode renderLayer;

		/**
		 * True if base layer is translucent.
		 */
		public final boolean isTranslucent;

		private final MaterialShaderImpl shader;

		private final MaterialConditionImpl condition;

		private final CompositeMaterial[] blendModeVariants = new CompositeMaterial[4];

		private final DrawableMaterial[] drawables = new DrawableMaterial[MAX_SPRITE_DEPTH];

		protected CompositeMaterial(int index, long bits, MaterialShaderImpl shader) {
			this.index = index;
			this.bits = bits;
			this.shader = shader;
			condition = MaterialConditionImpl.fromIndex(CONDITION.getValue(bits));
			hasAo = !disableAo(0) || (spriteDepth() > 1 && !disableAo(1)) || (spriteDepth() == 3 && !disableAo(2));
			final BlendMode baseLayer = blendMode(0);
			final int depth = spriteDepth();

			if(baseLayer == BlendMode.SOLID) {
				isTranslucent = false;
			} else {
				// if there is no solid base layer and any layer is translucent then whole material must render on translucent pass
				isTranslucent = (baseLayer == BlendMode.TRANSLUCENT)
						|| (depth > 1 && blendMode(1) == BlendMode.TRANSLUCENT)
						|| (depth == 3 && blendMode(2) == BlendMode.TRANSLUCENT);
			}
		}

		private static final ThreadLocal<Finder> variantFinder = ThreadLocal.withInitial(Finder::new);

		private void setupVariants() {
			boolean needsBlendModeVariant = blendMode(0) == BlendMode.DEFAULT;

			final int depth = spriteDepth();
			if(!needsBlendModeVariant && depth > 1) {
				needsBlendModeVariant = blendMode(1) == BlendMode.DEFAULT;
				if(!needsBlendModeVariant && depth == 3) {
					needsBlendModeVariant = blendMode(2) == BlendMode.DEFAULT;
				}
			}

			final Finder finder = variantFinder.get();

			if(needsBlendModeVariant) {
				for(int i = 0; i < 4; i++) {
					final BlendMode layer = LAYERS[i];

					assert layer != BlendMode.DEFAULT;

					finder.bits = bits;
					finder.shader = shader;

					if(finder.blendMode(0) == BlendMode.DEFAULT) {
						finder.blendMode(0, layer);
					}

					if(depth > 1) {
						if(finder.blendMode(1) == BlendMode.DEFAULT) {
							finder.blendMode(1, layer);
						}
						if(depth == 3 && finder.blendMode(2) == BlendMode.DEFAULT) {
							finder.blendMode(2, layer);
						}
					}

					blendModeVariants[i] = finder.findInternal(true);

					assert blendModeVariants[i].blendMode(0) !=  BlendMode.DEFAULT;
				}
			} else {
				// we are a renderable material, so set up control flags needed by shader
				for(int i = 0; i < 4; i++) {
					blendModeVariants[i] = this;
				}

				drawables[0] = new DrawableMaterial(0);

				if (depth > 1) {
					drawables[1] = new DrawableMaterial(1);

					if (depth > 2) {
						drawables[2] = new DrawableMaterial(2);
					}
				}
			}
		}

		/**
		 * If this material has one or more null blend modes, this returns
		 * a material with any such blend modes set to the given input. Typically
		 * this is only used for vanilla default materials that derive their
		 * blend mode from the block render layer, but it is also possible to
		 * specify materials with null blend modes to achieve the same behavior.<p>
		 *
		 * If a non-null blend mode is specified for every sprite layer, this
		 * will always return the current instance.<p>
		 *
		 * We need shader flags to accurately reflect the effective blend mode
		 * and we need that to be fast, and we also want the buffering logic to
		 * remain simple.  This solves all those problems.<p>
		 */
		public CompositeMaterial forBlendMode(int modeIndex) {
			assert blendModeVariants[modeIndex - 1].blendMode(0) != BlendMode.DEFAULT;
			return blendModeVariants[modeIndex - 1];
		}

		/**
		 * Returns a single-layer material appropriate for the base layer or overlay/decal layer given.
		 * @param spriteIndex
		 * @return
		 */
		public @Nullable DrawableMaterial forDepth(int spriteIndex) {
			assert spriteIndex < spriteDepth();
			return drawables[spriteIndex];
		}

		public int index() {
			return index;
		}

		public class DrawableMaterial {
			public MaterialShaderImpl shader() {
				return shader;
			}

			public MaterialConditionImpl condition() {
				return condition;
			}

			public final ShaderContext.Type shaderType;

			public DrawableMaterial(int depth) {
				shaderType = depth == 0 ? (blendMode(0) == BlendMode.TRANSLUCENT ? ShaderContext.Type.TRANSLUCENT : ShaderContext.Type.SOLID) : ShaderContext.Type.DECAL;
			}

		}
	}

	public static class Finder extends RenderMaterialImpl implements MaterialFinder {
		private MaterialShaderImpl shader = null;

		@Override
		public CompositeMaterial find() {
			return findInternal(true);
		}

		private synchronized CompositeMaterial findInternal(boolean setupVariants) {
			final MaterialShaderImpl s = shader == null ? ShaderManager.INSTANCE.getDefault() : shader;
			SHADER.setValue(s.getIndex(), this);
			CompositeMaterial result = MAP.get(bits);

			if (result == null) {
				result = new CompositeMaterial(LIST.size(), bits, s);
				LIST.add(result);
				MAP.put(result.bits, result);

				if (setupVariants) {
					result.setupVariants();
				}
			}

			assert result.blendMode(0) != BlendMode.CUTOUT || (FLAGS[CUTOUT_INDEX_START].getValue(result) &&  FLAGS[UNMIPPED_INDEX_START].getValue(result));

			return result;
		}

		@Override
		public Finder clear() {
			bits = DEFAULT_BITS;
			shader = null;
			return this;
		}

		@Override
		public Finder blendMode(int spriteIndex, BlendMode blendMode) {
			if (blendMode == null)  {
				blendMode = BlendMode.DEFAULT;
			}

			BLEND_MODES[spriteIndex].setValue(blendMode, this);

			switch(blendMode) {
			case CUTOUT:
				FLAGS[CUTOUT_INDEX_START + spriteIndex].setValue(true, this);
				FLAGS[UNMIPPED_INDEX_START + spriteIndex].setValue(true, this);
				break;
			case CUTOUT_MIPPED:
				FLAGS[CUTOUT_INDEX_START + spriteIndex].setValue(true, this);
				FLAGS[UNMIPPED_INDEX_START + spriteIndex].setValue(false, this);
				break;
			default:
				FLAGS[CUTOUT_INDEX_START + spriteIndex].setValue(false, this);
				FLAGS[UNMIPPED_INDEX_START + spriteIndex].setValue(false, this);
				break;
			}

			return this;
		}

		@Override
		public Finder disableColorIndex(int spriteIndex, boolean disable) {
			FLAGS[COLOR_DISABLE_INDEX_START + spriteIndex].setValue(disable, this);
			return this;
		}

		@Override
		public Finder spriteDepth(int depth) {
			if (depth < 1 || depth > MAX_SPRITE_DEPTH) {
				throw new IndexOutOfBoundsException("Invalid sprite depth: " + depth);
			}

			SPRITE_DEPTH.setValue(depth, this);
			return this;
		}

		@Override
		public Finder emissive(int spriteIndex, boolean isEmissive) {
			FLAGS[EMISSIVE_INDEX_START + spriteIndex].setValue(isEmissive, this);
			return this;
		}

		@Override
		public Finder disableDiffuse(int spriteIndex, boolean disable) {
			FLAGS[DIFFUSE_INDEX_START + spriteIndex].setValue(disable, this);
			return this;
		}

		@Override
		public Finder disableAo(int spriteIndex, boolean disable) {
			FLAGS[AO_INDEX_START + spriteIndex].setValue(disable, this);
			return this;
		}

		@Override
		public Finder shader(MaterialShader shader) {
			this.shader = (MaterialShaderImpl) shader;
			return this;
		}

		@Override
		public MaterialFinder condition(MaterialCondition condition) {
			CONDITION.setValue(((MaterialConditionImpl)condition).index, this);
			return this;
		}
	}
}
