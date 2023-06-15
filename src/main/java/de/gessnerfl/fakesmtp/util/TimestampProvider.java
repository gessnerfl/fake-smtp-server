package de.gessnerfl.fakesmtp.util;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TimestampProvider {

    public LocalDateTime now(){
        return LocalDateTime.now();
    }

}
