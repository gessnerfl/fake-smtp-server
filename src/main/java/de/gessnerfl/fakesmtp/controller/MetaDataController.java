package de.gessnerfl.fakesmtp.controller;

import de.gessnerfl.fakesmtp.model.ApplicationMetaData;
import org.springframework.boot.info.BuildProperties;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meta-data")
@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
public class MetaDataController {

    private final BuildProperties buildProperties;

    public MetaDataController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping
    public ApplicationMetaData get() {
        return new ApplicationMetaData(buildProperties.getVersion());
    }

}
