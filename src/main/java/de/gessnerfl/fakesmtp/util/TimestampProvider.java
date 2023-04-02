package de.gessnerfl.fakesmtp.util;

import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class TimestampProvider {

    public Date now(){
        return new Date();
    }

}
