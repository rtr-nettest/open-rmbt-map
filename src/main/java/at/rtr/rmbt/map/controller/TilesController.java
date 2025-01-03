package at.rtr.rmbt.map.controller;

import at.rtr.rmbt.map.constant.Constants;
import at.rtr.rmbt.map.constant.URIConstants;
import at.rtr.rmbt.map.dto.ApplicationVersionResponse;
import at.rtr.rmbt.map.dto.TilesRequest;
import at.rtr.rmbt.map.service.HeatmapTileService;
import at.rtr.rmbt.map.service.PointTileService;
import at.rtr.rmbt.map.service.ShapeTileService;
import at.rtr.rmbt.map.util.TileParameters;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TilesController {

    private final PointTileService pointTileService;
    private final ShapeTileService shapeTileService;
    private final HeatmapTileService heatmapTileService;

    ////https://m-cloud.netztest.at/RMBTMapServer/tiles/points/12/2232/1428.png?null&statistical_method=0.5&period=180&map_options=mobile/download
    @Operation(summary = "Get point tile for a specific point /zoom/x/y")
    @GetMapping(value = URIConstants.TILES_POINT, produces = "image/png")
    public byte[] getPointTiles(@PathVariable int zoom, @PathVariable int x, @PathVariable int y, TilesRequest parameters) {
        TileParameters params = new TileParameters.PointTileParameters(new TileParameters.Path(zoom, x, y), parameters);

        return this.pointTileService.getTile(params);
    }

    @Operation(summary = "Get shape tile for a specific point /zoom/x/y")
    @RequestMapping(method=RequestMethod.GET, value = URIConstants.TILES_SHAPE, produces = "image/png")
    public byte[] getShapeTiles(TilesRequest parameters) {
        TileParameters params = new TileParameters.ShapeTileParameters(new TileParameters.Path(parameters.getZoom(), parameters.getX(), parameters.getY()), parameters);

        return this.shapeTileService.getTile(params);
    }

    @Operation(summary = "Get heatmap tile for a specific point /zoom/x/y")
    @GetMapping(value = URIConstants.TILES_HEATMAP, produces = "image/png")
    public byte[] getHeatmapTiles(@PathVariable int zoom, @PathVariable int x, @PathVariable int y, TilesRequest parameters) {
        TileParameters params = new TileParameters(new TileParameters.Path(zoom, x, y), parameters, Constants.HEATMAP_DEFAULT_TRANSPARENCY);

        return this.pointTileService.getTile(params);
    }

}
