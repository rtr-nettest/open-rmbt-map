package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.dto.TilesInfoRequest;
import at.rtr.rmbt.map.dto.TilesInfoResponse;
import at.rtr.rmbt.map.model.TilesInfoOperatorResults;
import at.rtr.rmbt.map.util.MapServerOptions;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InfoService {
    @Value("${app.supportedLanguages}")
    private String supportedLanguagesAsArray;

    @Value("${app.defaultLanguage}")
    private String defaultLanguage;

    @PersistenceContext
    private EntityManager entityManager;

    public TilesInfoResponse getTilesInfo(TilesInfoRequest request) {
        TilesInfoResponse response = new TilesInfoResponse();
        response.setMapFilter(new TilesInfoResponse.MapFilter());

        ResourceBundle labels;
        String language;
        final List<String> langs = Arrays.asList(supportedLanguagesAsArray.split(","));
        if (langs.contains(request.getLanguage())) {
            language = request.getLanguage();
        } else {
            language = defaultLanguage;
        }
        labels = ResourceBundle.getBundle("SystemMessages", LocaleUtils.toLocale(language));

        response.getMapFilter().setMapTypes(this.getMapTypes(labels));
        response.getMapFilter().setMapFilters(this.getMapFilters(labels));


        return response;
    }


    private List<TilesInfoResponse.MapType> getMapTypes(ResourceBundle labels) {
        List<TilesInfoResponse.MapType> mapTypes = new ArrayList<>();
        Map<String, MapServerOptions.MapOption> mapOptionMap = MapServerOptions.getMapOptionMap();

        TilesInfoResponse.MapType currentType = null;
        List<TilesInfoResponse.MapOption> optionsArray = null;
        String lastType = null;

        for (Map.Entry<String, MapServerOptions.MapOption> entry : mapOptionMap.entrySet()) {
            String key = entry.getKey();
            MapServerOptions.MapOption mapOption = entry.getValue();
            String[] split = key.split("/");

            if (lastType == null || !lastType.equals(split[0])) {
                lastType = split[0];
                currentType = new TilesInfoResponse.MapType();
                optionsArray = new ArrayList<>();
                currentType.setOptions(optionsArray);
                currentType.setTitle(labels.getString(String.format("MAP_%s", lastType.toUpperCase())));
                mapTypes.add(currentType);
            }

            TilesInfoResponse.MapOption option = new TilesInfoResponse.MapOption();
            option.setMapOptions(key);
            option.setSummary(labels.getString(String.format("MAP_%s_SUMMARY", split[1].toUpperCase())));
            option.setTitle(labels.getString(String.format("RESULT_%s", split[1].toUpperCase())));
            option.setUnit(labels.getString(String.format("RESULT_%s_UNIT", split[1].toUpperCase())));
            option.setHeatmapColors(Arrays.asList(mapOption.getColorsHexStrings()));
            option.setHeatmapCaptions(Arrays.asList(mapOption.getCaptions()));
            option.setClassification(Arrays.asList(mapOption.getClassificationCaptions()));
            option.setOverlayType(mapOption.getOverlayType());
            optionsArray.add(option);
        }
        return mapTypes;
    }


    private Map<String, List<TilesInfoResponse.MapFilterOptions>> getMapFilters(ResourceBundle labels) {
        Map<String, List<TilesInfoResponse.MapFilterOptions>> mapFilters = new HashMap<>();

        try {
            TilesInfoResponse.MapFilterOptions mobileOperators = getOperatorsFilter(labels, true);
            TilesInfoResponse.MapFilterOptions notMobileOperators = getOperatorsFilter(labels, false);

            // Create filters for different types like "mobile", "wifi", etc.
            mapFilters.put("mobile", Arrays.asList(
                    getStatisticalMethodFilter(labels),
                    mobileOperators,
                    getTimeFilter(labels),
                    getTechnologyFilter(labels)
            ));

            mapFilters.put("wifi", Arrays.asList(
                    getStatisticalMethodFilter(labels),
                    notMobileOperators,
                    getTimeFilter(labels)
            ));

            mapFilters.put("browser", Arrays.asList(
                    getStatisticalMethodFilter(labels),
                    notMobileOperators,
                    getTimeFilter(labels)
            ));

            mapFilters.put("all", Arrays.asList(
                    getStatisticalMethodFilter(labels),
                    getTimeFilter(labels)
            ));

        } catch (SQLException e) {
            log.error("Error fetching map filters", e);
        }
        return mapFilters;
    }

    private TilesInfoResponse.MapFilterOptions getStatisticalMethodFilter(ResourceBundle labels) {
        TilesInfoResponse.MapFilterOptions filter = new TilesInfoResponse.MapFilterOptions();
        filter.setFilterOptions(new ArrayList<>());
        filter.setTitle(labels.getString("MAP_FILTER_STATISTICAL_METHOD"));

        final double[] statisticalMethodArray = { 0.8, 0.5, 0.2 };
        for (int stat = 1; stat <= statisticalMethodArray.length; stat++)
        {
            TilesInfoResponse.MapFilterOption option = new TilesInfoResponse.MapFilterOption();
            option.setTitle(labels.getString("MAP_FILTER_STATISTICAL_METHOD_" + stat + "_TITLE"));
            option.setSummary(labels.getString("MAP_FILTER_STATISTICAL_METHOD_" + stat + "_SUMMARY"));
            option.setStatisticalMethod(statisticalMethodArray[stat - 1]);
            if (stat == 2)  //2nd list entry is default (median)
                option.setDefaultOption(true);
            filter.getFilterOptions().add(option);
        }

        return filter;
    }

    private TilesInfoResponse.MapFilterOptions getOperatorsFilter(ResourceBundle labels, boolean mobile) throws SQLException {
        TilesInfoResponse.MapFilterOptions filterOptions = new TilesInfoResponse.MapFilterOptions();
        filterOptions.setFilterOptions(new ArrayList<>());
        filterOptions.setTitle(labels.getString("MAP_FILTER_CARRIER"));

        TilesInfoResponse.MapFilterOption defaultOption = new TilesInfoResponse.MapFilterOption();
        defaultOption.setDefaultOption(true);
        defaultOption.setTitle(labels.getString("MAP_FILTER_ALL_OPERATORS"));
        defaultOption.setSummary("");
        if (mobile) {
            defaultOption.setOperator("");
        }
        else {
            defaultOption.setProvider("");
        }
        //@TODO: In old code, "operator" was either String or Long
        filterOptions.getFilterOptions().add(defaultOption);

        final String sql = "SELECT uid,name,mcc_mnc as mccMnc,shortname FROM provider p WHERE p.map_filter=true"
                + (mobile ? " AND p.mcc_mnc IS NOT NULL" : " ") + " ORDER BY shortname";  // allow mobile networks for wifi/browser

        final Query ps = entityManager.createNativeQuery(sql, "TilesInfoOperatorResults");
        List<TilesInfoOperatorResults> results = ps.getResultList();
        if (!results.isEmpty()) {
            for (TilesInfoOperatorResults result : results) {
                TilesInfoResponse.MapFilterOption filter = new TilesInfoResponse.MapFilterOption();
                filter.setTitle(result.getShortname());
                filter.setSummary(result.getName());
                if (mobile) {
                    filter.setOperator(result.getUid());
                } else {
                    filter.setProvider(result.getUid());
                }
                filterOptions.getFilterOptions().add(filter);
            }
        }

        return filterOptions;
    }

    private TilesInfoResponse.MapFilterOptions getTimeFilter(ResourceBundle labels) {
        TilesInfoResponse.MapFilterOptions filterOptions = new TilesInfoResponse.MapFilterOptions();
        filterOptions.setTitle(labels.getString("MAP_FILTER_PERIOD"));
        filterOptions.setFilterOptions(new ArrayList<>());

        List<Integer> daysList = List.of(1,7,30,90,180,365,730,1460,2920);
        for (int day : daysList) {
            TilesInfoResponse.MapFilterOption option = new TilesInfoResponse.MapFilterOption();
            option.setPeriod(day);
            option.setSummary(labels.getString("MAP_FILTER_PERIOD_" + day + "_DAYS"));
            option.setTitle(labels.getString("MAP_FILTER_PERIOD_" + day + "_DAYS"));

            if (day == 180) {
                option.setDefaultOption(true);
            }
            filterOptions.getFilterOptions().add(option);
        }

        // Set filter title and options
        return filterOptions;
    }

    private TilesInfoResponse.MapFilterOptions getTechnologyFilter(ResourceBundle labels) {
        TilesInfoResponse.MapFilterOptions filterOptions = new TilesInfoResponse.MapFilterOptions();
        filterOptions.setTitle(labels.getString("MAP_FILTER_TECHNOLOGY"));
        filterOptions.setFilterOptions(new ArrayList<>());

        TilesInfoResponse.MapFilterOption filterOption = new TilesInfoResponse.MapFilterOption();
        filterOption.setTitle(labels.getString("MAP_FILTER_TECHNOLOGY_ANY"));
        filterOption.setSummary(labels.getString("MAP_FILTER_TECHNOLOGY_ANY"));
        filterOption.setDefaultOption(true);
        filterOption.setTechnology("");
        filterOptions.getFilterOptions().add(filterOption);


        filterOption = new TilesInfoResponse.MapFilterOption();
        filterOption.setTitle(labels.getString("MAP_FILTER_TECHNOLOGY_2G"));
        filterOption.setSummary(labels.getString("MAP_FILTER_TECHNOLOGY_2G"));
        filterOption.setTechnology("2");
        filterOptions.getFilterOptions().add(filterOption);

        filterOption = new TilesInfoResponse.MapFilterOption();
        filterOption.setTitle(labels.getString("MAP_FILTER_TECHNOLOGY_3G"));
        filterOption.setSummary(labels.getString("MAP_FILTER_TECHNOLOGY_3G"));
        filterOption.setTechnology("3");
        filterOptions.getFilterOptions().add(filterOption);

        filterOption = new TilesInfoResponse.MapFilterOption();
        filterOption.setTitle(labels.getString("MAP_FILTER_TECHNOLOGY_4G"));
        filterOption.setSummary(labels.getString("MAP_FILTER_TECHNOLOGY_4G"));
        filterOption.setTechnology("4");
        filterOptions.getFilterOptions().add(filterOption);

        filterOption = new TilesInfoResponse.MapFilterOption();
        filterOption.setTitle(labels.getString("MAP_FILTER_TECHNOLOGY_5G"));
        filterOption.setSummary(labels.getString("MAP_FILTER_TECHNOLOGY_5G"));
        filterOption.setTechnology("5");
        filterOptions.getFilterOptions().add(filterOption);


        return filterOptions;
    }
}
