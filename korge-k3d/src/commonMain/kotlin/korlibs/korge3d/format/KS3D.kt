package korlibs.korge3d.format

import korlibs.datastructure.IndexedTable
import korlibs.datastructure.fastForEach
import korlibs.memory.*
import korlibs.korge3d.Library3D
import korlibs.io.file.VfsFile
import korlibs.io.stream.MemorySyncStreamToByteArray
import korlibs.io.stream.write32LE
import korlibs.io.stream.writeBytes
import korlibs.io.stream.writeString
import korlibs.io.stream.writeStringVL
import korlibs.io.stream.writeU_VL
import korlibs.math.*

// KorGE Scene 3D file format

object KS3D {
}


suspend fun VfsFile.writeKs3d(library: Library3D) {
    val names = IndexedTable<String>()

    library.geometryDefs.fastForEach { key, value ->
        names.add(key)
    }

    writeBytes(MemorySyncStreamToByteArray {
        writeString("KS3D")
        write32LE(0) // Version
        write32LE(names.size)
        for (name in names.instances) writeStringVL(name)
        //TODO: handle mesh.indexArray
        library.geometryDefs.fastForEach { key, geom ->
            val mesh = geom.mesh
            writeU_VL(names[key])
            writeU_VL(mesh.hasTexture.toInt())
            writeU_VL(mesh.maxWeights)
            writeU_VL(mesh.vertexCount)
            mesh.vertexBuffers.fastForEach {
                writeU_VL(it.buffer.size)
                val temp = ByteArray(it.buffer.size)
                it.buffer.getArrayInt8(0, temp, 0, it.buffer.size)
                writeBytes(temp)
            }
        }
    })
}


suspend fun VfsFile.readKs3d(): Library3D {
    TODO()
}
