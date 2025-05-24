package cinterop.types

import cinterop.NativeType
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.LongVar

@OptIn(ExperimentalForeignApi::class)
object NativeIntVar : NativeType<IntVar>

@OptIn(ExperimentalForeignApi::class)
object NativeLongVar : NativeType<LongVar>

@OptIn(ExperimentalForeignApi::class)
object NativeByteVar : NativeType<ByteVar>

@OptIn(ExperimentalForeignApi::class)
object NativeBooleanVar : NativeType<BooleanVar>

@OptIn(ExperimentalForeignApi::class)
object NativeFloatVar : NativeType<FloatVar>

@OptIn(ExperimentalForeignApi::class)
object NativeDoubleVar : NativeType<DoubleVar>

@OptIn(ExperimentalForeignApi::class)
object NativeCPointerVarByteVar : NativeType<CPointerVar<ByteVar>>
