package pablog.selextrace.motif;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pablog.selextrace.config.AptaTraceConfiguration;
import pablog.selextrace.domain.experiment.Experiment;
import pablog.selextrace.domain.experiment.SelectionCycle;
import pablog.selextrace.domain.pool.AptamerBounds;
import pablog.selextrace.domain.pool.AptamerPool;
import pablog.selextrace.domain.pool.StructurePool;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class AptaTraceMotif {

    private static final Logger log = LoggerFactory.getLogger(AptaTraceMotif.class);

    private static final char[] NUCLEOTIDES = {'A', 'G', 'T', 'C'};
    private static final int[] FOUR_TO_POWER = {1, 4, 16, 64, 256, 1024, 4096, 16384, 65536, 262144, 1048576, 4194304, 16777216};

    private static final double theta = 10.0;

    public MotifAnalysisRun run(Experiment experiment, StructurePool structurePool, AptaTraceConfiguration config) {
        // Verify params are not null
        Objects.requireNonNull(experiment, "experiment is required");
        Objects.requireNonNull(structurePool, "structurePool is required");
        Objects.requireNonNull(config, "config is required");

        // Extract config params
        final int kmerLength = config.kmerLength();
        final int singletonThreshold = config.alpha();

        List<SelectionCycle> cycles = experiment.getSelectionCycles()
                .stream()
                .filter(Objects::nonNull)
                .toList();

        if (cycles.isEmpty()) {
            throw new IllegalStateException("No selection cycles found");
        }

        List<String> roundNames = cycles.stream().map(SelectionCycle::getName).toList();

        ArrayList<String> kmers = new ArrayList<>();
        generateAllKmers(0, "", kmers, kmerLength);

        KContextTrace[] traces = new KContextTrace[kmers.size()];
        for (int i = 0; i < kmers.size(); i++) {
            traces[i] = new KContextTrace(kmers.get(i), cycles.size(), 5);
        }

        int[] readCounts = new int[cycles.size()];
        int lastRoundCount = populateKmerTraces(experiment.getPool(), structurePool, cycles, kmerLength, singletonThreshold, traces, readCounts);

        // TODO: Check -> original did reverse order -> (int i = numKmers - 1; i >= 0; i--)
        for (KContextTrace trace : traces) {
            trace.setStrongPresence(lastRoundCount);
            trace.checkEnoughOccurrences();
            if (trace.hasEnoughOccurrences()) {
                trace.normalizeProfile();
                trace.deriveSelectionContext();
                trace.calculateSingletonKLScore(readCounts);
                trace.calculateKLScore(readCounts);
            }
        }

        // the arraylist storing background context shifting scores for aptamers with singleton occurrences
        ArrayList<Double> singletonKLScores = new ArrayList<>();
        // the arraylist storing context shifting scores for aptamers with non-singleton occurrences
        ArrayList<Double> kmerKLScores = new ArrayList<>();
        // the arraylist storing the proportions of kmer occurrences in the final selection round
        ArrayList<Double> proportions = new ArrayList<>();

        for (KContextTrace trace : traces) {
            if (trace.hasEnoughOccurrences()
                    && trace.getSingletonKLScore() >= 0.000001
                    && trace.getKLScore() >= 0.000001) {
                singletonKLScores.add(Math.log(trace.getSingletonKLScore()));
                kmerKLScores.add(Math.log(trace.getKLScore()));
                proportions.add(trace.getProportion());
            }
        }

        if (kmerKLScores.isEmpty()) {
            log.info("Could not find enough aptamers above alpha to continue with motif elucidation");
            return new MotifAnalysisRun(roundNames, List.of(), 0, lastRoundCount);
        }

        // the array storing background context shifting scores for aptamers with singleton occurrences
        double[] singletonKLScoreArray = toDoubleArray(singletonKLScores);
        // the sorted array storing context shifting scores for aptamers with non-singleton occurrences
        double[] sortedKLScoreArray = toDoubleArray(kmerKLScores);
        // the sorted array storing the proportions of kmer occurrences in the final selection round
        double[] sortedProportionArray = toDoubleArray(proportions);
        Arrays.sort(sortedKLScoreArray);
        Arrays.sort(sortedProportionArray);

        int thetaIndex = percentileIndex(sortedKLScoreArray.length);
        double topThetaValue = sortedKLScoreArray[thetaIndex];
        double topThetaProportion = sortedProportionArray[thetaIndex];

        double pValue = 0.01;
        boolean hasSignificantKmer = false;
        for (KContextTrace trace : traces) {
            if (trace.hasEnoughOccurrences() && trace.isSignificant(singletonKLScoreArray, topThetaValue, pValue)) {
                hasSignificantKmer = true;
                break;
            }
        }
        if (!hasSignificantKmer) {
            pValue = 0.05;
        }

        // to select the kmers with statistically significant context scores
        // in the case there are many such scores just take the ones within top
        // 10 percent only
        ArrayList<KContextTrace> significant = new ArrayList<>();
        for (KContextTrace trace : traces) {
            if (trace.hasEnoughOccurrences()) {
                boolean significantByScore = trace.isSignificant(singletonKLScoreArray, topThetaValue, pValue);
                boolean significantByPresence = trace.getProportion() >= topThetaProportion
                        && trace.isSignificant(singletonKLScoreArray, -3.0, pValue)
                        && trace.hasStrongPresence();

                if (significantByScore || significantByPresence) {
                    significant.add(trace);
                }
            }
        }

        Collections.sort(significant);
        if (significant.isEmpty()) {
            // TODO: not in the original implementation
            log.info("Could not find any significant kmers to continue with motif elucidation");
            return new MotifAnalysisRun(roundNames, List.of(), 0, lastRoundCount);
        }

        ClusterState clusterState = clusterSignificantKmers(significant, cycles.size());
        populateMotifProfiles(experiment.getPool(), structurePool, cycles, kmerLength, singletonThreshold, readCounts, clusterState);
        List<MotifAnalysisProfile> profiles = finalizeProfiles(
            experiment.getPool(),
            cycles,
            kmerLength,
            clusterState,
            readCounts,
            config.filterClusters()
        );

        return new MotifAnalysisRun(roundNames, profiles, significant.size(), lastRoundCount);
    }

    private int populateKmerTraces(
            AptamerPool pool,
            StructurePool structurePool,
            List<SelectionCycle> cycles,
            int kmerLength,
            int singletonThreshold,
            KContextTrace[] traces,
            int[] readCounts
    ) {
        int lastRoundCount = 0;
        int[] observedRounds = new int[cycles.size()];
        int[] observedCounts = new int[cycles.size()];

        // TODO: Check continues (they were not there on the original implementation)
        for (Map.Entry<Integer, byte[]> aptamerEntry : pool.inverse_view_iterator()) {
            int aptamerId = aptamerEntry.getKey();
            String aptamer = new String(aptamerEntry.getValue(), StandardCharsets.UTF_8);
            AptamerBounds bounds = pool.getAptamerBounds(aptamerId);
            if (bounds == null || aptamer.length() < kmerLength) {
                continue;
            }

            int observedCount = collectObservedRounds(cycles, aptamerId, observedRounds, observedCounts);
            if (observedCount == 0) {
                continue;
            }

            for (int index = 0; index < observedCount; index++) {
                readCounts[observedRounds[index]] += observedCounts[index];
                if (observedRounds[index] == cycles.size() - 1) {
                    lastRoundCount += observedCounts[index];
                }
            }

            double[][] contextPrefix = buildContextPrefixArray(structurePool.getStructure(aptamerId), aptamer.length(), aptamerId);
            IntOpenHashSet seen = new IntOpenHashSet();
            double[] averageContext = new double[5];
            int startPos = bounds.startIndex + kmerLength - 1;
            int endPos = bounds.endIndex;
            int kmerId = -1;

            // iterate through every kmer of the aptamer under consideration
            // and sum up its number of occurrences and the sums of the
            // probabilities of being in various structural context
            for (int position = startPos; position < endPos; position++) {
                if (position >= aptamer.length()) {
                    break;
                }

                if (position == startPos) {
                    kmerId = calculateId(aptamer.substring(position - kmerLength + 1, position + 1));
                } else {
                    kmerId = calculateNewId(kmerId, aptamer.charAt(position - kmerLength), aptamer.charAt(position), kmerLength);
                }

                for (int context = 0; context < 5; context++) {
                    averageContext[context] = (contextPrefix[context][position] - contextPrefix[context][position - kmerLength + 1])
                            / (kmerLength * 1.0);
                }

                if (!seen.contains(kmerId)) {
                    seen.add(kmerId);
                    for (int index = 0; index < observedCount; index++) {
                        traces[kmerId].addTotalCount(observedRounds[index], observedCounts[index]);
                    }
                }

                for (int index = 0; index < observedCount; index++) {
                    if (singletonThreshold > 0) {
                        if (observedCounts[index] > singletonThreshold) {
                            traces[kmerId].addContextProb(observedRounds[index], observedCounts[index], averageContext);
                        }
                        if (observedCounts[index] <= singletonThreshold) {
                            traces[kmerId].addSingletonContextProb(observedRounds[index], observedCounts[index], averageContext);
                        }
                    } else {
                        traces[kmerId].addSingletonContextProb(observedRounds[index], observedCounts[index], averageContext);
                    }
                }
            }
        }

        return lastRoundCount;
    }

    private ClusterState clusterSignificantKmers(List<KContextTrace> significant, int roundCount) {
        boolean[] assigned = new boolean[significant.size()];
        // map a kmer id to a cluster id
        Int2IntOpenHashMap kmerToCluster = new Int2IntOpenHashMap();
        // the motif profiles of clusters of kmers
        ArrayList<MotifProfile> profiles = new ArrayList<>();
        // the array storing the ids of the seeds of the clusters
        ArrayList<Integer> seedIndexes = new ArrayList<>();

        // iterate the sorted list of kmers by their proportions starting with
        // the kmer with the highest proportion in the last selection round
        // pick the first one that is not chosen as the seed of a new cluster
        // then find similar kmers with less portion and put in the cluster
        for (int i = significant.size() - 1; i >= 0; i--) {
            if (assigned[i] || !significant.get(i).hasStrongPresence()) {
                continue;
            }

            ArrayList<String> clusterKmers = new ArrayList<>();
            clusterKmers.add(significant.get(i).getKmer());
            assigned[i] = true;

            int clusterId = profiles.size();
            kmerToCluster.put(calculateId(significant.get(i).getKmer()), clusterId);

            // pick similar kmers and add to the cluster
            for (int candidate = i - 1; candidate >= 0; candidate--) {
                if (assigned[candidate]) {
                    continue;
                }

                KContextTrace seed = significant.get(i);
                KContextTrace trace = significant.get(candidate);
                if (hasGoodOverlap(seed.getKmer(), trace.getKmer())
                        && trace.getSelectionContext() == seed.getSelectionContext()) {
                    clusterKmers.add(trace.getKmer());
                    kmerToCluster.put(calculateId(trace.getKmer()), clusterId);
                    assigned[candidate] = true;
                }
            }

            String[] alignments = multipleAlignment(clusterKmers);
            MotifProfile profile = new MotifProfile(alignments[0].length(), roundCount);
            profile.setSeed(significant.get(i).getKmer());
            profile.setSeedpValue(significant.get(i).getPValue());  // TODO: find in the original
            for (int alignmentIndex = 0; alignmentIndex < clusterKmers.size(); alignmentIndex++) {
                profile.addKmer(clusterKmers.get(alignmentIndex)); // TODO: revise
                profile.addKmerAlignment(clusterKmers.get(alignmentIndex), alignments[alignmentIndex]);
            }

            profiles.add(profile);
            seedIndexes.add(i);
        }

        return new ClusterState(significant, profiles, kmerToCluster, seedIndexes);
    }

    private void populateMotifProfiles(
            AptamerPool pool,
            StructurePool structurePool,
            List<SelectionCycle> cycles,
            int kmerLength,
            int singletonThreshold,
            int[] readCounts,
            ClusterState clusterState
    ) {
        int[] observedRounds = new int[cycles.size()];
        int[] observedCounts = new int[cycles.size()];
        Int2IntOpenHashMap idToCount = clusterState.idToCount;

        for (Map.Entry<Integer, byte[]> aptamerEntry : pool.inverse_view_iterator()) {
            int aptamerId = aptamerEntry.getKey();
            String aptamer = new String(aptamerEntry.getValue(), StandardCharsets.UTF_8);
            AptamerBounds bounds = pool.getAptamerBounds(aptamerId);
            if (bounds == null || aptamer.length() < kmerLength) {
                continue;
            }

            int observedCount = collectObservedRounds(cycles, aptamerId, observedRounds, observedCounts);
            // we need to make sure that the aptamer in question is not specific to a negative round.
            // in that case `observedCount` would be 0 here and we can skip the rest of the calculation
            if (observedCount == 0) {
                continue;
            }

            double[][] contextPrefix = buildContextPrefixArray(structurePool.getStructure(aptamerId), aptamer.length(), aptamerId);
            IntOpenHashSet seenClusters = new IntOpenHashSet();
            double[] averageContext = new double[5];
            int startPos = bounds.startIndex + kmerLength - 1;
            int endPos = bounds.endIndex;
            int kmerId = -1;
            boolean presentInLastRound = observedRounds[observedCount - 1] == cycles.size() - 1;
            int lastRoundAptamerCount = presentInLastRound ? observedCounts[observedCount - 1] : 0;
            if (presentInLastRound) {
                idToCount.put(aptamerId, lastRoundAptamerCount); // TODO: revise
            }

            // iterate through each kmer of the aptamer and decide whether the
            // kmer is in the list of kmers with significant context shifting
            // scores
            // if it is, summing the total number of occurrences and the sums of
            // probabilities of being in various structural context of its
            // motifs
            for (int position = startPos; position < endPos; position++) {
                if (position >= aptamer.length()) {
                    break;
                }

                String kmer = aptamer.substring(position - kmerLength + 1, position + 1);
                if (position == startPos) {
                    kmerId = calculateId(kmer);
                } else {
                    kmerId = calculateNewId(kmerId, aptamer.charAt(position - kmerLength), aptamer.charAt(position), kmerLength);
                }

                if (!clusterState.kmerToCluster.containsKey(kmerId)) {
                    continue;
                }

                int motifIndex = clusterState.kmerToCluster.get(kmerId);
                MotifProfile profile = clusterState.profiles.get(motifIndex);

                if (presentInLastRound) {
                    String alignment = fillBlanks(profile.getKmerAlignment(kmer), aptamer, position - kmerLength + 1);
                    if (lastRoundAptamerCount > singletonThreshold) {
                        profile.addToPWM(alignment, lastRoundAptamerCount);
                    } else {
                        profile.addToSingletonPWM(alignment, lastRoundAptamerCount);
                    }
                }

                for (int context = 0; context < 5; context++) {
                    averageContext[context] = (contextPrefix[context][position] - contextPrefix[context][position - kmerLength + 1])
                            / (kmerLength * 1.0);
                }

                if (!seenClusters.contains(motifIndex)) {
                    seenClusters.add(motifIndex);
                    for (int index = 0; index < observedCount; index++) {
                        profile.addTotalCount(observedRounds[index], observedCounts[index]);
                    }
                    if (presentInLastRound && lastRoundAptamerCount > singletonThreshold) {
                        profile.addOccId(aptamerId, lastRoundAptamerCount);
                    }
                }

                for (int i = 0; i < observedCount; i++) {
                    if (observedCounts[i] > singletonThreshold || observedRounds[i] == 0) {
                        profile.addContextProb(observedRounds[i], observedCounts[i], averageContext);
                    }
                    if (observedCounts[i] <= singletonThreshold) {
                        profile.addSingletonContextProb(observedRounds[i], observedCounts[i], averageContext);
                    }
                }
            }
        }
    }

    private List<MotifAnalysisProfile> finalizeProfiles(
            AptamerPool pool,
            List<SelectionCycle> cycles,
            int kmerLength,
            ClusterState clusterState,
            int[] readCounts,
            boolean filterClusters
    ) {
        boolean[] filtered = new boolean[clusterState.profiles.size()];

        if (filterClusters) {
            IntOpenHashSet currentOccurrences = new IntOpenHashSet();
            // filters out the smaller motifs that their intersection with
            // larger motifs more than 2/3 of their sizes
            for (int i = 0; i < clusterState.profiles.size(); i++) {
                MotifProfile profile = clusterState.profiles.get(i);
                if (profile.getOverlapPercentage(currentOccurrences, clusterState.idToCount) <= 0.67) {
                    profile.addTo(currentOccurrences, clusterState.idToCount);
                } else {
                    filtered[i] = true;
                }
            }
        }

        List<List<MotifClusterMember>> memberAptamers = collectMemberAptamers(
                pool,
                cycles,
                kmerLength,
                clusterState,
                filtered,
                readCounts
        );

        ArrayList<MotifAnalysisProfile> snapshots = new ArrayList<>();
        for (int index = 0; index < clusterState.profiles.size(); index++) {
            if (filtered[index]) {
                continue;
            }

            MotifProfile profile = clusterState.profiles.get(index);
            KContextTrace seed = clusterState.significant.get(clusterState.seedIndexes.get(index));
            profile.normalizeProfile();
            profile.calculateProportion(readCounts);
            profile.trim();

            LinkedHashMap<String, String> alignments = new LinkedHashMap<>();
            for (String kmer : profile.getKmers()) {
                alignments.put(kmer, profile.getKmerAlignment(kmer));
            }

            ArrayList<Integer> aptamerIds = new ArrayList<>(profile.getOccSet());
            Collections.sort(aptamerIds);

            double[][] pwm = profile.getPWM();
            snapshots.add(new MotifAnalysisProfile(
                    profile.getSeed(),
                    deriveConsensus(pwm),
                    seed.getPValue(),
                    seed.getProportion(),
                    profile.getProportion(),
                    seed.getSelectionContext(),
                    profile.getKmers(), // TODO: revise, result differs from original implementation
                    alignments,
                    aptamerIds,
                    memberAptamers.get(index),
                    pwm,
                    profile.getTraceMatrix()
            ));
        }

        return snapshots;
    }

    private List<List<MotifClusterMember>> collectMemberAptamers(
            AptamerPool pool,
            List<SelectionCycle> cycles,
            int kmerLength,
            ClusterState clusterState,
            boolean[] filtered,
            int[] readCounts
    ) {
        List<List<MotifClusterMember>> membersByMotif = new ArrayList<>(clusterState.profiles.size());
        for (int index = 0; index < clusterState.profiles.size(); index++) {
            membersByMotif.add(new ArrayList<>());
        }

        if (cycles.isEmpty()) {
            return membersByMotif;
        }

        SelectionCycle lastRound = cycles.getLast();

        int[] aptamerIds = new int[lastRound.getUniqueSize()];
        int[] aptamerCounts = new int[lastRound.getUniqueSize()];
        int cursor = 0;
        for (Map.Entry<Integer, Integer> entry : lastRound.iterator()) {
            aptamerIds[cursor] = entry.getKey();
            aptamerCounts[cursor] = entry.getValue();
            cursor++;
        }

        sortByCountsAscending(aptamerIds, aptamerCounts);
        double lastRoundReadCount = readCounts.length == 0 ? 0.0 : readCounts[readCounts.length - 1];

        for (int index = aptamerIds.length - 1; index >= 0; index--) {
            int aptamerId = aptamerIds[index];
            byte[] aptamerBytes = pool.getAptamer(aptamerId);
            AptamerBounds bounds = pool.getAptamerBounds(aptamerId);
            if (aptamerBytes == null || bounds == null) {
                continue;
            }

            String aptamer = new String(aptamerBytes, StandardCharsets.UTF_8);
            int aptamerCount = aptamerCounts[index];
            int startPos = bounds.startIndex + kmerLength - 1;
            int endPos = bounds.endIndex;
            IntOpenHashSet seenClusters = new IntOpenHashSet();
            int kmerId = -1;

            for (int position = startPos; position < endPos; position++) {
                if (position >= aptamer.length()) {
                    break;
                }

                String kmer = aptamer.substring(position - kmerLength + 1, position + 1);
                if (position == startPos) {
                    kmerId = calculateId(kmer);
                } else {
                    kmerId = calculateNewId(kmerId, aptamer.charAt(position - kmerLength), aptamer.charAt(position), kmerLength);
                }

                if (!clusterState.kmerToCluster.containsKey(kmerId)) {
                    continue;
                }

                int motifIndex = clusterState.kmerToCluster.get(kmerId);
                if (filtered[motifIndex] || seenClusters.contains(motifIndex)) {
                    continue;
                }

                double proportion = lastRoundReadCount <= 0.0 ? 0.0 : aptamerCount / lastRoundReadCount;
                membersByMotif.get(motifIndex).add(new MotifClusterMember(aptamerId, aptamerCount, proportion));
                seenClusters.add(motifIndex);
            }
        }

        return membersByMotif;
    }

    private void sortByCountsAscending(int[] ids, int[] counts) {
        for (int i = 1; i < counts.length; i++) {
            int currentCount = counts[i];
            int currentId = ids[i];
            int j = i - 1;
            while (j >= 0 && counts[j] > currentCount) {
                counts[j + 1] = counts[j];
                ids[j + 1] = ids[j];
                j--;
            }
            counts[j + 1] = currentCount;
            ids[j + 1] = currentId;
        }
    }

    private int collectObservedRounds(
            List<SelectionCycle> cycles,
            int aptamerId,
            int[] observedRounds,
            int[] observedCounts
    ) {
        int observed = 0;
        for (int round = 0; round < cycles.size(); round++) {
            int cardinality = cycles.get(round).getAptamerCardinality(aptamerId);
            if (cardinality > 0) {
                observedRounds[observed] = round;
                observedCounts[observed] = cardinality;
                observed++;
            }
        }
        return observed;
    }

    private double[][] buildContextPrefixArray(double[] rawProfile, int aptamerLength, int aptamerId) {
        if (rawProfile == null) {
            throw new IllegalStateException("Missing structure profile for aptamer " + aptamerId);
        }
        if (rawProfile.length != aptamerLength * 5) {
            throw new IllegalStateException("Structure profile length does not match aptamer length for aptamer " + aptamerId);
        }

        double[][] contextPrefix = new double[5][aptamerLength];
        for (int context = 0; context < 5; context++) {
            contextPrefix[context][0] = rawProfile[context * aptamerLength];
            for (int index = 1; index < aptamerLength; index++) {
                contextPrefix[context][index] = rawProfile[context * aptamerLength + index] + contextPrefix[context][index - 1];
            }
        }
        return contextPrefix;
    }

    private int percentileIndex(int length) {
        int index = (int) Math.floor(((100.0 - AptaTraceMotif.theta) / 100.0) * length);

        // TODO: legacy logging
        // in the case we have many significant context shifting scores just
        // take at most top 10 percent of the scores
        // and topThetaValue is the 90 quantile of all the context shifting
        // scores
        log.info("sortedKLScoreArr.length: {}", length);
        log.info("index: {}", index);

        if (index < 0) {
            // TODO: Not in the original implementation
            log.info("unexpected index: {}", index);
            return 0;
        }
        if (index >= length) {
            // TODO: Not in the original implementation
            log.info("unexpected index: {}", index);
            return length - 1;
        }
        return index;
    }

    private double[] toDoubleArray(List<Double> values) {
        double[] array = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    private static int getNucleotideId(char nucleotide) {
        return switch (nucleotide) {
            case 'A' -> 0;
            case 'G' -> 1;
            case 'T', 'U' -> 2;
            case 'C' -> 3;
            default -> -1;
        };
    }

    /**
     * A procedure to calculate the id of a kmer when it is overlapping with its
     * left kmer when sliding character by character of when sliding from left to
     * right of an aptamer sequence from left to right
     *
     * @param previousId
     *            the id of left kmer
     * @param outgoing
     *            the leftmost character of the left kmer
     * @param incoming
     *            the rightmost character of the right kmer
     * @param kmerLength
     *            length of the kmer
     * @return id of the right kmer
     */
    private static int calculateNewId(int previousId, char outgoing, char incoming, int kmerLength) {
        return 4 * (previousId - getNucleotideId(outgoing) * FOUR_TO_POWER[kmerLength - 1]) + getNucleotideId(incoming);
    }

    /**
     * return the id of a given k-mer, the id will be the index of the given k-mer
     * in kmersArr
     */
    private static int calculateId(String kmer) {
        int id = 0;
        for (int i = 0; i < kmer.length(); i++) {
            id += getNucleotideId(kmer.charAt(i)) * FOUR_TO_POWER[kmer.length() - i - 1];
        }
        return id;
    }

    /**
     * A recursive procedure to generate all possible number of k-mers given the
     * length klength
     */
    private void generateAllKmers(int depth, String current, ArrayList<String> kmers, int kmerLength) {
        if (depth == kmerLength) {
            kmers.add(current);
            return;
        }

        for (char nucleotide : NUCLEOTIDES) {
            generateAllKmers(depth + 1, current + nucleotide, kmers, kmerLength);
        }
    }

    private String fillBlanks(String alignment, String aptamer, int position) {
        StringBuilder filled = new StringBuilder(alignment.length());
        int firstPosition = 0;
        for (int i = 0; i < alignment.length(); i++) {
            if (alignment.charAt(i) != '-') {
                firstPosition = i;
                break;
            }
        }

        for (int i = 0; i < alignment.length(); i++) {
            char nucleotide = alignment.charAt(i);
            if (nucleotide != '-') {
                filled.append(nucleotide);
                continue;
            }

            // In case we have no primers, we need to take care of the boundary cases.
            int aptamerIndex = position - (firstPosition - i);
            if (aptamerIndex < 0 || aptamerIndex >= aptamer.length()) {
                filled.append('-');
            } else {
                filled.append(aptamer.charAt(aptamerIndex));
            }
        }

        return filled.toString();
    }

    /**
     * Pair alignment of two kmers `left` and `right`
     */
    private String[] pairAlignment(String left, String right) {
        String bestLeft = "";
        String bestRight = "";
        int maxShift = 2;
        int bestScore = left.length() + 1;

        for (int shift = -maxShift; shift <= maxShift; shift++) {
            StringBuilder shiftedLeft = new StringBuilder(left);
            StringBuilder shiftedRight = new StringBuilder(right);
            int replacements = 0;
            int indels = 0;
            int bestCommonSubstring = 0;
            int currentCommonSubstring = 0;

            if (shift < 0) {
                for (int count = 0; count < -shift; count++) {
                    shiftedLeft.insert(0, "-");
                    shiftedRight.append("-");
                }
            } else if (shift > 0) {
                for (int count = 0; count < shift; count++) {
                    shiftedLeft.append("-");
                    shiftedRight.insert(0, "-");
                }
            }

            for (int index = 0; index < shiftedLeft.length(); index++) {
                char leftChar = shiftedLeft.charAt(index);
                char rightChar = shiftedRight.charAt(index);
                if (leftChar == '-' || rightChar == '-') {
                    indels++;
                    currentCommonSubstring = 0;
                } else if (leftChar != rightChar) {
                    replacements++;
                    currentCommonSubstring = 0;
                } else {
                    currentCommonSubstring++;
                    bestCommonSubstring = Math.max(bestCommonSubstring, currentCommonSubstring);
                }
            }

            if ((indels + replacements) <= bestScore && indels <= 4) {
                bestScore = indels + replacements;
                bestLeft = shiftedLeft.toString();
                bestRight = shiftedRight.toString();
            }
        }

        return new String[]{bestLeft, bestRight};
    }

    /**
     * To compute alignment of all the kmers in a give cluster of kmers stored in
     * `kmers`
     */
    private String[] multipleAlignment(ArrayList<String> kmers) {
        String seed = kmers.getFirst();
        String[] alignments = new String[kmers.size()];
        int[] leftGaps = new int[kmers.size()];     // number of left gaps
        int[] rightGaps = new int[kmers.size()];    // number of right gaps
        int maxLeft = 0;    // max gaps on the left
        int maxRight = 0;   // max gaps on the right

        alignments[0] = seed;
        for (int i = 1; i < kmers.size(); i++) {
            String[] pair = pairAlignment(seed, kmers.get(i));
            String seedAlignment = pair[0];
            alignments[i] = pair[1];

            int left = 0;
            while (left < seedAlignment.length() && seedAlignment.charAt(left) == '-') {
                left++;
            }
            leftGaps[i] = left;

            int right = seedAlignment.length();
            while (right > 0 && seedAlignment.charAt(right - 1) == '-') {
                right--;
            }
            rightGaps[i] = alignments[i].length() - right;

            maxLeft = Math.max(maxLeft, leftGaps[i]);
            maxRight = Math.max(maxRight, rightGaps[i]);
        }

        for (int i = 0; i < kmers.size(); i++) {
            while (leftGaps[i] < maxLeft) {
                alignments[i] = "-" + alignments[i];
                leftGaps[i]++;
            }
            while (rightGaps[i] < maxRight) {
                alignments[i] = alignments[i] + "-";
                rightGaps[i]++;
            }
        }

        return alignments;
    }

    /**
     * Determine whether two kmers has good overlap to put in the same cluster or
     * not
     *
     * @param left
     *            the first kmer
     * @param right
     *            the second kmer
     * @return true if they have good overlap
     */
    private boolean hasGoodOverlap(String left, String right) {
        int maxShift = 2;
        int bestScore = left.length() + 1;
        int bestIndels = bestScore;
        int bestReplacements = bestScore;
        int bestCommonSubstring = 0; // save the longest common substring of all the alignments when sliding kmer a from left to right of kmer b

        // sliding the kmer `left` from left to right of kmer `right`
        for (int shift = -maxShift; shift <= maxShift; shift++) {
            StringBuilder shiftedLeft = new StringBuilder(left);
            StringBuilder shiftedRight = new StringBuilder(right);
            int replacements = 0;
            int indels = 0;
            int commonSubstring = 0;
            int longestCommonSubstring = 0;

            if (shift < 0) {
                for (int count = 0; count < -shift; count++) {
                    shiftedLeft.insert(0, "-");
                    shiftedRight.append("-");
                }
            } else if (shift > 0) {
                for (int count = 0; count < shift; count++) {
                    shiftedLeft.append("-");
                    shiftedRight.insert(0, "-");
                }
            }

            for (int index = 0; index < shiftedLeft.length(); index++) {
                char leftChar = shiftedLeft.charAt(index);
                char rightChar = shiftedRight.charAt(index);
                if (leftChar == '-' || rightChar == '-') {
                    indels++;
                    commonSubstring = 0;
                } else if (leftChar != rightChar) {
                    replacements++;
                    commonSubstring = 0;
                } else {
                    commonSubstring++;
                    longestCommonSubstring = Math.max(longestCommonSubstring, commonSubstring);
                }
            }

            if ((indels + replacements) <= bestScore) {
                bestScore = indels + replacements;
                bestIndels = indels;
                bestReplacements = replacements;
                bestCommonSubstring = longestCommonSubstring;
            }
        }

        if (bestIndels == 0) {
            return bestReplacements == 1;
        }
        if (left.length() <= 6) {
            return bestCommonSubstring >= 4;
        }
        // left.length() => 7
        return bestCommonSubstring >= 5;
    }

    private String deriveConsensus(double[][] pwm) {
        if (pwm.length == 0) {
            return "";
        }

        char[] alphabet = {'A', 'C', 'G', 'T'};
        StringBuilder consensus = new StringBuilder(pwm.length);
        for (double[] row : pwm) {
            int bestIndex = 0;
            double bestValue = Double.NEGATIVE_INFINITY;
            for (int index = 0; index < row.length; index++) {
                if (row[index] > bestValue) {
                    bestValue = row[index];
                    bestIndex = index;
                }
            }
            consensus.append(alphabet[bestIndex]);
        }
        return consensus.toString();
    }

    private static final class ClusterState {
        private final List<KContextTrace> significant;
        private final ArrayList<MotifProfile> profiles;
        private final Int2IntOpenHashMap kmerToCluster;
        private final ArrayList<Integer> seedIndexes;
        private final Int2IntOpenHashMap idToCount = new Int2IntOpenHashMap();

        private ClusterState(
                List<KContextTrace> significant,
                ArrayList<MotifProfile> profiles,
                Int2IntOpenHashMap kmerToCluster,
                ArrayList<Integer> seedIndexes
        ) {
            this.significant = significant;
            this.profiles = profiles;
            this.kmerToCluster = kmerToCluster;
            this.seedIndexes = seedIndexes;
        }
    }
}