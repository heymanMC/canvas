#include canvas:shaders/api/vertex.glsl
#include canvas:shaders/api/world.glsl
#include canvas:shaders/lib/math.glsl
#include frex:shaders/lib/noise/noise4d.glsl

/******************************************************
  canvas:shaders/material/leaves.vert

  Based on "GPU-Generated Procedural Wind Animations for Trees"
  by Renaldas Zioma in GPU Gems 3, 2007
  https://developer.nvidia.com/gpugems/gpugems3/part-i-geometry/chapter-6-gpu-generated-procedural-wind-animations-trees
******************************************************/

#define NOISE_SCALE 0.125

void cv_startVertex(inout cv_VertexData data) {
	float rain = cv_rainGradient();
	float globalWind = 0.2 + rain * 0.2;

	// wind gets stronger higher in the world
	globalWind *= (0.5 + smoothstep(64.0, 255.0, data.vertex.y));

	float t = cv_renderSeconds() * 0.05;

	// NB: with batched matrix the precision seems to be off enough at
	// batch region boundaries to cause discontinuities if we don't
	// multiply the components before adding.
	// Doesn't seem like it should be that inaccurate.
	vec3 modelOrigin = cv_modelOriginWorldPos()* NOISE_SCALE;

	vec3 pos = data.vertex.xyz * NOISE_SCALE + modelOrigin;
	float wind = snoise(vec4(pos, t)) * globalWind;

	data.vertex.x += (cos(t) * cos(t * 3) * cos(t * 5) * cos(t * 7) + sin(t * 25)) * wind;
	data.vertex.z += sin(t * 19) * wind;
}
