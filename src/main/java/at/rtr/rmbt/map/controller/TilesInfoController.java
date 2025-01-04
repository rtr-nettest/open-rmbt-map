package at.rtr.rmbt.map.controller;

import at.rtr.rmbt.map.constant.URIConstants;
import at.rtr.rmbt.map.dto.TilesInfoRequest;
import at.rtr.rmbt.map.dto.TilesInfoResponse;
import at.rtr.rmbt.map.service.InfoService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TilesInfoController {

    private final InfoService infoService;

    @Operation(summary = "Get info on available tiles")
    @PostMapping(URIConstants.TILES_INFORMATION)
    public TilesInfoResponse getTilesInfo(@RequestBody TilesInfoRequest request) {
        return infoService.getTilesInfo(request);
    }
}
