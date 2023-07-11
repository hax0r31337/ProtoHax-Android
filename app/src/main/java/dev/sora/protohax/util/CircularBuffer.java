package dev.sora.protohax.util;


public class CircularBuffer {

    private int pointer = 0;
    private final int capacity;
    private final String[] array;

    // Constructor
    public CircularBuffer(int capacity) {
        // Initializing the capacity of the array
        this.capacity = capacity;

        // Initializing the array
        array = new String[capacity];
    }

    // Addition of elements
    public void add(String element) {
        if (pointer == capacity) {
            pointer = 0;
        }
        array[pointer] = element;
        pointer++;
    }

    public String[] getArray() {
        final String[] orderedArray = new String[capacity];
        if (pointer == 0 || pointer == capacity) {
            System.arraycopy(array, 0, orderedArray, 0, capacity);
        } else {
            System.arraycopy(array, pointer, orderedArray, 0, capacity - pointer);
            System.arraycopy(array, 0, orderedArray, capacity - pointer, pointer);
        }
        return orderedArray;
    }

    public void wipe() {
        for (int i = 0; i < capacity; i++) {
            array[i] = null;
        }
    }
}
