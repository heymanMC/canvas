#include canvas:shaders/internal/process/header.glsl

/******************************************************
  canvas:shaders/internal/process/bloom.vert
******************************************************/
uniform sampler2D _cvu_base;
uniform sampler2D _cvu_bloom0;
uniform sampler2D _cvu_bloom1;
uniform sampler2D _cvu_bloom2;
uniform ivec2 _cvu_size;

attribute vec2 in_uv;

varying vec2 _cvv_texcoord;

void main() {
	vec4 outPos = gl_ProjectionMatrix * vec4(gl_Vertex.xy * _cvu_size, 0.0, 1.0);
	gl_Position = vec4(outPos.xy, 0.2, 1.0);
	_cvv_texcoord = in_uv;
}