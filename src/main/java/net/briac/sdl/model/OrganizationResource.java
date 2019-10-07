package net.briac.sdl.model;

public class OrganizationResource {
    public String Id;
    public String Name;
    public String Description;
    public String ResourceType;
    public String ParentOragnizationId; // sic
    public String[] LinkedOragnizationIds;;
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("OrganizationResource [Id=").append(Id).append(", Name=").append(Name).append(", Description=").append(Description).append(", ResourceType=").append(ResourceType).append("]");
        return builder.toString();
    }

}
