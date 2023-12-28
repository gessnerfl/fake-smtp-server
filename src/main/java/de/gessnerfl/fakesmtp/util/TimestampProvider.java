package de.gessnerfl.fakesmtp.util;

import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class TimestampProvider {

    public ZonedDateTime now(){
        return ZonedDateTime.now(ZoneId.of("UTC"));
    }

}
