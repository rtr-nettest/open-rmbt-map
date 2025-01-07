package at.rtr.rmbt.map.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@RedisHash
public class CachedTile implements Serializable {
    private Instant creationTime;
    private byte[] tileContent;
}
