package hashmap;

import java.util.*;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author YOUR NAME HERE
 */
public class MyHashMap<K, V> implements Map61B<K, V> {

    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }

    /* Instance Variables */
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final double DEFAULT_MAX_LOAD_FACTOR = 0.75;
    private final double maxLoadFactor;
    private Collection<Node>[] buckets;
    private int size = 0;

    /** Constructors */
    public MyHashMap() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    public MyHashMap(int initialSize) {
        this(initialSize, DEFAULT_MAX_LOAD_FACTOR);
    }

    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    public MyHashMap(int initialSize, double maxLoad) {
        buckets = createTable(initialSize);
        maxLoadFactor = maxLoad;
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key, value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     */
    protected Collection<Node> createBucket() {
        return new LinkedList<>();
    }

    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    @SuppressWarnings("unchecked")
    private Collection<Node>[] createTable(int tableSize) {
        Collection<Node>[] table = new Collection[tableSize];
        for (int i = 0; i < tableSize; i++) {
            table[i] = createBucket();
        }
        return table;
    }

    @Override
    public void clear() {
        buckets = createTable(DEFAULT_INITIAL_CAPACITY);
        size = 0;
    }

    @Override
    public boolean containsKey(K key) {
        return getNode(key) != null;
    }

    @Override
    public V get(K key) {
        Node node = getNode(key);
        return node == null ? null : node.value;
    }

    private Node getNode(K key) {
        int bucketIndex = getIndex(key);
        return getNode(key, bucketIndex);
    }

    private Node getNode(K key, int bucketIndex) {
        for (Node node : buckets[bucketIndex]) {
            if (node.key.equals(key)) {
                return node;
            }
        }
        return null;
    }

    private int getIndex(K key) {
        return getIndex(key, buckets);
    }

    private int getIndex(K key, Collection<Node>[] table) {
        int keyHash = key.hashCode();
        return Math.floorMod(keyHash, table.length);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void put(K key, V value) {
        int bucketIndex = getIndex(key);
        Node node = getNode(key, bucketIndex);
        if (node != null) {
            node.value = value;
            return;
        }
        node = createNode(key, value);
        buckets[bucketIndex].add(node);
        size += 1;
        if (hasReachedMaxCapacity()) {
            resize(buckets.length * 2);
        }
    }

    private boolean hasReachedMaxCapacity() {
        return (double) size / buckets.length > maxLoadFactor;
    }

    private void resize(int capacity) {
        Collection<Node>[] newBuckets = createTable(capacity);
        Iterator<Node> iter = new MyHashMapNodeIterator();
        while (iter.hasNext()) {
            Node node = iter.next();
            int bucketIndex = getIndex(node.key, newBuckets);
            newBuckets[bucketIndex].add(node);
        }
        buckets = newBuckets;
    }

    @Override
    public Set<K> keySet() {
        HashSet<K> keySet = new HashSet<>();
        for (K key : this) {
            keySet.add(key);
        }
        return keySet;
    }

    @Override
    public V remove(K key) {
        int bucketIndex = getIndex(key);
        Node node = getNode(key, bucketIndex);
        if (node == null) {
            return null;
        }
        size -= 1;
        buckets[bucketIndex].remove(node);
        return node.value;
    }

    @Override
    public V remove(K key, V value) {
        int bucketIndex = getIndex(key);
        Node node = getNode(key, bucketIndex);
        if (node == null || !node.value.equals(value)) {
            return null;
        }
        size -= 1;
        buckets[bucketIndex].remove(node);
        return node.value;
    }

    @Override
    public Iterator<K> iterator() {
        return new MyHashMapIterator();
    }

    private class MyHashMapIterator implements Iterator<K> {
        private final Iterator<Node> nodeIterator = new MyHashMapNodeIterator();

        @Override
        public boolean hasNext() {
            return nodeIterator.hasNext();
        }

        @Override
        public K next() {
            return nodeIterator.next().key;
        }
    }

    private class MyHashMapNodeIterator implements Iterator<Node> {
        private final Iterator<Collection<Node>> bucketsIterator = Arrays.stream(buckets).iterator();
        private Iterator<Node> currentBucketIterator;
        private int nodesLeft = size;

        @Override
        public boolean hasNext() {
            return nodesLeft > 0;
        }

        @Override
        public Node next() {
            if (currentBucketIterator == null || !currentBucketIterator.hasNext()) {
                Collection<Node> currentBucket = bucketsIterator.next();
                while (currentBucket.isEmpty()) {
                    currentBucket = bucketsIterator.next();
                }
                currentBucketIterator = currentBucket.iterator();
            }
            nodesLeft -= 1;
            return currentBucketIterator.next();
        }
    }

}
