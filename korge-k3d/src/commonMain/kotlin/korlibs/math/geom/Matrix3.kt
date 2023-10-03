package korlibs.math.geom

import kotlin.math.*

data class Matrix3 private constructor(
    private val data: FloatArray,
) {
    private constructor(
        v00: Float, v10: Float, v20: Float,
        v01: Float, v11: Float, v21: Float,
        v02: Float, v12: Float, v22: Float,
    ) : this(
        floatArrayOf(
            v00, v10, v20,
            v01, v11, v21,
            v02, v12, v22,
        )
    )

    init {
        check(data.size == 9)
    }

    val v00: Float get() = data[0];
    val v10: Float get() = data[1];
    val v20: Float get() = data[2];
    val v01: Float get() = data[3];
    val v11: Float get() = data[4];
    val v21: Float get() = data[5];
    val v02: Float get() = data[6];
    val v12: Float get() = data[7];
    val v22: Float get() = data[8];

    val c0: Vector3 get() = Vector3.fromArray(data, 0)
    val c1: Vector3 get() = Vector3.fromArray(data, 4)
    val c2: Vector3 get() = Vector3.fromArray(data, 8)
    fun c(column: Int): Vector3 {
        if (column < 0 || column >= 3) error("Invalid column $column")
        return Vector3.fromArray(data, column * 3)
    }

    val r0: Vector3 get() = Vector3(v00, v01, v02)
    val r1: Vector3 get() = Vector3(v10, v11, v12)
    val r2: Vector3 get() = Vector3(v20, v21, v22)

    fun r(row: Int): Vector3 = when (row) {
        0 -> r0
        1 -> r1
        2 -> r2
        else -> error("Invalid row $row")
    }

    operator fun get(row: Int, column: Int): Float {
        if (column !in 0..2 || row !in 0..2) error("Invalid index $row,$column")
        return data[row * 3 + column]
    }

    fun transform(v: Vector3): Vector3 = Vector3(r0.dot(v), r1.dot(v), r2.dot(v))

    fun inverted(): Matrix3 {
        val determinant = v00 * (v11 * v22 - v21 * v12) -
            v01 * (v10 * v22 - v12 * v20) +
            v02 * (v10 * v21 - v11 * v20)

        if (determinant == 0.0f) throw ArithmeticException("Matrix is not invertible.")

        val invDet = 1.0f / determinant

        return fromRows(
            (v11 * v22 - v21 * v12) * invDet,
            (v02 * v21 - v01 * v22) * invDet,
            (v01 * v12 - v02 * v11) * invDet,
            (v12 * v20 - v10 * v22) * invDet,
            (v00 * v22 - v02 * v20) * invDet,
            (v10 * v02 - v00 * v12) * invDet,
            (v10 * v21 - v20 * v11) * invDet,
            (v20 * v01 - v00 * v21) * invDet,
            (v00 * v11 - v10 * v01) * invDet,
        )
    }

    override fun toString(): String = buildString {
        append("Matrix3(\n")
        for (row in 0 until 3) {
            append("  [ ")
            for (col in 0 until 3) {
                if (col != 0) append(", ")
                val v = get(row, col)
                if (floor(v) == v) append(v.toInt()) else append(v)
            }
            append(" ],\n")
        }
        append(")")
    }

    fun transposed(): Matrix3 = Matrix3.fromColumns(r0, r1, r2)

    companion object {
        fun fromRows(
            r0: Vector3, r1: Vector3, r2: Vector3
        ): Matrix3 = Matrix3(
            r0.x, r1.x, r2.x,
            r0.y, r1.y, r2.y,
            r0.z, r1.z, r2.z,
        )

        fun fromColumns(
            c0: Vector3, c1: Vector3, c2: Vector3
        ): Matrix3 = Matrix3(
            c0.x, c0.y, c0.z,
            c1.x, c1.y, c1.z,
            c2.x, c2.y, c2.z,
        )

        fun fromColumns(
            v00: Float, v10: Float, v20: Float,
            v01: Float, v11: Float, v21: Float,
            v02: Float, v12: Float, v22: Float,
        ): Matrix3 = Matrix3(
            v00, v10, v20,
            v01, v11, v21,
            v02, v12, v22,
        )

        fun fromRows(
            v00: Float, v01: Float, v02: Float,
            v10: Float, v11: Float, v12: Float,
            v20: Float, v21: Float, v22: Float,
        ): Matrix3 = Matrix3(
            v00, v10, v20,
            v01, v11, v21,
            v02, v12, v22,
        )
    }
}

fun Vector3F.Companion.fromArray(array: FloatArray, offset: Int): Vector3 =
    Vector3F(array[offset + 0], array[offset + 1], array[offset + 2])

fun Matrix4.extract3x3(): Matrix3 = Matrix3.fromRows(
    v00, v01, v02,
    v10, v11, v12,
    v20, v21, v22
)

fun Quaternion.toRotation3x3Matrix(): Matrix3 = Matrix3.fromColumns(
    1 - 2 * y.sq - 2 * z.sq, 2 * x * y - 2 * z * w, 2 * x * z + 2 * y * w,
    2 * x * y + 2 * z * w, 1 - 2 * x.sq - 2 * z.sq, 2 * y * z - 2 * x * w,
    2 * x * z - 2 * y * w, 2 * y * z + 2 * x * w, 1 - 2 * x.sq - 2 * y.sq
)

private val Float.sq: Float get() = this * this
