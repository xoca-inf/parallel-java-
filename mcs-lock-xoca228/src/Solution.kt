import java.util.concurrent.atomic.*

class Solution(private val env: Environment) : Lock<Solution.Node> {
    private val tail: AtomicReference<Node> = AtomicReference()

    override fun lock(): Node {
        val my = Node()
        my.locked.value = true
        val pred = tail.getAndSet(my)
        if (pred != null) {
            pred.next.value = my
            while (my.locked.value) {
                env.park()
            }
        }
        return my
    }

    override fun unlock(node: Node) {
        if (node.next.get() == null) {
            if (tail.compareAndSet(node, null)) {
                return
            } else {
                while (node.next.get() == null) { /* pass */ }
            }
        }
        node.next.value.locked.value = false
        env.unpark(node.next.get().thread)
    }

    class Node {
        val thread = Thread.currentThread()
        val locked: AtomicReference<Boolean> = AtomicReference(false)
        val next: AtomicReference<Node> = AtomicReference()
    }
}