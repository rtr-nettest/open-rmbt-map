package at.rtr.rmbt.map.controller;

import at.rtr.rmbt.map.constant.URIConstants;
import at.rtr.rmbt.map.dto.MarkerRequest;
import at.rtr.rmbt.map.dto.MarkerResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public class MarkerController {

    @PostMapping(value = URIConstants.MARKERS, produces = "application/json")
    public MarkerResponse getMarkersForPoint(@RequestBody MarkerRequest request) {

        return null;
    }
}
