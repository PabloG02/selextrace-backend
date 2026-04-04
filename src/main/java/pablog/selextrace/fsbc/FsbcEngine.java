package pablog.selextrace.fsbc;

import org.springframework.stereotype.Component;
import pablog.selextrace.config.FsbcConfiguration;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.domain.experiment.SelectionCycle;
import pablog.selextrace.domain.pool.AptamerBounds;
import pablog.selextrace.model.FsbcClusterSeed;
import pablog.selextrace.model.FsbcStringResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Component
public class FsbcEngine {

    public FsbcRunResult run(Experiment experiment, SelectionCycle selectionCycle, FsbcConfiguration config) {
        Objects.requireNonNull(experiment, "experiment is required");
        Objects.requireNonNull(selectionCycle, "selectionCycle is required");
        Objects.requireNonNull(config, "config is required");

        InputData inputData = extractInputData(experiment, selectionCycle, config.rnaSequence());
        if (inputData.sequenceCounts().isEmpty()) {
            return new FsbcRunResult(
                    Map.of(),
                    List.of(),
                    List.of(),
                    inputData.totalSequenceCount(),
                    0
            );
        }

        Map<Character, Double> baseRatios = computeBaseRatios(inputData.sequenceCounts());
        int maxSequenceLength = inputData.sequenceCounts().keySet().stream().mapToInt(String::length).max().orElse(0);
        Map<Integer, Integer> lengthDistribution = computeLengthDistribution(inputData.sequenceCounts());
        List<Character> alphabet = config.rnaSequence()
                ? List.of('A', 'U', 'C', 'G')
                : List.of('A', 'T', 'C', 'G');

        List<CandidateScore> rankedCandidates = searchStrings(
                inputData.sequenceCounts(),
                baseRatios,
                lengthDistribution,
                maxSequenceLength,
                config.minLength(),
                config.maxLength(),
                inputData.totalSequenceCount(),
                alphabet,
                Runtime.getRuntime().availableProcessors()
        );

        ClusteringResult clustering = clusterAptamers(inputData.aptamers(), rankedCandidates);

        return new FsbcRunResult(
                clustering.aptamerToCluster(),
                toRankedStrings(rankedCandidates),
                clustering.clusterSeeds(),
                inputData.totalSequenceCount(),
                inputData.sequenceCounts().size()
        );
    }

    InputData extractInputData(Experiment experiment, SelectionCycle selectionCycle, boolean rnaSequence) {
        Map<String, Integer> sequenceCounts = new LinkedHashMap<>();
        List<AptamerInput> aptamers = new ArrayList<>();
        int totalSequenceCount = 0;

        for (Map.Entry<Integer, Integer> entry : selectionCycle.iterator()) {
            int aptamerId = entry.getKey();
            int count = entry.getValue();
            if (count <= 0) {
                continue;
            }

            byte[] aptamerBytes = experiment.getPool().getAptamer(aptamerId);
            AptamerBounds bounds = experiment.getPool().getAptamerBounds(aptamerId);
            if (aptamerBytes == null || bounds == null) {
                continue;
            }

            String aptamer = new String(aptamerBytes, StandardCharsets.UTF_8).toUpperCase(Locale.ROOT);
            if (bounds.startIndex < 0 || bounds.endIndex > aptamer.length() || bounds.startIndex >= bounds.endIndex) {
                continue;
            }

            String randomizedRegion = aptamer.substring(bounds.startIndex, bounds.endIndex);
            randomizedRegion = normalizeAlphabet(randomizedRegion, rnaSequence);
            if (!isValidAlphabet(randomizedRegion, rnaSequence)) {
                continue;
            }

            sequenceCounts.merge(randomizedRegion, count, Integer::sum);
            aptamers.add(new AptamerInput(aptamerId, randomizedRegion, count));
            totalSequenceCount += count;
        }

        return new InputData(sequenceCounts, aptamers, totalSequenceCount);
    }

    // Get the ratio of the nucleic acid bases.
    Map<Character, Double> computeBaseRatios(Map<String, Integer> sequenceCounts) {
        Map<Character, Double> baseRatios = new HashMap<>();
        double total = 0;

        for (Map.Entry<String, Integer> entry : sequenceCounts.entrySet()) {
            int weight = entry.getValue();
            for (int index = 0; index < entry.getKey().length(); index++) {
                char base = entry.getKey().charAt(index);
                baseRatios.merge(base, weight * 1.0, Double::sum);
                total += weight;
            }
        }

        if (total == 0) {
            return baseRatios;
        }

        double totalWeight = total;
        baseRatios.replaceAll((_, value) -> value / totalWeight);
        return baseRatios;
    }

