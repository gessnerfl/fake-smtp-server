package de.gessnerfl.fakesmtp.util;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Service
public class TimestampProvider {

    public LocalDateTime now(){
        return LocalDateTime.now();
    }

}
