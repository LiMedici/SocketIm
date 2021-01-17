package com.mrmedici.clink.source;

/**
 * 最小堆(完全二叉树，可以使用数组数据结构)，并不是一颗满的二叉树
 * 性质1:二叉堆是一颗完全二叉树
 * 性质2:堆中某个节点的值总是小于等于其子节点, 就是最小堆
 */
public class MinHeap<E extends Comparable<E>> {
    private Array<E> array;


    public MinHeap(){
        array = new Array<>();
    }

    public MinHeap(int capacity){
        array = new Array<>(capacity);
    }

    public MinHeap(E[] arr){
        array = new Array<>(arr);
        // 进入堆化过程
        // heapify过程
        // 时间复杂度O(n)
        for (int i = parent(arr.length - 1); i >= 0; i--){
            siftDown(i);
        }
    }

    // 返回堆中的元素个数
    public int getSize(){
        return array.getSize();
    }

    // 返回一个布尔值, 表示堆中是否为空
    public boolean isEmpty(){
        return array.isEmpty();
    }

    // 返回完全二叉树的数组表示中，一个索引所表示的元素的父亲节点的索引
    public int parent(int index){
        if(index == 0)
            throw new IllegalArgumentException("index-0 doesn't have parent.");
        return (index - 1) / 2;
    }

    // 返回完全二叉树的数组表示中，一个索引所表示的元素的左孩子节点的索引
    public int leftChild(int index){
        return index * 2 + 1;
    }

    // 返回完全二叉树的数组表示中，一个索引所表示的元素的右孩子节点的索引
    public int rightChild(int index){
        return index * 2 + 2;
    }

    // 向堆中添加元素
    public void add(E e){
        array.addLast(e);
        // 元素上浮操作
        siftUp(array.getSize() - 1);
    }

    // 元素上浮操作
    private void siftUp(int k){
        while (k > 0 && array.get(k).compareTo(array.get(parent(k))) < 0){
            array.swap(k,parent(k));
            k = parent(k);
        }
    }

    // 返回堆中的最小元素
    public E findMin(){
        return array.getFirst();
    }

    // 取出堆中最小元素
    public E extractMin(){
        E ret = findMin();

        // 堆第一个元素与最后一个元素互换位置
        array.swap(0,array.getSize() - 1);
        // 删除最后一个元素
        array.removeLast();
        // 元素下沉
        siftDown(0);

        return ret;
    }

    // 元素下沉操作
    private void siftDown(int k){
        while (leftChild(k) < array.getSize()){
            int j = leftChild(k);
            if(j + 1 < array.getSize() &&
                    array.get(j + 1).compareTo(array.get(j)) < 0){
                j++;
            }

            // data[j] 是 leftChild 和 rightChild 中的最小值
            if(array.get(k).compareTo(array.get(j)) <= 0){
                break;
            }

            array.swap(k, j);
            k = j;

        }
    }

    // 取出堆中的最小元素，并且替换成元素e
    public E replace(E e){
        E ret = findMin();
        array.set(0,e);
        siftDown(0);
        return ret;
    }

    @Override
    public String toString() {
        return array.toString();
    }
}