    private Map<Integer, Integer> computeLengthDistribution(Map<String, Integer> sequenceCounts) {
        Map<Integer, Integer> distribution = new HashMap<>();
        for (Map.Entry<String, Integer> entry : sequenceCounts.entrySet()) {
            distribution.merge(entry.getKey().length(), entry.getValue(), Integer::sum);
        }
        return distribution;
    }

    private List<CandidateScore> searchStrings(
            Map<String, Integer> sequenceCounts,
            Map<Character, Double> baseRatios,
            Map<Integer, Integer> lengthDistribution,
            int maxSequenceLength,
            int minLength,
            int maxLength,
            int totalSequenceCount,
            List<Character> alphabet,
            int threadCount
    ) {
        List<CandidateScore> allCandidates = new ArrayList<>();
        List<CandidateScore> currentLevel = filterPositiveScores(
                evaluateCandidates(
                        generateSequences(alphabet, minLength),
                        sequenceCounts,
                        baseRatios,
                        lengthDistribution,
                        maxSequenceLength,
                        totalSequenceCount,
                        threadCount
                )
        );
        normalizeScores(currentLevel);
        allCandidates.addAll(currentLevel);

        for (int currentLength = minLength + 1; currentLength <= maxLength && !currentLevel.isEmpty(); currentLength += 1) {
            Map<String, Double> parentZScores = new HashMap<>();
            List<String> nextCandidates = new ArrayList<>(currentLevel.size() * alphabet.size());
            for (CandidateScore candidate : currentLevel) {
                parentZScores.put(candidate.subsequence(), candidate.zScore());
                for (char base : alphabet) {
                    nextCandidates.add(candidate.subsequence() + base);
                }
            }

            List<CandidateScore> nextLevel = evaluateCandidates(
                    nextCandidates,
                    sequenceCounts,
                    baseRatios,
                    lengthDistribution,
                    maxSequenceLength,
                    totalSequenceCount,
                    threadCount
            ).stream()
                    .filter(candidate -> candidate.zScore() > parentZScores.getOrDefault(
                            candidate.subsequence().substring(0, candidate.subsequence().length() - 1),
                            Double.NEGATIVE_INFINITY
                    ))
                    .toList();

            currentLevel = filterPositiveScores(nextLevel);
            normalizeScores(currentLevel);
            allCandidates.addAll(currentLevel);
        }

        allCandidates.sort((left, right) -> {
            int normalizedCompare = Double.compare(right.normalizedZScore(), left.normalizedZScore());
            if (normalizedCompare != 0) {
                return normalizedCompare;
            }

            int zCompare = Double.compare(right.zScore(), left.zScore());
            if (zCompare != 0) {
                return zCompare;
            }

            int observedCompare = Integer.compare(right.observedCount(), left.observedCount());
            if (observedCompare != 0) {
                return observedCompare;
            }

            return left.subsequence().compareTo(right.subsequence());
        });

        return allCandidates;
    }

    private List<CandidateScore> filterPositiveScores(List<CandidateScore> candidates) {
        return new ArrayList<>(candidates.stream().filter(candidate -> candidate.zScore() > 0).toList());
    }

    private void normalizeScores(List<CandidateScore> candidates) {
        if (candidates.isEmpty()) {
            return;
        }

        double mean = candidates.stream().mapToDouble(CandidateScore::zScore).average().orElse(0);
        double variance = candidates.stream()
                .mapToDouble(candidate -> {
                    double delta = candidate.zScore() - mean;
                    return delta * delta;
                })
                .average()
                .orElse(0);
        double standardDeviation = Math.sqrt(variance);

        for (int index = 0; index < candidates.size(); index += 1) {
            CandidateScore candidate = candidates.get(index);
            double normalized = standardDeviation == 0 ? 0 : (candidate.zScore() - mean) / standardDeviation;
            candidates.set(index, candidate.withNormalizedZScore(normalized));
        }
    }

    private List<CandidateScore> evaluateCandidates(
            List<String> candidates,
            Map<String, Integer> sequenceCounts,
            Map<Character, Double> baseRatios,
            Map<Integer, Integer> lengthDistribution,
            int maxSequenceLength,
            int totalSequenceCount,
            int threadCount
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> observedCounts = computeObservedCounts(candidates, sequenceCounts, threadCount);
        List<CandidateScore> scores = new ArrayList<>(candidates.size());

        for (String candidate : candidates) {
            double[] probabilities = calculateSequenceInclusionProbabilities(candidate, baseRatios, maxSequenceLength);
            double expectedFraction = calculateExpectedFraction(probabilities, lengthDistribution, totalSequenceCount);
            int observedCount = observedCounts.getOrDefault(candidate, 0);
            double variance = expectedFraction * (1 - expectedFraction) * totalSequenceCount;
            double denominator = variance <= 0 ? 0 : Math.sqrt(variance);
            double zScore = denominator == 0 ? 0 : (observedCount - (totalSequenceCount * expectedFraction)) / denominator;

            scores.add(new CandidateScore(
                    candidate,
                    candidate.length(),
                    observedCount,
                    expectedFraction,
                    zScore,
                    0
            ));
        }

        return scores;
    }

