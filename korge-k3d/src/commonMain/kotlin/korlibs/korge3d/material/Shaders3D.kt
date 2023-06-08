package korlibs.korge3d.material

import korlibs.graphics.*
import korlibs.graphics.shader.*
import korlibs.graphics.shader.gl.*

open class Shaders3D {

	//@ThreadLocal
	private val programCache = LinkedHashMap<String, Program>()

	var printShaders = false
    //var printShaders = true

	@Suppress("RemoveCurlyBracesFromTemplate")
	fun getProgram3D(nlights: Int, nweights: Int, meshMaterial: PBRMaterial3D?, hasTexture: Boolean): Program {
		return programCache.getOrPut("program_L${nlights}_W${nweights}_M${meshMaterial?.kind}_T${hasTexture}") {
			StandardShader3D(nlights, nweights, meshMaterial, hasTexture).program.apply {
				if (printShaders) {
					println(GlslGenerator(ShaderType.VERTEX, GlslConfig(GLVariant.DESKTOP_GENERIC, AGFeatures.Mutable(isUniformBuffersSupported = true))).generate(this.vertex))
					println(GlslGenerator(ShaderType.FRAGMENT, GlslConfig(GLVariant.DESKTOP_GENERIC, AGFeatures.Mutable(isUniformBuffersSupported = true))).generate(this.fragment))
				}
			}
		}
	}


    object K3DPropsUB : UniformBlock(fixedLocation = 1) {
        val u_Shininess by float()
        val u_IndexOfRefraction by float()
        val u_AmbientColor by vec4()
        val u_OcclusionStrength by float()
        val u_BindShapeMatrix by mat4()
        val u_BindShapeMatrixInv by mat4()
        val u_ModMat by mat4()
        val u_NormMat by mat4()
    }

    object Bones4UB : UniformBlock(fixedLocation = 2) {
        //val MAX_BONE_MATS = 16
        val MAX_BONE_MATS = 64

        val u_BoneMat0 by mat4()
        val u_BoneMat1 by mat4()
        val u_BoneMat2 by mat4()
        val u_BoneMat3 by mat4()
        val u_BoneMats = arrayOf(u_BoneMat0, u_BoneMat1, u_BoneMat2, u_BoneMat3)
    }

    companion object {
        val u_ProjMat = DefaultShaders.u_ProjMat
        val u_ViewMat = DefaultShaders.u_ViewMat

		val a_pos = Attribute("a_Pos", VarType.Float3, normalized = false, precision = Precision.HIGH, fixedLocation = 0)
		val a_norm = Attribute("a_Norm", VarType.Float3, normalized = false, precision = Precision.LOW, fixedLocation = 1)
		val a_tex = Attribute("a_TexCoords", VarType.Float2, normalized = false, precision = Precision.MEDIUM, fixedLocation = 2)
        val a_col = Attribute("a_Col", VarType.Float3, normalized = true, Precision.LOW, fixedLocation = 3)
		val a_boneIndex = Array(4) { Attribute("a_BoneIndex$it", VarType.Float4, normalized = false, fixedLocation = 4 + it) }
		val a_weight = Array(4) { Attribute("a_Weight$it", VarType.Float4, normalized = false, fixedLocation = 8 + it) }
		val v_col = Varying("v_Col", VarType.Float3)

		val v_Pos = Varying("v_Pos", VarType.Float3, precision = Precision.HIGH)
		val v_Norm = Varying("v_Norm", VarType.Float3, precision = Precision.HIGH)
		val v_TexCoords = Varying("v_TexCoords", VarType.Float2, precision = Precision.MEDIUM)

		val v_Temp1 = Varying("v_Temp1", VarType.Float4)

		val PROGRAM_COLOR_3D = Program(
			vertex = VertexShader {
				SET(v_col, a_col)
				SET(out, u_ProjMat * K3DPropsUB.u_ModMat * u_ViewMat * vec4(a_pos, 1f.lit))
			},
			fragment = FragmentShader {
				SET(out, vec4(v_col, 1f.lit))
				//SET(out, vec4(1f.lit, 1f.lit, 1f.lit, 1f.lit))
			},
			name = "programColor3D"
		)

        val PROGRAM_DEBUG_COLOR_3D = Program(
            vertex = VertexShader {
                SET(out, u_ProjMat * K3DPropsUB.u_ModMat * u_ViewMat * vec4(a_pos, 1f.lit))
            },
            fragment = FragmentShader {
                SET(out, vec4(1f.lit, 0f.lit, 1f.lit, 1f.lit))
                //SET(out, vec4(1f.lit, 1f.lit, 1f.lit, 1f.lit))
            },
            name = "programColor3D"
        )

		val lights = (0 until 4).map { LightUB(it, 6 + it) }

		val emission = MaterialUB("emission", 2, 1)
		val ambient = MaterialUB("ambient", 3, 2)
		val diffuse = MaterialUB("diffuse", 4, 3)
		val specular = MaterialUB("specular", 5, 4)
        val u_OcclussionTexUnit by Sampler("u_texUnit_occlusion", 5, SamplerVarType.Sampler2D)


		val layoutPosCol = VertexLayout(a_pos, a_col)

		private val FLOATS_PER_VERTEX = layoutPosCol.totalSize / Int.SIZE_BYTES /*Float.SIZE_BYTES is not defined*/
	}

