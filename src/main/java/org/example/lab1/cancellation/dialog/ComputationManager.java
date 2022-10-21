package org.example.lab1.cancellation.dialog;

import org.example.lab1.Constants;
import sun.misc.Signal;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

public class ComputationManager {

    private final Executor executor;
    private final ServerSocket server;

    private BiFunction<Integer, Integer, Integer> bitwiseOperation;

    public ProcessBuilder processBuilderF;
    public ProcessBuilder processBuilderG;
    private Process fProcess;
    private Process gProcess;
    private final Integer value;

    private final List<Runnable> tasks;
    private final List<Integer> functionResults;

    private Boolean fFailure;
    private Boolean gFailure;
    private final AtomicBoolean cancel;
    private final AtomicBoolean resultComputed;

    private Integer remainedComputations;
    private final List<Integer> softFailCounters;

    ComputationManager(Integer inputValue) throws IOException {
        this.value = inputValue;

        tasks = assignTasks();
        functionResults = new ArrayList<>();
        assignBitwiseOperation();

        server = new ServerSocket(Constants.PORT);
        executor = Executors.newFixedThreadPool(2);

        processBuilderF = new ProcessBuilder("java", "-jar", "./out/artifacts/Lab1_FProcess/Lab1.jar");
        processBuilderG = new ProcessBuilder("java", "-jar", "./out/artifacts/Lab1_GProcess/Lab1.jar");

        softFailCounters = Arrays.asList(0, 0);
        remainedComputations = 2;
        fFailure = false;
        gFailure = false;
        cancel = new AtomicBoolean(false);
        resultComputed = new AtomicBoolean(false);
    }

    private void assignBitwiseOperation(){
        bitwiseOperation = (x, y) -> x * y;
    }

    private void initSignalHandler(){
        Signal.handle(new Signal("INT"), signal -> showCancellationPrompt());
    }

    private void showCancellationPrompt(){
        System.out.println("Confirm that computation should be stopped");
        long start = System.currentTimeMillis();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true){
            if(resultComputed.get()) {
                return;
            } else if(System.currentTimeMillis() - start > Constants.CANCELLATION_PERIOD){
                System.out.println("No action for 5 seconds. Proceeding...");
                initSignalHandler();
                return;
            } else {
                try {
                    if (reader.ready()){
                        String s = "";
                        try {
                            s = reader.readLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if(!s.isEmpty()){
                           if(s.equals("y")){
                               System.out.println("Cancelling computation..");
                               cancel.set(true);
                               return;
                           } else if (s.equals("n")) {
                               initSignalHandler();
                               System.out.println("Proceeding...");
                               return;
                           } else {
                               System.out.println("Please enter valid response 'y' or 'n'");
                           }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void startComputing(){
        initSignalHandler();

        for(Runnable task: tasks) {
            executor.execute(task);
        }

        passValueToChannels(remainedComputations);

        while (true) {
            if (!fProcess.isAlive() && !gProcess.isAlive()) {
                readResultsFromChannels();
                if (remainedComputations == 0) {
                    break;
                } else if (cancel.get()) {
                    break;
                } else {
                    passValueToChannels(remainedComputations);
                }
            } else if (cancel.get()) {
                break;
            }
        }
    }

    private List<Runnable> assignTasks(){
        Runnable f = () -> {
            try {
                fProcess = processBuilderF.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        Runnable g = () -> {
            try {
                gProcess =  processBuilderG.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        List<Runnable> tasks = new ArrayList<>();
        tasks.add(f);
        tasks.add(g);
        return tasks;
    }

    private void passValueToChannels(Integer tasksNumber){
        for(int i = 0; i < tasksNumber; i++) {
            try (
                Socket socket = server.accept();
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
            ){
                out.writeObject(value);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void readResultsFromChannels(){
        int newRemainder = remainedComputations;
        for(int i = 0; i < remainedComputations; i++) {
            try (Socket socket = server.accept();
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                if(cancel.get()){
                    return;
                }
                String res = (String) in.readObject();
                String processIndex = res.substring(0, 1);
                res = res.substring(1);

                if(res.equals("hard fail")){
                    if(processIndex.equals("f")) {
                        fFailure = true;
                    } else {
                        gFailure = true;
                    }

                    System.out.println(processIndex + "-function hard failed");
                    newRemainder--;
                } else if (res.equals("soft fail")){
                    if(processIndex.equals("f")) {
                        if(softFailCounters.get(0) < Constants.SOFT_FAIL_RETRIES) {
                            executor.execute(tasks.get(0));
                            softFailCounters.set(0, softFailCounters.get(0) + 1);
                            System.out.println(processIndex + "-function soft failed. Restarting computation...");
                        } else {
                            fFailure = true;
                            System.out.println(processIndex + "-function hard failed after "
                                    + Constants.SOFT_FAIL_RETRIES + " retries");
                            newRemainder--;
                        }
                    } else {
                        if(softFailCounters.get(1) < Constants.SOFT_FAIL_RETRIES) {
                            executor.execute(tasks.get(1));
                            softFailCounters.set(1, softFailCounters.get(1) + 1);
                            System.out.println(processIndex + "-function soft failed. Restarting computation...");
                        } else {
                            gFailure = true;
                            System.out.println(processIndex + "-function hard failed after "
                                    + Constants.SOFT_FAIL_RETRIES + " retries");
                            newRemainder--;
                        }
                    }
                } else {
                    functionResults.add(Integer.parseInt(res));
                    newRemainder--;
                }
            } catch (IOException | ClassNotFoundException e ) {
                System.out.println(e);
            }
        }

        remainedComputations = newRemainder;
    }

    public Integer getFStatus(){
        return fFailure ? -1 : fProcess.isAlive() ? softFailCounters.get(0) + 1 : 0;
    }
    public Integer getGStatus(){
        return gFailure ? -1 : gProcess.isAlive() ? softFailCounters.get(1) + 1 : 0;
    }

    public Boolean getCancellationStatus(){
        return cancel.get();
    }

    public Integer getResult(){
        return bitwiseOperation.apply(functionResults.get(0), functionResults.get(1));
    }
}
