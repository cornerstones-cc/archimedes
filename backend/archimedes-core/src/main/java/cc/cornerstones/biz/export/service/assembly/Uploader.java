package cc.cornerstones.biz.export.service.assembly;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple2;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.serviceconnection.dto.DfsServiceAgentDto;
import cc.cornerstones.biz.administration.serviceconnection.service.inf.DfsServiceAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class Uploader {
    private static final Logger LOGGER = LoggerFactory.getLogger(Downloader.class);

    @Autowired
    private DfsServiceAgentService dfsServiceAgentService;

    public AbcTuple2<Long, String> upload(
            File file,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        DfsServiceAgentDto dfsServiceAgentDto =
                this.dfsServiceAgentService.getPreferredDfsServiceAgent(null);

        String fileId = this.dfsServiceAgentService.uploadFile(dfsServiceAgentDto.getUid(), file, null);

        return new AbcTuple2<>(dfsServiceAgentDto.getUid(), fileId);
    }
}
