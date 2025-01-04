package at.rtr.rmbt.map.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class MapFiltersResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("map_filters")
    private List<MapFilter> mapFilters;

    public void addMapFilter(MapFilter mapFilter) {
        if (this.mapFilters == null) {
            this.mapFilters = new ArrayList<>();
        }
        this.mapFilters.add(mapFilter);
    }

    @Getter
    @Setter
    public static class MapFilter extends Option {
        @JsonProperty("icon")
        private String icon;
    }

    @Getter
    @Setter
    public static class Option {

        @JsonProperty("title")
        private String title;

        @JsonProperty("summary")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String summary;

        @JsonProperty("options")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<Option> options;

        @JsonProperty("functions")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<Function> functions;

        @JsonProperty("params")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Map<String, Object> params;

        @JsonProperty("depends_on")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private Map<String, Boolean> dependsOn;

        @JsonProperty("default")
        private boolean isDefault;

        public void addOption(Option option) {
            if (this.options == null) {
                this.options = new ArrayList<>();
            }
            this.options.add(option);
        }


        public void addDependsOn(String key, Boolean value) {
            if (this.dependsOn == null) {
                this.dependsOn = new LinkedHashMap<>();
            }
            this.dependsOn.put(key, value);
        }


        public void addParameter(String key, Object value) {
            if (this.params == null) {
                this.params = new LinkedHashMap<>();
            }
            this.params.put(key, value);
        }

        public void addFunction(Function function) {
            if (this.functions == null) {
                this.functions = new ArrayList<>();
            }
            this.functions.add(function);
        }
    }

    @Getter
    @Setter
    public static class Function {

        @JsonProperty("func_name")
        private String funcName;

        @JsonProperty("func_params")
        private Map<String, Object> funcParams;

        public void addFuncParameter(String key, Object value) {
            if (this.funcParams == null) {
                this.funcParams = new LinkedHashMap<>();
            }
            this.funcParams.put(key, value);
        }
    }
}