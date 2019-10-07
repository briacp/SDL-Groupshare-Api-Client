
package net.briac.sdl.model;

import java.util.List;

public class SearchResults {

    public Object subsegmentSearchResults;
    public SourceDocument sourceDocument;
    public Object documentPlaceables;
    public Boolean multipleTranslations;
    public List<Result> results = null;
    public SortOrder sortOrder;
    public String sourceHash;
    public Integer count;
    public String[] tmId;
    public Source source;

}
