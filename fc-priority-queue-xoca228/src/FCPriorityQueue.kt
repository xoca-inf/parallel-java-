import kotlinx.atomicfu.AtomicArray
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls
import java.util.*

class FCPriorityQueue<E : Comparable<E>> {
    private val arraySize: Int
    private val q: PriorityQueue<E>
    private val lock: AtomicBoolean
    private val fcTaskArray: AtomicArray<Any?>

    init {
        arraySize = Runtime.getRuntime().availableProcessors() * 4
        q = PriorityQueue<E>()
        lock = atomic(false)
        fcTaskArray = atomicArrayOfNulls(arraySize)
    }

    private class ClosureHolder<E : Comparable<E>>(c: () -> E?) {
        val closure: () -> E? = c
    }

    private class ValueHolder<E : Comparable<E>>(v: E?) {
        val value = v
    }


    private fun tryLock(): Boolean {
        return lock.compareAndSet(expect = false, update = true)
    }

    private fun unlock() {
        lock.getAndSet(false)
    }


    private fun addTask(taskHolder: ClosureHolder<E>): Int {
        var i = Random().nextInt(arraySize)
        while (!fcTaskArray[i].compareAndSet(null, taskHolder)) {
            i = (i + 1) % arraySize
        }
        return i
    }

    private fun awaitEnding(index: Int): ValueHolder<E>? {
        while (true) {
            val task = fcTaskArray[index].value
            if (task is ValueHolder<*>) {
                fcTaskArray[index].getAndSet(null)
                return (task as ValueHolder<E>)
            }
            if (tryLock()) {
                return null
            }
        }
    }

    private fun combineLoop() {
        for (i in (0 until arraySize)) {
            val task = fcTaskArray[i].value
            if (task != null && task is ClosureHolder<*>) {
                val r: E? = task.closure() as E?
                val holder = ValueHolder(r)
                fcTaskArray[i].getAndSet(holder)
            }
        }
    }

    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        if (tryLock()) {
            val r = q.poll()
            combineLoop()
            unlock()
            return r
        } else {
            val task: () -> E? = {
                q.poll()
            }
            val ch = ClosureHolder(task)
            val ind = addTask(ch)
            val res = awaitEnding(ind)
            return if (res != null) {
                res.value
            } else {
                var r: E? = null
                val maybeValue = fcTaskArray[ind].value
                r = if (maybeValue is ValueHolder<*>) {
                    fcTaskArray[ind].getAndSet(null)
                    (maybeValue as ValueHolder<E>).value
                } else {
                    q.poll()
                }
                fcTaskArray[ind].getAndSet(null)
                combineLoop()
                unlock()
                r
            }
        }
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        if (tryLock()) {
            val r = q.peek()
            combineLoop()
            unlock()
            return r
        } else {
            val task: () -> E? = {
                q.peek()
            }
            val ch = ClosureHolder(task)
            val ind = addTask(ch)
            val res = awaitEnding(ind)
            return if (res != null) {
                res.value
            } else {
                var r: E? = null
                val maybeValue = fcTaskArray[ind].value
                r = if (maybeValue is ValueHolder<*>) {
                    fcTaskArray[ind].getAndSet(null)
                    (maybeValue as ValueHolder<E>).value
                } else {
                    q.peek()
                }
                fcTaskArray[ind].getAndSet(null)
                combineLoop()
                unlock()
                r
            }
        }
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        if (tryLock()) {
            q.add(element)
            combineLoop()
            unlock()
            return
        } else {
            val task: () -> E? = {
                q.add(element)
                element
            }
            val ch = ClosureHolder(task)
            val ind = addTask(ch)
            val res = awaitEnding(ind)
            if (res != null) {
                return
            } else {
                var r: E? = null
                val maybeValue = fcTaskArray[ind].value
                if (maybeValue is ValueHolder<*>) {
                } else {
                    q.add(element)
                }
                fcTaskArray[ind].getAndSet(null)
                combineLoop()
                unlock()
                return
            }
        }
    }
}