    private Map<String, Integer> computeObservedCounts(
            List<String> candidates,
            Map<String, Integer> sequenceCounts,
            int threadCount
    ) {
        if (candidates.isEmpty()) {
            return Map.of();
        }

        Set<String> candidateSet = new LinkedHashSet<>(candidates);
        int length = candidates.get(0).length();

        Supplier<Map<String, Integer>> supplier = () -> createSequenceCountStream(sequenceCounts, threadCount)
                .map(entry -> computeObservedCountsForSequence(entry.getKey(), entry.getValue(), candidateSet, length))
                .reduce(new HashMap<>(), FsbcEngine::mergeObservedCounts, FsbcEngine::mergeObservedCounts);

        return executeWithThreadCount(threadCount, supplier);
    }

    private static Map<String, Integer> computeObservedCountsForSequence(
            String sequence,
            int count,
            Set<String> candidates,
            int length
    ) {
        Map<String, Integer> observed = new HashMap<>();
        if (sequence.length() < length) {
            return observed;
        }

        Set<String> seen = new HashSet<>();
        for (int index = 0; index <= sequence.length() - length; index += 1) {
            String subsequence = sequence.substring(index, index + length);
            if (candidates.contains(subsequence)) {
                seen.add(subsequence);
            }
        }

        for (String subsequence : seen) {
            observed.put(subsequence, count);
        }
        return observed;
    }

    private static Map<String, Integer> mergeObservedCounts(Map<String, Integer> left, Map<String, Integer> right) {
        Map<String, Integer> merged = new HashMap<>(left);
        right.forEach((key, value) -> merged.merge(key, value, Integer::sum));
        return merged;
    }

    private double calculateExpectedFraction(double[] probabilities, Map<Integer, Integer> lengthDistribution, int totalSequenceCount) {
        if (totalSequenceCount == 0) {
            return 0;
        }

        double expectedCount = 0;
        for (Map.Entry<Integer, Integer> entry : lengthDistribution.entrySet()) {
            int length = entry.getKey();
            if (length <= 0 || length > probabilities.length) {
                continue;
            }
            expectedCount += entry.getValue() * probabilities[length - 1];
        }
        return expectedCount / totalSequenceCount;
    }

    private double[] calculateSequenceInclusionProbabilities(String subsequence, Map<Character, Double> baseRatios, int sequenceLength) {
        int subsequenceLength = subsequence.length();
        double[] probabilities = new double[sequenceLength];
        if (subsequenceLength == 0 || sequenceLength == 0) {
            return probabilities;
        }

        List<String> overlaps = findSelfOverlaps(subsequence);
        double subsequenceProbability = calculateSubsequenceProbability(subsequence, baseRatios);
        if (subsequenceProbability == 0) {
            return probabilities;
        }

        if (subsequenceLength <= sequenceLength) {
            probabilities[subsequenceLength - 1] = subsequenceProbability;
        }

        for (int index = subsequenceLength; index < sequenceLength; index += 1) {
            double probability = probabilities[index - 1] + subsequenceProbability * (1 - probabilities[index - subsequenceLength]);
            for (String overlap : overlaps) {
                int overlapIndex = index - subsequenceLength + overlap.length();
                double overlapProbability = calculateSubsequenceProbability(overlap, baseRatios);
                if (overlapProbability == 0 || overlapIndex <= 0 || overlapIndex >= probabilities.length) {
                    continue;
                }
                probability -= (subsequenceProbability / overlapProbability) * (probabilities[overlapIndex] - probabilities[overlapIndex - 1]);
            }
            probabilities[index] = Math.max(0, Math.min(1, probability));
        }

        return probabilities;
    }

    private List<String> findSelfOverlaps(String subsequence) {
        if (subsequence.length() <= 1) {
            return List.of();
        }

        List<String> overlaps = new ArrayList<>();
        for (int index = 1; index < subsequence.length(); index += 1) {
            String suffix = subsequence.substring(subsequence.length() - index);
            String prefix = subsequence.substring(0, index);
            if (suffix.equals(prefix)) {
                overlaps.add(suffix);
            }
        }
        return overlaps;
    }

    private double calculateSubsequenceProbability(String subsequence, Map<Character, Double> baseRatios) {
        double probability = 1;
        for (int index = 0; index < subsequence.length(); index += 1) {
            probability *= baseRatios.getOrDefault(subsequence.charAt(index), 0.0);
        }
        return probability;
    }

