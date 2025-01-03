package at.rtr.rmbt.map.controller;

import at.rtr.rmbt.map.constant.URIConstants;
import at.rtr.rmbt.map.dto.MarkerRequest;
import at.rtr.rmbt.map.dto.MarkerResponse;
import at.rtr.rmbt.map.service.MarkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MarkerController {

    private final MarkerService markerService;

    //https://m-cloud.netztest.at/RMBTMapServer/tiles/markers
    @PostMapping(value = URIConstants.MARKERS, produces = "application/json")
    public MarkerResponse getMarkersForPoint(@RequestBody MarkerRequest request) {
        MarkerResponse markersForPoint = markerService.getMarkersForPoint(request);
        return markersForPoint;
    }
}
