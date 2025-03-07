package at.rtr.rmbt.map.controller;

import at.rtr.rmbt.map.constant.Constants;
import at.rtr.rmbt.map.constant.URIConstants;
import at.rtr.rmbt.map.dto.TilesRequest;
import at.rtr.rmbt.map.service.HeatmapTileService;
import at.rtr.rmbt.map.service.PointTileService;
import at.rtr.rmbt.map.service.ShapeTileService;
import at.rtr.rmbt.map.util.TileParameters;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TilesController {

    private final PointTileService pointTileService;
    private final ShapeTileService shapeTileService;
    private final HeatmapTileService heatmapTileService;

    ////https://m-cloud.netztest.at/RMBTMapServer/tiles/points/12/2232/1428.png?null&statistical_method=0.5&period=180&map_options=mobile/download
    @Operation(summary = "Get point tile for a specific point /zoom/x/y")
    @GetMapping(value = URIConstants.TILES_ENDPOINT_VARS, produces = {"image/png", "application/json"})
    //Unfortunately, DTO with renaming params seems to not be supported, therefore this list
    public byte[] getTiles(@PathVariable URIConstants.TILE_TYPE type,
                           @PathVariable int zoom,
                           @PathVariable int x,
                           @PathVariable int y,
                           @RequestParam(name = "statistical_method", required = false) Float statisticalMethod,
                           @RequestParam(name = "size", required = false) Integer size,
                           @RequestParam(name = "map_options") String mapOptions,
                           @RequestParam(name = "developerCode", required = false) String developerCode,
                           @RequestParam(name = "transparency", required = false) Double transparency,
                           @RequestParam(name = "point_diameter", required = false) Double pointDiameter,
                           @RequestParam(name = "no_fill", required = false) Boolean noFill,
                           @RequestParam(name = "no_color", required = false) Boolean noColor,
                           @RequestParam(name = "hightlight", required = false) UUID hightlightUuid,
                           @RequestParam(name = "operator", required = false) String operator,
                           @RequestParam(name = "provider", required = false) String provider,
                           @RequestParam(name = "technology", required = false) String technology,
                           @RequestParam(name = "period", required = false) Integer period,
                           @RequestParam(name = "age", required = false) Integer age,
                           @RequestParam(name = "user_server_selection", required = false) String userServerSelection) {
        TilesRequest request = new TilesRequest(
                zoom, x, y,
                statisticalMethod, size, mapOptions, developerCode, transparency, pointDiameter,
                noFill, noColor, hightlightUuid,
                operator, provider, technology, period, age, userServerSelection
        );

        switch(type) {
            case points -> {
                return this.getPointTiles(request);
            }
            case shapes -> {
                return this.getShapeTiles(request);
            }
            case heatmap -> {
                return this.getHeatmapTiles(request);
            }
        }
        return null;
    }

    @Operation(summary = "Get point tile for a specific point /zoom/x/y")
    @GetMapping(value = URIConstants.TILES_ENDPOINT, produces = {"image/png", "application/json"})
    //Unfortunately, DTO with renaming params seems to not be supported, therefore this list
    public ResponseEntity<byte[]> getTilesbyPath(@PathVariable URIConstants.TILE_TYPE type,
                           @RequestParam(name = "path", required = true) String path,
                           @RequestParam(name = "statistical_method", required = false) Float statisticalMethod,
                           @RequestParam(name = "size", required = false) Double size,
                           @RequestParam(name = "map_options") String mapOptions,
                           @RequestParam(name = "developerCode", required = false) String developerCode,
                           @RequestParam(name = "transparency", required = false) Double transparency,
                           @RequestParam(name = "point_diameter", required = false) Double pointDiameter,
                           @RequestParam(name = "no_fill", required = false) Boolean noFill,
                           @RequestParam(name = "no_color", required = false) Boolean noColor,
                           @RequestParam(name = "hightlight", required = false) UUID hightlightUuid,
                           @RequestParam(name = "operator", required = false) String operator,
                           @RequestParam(name = "provider", required = false) String provider,
                           @RequestParam(name = "technology", required = false) String technology,
                           @RequestParam(name = "period", required = false) Integer period,
                           @RequestParam(name = "age", required = false) Integer age,
                           @RequestParam(name = "user_server_selection", required = false) String userServerSelection) {
        //used by iOS
        //3/5/6
        List<Integer> pathParts;
        try {
            pathParts = Arrays.stream(path.split("/")).map(i -> Integer.parseInt(i)).collect(Collectors.toList());
            if (pathParts.size() != 3) {
                throw new RuntimeException("Invalid path parameters");
            }
        }
        catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }

        return ResponseEntity.status(200).contentType(MediaType.IMAGE_PNG).body(getTiles(type, pathParts.get(0), pathParts.get(1), pathParts.get(2),
                statisticalMethod, size.intValue(), mapOptions, developerCode, transparency, pointDiameter,
                noFill, noColor, hightlightUuid, operator, provider, technology, period, age, userServerSelection));
    }




    public byte[] getPointTiles(TilesRequest parameters) {
        TileParameters params = new TileParameters.PointTileParameters(new TileParameters.Path(parameters.getZoom(), parameters.getX(), parameters.getY()), parameters);
        return this.pointTileService.getTile(params);
    }

    public byte[] getShapeTiles(TilesRequest parameters) {
        TileParameters params = new TileParameters.ShapeTileParameters(new TileParameters.Path(parameters.getZoom(), parameters.getX(), parameters.getY()), parameters);
        return this.shapeTileService.getTile(params);
    }

    public byte[] getHeatmapTiles(TilesRequest parameters) {
        TileParameters params = new TileParameters(new TileParameters.Path(parameters.getZoom(), parameters.getX(), parameters.getY()), parameters, Constants.HEATMAP_DEFAULT_TRANSPARENCY);
        return this.heatmapTileService.getTile(params);
    }

}
