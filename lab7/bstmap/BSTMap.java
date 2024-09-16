package bstmap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class BSTMap<K extends Comparable<K>, V> implements Map61B<K, V> {
    private Node root;
    private int size = 0;

    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    @Override
    public boolean containsKey(K key) {
        return contains(root, key);
    }

    private boolean contains(Node node, K key) {
        if (node == null) {
            return false;
        }
        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            return contains(node.left, key);
        } else if (cmp > 0) {
            return contains(node.right, key);
        }
        return true;
    }

    @Override
    public V get(K key) {
        return get(root, key);
    }

    private V get(Node node, K key) {
        if (node == null) {
            return null;
        }
        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            return get(node.left, key);
        } else if (cmp > 0) {
            return get(node.right, key);
        }
        return node.value;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void put(K key, V value) {
        root = put(root, key, value);
        size += 1;
    }

    private Node put(Node node, K key, V value) {
        if (node == null) {
            return new Node(key, value);
        }
        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.left = put(node.left, key, value);
        } else if (cmp > 0) {
            node.right = put(node.right, key, value);
        } else {
            node.value = value;
        }
        return node;
    }

    @Override
    public Set<K> keySet() {
        HashSet<K> set = new HashSet<>();
        addKeys(root, set);
        return set;
    }

    private void addKeys(Node node, Set<K> set) {
        if (node == null) {
            return;
        }
        set.add(node.key);
        addKeys(node.left, set);
        addKeys(node.right, set);
    }

    @Override
    public V remove(K key) {
        if (containsKey(key)) {
            V targetValue = get(key);
            root = remove(root, key);
            size -= 1;
            return targetValue;
        }
        return null;
    }

    private Node remove(Node node, K key) {
        if (node == null) {
            return null;
        }
        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.left = remove(node.left, key);
        } else if (cmp > 0) {
            node.right = remove(node.right, key);
        } else {
            if (node.left == null) {
                return node.right;
            }
            if (node.right == null) {
                return node.left;
            }
            Node orignialNode = node;
            node = getMinChild(node.right);
            node.left = orignialNode.left;
            node.right = remove(orignialNode.right, node.key);
        }
        return node;
    }

    private Node getMinChild(Node node) {
        if (node.left == null) {
            return node;
        }
        return getMinChild(node.left);
    }

    @Override
    public V remove(K key, V value) {
        if (containsKey(key)) {
            V targetValue = get(key);
            if (targetValue.equals(value)) {
                root = remove(root, key);
                size -= 1;
                return targetValue;
            }
        }
        return null;
    }

    @Override
    public Iterator<K> iterator() {
        return keySet().iterator();
    }

    public void printInOrder() {

    }

    private void printInOrder(Node node) {
        if (node == null) {
            return;
        }
        printInOrder(node.left);
        System.out.print(node.key.toString() + " -> " + node.value.toString() + "\n");
        printInOrder(node.right);
    }

    private class Node {
        public final K key;
        public V value;
        public Node left, right;

        public Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
