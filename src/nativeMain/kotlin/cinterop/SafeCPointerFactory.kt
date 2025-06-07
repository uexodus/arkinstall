package cinterop

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.reflect.KClass

@OptIn(ExperimentalForeignApi::class)
interface SafeCPointerFactory<T : CPointed, N : NativeType<T>, Wrapper> {
    val pointedType: KClass<N>
    fun createOwned(cPointer: CPointer<T>): Wrapper
    fun createBorrowed(cPointer: CPointer<T>): Wrapper
}