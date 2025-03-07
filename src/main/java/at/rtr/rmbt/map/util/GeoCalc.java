/*******************************************************************************
 * Copyright 2013-2014 alladin-IT GmbH
 * Copyright 2013-2014 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package at.rtr.rmbt.map.util;

import at.rtr.rmbt.map.service.TileGenerationService;

public class GeoCalc
{
    
    // see
    // http://www.maptiler.org/google-maps-coordinates-tile-bounds-projection/
    protected static final double MAX_EXTENT = 20037508.342789244;
    
    /**
     * @param zoom
     *            zoomlevel
     * @return resolution in meters per pixel
     */
    public static double getResFromZoom(final int tileSize, final int zoom)
    {
        final long powZoom = 1 << zoom;
        return MAX_EXTENT * 2 / tileSize / powZoom;
    }
    
    public static TileGenerationService.DBox xyToMeters(final int tileSize, final int x, final int y, final int zoom)
    {
        final TileGenerationService.DBox result = new TileGenerationService.DBox();
        
        final long powZoom = 1 << zoom;
        result.setRes(MAX_EXTENT * 2 / tileSize / powZoom);
        final double w = MAX_EXTENT * 2 / powZoom;
        final double myY = powZoom - 1 - y;
        
        result.setX1(x * w - MAX_EXTENT);
        result.setY1(myY * w - MAX_EXTENT);
        result.setX2((x + 1) * w - MAX_EXTENT);
        result.setY2((myY + 1) * w - MAX_EXTENT);
        
        return result;
    }
    
    public static double latToMeters(final double lat)
    {
        return Math.log(Math.tan((90.0 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0) * MAX_EXTENT / 180.0;
    }
    
    public static double lonToMeters(final double lon)
    {
        return lon * MAX_EXTENT / 180.0;
    }
    
}
