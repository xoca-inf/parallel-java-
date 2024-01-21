package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private AtomicRef<Node> head;
    private AtomicRef<Node> tail;

    public MSQueue() {
        Node dummy = new Node(0, null);
        this.head = new AtomicRef<>(dummy);
        this.tail = new AtomicRef<>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x, null);
        Node curTail = tail.getValue();
        while (true) {
            curTail = tail.getValue();
            Node nextTailNode = curTail.next.getValue();
            if (curTail == tail.getValue()) {
                if (nextTailNode == null) {
                    if (curTail.next.compareAndSet(null, newTail)) {
                        break;
                    }
                } else {
                    tail.compareAndSet(curTail, nextTailNode);
                }
            }
        }
        tail.compareAndSet(curTail, newTail);
    }

    @Override
    public int dequeue() {
        while (true) {
            Node curHead = head.getValue();
            Node curTail = tail.getValue();
            Node nextHead = head.getValue().next.getValue();
            if (curHead == head.getValue()) {
                if (curHead == curTail) {
                    if (nextHead == null) {
                        return Integer.MIN_VALUE;
                    }
                    tail.compareAndSet(curTail, nextHead);
                } else {
                    if (head.compareAndSet(curHead, nextHead)) {
                        return nextHead.x;
                    }
                }
            }
        }
    }

    @Override
    public int peek() {
        while (true) {
            Node curHead = head.getValue();
            Node curTail = tail.getValue();
            Node nextHead = curHead.next.getValue();
            if (curHead == head.getValue()) {
                if (curHead == curTail) {
                    if (nextHead == null) {
                        return Integer.MIN_VALUE;
                    }
                    tail.compareAndSet(curTail, nextHead);
                } else {
                    return nextHead.x;
                }
            }
        }
    }

    private class Node {
        final int x;
        AtomicRef<Node> next;

        Node(int x) {
            this.x = x;
        }

        Node(int x, Node next) {
            this.x = x;
            this.next = new AtomicRef<>(next);
        }
    }

    public static void main(String[] args) {
        MSQueue queue = new MSQueue();
        queue.enqueue(2);
        queue.dequeue();
        System.out.println(queue.peek());
    }
}