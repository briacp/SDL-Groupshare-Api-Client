package net.briac.sdl.model;

public class Organization {
    public String UniqueId;
    public String Name;
    public String Description;
    public String Path;
    public String ParentOrganizationId;
    public String[] ChildOrganizations;
    public Boolean IsLibrary;
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Organization [UniqueId=").append(UniqueId).append(", Name=").append(Name).append(", Description=").append(Description).append("]");
        return builder.toString();
    }

}
