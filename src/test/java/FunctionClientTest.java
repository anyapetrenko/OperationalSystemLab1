import org.example.lab1.Constants;
import org.example.lab1.compfunc.FProcess;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

public class FunctionClientTest {

    private ServerSocket server;
    private ExecutorService executorService;
    private Integer value;
    Future<?> task;
    private Integer failure;
    private Integer result;

    @Test
    public void testValuesPassing(){
        value = 2;
        System.out.println("Start socket server");
        try {
            server = new ServerSocket(Constants.PORT);
            executorService = Executors.newFixedThreadPool(2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        launchProcessAsThread();
        passValueToChannels();

        while (true){
            if(task.isDone()){
                readResultsFromChannels();
                break;
            }
        }

        executorService.shutdown();

        assertEquals(10, result);
    }

    private void launchProcessAsThread(){
        Runnable fTask = () -> {
            System.out.println("Start F process");
            FProcess.main(new String[]{"a"});
        };

        task = executorService.submit(fTask);
    }

    private void passValueToChannels(){
        System.out.println("Pass value to sockets");

        try (
                Socket socket = server.accept();
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
            ){
                out.writeObject(value);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        System.out.println("Values passed");
    }

    private void readResultsFromChannels(){
        try (Socket socket = server.accept();
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            System.out.println("processing values");
            String res = (String) in.readObject();
            System.out.println("Output is " + res);
            res = res.replace("f", "");
            if(res.equals("hard fail")){
                failure = 2;
            } else if (res.equals("soft fail") && failure < 1){
                failure = 1;
            } else {
                result = Integer.parseInt(res);
            }
        } catch (IOException | ClassNotFoundException e ) {
            System.out.println(e);
        }
    }
}
