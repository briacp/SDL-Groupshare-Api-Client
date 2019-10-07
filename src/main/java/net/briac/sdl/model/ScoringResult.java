
package net.briac.sdl.model;

import java.util.List;

public class ScoringResult {

    public Object appliedPenalties;
    public Integer baseScore;
    public Object editDistance;
    public Boolean isExactMatch;
    public Boolean isStructureContextMatch;
    public Integer match;
    public List<MatchingConcordanceRange> matchingConcordanceRanges = null;
    public Boolean memoryTagsDeleted;
    public Integer resolvedPlaceables;
    public Boolean tagMismatch;
    public Boolean targetSegmentDiffers;
    public String textContextMatch;

}
