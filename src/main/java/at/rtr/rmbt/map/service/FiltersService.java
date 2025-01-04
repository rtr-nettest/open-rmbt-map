package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.constant.Constants;
import at.rtr.rmbt.map.dto.MapFiltersResponse;
import at.rtr.rmbt.map.dto.TilesInfoRequest;
import at.rtr.rmbt.map.model.TilesInfoOperatorResults;
import at.rtr.rmbt.map.util.MapServerOptions;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FiltersService {
    @Value("${app.supportedLanguages}")
    private String supportedLanguagesAsArray;

    @Value("${app.defaultLanguage}")
    private String defaultLanguage;

    @PersistenceContext
    private EntityManager entityManager;

    public MapFiltersResponse getMapFilters(TilesInfoRequest request) {
        MapFiltersResponse response = new MapFiltersResponse();

        ResourceBundle labels;
        String language;
        final List<String> langs = Arrays.asList(supportedLanguagesAsArray.split(","));
        if (langs.contains(request.getLanguage())) {
            language = request.getLanguage();
        } else {
            language = defaultLanguage;
        }
        labels = ResourceBundle.getBundle("SystemMessages", LocaleUtils.toLocale(language));


        final double[] statisticalMethodArray = {0.8, 0.5, 0.2};
        final boolean[] defaultArray = {false, true, false};

        final MapFiltersResponse.MapFilter statisticalMethods = new MapFiltersResponse.MapFilter();
        statisticalMethods.setTitle(labels.getString("MAP_FILTER_STATISTICAL_METHOD"));
        statisticalMethods.setOptions(new ArrayList<>());

        for (int stat = 1; stat <= statisticalMethodArray.length; stat++) {
            final MapFiltersResponse.Option o = new MapFiltersResponse.Option();
            o.setTitle(labels.getString("MAP_FILTER_STATISTICAL_METHOD_" + stat + "_TITLE"));
            o.setSummary(labels.getString("MAP_FILTER_STATISTICAL_METHOD_" + stat + "_SUMMARY"));
            o.addParameter("statistical_method", statisticalMethodArray[stat - 1]);
            o.setDefault(defaultArray[stat - 1]);
            statisticalMethods.getOptions().add(o);
        }


        response.addMapFilter(getMapTypeList(labels));
        response.addMapFilter(getTechnology(labels));
        response.addMapFilter(getOperators(labels, true));
        response.addMapFilter(getOperators(labels, false));
        response.addMapFilter(getAppearanceTypeList(labels));
        response.addMapFilter(getOverlayTypeList(labels));
        response.addMapFilter(getTimes(labels));
        response.addMapFilter(statisticalMethods);

        return response;
    }

    private MapFiltersResponse.MapFilter getMapTypeList(ResourceBundle labels) {
        String lastType = null;

        final MapFiltersResponse.MapFilter mapTypeOption = new MapFiltersResponse.MapFilter();
        mapTypeOption.setTitle(labels.getString(String.format("MAP_TYPE")));
        mapTypeOption.setIcon(labels.getString("MAP_TYPE_ICON"));

        final MapFiltersResponse.Function dropOptionFunction = new MapFiltersResponse.Function();
        dropOptionFunction.setFuncName("drop_param");
        dropOptionFunction.addFuncParameter("key", "map_type_is_mobile");
        mapTypeOption.addFunction(dropOptionFunction);

        MapFiltersResponse.Option subOption = new MapFiltersResponse.Option();

        final Map<String, MapServerOptions.MapOption> mapOptionMap = MapServerOptions.getMapOptionMap();
        for (final Map.Entry<String, MapServerOptions.MapOption> entry : mapOptionMap.entrySet()) {
            final String key = entry.getKey();
            final MapServerOptions.MapOption mapOption = entry.getValue();
            final String[] split = key.split("/");

            if (lastType == null || !lastType.equals(split[0])) {
                lastType = split[0];
                subOption = new MapFiltersResponse.Option();
                subOption.setTitle(labels.getString(String.format("MAP_%s", lastType.toUpperCase())));
                subOption.addParameter("map_type_is_mobile", lastType.equalsIgnoreCase("MOBILE"));
                mapTypeOption.addOption(subOption);
            }

            final String type = split[1].toUpperCase();
            final MapFiltersResponse.Option subOptionItem = new MapFiltersResponse.Option();
            subOptionItem.setTitle(labels.getString(String.format("RESULT_%s", type)));
            subOptionItem.setSummary(labels.getString(String.format("MAP_%s_SUMMARY", type)));
            subOptionItem.addParameter("map_options", key);
            if ("mobile/download".equals(key)) {
                subOptionItem.setDefault(true);
            }
            subOptionItem.addParameter("overlay_type", mapOption.overlayType);
            subOption.addOption(subOptionItem);
        }

        return mapTypeOption;
    }

    private MapFiltersResponse.MapFilter getTechnology(ResourceBundle labels) {
        final MapFiltersResponse.MapFilter option = new MapFiltersResponse.MapFilter();
        option.setTitle(labels.getString("MAP_FILTER_TECHNOLOGY"));
        option.setIcon("MAP_FILTER_TECHNOLOGY_ICON");
        option.addDependsOn("map_type_is_mobile", true);

        for (int i = 0; i < Constants.OPTION_TECHNOLOGY_TITLE.length; i++) {
            // expects resources in the format MAP_FILTER_TECHNOLOGY_<title> and MAP_FILTER_TECHNOLOGY_<title>_SUMMARY
            String title = "MAP_FILTER_TECHNOLOGY_" + Constants.OPTION_TECHNOLOGY_TITLE[i];
            final MapFiltersResponse.Option o = new MapFiltersResponse.Option();
            o.setTitle(labels.getString(title));
            o.setSummary(labels.getString(title + "_SUMMARY"));
            o.addParameter("technology", Constants.OPTION_TECHNOLOGY_VALUE[i]);
            if ("".equals(Constants.OPTION_TECHNOLOGY_VALUE[i])) {
                o.setDefault(true);
            }
            option.addOption(o);
        }


        return option;
    }

    private MapFiltersResponse.MapFilter getOperators(ResourceBundle labels, final boolean mobile) {
        final MapFiltersResponse.Option optionAll = new MapFiltersResponse.Option();
        optionAll.setTitle(labels.getString("MAP_FILTER_ALL_OPERATORS"));
        optionAll.addParameter(mobile ? "operator" : "provider", "");
        optionAll.setDefault(true);

        final MapFiltersResponse.MapFilter option = new MapFiltersResponse.MapFilter();
        option.setTitle(labels.getString("MAP_FILTER_CARRIER"));
        option.setIcon(labels.getString("MAP_FILTER_CARRIER_ICON"));
        option.addOption(optionAll);
        option.addDependsOn("map_type_is_mobile", mobile);

        final String sql = "SELECT uid,name,mcc_mnc as mccMnc,shortname FROM provider p WHERE p.map_filter=true"
                + (mobile ? " AND p.mcc_mnc IS NOT NULL" : " ") + " ORDER BY shortname";  // allow mobile networks for wifi/browser

        final Query ps = entityManager.createNativeQuery(sql, "TilesInfoOperatorResults");
        List<TilesInfoOperatorResults> results = ps.getResultList();
        if (!results.isEmpty()) {
            for (TilesInfoOperatorResults result : results) {
                final MapFiltersResponse.Option o = new MapFiltersResponse.Option();
                o.setTitle(result.getShortname());
                o.setSummary(result.getName());
                o.addParameter(mobile ? "operator" : "provider", result.getUid());
                option.addOption(o);
            }
        }

        return option;
    }

    public enum AppearanceType {
        NORMAL,
        SAT;

        public MapFiltersResponse.Function getFunctionDef() {
            final MapFiltersResponse.Function f = new MapFiltersResponse.Function();
            f.setFuncName("change_appearance");
            f.addFuncParameter("type", name().toLowerCase(Locale.US));
            return f;
        }
    }

    private MapFiltersResponse.MapFilter getAppearanceTypeList(ResourceBundle labels) {
        // now with icon and summary
        final MapFiltersResponse.MapFilter option = new MapFiltersResponse.MapFilter();
        option.setTitle(labels.getString("MAP_APPEARANCE"));
        option.setIcon(labels.getString("MAP_APPEARANCE_ICON"));

        for (AppearanceType overlay : AppearanceType.values()) {
            final MapFiltersResponse.Option o = new MapFiltersResponse.Option();
            o.setTitle(labels.getString("MAP_APPEARANCE_" + overlay.name()));
            o.setSummary(labels.getString("MAP_APPEARANCE_" + overlay.name() + "_SUMMARY"));

            o.addFunction(overlay.getFunctionDef());

            if (AppearanceType.NORMAL.equals(overlay)) {
                o.setDefault(true);
            }

            option.addOption(o);
        }

        return option;
    }

    @Getter
    public enum OverlayType {
        AUTO,
        HEATMAP("/RMBTMapServer/tiles/heatmap", "heatmap", 100000000, 256),
        POINTS("/RMBTMapServer/tiles/points", "points", 100000000, 256),
        SHAPES("/RMBTMapServer/tiles/shapes", "shapes", 200000000, 512);

        final String path;
        final String type;
        final int zIndex;
        final int tileSize;

        OverlayType() {
            this(null, "automatic", 0, 0);
        }

        OverlayType(final String path, final String type, final int zIndex, final int tileSize) {
            this.path = path;
            this.zIndex = zIndex;
            this.type = type;
            this.tileSize = tileSize;
        }

        public MapFiltersResponse.Function getFunctionDef(final String functionName) {
            final MapFiltersResponse.Function f = new MapFiltersResponse.Function();
            f.setFuncName(functionName);
            f.addFuncParameter("path", getPath());
            f.addFuncParameter("z_index", getZIndex());
            f.addFuncParameter("tile_size", getTileSize());
            f.addFuncParameter("type", getType());
            return f;
        }
    }

    private MapFiltersResponse.MapFilter getOverlayTypeList(ResourceBundle labels) {
        final MapFiltersResponse.MapFilter option = new MapFiltersResponse.MapFilter();
        option.setTitle(labels.getString("OVERLAY_TYPE"));
        option.setIcon(labels.getString("OVERLAY_TYPE_ICON"));


        for (OverlayType overlay : OverlayType.values()) {
            final MapFiltersResponse.Option o = new MapFiltersResponse.Option();
            o.setTitle(labels.getString("OVERLAY_" + overlay.name()));
            o.setSummary(labels.getString("OVERLAY_" + overlay.name() + "_SUMMARY"));

            o.addFunction(overlay.getFunctionDef("set_overlay"));

            if (OverlayType.AUTO.equals(overlay)) {
                o.addFunction(OverlayType.HEATMAP.getFunctionDef("add_alt_overlay"));
                o.addFunction(OverlayType.POINTS.getFunctionDef("add_alt_overlay"));
                o.setDefault(true);
            }

            option.addOption(o);
        }

        return option;
    }

    private final static int[] OPTION_TIMES_VALUE = new int[]{1, 7, 30, 90, 180, 365, 730, 1460, 2920};

    private MapFiltersResponse.MapFilter getTimes(ResourceBundle labels) {
        final MapFiltersResponse.MapFilter option = new MapFiltersResponse.MapFilter();
        option.setTitle(labels.getString("MAP_FILTER_PERIOD"));
        option.setIcon(labels.getString("MAP_FILTER_PERIOD_ICON"));

        // expects resources in the format MAP_FILTER_PERIOD_<n>_DAYS and MAP_FILTER_PERIOD_<n>_DAYS_SUMMARY
        for (int days : OPTION_TIMES_VALUE) {
            String title = "MAP_FILTER_PERIOD_" + days + "_DAYS";
            final MapFiltersResponse.Option o = new MapFiltersResponse.Option();
            o.setTitle(labels.getString(title));
            o.setSummary(labels.getString(title + "_SUMMARY"));
            o.addParameter("period", days);
            if (days == 180) {
                o.setDefault(true);
            }
            option.addOption(o);
        }

        return option;
    }

}
