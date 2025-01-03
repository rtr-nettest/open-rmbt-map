package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.dto.CapabilitiesRequest;
import at.rtr.rmbt.map.dto.MarkerRequest;
import at.rtr.rmbt.map.dto.MarkerResponse;
import at.rtr.rmbt.map.model.MarkerQueryResult;
import at.rtr.rmbt.map.util.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkerService {


    @Value("${app.supportedLanguages}")
    private String supportedLanguagesAsArray;

    @Value("${app.defaultLanguage}")
    private String defaultLanguage;

    @PersistenceContext
    private EntityManager entityManager;

    private static int MAX_PROVIDER_LENGTH = 22;
    private static int CLICK_RADIUS = 10;

    public MarkerResponse getMarkersForPoint(MarkerRequest parameters) {
        ResourceBundle labels;
        String language;
        MarkerResponse answer = new MarkerResponse();

        // Load Language Files for Client

        final List<String> langs = Arrays.asList(supportedLanguagesAsArray.split(","));

        if (langs.contains(parameters.getLanguage())) {
            language = parameters.getLanguage();
        } else {
            language = defaultLanguage;
        }
        labels = ResourceBundle.getBundle("SystemMessages", LocaleUtils.toLocale(language));

        UUID requestOpenTestUUID = null;

        if (parameters.getOpenTestUuid() != null) {
            try {
                requestOpenTestUUID = UUID.fromString(parameters.getOpenTestUuid());
            } catch (final Exception e) {
                requestOpenTestUUID = null;
            }
        }


        int zoom = 1;
        double geo_x = 0;
        double geo_y = 0;
        int size = 0;
        boolean useXY = false;
        boolean useLatLon = false;
        CapabilitiesRequest capabilities = parameters.getCapabilities();

        if (parameters.getCoordinates() != null) {
            MarkerRequest.MarkerRequestCoordinates coords = parameters.getCoordinates();
            if (coords.getX() != null && coords.getY() != null) {
                useXY = true;
            } else if (coords.getLatitude() != null && coords.getLongitude() != null) {
                useLatLon = true;
            }

            if (coords.getZoom() != null && (useXY || useLatLon)) {
                zoom = coords.getZoom();
                if (useXY) {
                    geo_x = coords.getX();
                    geo_y = coords.getY();
                } else if (useLatLon) {
                    final double tmpLat = coords.getLatitude();
                    final double tmpLon = coords.getLongitude();
                    geo_x = GeoCalc.lonToMeters(tmpLon);
                    geo_y = GeoCalc.latToMeters(tmpLat);
                    //                        System.out.println(String.format("using %f/%f", geo_x, geo_y));
                }

                if (coords.getSize() != null) {
                    size = coords.getSize();
                }

            }
        }
        if (requestOpenTestUUID != null || (zoom != 0 && geo_x != 0 && geo_y != 0)) {
            double radius = 0;
            if (size > 0)
                radius = size * GeoCalc.getResFromZoom(256, zoom); // TODO use real tile size
            else
                radius = CLICK_RADIUS * GeoCalc.getResFromZoom(256, zoom);  // TODO use real tile size
            final double geo_x_min = geo_x - radius;
            final double geo_x_max = geo_x + radius;
            final double geo_y_min = geo_y - radius;
            final double geo_y_max = geo_y + radius;

            String hightlightUUIDString = null;
            UUID highlightUUID = null;

            String optionStr = null;
            if (parameters.getOptions() != null) {
                optionStr = parameters.getOptions().getOptions();
            }
            if (optionStr == null || optionStr.length() == 0) { // set
                // default
                optionStr = "mobile/download";
            }

            final MapServerOptions.MapOption mo = MapServerOptions.getMapOptionMap().get(optionStr);

            final List<MapServerOptions.SQLFilter> filters = new ArrayList<MapServerOptions.SQLFilter>(MapServerOptions.getDefaultMapFilters());
            filters.add(MapServerOptions.getAccuracyMapFilter());


            if (parameters.getFilter() != null) {
                //final Iterator<?> keys = mapFilterObj.keys(); //@TODO

                if (parameters.getFilter().getHighlightUuid() != null) {
                    hightlightUUIDString = parameters.getFilter().getHighlightUuid();
                    try {
                        highlightUUID = UUID.fromString(hightlightUUIDString);
                    } catch (final Exception e) {
                        highlightUUID = null;
                    }
                }
                if (parameters.getFilter().getFourColorSupported() != null) {
                    // clients supporting four color classification will add this key

                    //@TODO ThS
                    //capabilities.getClassificationCapability().setCount(mapFilterObj.getBoolean(key) ? 4 : ClassificationCapability.DEFAULT_CLASSIFICATON_COUNT);
                }

                //TODO handle all other ThS
                /*
                while (keys.hasNext())
                {
                    final String key = (String) keys.next();
                    if (mapFilterObj.get(key) instanceof Object)

                        else
                        {
                            final MapFilter mapFilter = MapServerOptions.getMapFilterMap().get(key);
                            if (mapFilter != null) {
                                try {
                                    filters.add(mapFilter.getFilter(mapFilterObj.getString(key)));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                }*/
            }

            if (entityManager != null) {
                Query ps = null;

                final StringBuilder whereSQL = new StringBuilder(mo.sqlFilter);
                if (requestOpenTestUUID == null)
                    for (final MapServerOptions.SQLFilter sf : filters)
                        whereSQL.append(" AND ").append(sf.getWhere());
                else
                    whereSQL.setLength(0);

                final String sql = String
                        //need to put alias in quote so postgres does respect case-sensitiveness
                        //also, the number of columns must not differ
                        .format("SELECT"
                                + (useLatLon ? " geo_lat lat, geo_long lon, NULL x, NULL y"
                                : " NULL lat, NULL lon, ST_X(t.location) x, ST_Y(t.location) y")
                                + ", t.time, t.timezone, "
                                + " t.speed_download \"speedDownload\", t.speed_upload \"speedUpload\", t.ping_median \"pingMedian\", t.network_type \"networkType\","
                                + " t.signal_strength \"signalStrength\", t.lte_rsrp \"lteRsrp\", t.wifi_ssid \"wifiSSID\","
                                + " t.network_operator_name \"networkOperatorName\", t.network_operator \"networkOperator\","
                                + " t.network_sim_operator \"networkSimOperator\", t.roaming_type \"roamingType\", t.public_ip_as_name, " //TODO: sim_operator obsoleted by sim_name
                                + " mprov.shortname \"mobileProviderName\"," // TODO: obsoleted by mobile_network_name
                                + " prov.shortname provider_text, t.open_test_uuid openTestUuid,"
                                + " COALESCE(mprov.shortname, t.network_operator_name, prov.shortname, msim.shortname,msim.name,"
                                + "    prov.name, mprov.name, t.public_ip_as_name, network_sim_operator) \"providerName\", "
                                + " COALESCE(mnwk.shortname,mnwk.name) \"mobileNetworkName\","
                                + " COALESCE(msim.shortname,msim.name) \"mobileSimName\", "
                                + (highlightUUID == null ? " NULL AS uid, NULL AS uuid " : " c.uid, c.uuid")
                                + " FROM v_test2 t"
                                + " LEFT JOIN mccmnc2name mnwk ON t.mobile_network_id=mnwk.uid"
                                + " LEFT JOIN mccmnc2name msim ON t.mobile_sim_id=msim.uid"
                                + " LEFT JOIN provider prov    ON t.provider_id=prov.uid"
                                + " LEFT JOIN provider mprov   ON t.mobile_provider_id=mprov.uid"
                                + (highlightUUID == null ? ""
                                : " LEFT JOIN client c ON (t.client_id=c.uid AND t.uuid=?)")
                                + " WHERE"
                                + " %s"
                                + (requestOpenTestUUID != null ?
                                " t.open_test_uuid=? "
                                : " AND location && ST_SetSRID(ST_MakeBox2D(ST_Point(?,?), ST_Point(?,?)), 900913)")
                                + " ORDER BY" + (highlightUUID == null ? "" : " c.uid ASC,")
                                + " t.uid DESC" + " LIMIT 5", whereSQL);

                //System.out.println("SQL: " + sql);
                ps = entityManager.createNativeQuery(sql, "MarkerResultMapping");


                int i = 1;

                if (highlightUUID != null) {
                    ps.setParameter(i++, highlightUUID);
                }


                // filter by location if not selected by open_test_uuid
                if (requestOpenTestUUID == null) {
                    for (final MapServerOptions.SQLFilter sf : filters) {
                        try {
                            i = sf.fillParams(i, ps);
                        } catch (SQLException e) {
                            log.error("Error filling in parameter ", e);
                        }
                    }
                    ps.setParameter(i++, geo_x_min);
                    ps.setParameter(i++, geo_y_min);
                    ps.setParameter(i++, geo_x_max);
                    ps.setParameter(i++, geo_y_max);
                } else {
                    ps.setParameter(i++, requestOpenTestUUID);
                }


                //System.out.println("SQL: " + ps.toString());

                //List results1 = ps.unwrap(org.hibernate.query.Query.class)
//                        .setResultTransformer(new AliasToBeanResultTransformer(MarkerQueryResult.class)).getResultList();
                List<MarkerQueryResult> results = ps.getResultList();
                if (!results.isEmpty()) {

                    final Locale locale = LocaleUtils.toLocale(language);

                    for (MarkerQueryResult rs : results) {
                        //final JSONObject jsonItem = new JSONObject();
                        MarkerResponse.SingleMarker markerResponse = new MarkerResponse.SingleMarker();

                        //JSONArray jsonItemList = new JSONArray();

                        // RMBTClient Info
                        if (highlightUUID != null && rs.getUuid() != null) {
                            markerResponse.setHighlight(true);
                        }


                        final double res_x = (rs.getX() != null) ? rs.getX() : rs.getLat();
                        final double res_y = (rs.getY() != null) ? rs.getY() : rs.getLon();
                        final String openTestUUID = rs.getOpenTestUuid().toString();

                        markerResponse.setLatitude(res_x);
                        markerResponse.setLongitude(res_y);
                        markerResponse.setOpenTestUuid("O" + openTestUUID);
                        // marker.put("uid", uid);

                        final Date date = rs.getTime();
                        final String tzString = rs.getTimezone();
                        final TimeZone tz = TimeZone.getTimeZone(tzString);
                        final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                                DateFormat.MEDIUM, locale);
                        dateFormat.setTimeZone(tz);
                        markerResponse.setTimeString(dateFormat.format(date));

                        //time as UNIX time (UTC) e.g. 1445361731053
                        final long time = date.getTime();
                        markerResponse.setTime(time);

                        final int fieldDown = rs.getSpeedDownload().intValue();
                        MarkerResponse.SingleMarkerMetricItem singleItem = new MarkerResponse.SingleMarkerMetricItem();
                        singleItem.setTitle(labels.getString("RESULT_DOWNLOAD"));
                        final String downloadString = String.format("%s %s",
                                FormatUtils.formatSpeed(fieldDown), labels.getString("RESULT_DOWNLOAD_UNIT"));
                        singleItem.setValue(downloadString);
                        singleItem.setClassification(
                                Classification.classify(Classification.THRESHOLD_DOWNLOAD, fieldDown, capabilities.getClassification().getCount()));


                        markerResponse.getMeasurement().add(singleItem);


                        final int fieldUp = rs.getSpeedUpload();
                        singleItem = new MarkerResponse.SingleMarkerMetricItem();
                        singleItem.setTitle(labels.getString("RESULT_UPLOAD"));
                        final String uploadString = String.format("%s %s",
                                FormatUtils.formatSpeed(fieldUp),
                                labels.getString("RESULT_UPLOAD_UNIT"));
                        singleItem.setValue(uploadString);
                        singleItem.setClassification(Classification.classify(Classification.THRESHOLD_UPLOAD, fieldUp, capabilities.getClassification().getCount()));

                        markerResponse.getMeasurement().add(singleItem);

                        MarkerResponse.SingleMarkerMeasurementResult measurementResult = new MarkerResponse.SingleMarkerMeasurementResult();
                        {
                            measurementResult.setDownloadKbps(fieldDown);
                            measurementResult.setDownloadClassification(Classification.classify(Classification.THRESHOLD_DOWNLOAD, fieldDown, capabilities.getClassification().getCount()));
                            measurementResult.setUploadKbps(fieldUp);
                            measurementResult.setUploadClassification(Classification.classify(Classification.THRESHOLD_UPLOAD, fieldUp, capabilities.getClassification().getCount()));
                        }

                        final long fieldPing = rs.getPingMedian();
                        singleItem = new MarkerResponse.SingleMarkerMetricItem();
                        singleItem.setTitle(labels.getString("RESULT_PING"));
                        final String pingString = String.format("%s %s", FormatUtils.formatPing(rs.getPingMedian()),
                                labels.getString("RESULT_PING_UNIT"));
                        singleItem.setValue(pingString);
                        singleItem.setClassification(Classification.classify(Classification.THRESHOLD_PING, fieldPing, capabilities.getClassification().getCount()));

                        markerResponse.getMeasurement().add(singleItem);
                        measurementResult.setPingMs(fieldPing / 1000000d);
                        measurementResult.setPingClassification(Classification.classify(Classification.THRESHOLD_PING, fieldPing, capabilities.getClassification().getCount()));

                        final Integer networkType = rs.getNetworkType();


                        if (rs.getSignalStrength() != null && rs.getSignalStrength() != 0) {
                            final int signalValue = rs.getSignalStrength();
                            final int[] threshold = networkType == 99 || networkType == 0 ? Classification.THRESHOLD_SIGNAL_WIFI
                                    : Classification.THRESHOLD_SIGNAL_MOBILE;
                            singleItem = new MarkerResponse.SingleMarkerMetricItem();
                            singleItem.setTitle(labels.getString("RESULT_SIGNAL"));
                            singleItem.setValue(signalValue + " " + labels.getString("RESULT_SIGNAL_UNIT"));
                            singleItem.setClassification(Classification.classify(threshold, signalValue, capabilities.getClassification().getCount()));
                            markerResponse.getMeasurement().add(singleItem);
                            measurementResult.setSignalStrength(signalValue);
                            measurementResult.setSignalClassification(Classification.classify(threshold, signalValue, capabilities.getClassification().getCount()));
                        }


                        if (rs.getLteRsrp() != null && rs.getLteRsrp() != 0) {
                            final int lteRsrpValue = rs.getLteRsrp();
                            final int[] threshold = Classification.THRESHOLD_SIGNAL_RSRP;
                            singleItem = new MarkerResponse.SingleMarkerMetricItem();
                            singleItem.setTitle(labels.getString("RESULT_LTE_RSRP"));
                            singleItem.setValue(lteRsrpValue + " " + labels.getString("RESULT_LTE_RSRP_UNIT"));
                            singleItem.setClassification(Classification.classify(threshold, lteRsrpValue, capabilities.getClassification().getCount()));
                            markerResponse.getMeasurement().add(singleItem);
                            measurementResult.setLteRsrp(lteRsrpValue);
                            measurementResult.setSignalClassification(Classification.classify(threshold, lteRsrpValue, capabilities.getClassification().getCount()));
                        }

                        markerResponse.setResult(measurementResult);

                        //Network info
                        MarkerResponse.NetworkInfo networkInfo = new MarkerResponse.NetworkInfo();

                        MarkerResponse.SingleMarkerMetricItem networkInfoItem = new MarkerResponse.SingleMarkerMetricItem();
                        networkInfoItem.setTitle(labels.getString("RESULT_NETWORK_TYPE"));
                        networkInfoItem.setValue(HelperFunctions.getNetworkTypeName(networkType));
                        markerResponse.getNetwork().add(networkInfoItem);
                        networkInfo.setNetworkTypeLabel(HelperFunctions.getNetworkTypeName(networkType));


                        if (networkType == 98 || networkType == 99) // mobile wifi or browser
                        {
                            String providerText = "Unknown";
                            if (!StringUtils.isBlank(rs.getProviderName()))
                                providerText = rs.getProviderName();

                            if (!StringUtils.isBlank(providerText)) {
                                networkInfo.setProviderName(providerText);
                                if (providerText.length() > (MAX_PROVIDER_LENGTH + 3)) {
                                    providerText = providerText.substring(0, MAX_PROVIDER_LENGTH) + "...";
                                }


                                singleItem = new MarkerResponse.SingleMarkerMetricItem();
                                singleItem.setTitle(labels.getString("RESULT_PROVIDER"));
                                singleItem.setValue(providerText);
                                markerResponse.getNetwork().add(singleItem);
                            }
                            if (networkType == 99)  // mobile wifi
                            {
                                if (highlightUUID != null && rs.getUuid() != null) // own test
                                {
                                    final String ssid = rs.getWifiSSID();
                                    if (ssid != null && ssid.length() != 0) {
                                        singleItem = new MarkerResponse.SingleMarkerMetricItem();
                                        singleItem.setTitle(labels.getString("RESULT_WIFI_SSID"));
                                        singleItem.setValue(ssid.toString());
                                        markerResponse.getNetwork().add(singleItem);
                                        networkInfo.setWifiSSID(ssid.toString());
                                    }
                                }
                            }
                        } else // mobile
                        {
                            //network
                            if (!StringUtils.isBlank(rs.getNetworkOperator())) {
                                final String mobileNetworkString;
                                if (rs.getRoamingType() != 2) { //not international roaming - display name of home network
                                    if (StringUtils.isBlank(rs.getMobileSimName())) {
                                        mobileNetworkString = rs.getNetworkOperator();
                                    } else {
                                        mobileNetworkString = String.format("%s (%s)", rs.getMobileSimName(), rs.getNetworkOperator());
                                    }
                                } else { //international roaming - display name of network
                                    if (StringUtils.isBlank(rs.getMobileSimName())) {
                                        mobileNetworkString = rs.getNetworkOperator();
                                    } else {
                                        mobileNetworkString = String.format("%s (%s)", rs.getMobileNetworkName(), rs.getNetworkOperator());
                                    }
                                }

                                singleItem = new MarkerResponse.SingleMarkerMetricItem();
                                singleItem.setTitle(labels.getString("RESULT_MOBILE_NETWORK"));
                                singleItem.setValue(mobileNetworkString);
                                markerResponse.getNetwork().add(singleItem);
                                networkInfo.setProviderName(mobileNetworkString);

                            }
                            //home network (sim)
                            else if (!StringUtils.isBlank(rs.getNetworkSimOperator())) {
                                final String mobileNetworkString;

                                if (StringUtils.isBlank(rs.getMobileSimName()))
                                    mobileNetworkString = rs.getNetworkSimOperator();
                                else
                                    mobileNetworkString = String.format("%s (%s)", rs.getMobileSimName(), rs.getNetworkSimOperator());


                                singleItem = new MarkerResponse.SingleMarkerMetricItem();
                                singleItem.setTitle(labels.getString("RESULT_HOME_NETWORK"));
                                singleItem.setValue(mobileNetworkString);
                                networkInfo.setProviderName(mobileNetworkString);
                                markerResponse.getNetwork().add(singleItem);
                            }

                            if (rs.getRoamingType() != null && rs.getRoamingType() == 2) //only display info on international roaming
                            {
                                singleItem = new MarkerResponse.SingleMarkerMetricItem();
                                singleItem.setTitle(labels.getString("RESULT_ROAMING"));
                                singleItem.setValue(HelperFunctions.getRoamingType(labels, rs.getRoamingType()));
                                networkInfo.setRoamingTypeLabel(HelperFunctions.getRoamingType(labels, rs.getRoamingType()));
                                markerResponse.getNetwork().add(singleItem);
                            }
                        }

                        markerResponse.setNetworkInfo(networkInfo);

                        answer.getMeasurements().add(markerResponse);

                        if (markerResponse.getMeasurement().size() == 0) {
                            System.out.println("Error getting Results.");
                        }
                    }
                } else {
                    System.out.println("Error executing SQL.");
                }
            } else {
                System.out.println("No Database Connection.");
            }
        } else {
            System.out.println("Expected request is missing.");
        }

        return answer;
    }
}
