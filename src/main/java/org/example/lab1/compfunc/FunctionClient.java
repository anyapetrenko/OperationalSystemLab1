package org.example.lab1.compfunc;


import org.example.lab1.Constants;
import org.example.lab1.compfunc.advanced.IntOps;

import java.io.*;
import java.net.Socket;

import java.util.Optional;
import java.util.function.Function;

public class FunctionClient {

    private final String type;
    private Function <Integer, Optional<Optional<Integer>>> function;
    private Integer value;
    private String result;

    public FunctionClient(String type) {
        this.type = type;
        assignFunction(type);
    }

    private void assignFunction(String type){
        if(type.equals("f")) {
            function = IntOps::trialF;
        }

        if (type.equals("g")) {
            function = IntOps::trialG;
        }
    }

    public void computeResult() {

        System.out.println(Thread.currentThread().getName() + ": Connecting to server");
        try (
            Socket socket = new Socket(Constants.IP, Constants.PORT);
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream()))
        {
            value = (Integer) in.readObject();
            System.out.println(Thread.currentThread().getName() + ": Value was read: " + value);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }

        Optional<Optional<Integer>> packed_result = function.apply(value);

        if(!packed_result.isPresent()){
            result = type + "hard fail";
        } else if (!packed_result.get().isPresent()){
            result = type + "soft fail";
        } else {
            result = type + packed_result.get().get();
        }

        passResultToChannels();
    }

    private void passResultToChannels(){
        System.out.println(Thread.currentThread().getName() + ": Connecting to server");
        try (
                Socket socket = new Socket(Constants.IP, Constants.PORT);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()))
        {
            out.writeObject(result);
            System.out.println(Thread.currentThread().getName() + ": Value is in the socket: " + value);
            out.flush();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        System.out.println(result);
    }

}