    data class LightUB(val index: Int, val fixLocation: Int) : UniformBlock(fixedLocation = fixLocation) {
        //inner class Light()
        val u_SourcePos by vec4("u_lightPos${index}")
        val u_Color by vec4("u_lightColor${index}")
        val u_Attenuation by vec4("u_lightAttenuation${index}")
    }

    data class MaterialUB(val kind: String, val fixLocation: Int, val textureIndex: Int) : UniformBlock(fixedLocation = fixLocation) {
        val u_color by vec4("u_${kind}_color")
        val u_texUnit by Sampler("u_${kind}_texUnit", textureIndex, SamplerVarType.Sampler2D)
        //val u_texUnit by int("u_${kind}_texUnit")
    }

	//class MaterialLightUniform(val kind: String) {
	//	//val mat = Material3D
	//	val u_color = Uniform("u_${kind}_color", VarType.Float4)
	//	val u_texUnit = Uniform("u_${kind}_texUnit", VarType.Sampler2D)
	//}
}


data class StandardShader3D(
    override val nlights: Int,
    override val nweights: Int,
    override val meshMaterial: PBRMaterial3D?,
    override val hasTexture: Boolean
) : AbstractStandardShader3D()


abstract class AbstractStandardShader3D() : BaseShader3D() {
	abstract val nlights: Int
	abstract val nweights: Int
	abstract val meshMaterial: PBRMaterial3D?
	abstract val hasTexture: Boolean

	override fun Program.Builder.vertex() = Shaders3D.run {
		val modelViewMat = createTemp(VarType.Mat4)
		val normalMat = createTemp(VarType.Mat4)

		val boneMatrix = createTemp(VarType.Mat4)

		val localPos = createTemp(VarType.Float4)
		val localNorm = createTemp(VarType.Float4)
		val skinPos = createTemp(VarType.Float4)

		//if (nweights == 0) {
			//SET(boneMatrix, mat4Identity())
			SET(localPos, vec4(a_pos, 1f.lit))
			SET(localNorm, vec4(a_norm, 0f.lit))
		//} else {
		//	SET(localPos, vec4(0f.lit))
		//	SET(localNorm, vec4(0f.lit))
		//	SET(skinPos, u_BindShapeMatrix * vec4(a_pos["xyz"], 1f.lit))
		//	for (wIndex in 0 until nweights) {
		//		IF(getBoneIndex(wIndex) ge 0.lit) {
		//			SET(boneMatrix, getBone(wIndex))
		//			SET(localPos, localPos + boneMatrix * vec4(skinPos["xyz"], 1f.lit) * getWeight(wIndex))
		//			SET(localNorm, localNorm + boneMatrix * vec4(a_norm, 0f.lit) * getWeight(wIndex))
		//		}
		//	}
		//	SET(localPos, u_BindShapeMatrixInv * localPos)
		//}

		SET(modelViewMat, u_ViewMat * Shaders3D.K3DPropsUB.u_ModMat)
		SET(normalMat, Shaders3D.K3DPropsUB.u_NormMat)
		SET(v_Pos, vec3(modelViewMat * (vec4(localPos["xyz"], 1f.lit))))
		SET(v_Norm, vec3(normalMat * normalize(vec4(localNorm["xyz"], 1f.lit))))
		if (hasTexture) SET(v_TexCoords, vec2(a_tex["x"], a_tex["y"]))
		SET(out, u_ProjMat * vec4(v_Pos, 1f.lit))
	}

	override fun Program.Builder.fragment() {
		val meshMaterial = meshMaterial
		if (meshMaterial != null) {
			computeMaterialLightColor(out, Shaders3D.diffuse, meshMaterial.diffuse)
		} else {
			SET(out, vec4(.5f.lit, .5f.lit, .5f.lit, 1f.lit))
		}

        //println("nlights=$nlights")
		for (n in 0 until nlights) {
			addLight(Shaders3D.lights[n], out)
		}

        if (meshMaterial != null) {
            if (meshMaterial.occlusionTexture != null) {
                // https://github.com/KhronosGroup/glTF/blob/main/specification/2.0/Specification.adoc#double-sided:~:text=The%20occlusion%20texture%3B%20it%20indicates%20areas%20that%20receive%20less%20indirect%20lighting
                // 1.0 + strength * (occlusionTexture - 1.0)

                SET(out["rgb"], out["rgb"] * (1f.lit + Shaders3D.K3DPropsUB.u_OcclusionStrength * (texture2D(Shaders3D.u_OcclussionTexUnit, Shaders3D.v_TexCoords["xy"]).r - 1f.lit)))
                //SET(out["rgb"], vec3(1f.lit, 1f.lit, 1f.lit) * texture2D(Shaders3D.u_OcclussionTexUnit, Shaders3D.v_TexCoords["xy"])["r"])
            }
        }
		//SET(out, vec4(v_Temp1.x, v_Temp1.y, v_Temp1.z, 1f.lit))
	}

