import kotlinx.atomicfu.*

class FAAQueue<T> {
    private val head: AtomicRef<Segment> // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private val tail: AtomicRef<Segment> // Tail pointer, similarly to the Michael-Scott queue

    init {
        val firstNode = Segment()
        head = atomic(firstNode)
        tail = atomic(firstNode)
    }

    /**
     * Adds the specified element [x] to the queue.
     */
    fun enqueue(x: T) {
        while (true) {
            val t = tail.value
            val enqIdx = t.enqIdx.getAndIncrement()
            if (enqIdx >= SEGMENT_SIZE) {
                val newTail = Segment(x)
                if (t.next.compareAndSet(null, newTail)) {
                    tail.compareAndSet(t, newTail)
                    return
                } else {
                    tail.compareAndSet(t, t.next.value!!)
                }
            } else {
                if (tail.value.elements[enqIdx].compareAndSet(null, x)) {
                    return
                }
            }
        }
    }

    /**
     * Retrieves the first element from the queue
     * and returns it; returns `null` if the queue
     * is empty.
     */
    fun dequeue(): T? {
        while (true) {
            val h = head.value
            val deqIdx = h.deqIdx.getAndIncrement()
            if (deqIdx >= SEGMENT_SIZE) {
                val headNext = h.next.value ?: return null
                head.compareAndSet(h, headNext)
                continue
            }
            val res = h.elements[deqIdx].getAndSet(DONE) ?: continue

            return res as T?
        }
    }

    /**
     * Returns `true` if this queue is empty;
     * `false` otherwise.
     */
    val isEmpty: Boolean get() {
        while (true) {
            val h = head.value
            val hn = h.next.value
            if (h.isEmpty) {
                if (hn == null) return true
                head.compareAndSet(h, hn)
                continue
            } else {
                return false
            }
        }
    }
}

private class Segment {
    val next: AtomicRef<Segment?> = atomic(null)
    val enqIdx = atomic(0) // index for the next enqueue operation
    val deqIdx = atomic(0) // index for the next dequeue operation
    val elements = atomicArrayOfNulls<Any>(SEGMENT_SIZE)

    constructor() // for the first segment creation

    constructor(x: Any?) { // each next new segment should be constructed with an element
        enqIdx.value = 1
        elements[0].value = x
    }

    val isEmpty: Boolean get() = deqIdx.value >= enqIdx.value || deqIdx.value >= SEGMENT_SIZE

}

private val DONE = Any() // Marker for the "DONE" slot state; to avoid memory leaks
const val SEGMENT_SIZE = 2 // DO NOT CHANGE, IMPORTANT FOR TESTS

