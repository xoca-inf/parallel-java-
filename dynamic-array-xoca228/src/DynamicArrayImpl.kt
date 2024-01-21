import kotlinx.atomicfu.*

class DynamicArrayImpl<E> : DynamicArray<E> {

    private val core: AtomicRef<Core<E>>
    private val _size: AtomicInt
    private val isMoved: IsMovedOrValue<E>

    init {
        val c = Core<E>(INITIAL_CAPACITY)
        core = atomic(c)
        _size = atomic(0)
        isMoved = IsMovedOrValue()
    }

    override fun get(index: Int): E {
        require(index < _size.value) { "Wrong index" }

        while (true) {
            val old = core.value.array[index].value
            if (old == isMoved) {
                val next = core.value.next.value
                if (next != null) {
                    val v = next.array[index].value
                    if (v != null) {
                        return v.get()
                    }
                }
            } else {
                return old!!.get()
            }
        }
    }

    override fun put(index: Int, element: E) {
        require(index < _size.value) { "Wrong index" }
        while (true) {
            val old = core.value.array[index].value
            if (old == isMoved) {
                val next = core.value.next.value
                if (next != null) {
                    val v = next.array[index].value
                    if (v != null) {
                        if (next.array[index].compareAndSet(v, IsMovedOrValue(element))) {
                            return
                        }
                    }
                }
            } else {
                if (core.value.array[index].compareAndSet(old, IsMovedOrValue(element))) {
                    return
                }
            }
        }
    }

    override fun pushBack(element: E) {
        loop1@ while (true) {
            val curSz = _size.value
            val curCap = core.value.capacity.value

            if (curSz + 1 >= curCap) {
                while (true) {
                    if (core.value.next.compareAndSet(null, Core(curCap * 2))) {
                        if (core.value.capacity.value != curCap) {
                            core.value.next.getAndSet(null)
                            continue@loop1
                        }
                        val newArr = core.value.next.value!!
                        for (i in 0 until curSz) {
                            while (true) {
                                val v = core.value.array[i].value
                                if (core.value.array[i].compareAndSet(v, isMoved)) {
                                    newArr.array[i].compareAndSet(null, v)
                                    break
                                } else {
                                    continue
                                }
                            }
                        }
                        core.getAndSet(newArr)
                    } else {
                        continue@loop1
                    }
                }
            }

            if (core.value.array[curSz].compareAndSet(null, IsMovedOrValue(element))) {
                _size.getAndIncrement()
                return
            }
        }
    }

    override val size: Int get() {
        return _size.value
    }
}

class IsMovedOrValue<E> {
    private var value: E?

    constructor() {
        value = null
    }

    constructor(v: E) {
        value = v
    }

    fun get(): E {
        return value!!
    }
}

private class Core<E>(capacity: Int) {
    val capacity = atomic(capacity)
    val array = atomicArrayOfNulls<IsMovedOrValue<E>>(capacity)
    val next: AtomicRef<Core<E>?> = atomic(null)
}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME