package org.example.lab1.compfunc.advanced;

import java.security.SecureRandom;
import java.util.Optional;

public class IntOps{
    public static Optional<Optional<Integer>> trialF(Integer x) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SecureRandom random = new SecureRandom();
        int r = random.nextInt(5);


        if(r < 4 && x == 1) {
            return Optional.of(Optional.empty());
        } else {
            return Optional.of(Optional.of(x + 8));
        }

    }

    public static Optional<Optional<Integer>> trialG(Integer x) {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SecureRandom random = new SecureRandom();
        int r = random.nextInt(22);

        if(x > -1 && x < 20) {
            if (r > 14) {
                return Optional.of(Optional.empty());
            } else if (r < 8) {
                return Optional.of(Optional.of(x * x));
            } else {
                return Optional.empty();
            }
        }

        return Optional.of(Optional.of(x * (x + 10)));
    }
}
