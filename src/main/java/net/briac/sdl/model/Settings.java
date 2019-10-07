package net.briac.sdl.model;

import java.util.List;

public class Settings {
    public Integer minScore;
    public Integer maxResults;
    public List<Filter> filters = null;
    public HardFilter hardFilter;
    public List<Penalty> penalties = null;
    public Boolean includeTokens;
    public Boolean includeAlignmentData;
}
