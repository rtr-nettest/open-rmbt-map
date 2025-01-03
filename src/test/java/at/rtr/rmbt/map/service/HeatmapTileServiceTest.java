package at.rtr.rmbt.map.service;

import at.rtr.rmbt.map.MapServerConfiguration;
import at.rtr.rmbt.map.constant.Constants;
import at.rtr.rmbt.map.dto.TilesRequest;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@RequiredArgsConstructor
@SpringBootTest
@ContextConfiguration(classes = MapServerConfiguration.class)
@ActiveProfiles("test")
class HeatmapTileServiceTest {

    @Autowired
    private HeatmapTileService heatmapTileService;

    @Test
    void testGenerateHeatmapTile() {
        //https://m-cloud.netztest.at/RMBTMapServer/tiles/heatmap/7/70/44.png?null&statistical_method=0.5&period=180&map_options=mobile/download

        TilesRequest tr = new TilesRequest(7,70,44,0.5f,null,"mobile/download", null, null, null, null, null, null, null, null, null, null, null, null);
        byte[] bytes = heatmapTileService.generateSingleTile(tr, Constants.TILE_TYPE.HEATMAP);
        System.out.println("Tile generated of size " + bytes.length);
    }
}