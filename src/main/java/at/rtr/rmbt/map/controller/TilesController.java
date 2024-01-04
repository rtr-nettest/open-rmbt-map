package at.rtr.rmbt.map.controller;

import at.rtr.rmbt.map.constant.URIConstants;
import at.rtr.rmbt.map.dto.ApplicationVersionResponse;
import at.rtr.rmbt.map.dto.TilesRequest;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

public class TilesController {

    ////https://m-cloud.netztest.at/RMBTMapServer/tiles/points/12/2232/1428.png?null&statistical_method=0.5&period=180&map_options=mobile/download
    @Operation(summary = "Get version of application")
    @GetMapping(value = URIConstants.TILES_POINT, produces = "image/png")
    public byte[] getPointTiles(@PathVariable int zoom, @PathVariable int x, @PathVariable int y, @RequestBody TilesRequest parameters) {


        return null;
    }

}
