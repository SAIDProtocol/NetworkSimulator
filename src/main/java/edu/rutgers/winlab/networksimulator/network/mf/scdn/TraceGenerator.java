/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.networksimulator.network.mf.scdn;

import java.util.Collection;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 *
 * @author jiachen
 */
public class TraceGenerator {

    public static Function<Integer, Double> getZipfDistributionGenerator(double s, int count) {
        double below = 0;
        for (int i = 1; i <= count; i++) {
            below += 1 / Math.pow(i, s);
        }
        double belowFinal = below;
        return i -> 1 / Math.pow(i + 1, s) / belowFinal;
    }

    public static Supplier<Double> getNormalDistributionGenerator(Random rand, double mean, double stdDev) {
        return () -> rand.nextGaussian() * stdDev + mean;
    }

    public static Function<Double, Long> getDoubleToLongFunction(long min, long max) {
        long diff = max - min;
        return d -> (long) Math.round(diff * d) + min;
    }

    public static <T> Function<Integer, T> toIntFunction(Supplier<T> supplier) {
        return i -> supplier.get();
    }

    public static <T> Supplier<T> addFilter(Supplier<T> supplier, Predicate<T> filter) {
        return () -> {
            T ret;
            ret = supplier.get();
            while (!filter.test(ret)) {
                System.out.printf("reject: %s%n", ret);
                ret = supplier.get();
            }
//            do {
//                ret = supplier.get();
//            } while (!filter.test(ret));
            return ret;
        };
    }

    public static <T> void shuffleArray(T[] array, Random rand) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = rand.nextInt(i + 1);
            T a = array[index];
            array[index] = array[i];
            array[i] = a;
        }
    }

    public static void generateTrace(
            Random rand,
            int numberOfVideos,
            Function<Integer, Long> videoLengthGenerator,
            Function<Integer, Long> videoPopularityGenerator,
            Function<Integer, Double> videoViewPercentageGenerator) {
        Objects.requireNonNull(rand);
        Objects.requireNonNull(videoLengthGenerator);
        Objects.requireNonNull(videoPopularityGenerator);
        Objects.requireNonNull(videoViewPercentageGenerator);

        Long[] videoLengths = IntStream.range(0, numberOfVideos)
                .mapToObj(i -> videoLengthGenerator.apply(i))
                .toArray(Long[]::new);
        shuffleArray(videoLengths, rand);

        Integer[] views = IntStream.range(0, numberOfVideos)
                .mapToObj(i -> {
                    long count = videoPopularityGenerator.apply(i);
                    return IntStream.generate(() -> i).limit(count);
                }).flatMap(x -> x.boxed()).toArray(Integer[]::new);
        shuffleArray(views, rand);

        Long[] viewLengths = IntStream.range(0, views.length)
                .mapToLong(i -> {
                    int videoId = views[i];
                    long videoLength = videoLengths[videoId];
                    long viewLength = Math.round(videoLengthGenerator.apply(i) * videoLength);
                    viewLength = viewLength <= 0 ? 1 : viewLength;
                    viewLength = viewLength > videoLength ? videoLength : viewLength;
                    return viewLength;
                }).boxed().toArray(Long[]::new);

    }
}