	open fun Program.Builder.computeMaterialLightColor(out: Operand, uniform: Shaders3D.MaterialUB, light: PBRMaterial3D.Light) {
		when (light) {
			is PBRMaterial3D.LightColor -> {
				SET(out, uniform.u_color)
			}
			is PBRMaterial3D.LightTexture -> {
				SET(out, vec4(texture2D(uniform.u_texUnit, Shaders3D.v_TexCoords["xy"])["rgb"], 1f.lit))
			}
			else -> error("Unsupported MateriaList: $light")
		}
	}

	fun Program.Builder.mat4Identity() = Program.Func("mat4",
		1f.lit, 0f.lit, 0f.lit, 0f.lit,
		0f.lit, 1f.lit, 0f.lit, 0f.lit,
		0f.lit, 0f.lit, 1f.lit, 0f.lit,
		0f.lit, 0f.lit, 0f.lit, 1f.lit
	)


	open fun Program.Builder.addLight(light: Shaders3D.LightUB, out: Operand) {
		val v = Shaders3D.v_Pos
		val N = Shaders3D.v_Norm

		val L = createTemp(VarType.Float3)
		val E = createTemp(VarType.Float3)
		val R = createTemp(VarType.Float3)

		val attenuation = createTemp(VarType.Float1)
		val dist = createTemp(VarType.Float1)
		val NdotL = createTemp(VarType.Float1)
		val lightDir = createTemp(VarType.Float3)

		SET(L, normalize(light.u_SourcePos["xyz"] - v))
		SET(E, normalize(-v)) // we are in Eye Coordinates, so EyePos is (0,0,0)
		SET(R, normalize(-reflect(L, N)))

		val constantAttenuation = light.u_Attenuation.x
		val linearAttenuation = light.u_Attenuation.y
		val quadraticAttenuation = light.u_Attenuation.z
		SET(lightDir, light.u_SourcePos["xyz"] - Shaders3D.v_Pos)
		SET(dist, length(lightDir))
		//SET(dist, length(vec3(4f.lit, 1f.lit, 6f.lit) - vec3(0f.lit, 0f.lit, 0f.lit)))

		SET(attenuation, 1f.lit / (constantAttenuation + linearAttenuation * dist + quadraticAttenuation * dist * dist))
		//SET(attenuation, 1f.lit / (1f.lit + 0f.lit * dist + 0.00111109f.lit * dist * dist))
		//SET(attenuation, 0.9.lit)
		SET(NdotL, max(dot(normalize(N), normalize(lightDir)), 0f.lit))

		IF(NdotL ge 0f.lit) {
			SET(out["rgb"], out["rgb"] + (light.u_Color["rgb"] * NdotL + Shaders3D.K3DPropsUB.u_AmbientColor["rgb"]) * attenuation * Shaders3D.K3DPropsUB.u_Shininess)
		}
		//SET(out["rgb"], out["rgb"] * attenuation)
		//SET(out["rgb"], out["rgb"] + clamp(light.diffuse * max(dot(N, L), 0f.lit), 0f.lit, 1f.lit)["rgb"])
		//SET(out["rgb"], out["rgb"] + clamp(light.specular * pow(max(dot(R, E), 0f.lit), 0.3f.lit * u_Shininess), 0f.lit, 1f.lit)["rgb"])
	}

	fun Program.Builder.getBoneIndex(index: Int): Operand = int(Shaders3D.a_boneIndex[index / 4][index % 4])
	fun Program.Builder.getWeight(index: Int): Operand = Shaders3D.a_weight[index / 4][index % 4]
	fun Program.Builder.getBone(index: Int): Operand = Shaders3D.Bones4UB.u_BoneMats[index]
}


abstract class BaseShader3D {
	abstract fun Program.Builder.vertex()
	abstract fun Program.Builder.fragment()
	val program by lazy {
		Program(
			vertex = VertexShader { vertex() },
			fragment = FragmentShader { fragment() },
			name = this@BaseShader3D.toString()
		)
	}
}

private fun Program.Builder.transpose(a: Operand) = Program.Func("transpose", a)
private fun Program.Builder.inverse(a: Operand) = Program.Func("inverse", a)
private fun Program.Builder.int(a: Operand) = Program.Func("int", a)
private operator fun Operand.get(index: Operand) = Program.ArrayAccess(this, index)
