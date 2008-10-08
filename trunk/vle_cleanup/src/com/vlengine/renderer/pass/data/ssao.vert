
varying vec2 texCoord;

void main(void) {
   vec2 Pos = sign(gl_Vertex.xy);
   gl_Position = vec4(Pos.xy, 0, 1);
   texCoord.x = 0.5 * (1.0 + Pos.x);
   texCoord.y = 0.5 * (1.0 + Pos.y);

	//gl_Position = ftransform();
	//texCoord=gl_MultiTexCoord0.xy;
	//gl_FrontColor = gl_Color;
}