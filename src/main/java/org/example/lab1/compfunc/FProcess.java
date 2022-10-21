package org.example.lab1.compfunc;

import sun.misc.Signal;

public class FProcess {
    public static void main(String[] args) {
        initSignalHandler();
        FunctionClient f = new FunctionClient("f");
        f.computeResult();
        System.out.println("finished process");
    }

    private static void initSignalHandler(){
        Signal.handle(new Signal("INT"), signal -> initSignalHandler());
    }
}
