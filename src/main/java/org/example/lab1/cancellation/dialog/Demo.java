package org.example.lab1.cancellation.dialog;

import java.io.IOException;
import java.util.Scanner;

public class Demo {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int inputValue = 0;
        boolean correctResponse = false;
        while (!correctResponse) {
            System.out.println("X:");
            correctResponse = true;
            if (sc.hasNextInt()) {
                inputValue = sc.nextInt();
            } else {
                correctResponse = false;
            }
            sc.nextLine();
        }

        System.out.println("Value is read");

        try {
            ComputationManager manager = new ComputationManager(inputValue);
            System.out.println("Computation manager started");
            manager.startComputing();
            Integer fStatus = manager.getFStatus();
            Integer gStatus = manager.getGStatus();
            Boolean cancelStatus = manager.getCancellationStatus();

            if(fStatus + gStatus == -2) {
                System.out.println("Expression value failed");
            } else if(fStatus == 0 && gStatus == 0) {
                System.out.println("Expression value: " + manager.getResult());
            } else {

                System.out.println("Expression value undetermined");
            }

            if(cancelStatus){
                System.out.println("Computation cancelled using hard key");
                if(fStatus > 0){
                    System.out.println("f process did not finished. Amount of soft fails: " + (fStatus - 1));
                }
                if(gStatus > 0){
                    System.out.println("g process did not finished. Amount of soft fails: " + (fStatus - 1));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

}
