package software.bernie.geckolib3.geo.raw.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExtraInfo {

    private String name;
    private String tips = "";
    private String[] extraAnimationNames = null;
    private String[] authors = null;
    private String license = "All Rights Reserved";

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("tips")
    public String getTips() {
        return tips;
    }

    @JsonProperty("tips")
    public void setTips(String tips) {
        this.tips = tips;
    }

    @JsonProperty("extra_animation_names")
    public String[] getExtraAnimationNames() {
        return extraAnimationNames;
    }

    @JsonProperty("extra_animation_names")
    public void setExtraAnimationNames(String[] extraAnimationNames) {
        this.extraAnimationNames = extraAnimationNames;
    }

    @JsonProperty("authors")
    public String[] getAuthors() {
        return authors;
    }

    @JsonProperty("authors")
    public void setAuthors(String[] authors) {
        this.authors = authors;
    }

    @JsonProperty("license")
    public String getLicense() {
        return license;
    }

    @JsonProperty("license")
    public void setLicense(String license) {
        this.license = license;
    }
}
