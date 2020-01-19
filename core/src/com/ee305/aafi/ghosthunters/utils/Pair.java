package com.ee305.aafi.ghosthunters.utils;

public class Pair<E1,E2>
{
    private E1 obj1;
    private E2 obj2;

    public Pair(E1 obj1, E2 obj2) {
        this.obj1 = obj1;
        this.obj2 = obj2;
    }

    public E1 getObj1() {
        return obj1;
    }

    public E2 getObj2() {
        return obj2;
    }
}