    private List<FsbcStringResult> toRankedStrings(List<CandidateScore> rankedCandidates) {
        List<FsbcStringResult> rankedStrings = new ArrayList<>(rankedCandidates.size());
        for (int index = 0; index < rankedCandidates.size(); index += 1) {
            CandidateScore candidate = rankedCandidates.get(index);
            rankedStrings.add(new FsbcStringResult(
                    index + 1,
                    candidate.subsequence(),
                    candidate.length(),
                    candidate.observedCount(),
                    candidate.expectedFraction(),
                    candidate.zScore(),
                    candidate.normalizedZScore()
            ));
        }
        return rankedStrings;
    }

    private ClusteringResult clusterAptamers(List<AptamerInput> aptamers, List<CandidateScore> rankedCandidates) {
        Map<Integer, AptamerInput> remainingAptamers = new LinkedHashMap<>();
        for (AptamerInput aptamer : aptamers) {
            remainingAptamers.put(aptamer.aptamerId(), aptamer);
        }

        List<CandidateScore> remainingCandidates = new ArrayList<>(rankedCandidates);
        Map<Integer, Integer> aptamerToCluster = new HashMap<>();
        List<FsbcClusterSeed> clusterSeeds = new ArrayList<>();
        int clusterId = 0;

        while (!remainingCandidates.isEmpty() && !remainingAptamers.isEmpty()) {
            String seed = remainingCandidates.get(0).subsequence();
            List<AptamerInput> members = remainingAptamers.values().stream()
                    .filter(aptamer -> aptamer.sequence().contains(seed))
                    .toList();

            if (!members.isEmpty()) {
                int memberCount = members.size();
                int totalCount = members.stream().mapToInt(AptamerInput::count).sum();
                for (AptamerInput member : members) {
                    aptamerToCluster.put(member.aptamerId(), clusterId);
                    remainingAptamers.remove(member.aptamerId());
                }
                clusterSeeds.add(new FsbcClusterSeed(clusterId, seed, memberCount, totalCount));
                clusterId += 1;
            }

            remainingCandidates.removeIf(candidate -> candidate.subsequence().contains(seed));
        }

        return new ClusteringResult(aptamerToCluster, clusterSeeds);
    }

    private List<String> generateSequences(List<Character> alphabet, int length) {
        List<String> sequences = List.of("");
        for (int index = 0; index < length; index += 1) {
            List<String> next = new ArrayList<>(sequences.size() * alphabet.size());
            for (String prefix : sequences) {
                for (char base : alphabet) {
                    next.add(prefix + base);
                }
            }
            sequences = next;
        }
        return sequences;
    }

    private <T> T executeWithThreadCount(int threadCount, Supplier<T> supplier) {
        if (threadCount == 1) {
            return supplier.get();
        }
        if (threadCount <= 0) {
            return supplier.get();
        }

        ForkJoinPool pool = new ForkJoinPool(threadCount);
        try {
            return pool.submit(supplier::get).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FSBC execution was interrupted", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("FSBC execution failed", exception.getCause());
        } finally {
            pool.shutdown();
        }
    }

    private Stream<Map.Entry<String, Integer>> createSequenceCountStream(Map<String, Integer> sequenceCounts, int threadCount) {
        Stream<Map.Entry<String, Integer>> stream = sequenceCounts.entrySet().stream();
        return threadCount == 1 ? stream : stream.parallel();
    }

    private String normalizeAlphabet(String sequence, boolean rnaSequence) {
        return rnaSequence
                ? sequence.replace('T', 'U')
                : sequence.replace('U', 'T');
    }

    private boolean isValidAlphabet(String sequence, boolean rnaSequence) {
        String alphabet = rnaSequence ? "AUCG" : "ATCG";
        for (int index = 0; index < sequence.length(); index += 1) {
            if (alphabet.indexOf(sequence.charAt(index)) < 0) {
                return false;
            }
        }
        return true;
    }

    record AptamerInput(int aptamerId, String sequence, int count) {
    }

    record InputData(Map<String, Integer> sequenceCounts, List<AptamerInput> aptamers, int totalSequenceCount) {
    }

    record CandidateScore(
            String subsequence,
            int length,
            int observedCount,
            double expectedFraction,
            double zScore,
            double normalizedZScore
    ) {
        CandidateScore withNormalizedZScore(double value) {
            return new CandidateScore(subsequence, length, observedCount, expectedFraction, zScore, value);
        }
    }

    record ClusteringResult(Map<Integer, Integer> aptamerToCluster, List<FsbcClusterSeed> clusterSeeds) {
    }

    public record FsbcRunResult(
            Map<Integer, Integer> aptamerToCluster,
            List<FsbcStringResult> rankedStrings,
            List<FsbcClusterSeed> clusterSeeds,
            int totalSequenceCount,
            int uniqueSequenceCount
    ) {
    }
}
