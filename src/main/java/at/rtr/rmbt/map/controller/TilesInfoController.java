package at.rtr.rmbt.map.controller;

import at.rtr.rmbt.map.constant.URIConstants;
import at.rtr.rmbt.map.dto.MapFiltersResponse;
import at.rtr.rmbt.map.dto.TilesInfoRequest;
import at.rtr.rmbt.map.dto.TilesInfoResponse;
import at.rtr.rmbt.map.service.FiltersService;
import at.rtr.rmbt.map.service.InfoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TilesInfoController {

    private final InfoService infoService;

    private final FiltersService filtersService;

    @Operation(summary = "Get info on available tiles")
    @PostMapping(URIConstants.TILES_INFORMATION)
    public TilesInfoResponse getTilesInfo(@RequestBody TilesInfoRequest request) {
        return infoService.getTilesInfo(request);
    }


    @Operation(summary = "Get info on available tiles")
    @GetMapping(URIConstants.TILES_INFORMATION_V2)
    public MapFiltersResponse getMapFiltersV2Get() {
        return filtersService.getMapFilters(new TilesInfoRequest());
    }
    @Operation(summary = "Get info on available tiles")
    @PostMapping(URIConstants.TILES_INFORMATION_V2)
    public MapFiltersResponse getMapFiltersV2(@RequestBody TilesInfoRequest request) {
        return filtersService.getMapFilters(request);
    }
}
