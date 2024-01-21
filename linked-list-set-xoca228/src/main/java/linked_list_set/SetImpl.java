package linked_list_set;

import java.util.concurrent.atomic.AtomicReference;

public class SetImpl implements Set {
    private static class Node {
        AtomicReference<Node> next;
        int x;

        Node(int x, Node next) {
            this.next = new AtomicReference<>(next);
            this.x = x;
        }
    }

    private static class Window {
        AtomicReference<Node> cur, next;
    }

    private final AtomicReference<Node> head = new AtomicReference<>(new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null)));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        while(true) {
            Window w = new Window();
            w.cur = new AtomicReference<>(head.get());
            w.next = new AtomicReference<>(w.cur.get().next.get());
            while (w.next.get() != null && w.next.get().x < x) {
                w.cur.set(w.next.get());
                w.next.set(w.cur.get().next.get());
            }
            if (w.next.get() != null) {
                return w;
            }
        }
    }

    @Override
    public boolean add(int x) {
        boolean res;
        while(true) {
            Window w = findWindow(x);
            if (w.next.get().x == x) {
                res = false;
                break;
            } else if (w.cur.get().next.compareAndSet(w.next.get(), new Node(x, w.next.get()))){
                res = true;
                break;
            }
        }
        return res;
    }

    @Override
    public boolean remove(int x) {
        boolean res = false;
        while(true) {
            Window w = findWindow(x);
            if (w.next.get().x != x) {
                break;
            } else {
                Node oldNext = w.next.get().next.getAndSet(null);
                if (oldNext != null) {
                    if (w.cur.get().next.compareAndSet(w.next.get(), oldNext)) {
                        res = true;
                        break;
                    } else {
                        w.next.get().next.set(oldNext);
                    }
                }
            }
        }

        return res;
    }

    @Override
    public boolean contains(int x) {
        boolean res;
        while(true) {
            System.out.println();
            Window w = findWindow(x);
            res = w.next.get().x == x;
            if(w.next.get() == w.cur.get().next.get()) {
                break;
            }
        }
        return res;
    }
}