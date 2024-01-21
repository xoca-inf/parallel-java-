package dijkstra

import kotlinx.atomicfu.atomic
import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.locks.ReentrantLock
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> o1!!.distance.compareTo(o2!!.distance) }

// Returns `Integer.MAX_VALUE` if a path has not been found.

class MyQueue(workers: Int, comparator: Comparator<Node>) {
    private val workers: Int
    private val queueSize = atomic(0)
    private val arr = IntArray(workers)
    private val myList: List<Int>

    private val qu: ArrayList<PriorityQueue<Node>> = ArrayList()
    private val locksStatus: ArrayList<ReentrantLock> = ArrayList()

    init {
        this.workers = workers
        for (i in 0 until workers) {
            arr[i] = i
            locksStatus.add(ReentrantLock())
        }
        myList = arr.toList()
        for (i in 1..this.workers) {
            qu.add(PriorityQueue(1, comparator))
        }
    }

    fun add(x: Node) {
        var i = 0
        var flag = true
        val myShuffleList = myList.shuffled(Random())
        while (flag) {
            i++
            if (i == workers) {
                i = 0
            }
            if (locksStatus[myShuffleList[i]].tryLock()) {
                try {
                    queueSize.incrementAndGet()
                    qu[myShuffleList[i]].add(x)
                    flag = false
                } finally {
                    locksStatus[myShuffleList[i]].unlock()
                }
            }
        }
    }

    fun pop(): Node? {
        var i = 0
        var ans = Node()
        var flagDeleted = false
        val shuffleList = myList.shuffled(Random())
        while (!isEmpty()) {
            i++
            if (i == workers) {
                i = 0
            }
            if (locksStatus[shuffleList[i]].tryLock()) {
                try {
                    if (qu[shuffleList[i]].size > 0) {
                        ans = qu[shuffleList[i]].remove()
                        flagDeleted = true
                        queueSize.decrementAndGet()
                    }
                } finally {
                    locksStatus[shuffleList[i]].unlock()
                }
            }
            if (flagDeleted) {
                return ans
            }
        }
        return null
    }

    fun isEmpty(): Boolean {
        return queueSize.value <= 0
    }
}

fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    start.distance = 0
    val queue = MyQueue(workers, NODE_DISTANCE_COMPARATOR)
    queue.add(start)
    val onFinish = Phaser(workers + 1)
    repeat(workers) {
        thread {
            while (!queue.isEmpty()) {
                val cur: Node = queue.pop() ?: continue
                for (edge in cur.outgoingEdges) {
                    while (true) {
                        val oldEdgeDistance = edge.to.distance
                        val oldCurrentDistance = cur.distance
                        if (oldEdgeDistance > oldCurrentDistance + edge.weight) {
                            if (edge.to.casDistance(oldEdgeDistance, oldCurrentDistance + edge.weight)) {
                                queue.add(edge.to)
                                break
                            }
                            continue
                        }
                        break
                    }
                }
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}