package cc.cornerstones.biz.administration.serviceconnection.service.assembly;

import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.exceptions.AbcResourceIntegrityException;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.utils.AbcFileUtils;
import cc.cornerstones.arbutus.tinyid.service.IdHelper;
import cc.cornerstones.archimedes.extensions.AuthenticationServiceProvider;
import cc.cornerstones.archimedes.extensions.types.SignedInfo;
import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceAgentDo;
import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceComponentDo;
import cc.cornerstones.biz.administration.serviceconnection.persistence.AuthenticationServiceAgentRepository;
import cc.cornerstones.biz.administration.serviceconnection.persistence.AuthenticationServiceComponentRepository;
import cc.cornerstones.biz.administration.serviceconnection.share.constants.ServiceComponentTypeEnum;
import cc.cornerstones.biz.administration.usermanagement.entity.UserAccountDo;
import cc.cornerstones.biz.administration.usermanagement.entity.UserCredentialDo;
import cc.cornerstones.biz.administration.usermanagement.persistence.UserAccountRepository;
import cc.cornerstones.biz.administration.usermanagement.persistence.UserCredentialRepository;
import cc.cornerstones.biz.authentication.dto.AccountPasswordSignInDto;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import net.sf.cglib.core.Local;
import org.apache.commons.codec.digest.DigestUtils;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class AccountPasswordAuthenticationServiceProvider extends AuthenticationServiceProvider
        implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountPasswordAuthenticationServiceProvider.class);

    @Autowired
    private IdHelper idHelper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private AuthenticationServiceComponentRepository authenticationServiceComponentRepository;

    @Autowired
    private AuthenticationServiceAgentRepository authenticationServiceAgentRepository;

    private static final String NAME = "Account Password";
    private static final String DESCRIPTION = "Account Password";
    private static final Float SEQUENCE = 0f;
    private static final Boolean ENABLED = true;
    private static final Boolean PREFERRED = true;

    /**
     * Handle an application event.
     *
     * @param event the event to respond to
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        AuthenticationServiceComponentDo serviceComponentDo =
                this.authenticationServiceComponentRepository.findByName(NAME);
        if (serviceComponentDo == null) {
            serviceComponentDo = new AuthenticationServiceComponentDo();
            serviceComponentDo.setUid(this.idHelper.getNextDistributedId(AuthenticationServiceComponentDo.RESOURCE_NAME));
            serviceComponentDo.setName(NAME);
            serviceComponentDo.setObjectName(NAME.replaceAll("_", "__")
                    .replaceAll("\\s", "_")
                    .toLowerCase());
            serviceComponentDo.setDescription(DESCRIPTION);
            serviceComponentDo.setSequence(SEQUENCE);
            serviceComponentDo.setType(ServiceComponentTypeEnum.BUILTIN);
            serviceComponentDo.setResourceName("extensions/archimedes-ext-authn-accountpassword.js");
            serviceComponentDo.setEntryClassName(AccountPasswordAuthenticationServiceProvider.class.getName());

            try {
                String configurationTemplate = getConfigurationTemplate();
                serviceComponentDo.setConfigurationTemplate(configurationTemplate);
            } catch (Exception e) {
                LOGGER.error("failed to load configuration template", e);
            }

            BaseDo.create(serviceComponentDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
            this.authenticationServiceComponentRepository.save(serviceComponentDo);

            LOGGER.info("init built-in authentication service component::name = {}, uid = {}",
                    serviceComponentDo.getName(),
                    serviceComponentDo.getUid());
        }

        Iterable<AuthenticationServiceAgentDo> authenticationServiceAgentDoIterable = this.authenticationServiceAgentRepository.findAll();
        if (authenticationServiceAgentDoIterable.iterator().hasNext()) {
            return;
        } else {
            AuthenticationServiceAgentDo authenticationServiceAgentDo = new AuthenticationServiceAgentDo();
            authenticationServiceAgentDo.setUid(this.idHelper.getNextDistributedId(AuthenticationServiceAgentDo.RESOURCE_NAME));
            authenticationServiceAgentDo.setName(NAME);
            authenticationServiceAgentDo.setObjectName(NAME.replaceAll("_", "__")
                    .replaceAll("\\s", "_")
                    .toLowerCase());
            authenticationServiceAgentDo.setDescription(DESCRIPTION);
            authenticationServiceAgentDo.setSequence(1.0f);
            authenticationServiceAgentDo.setEnabled(Boolean.TRUE);
            authenticationServiceAgentDo.setPreferred(Boolean.TRUE);
            authenticationServiceAgentDo.setServiceComponentUid(serviceComponentDo.getUid());

            BaseDo.create(authenticationServiceAgentDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
            this.authenticationServiceAgentRepository.save(authenticationServiceAgentDo);

            LOGGER.info("init built-in authentication service agent::name = {}, uid = {}",
                    authenticationServiceAgentDo.getName(),
                    authenticationServiceAgentDo.getUid());
        }
    }

    @Override
    public String getConfigurationTemplate() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("extensions" +
                "/accountpassword_authn_service_provider_configuration_template.xml");
        if (inputStream == null) {
            throw new AbcResourceIntegrityException("cannot find resource");
        } else {
            return AbcFileUtils.readContent(inputStream);
        }
    }

    @Override
    public SignedInfo signIn(
            JSONObject input,
            String configuration) throws Exception {
        //
        // Step 1, pre-processing
        //
        AccountPasswordSignInDto accountPasswordSignInDto =
                JSONObject.toJavaObject(input, AccountPasswordSignInDto.class);

        if (accountPasswordSignInDto.getAccountName().equalsIgnoreCase(InfrastructureConstants.ROOT_USER_DISPLAY_NAME)) {
            return privilegedSignIn(accountPasswordSignInDto.getPassword(), configuration);
        }

        //
        // Step 2, core-processing
        //
        List<UserAccountDo> candidateUserAccountDoList =
                this.userAccountRepository.findByName(accountPasswordSignInDto.getAccountName());
        if (CollectionUtils.isEmpty(candidateUserAccountDoList)) {
            throw new AbcResourceNotFoundException("The account name does not exist or the password is incorrect");
        }

        String hashedInputPassword = DigestUtils.sha256Hex(accountPasswordSignInDto.getPassword());

        UserAccountDo confirmedUserAccountDo = null;
        for (UserAccountDo candidateUserAccountDo : candidateUserAccountDoList) {
            UserCredentialDo userCredentialDo = this.userCredentialRepository.findByUserUid(candidateUserAccountDo.getUserUid());
            if (userCredentialDo != null) {
                if (hashedInputPassword.equals(userCredentialDo.getCredential())) {
                    confirmedUserAccountDo = candidateUserAccountDo;
                    break;
                }
            }
        }
        if (confirmedUserAccountDo == null) {
            throw new AbcResourceNotFoundException("The account name does not exist or the password is incorrect");
        }

        //
        // Step 3, post-processing
        //
        SignedInfo signedInfo = new SignedInfo();
        signedInfo.setAccountTypeUid(confirmedUserAccountDo.getAccountTypeUid());
        signedInfo.setAccountName(confirmedUserAccountDo.getName());
        return signedInfo;
    }

    private SignedInfo privilegedSignIn(
            String password,
            String configuration) throws Exception {
        //
        // Step 1, pre-processing
        //

        //
        // Step 2, core-processing
        //
        String hashedInputPassword = DigestUtils.sha256Hex(password);

        UserCredentialDo userCredentialDo = this.userCredentialRepository.findByUserUid(InfrastructureConstants.ROOT_USER_UID);
        if (userCredentialDo == null) {
            throw new AbcResourceNotFoundException("The account name does not exist or the password is incorrect");
        }

        if (!hashedInputPassword.equals(userCredentialDo.getCredential())) {
            throw new AbcResourceNotFoundException("The account name does not exist or the password is incorrect");
        }

        //
        // Step 3, post-processing
        //
        SignedInfo signedInfo = new SignedInfo();
        signedInfo.setAccountTypeUid(InfrastructureConstants.ROOT_USER_ACCOUNT_TYPE_UID);
        signedInfo.setAccountName(InfrastructureConstants.ROOT_USER_DISPLAY_NAME);
        return signedInfo;
    }

    @Override
    public void signOut(
            JSONObject input,
            String configuration) throws Exception {
        super.signOut(input, configuration);
    }

    private static Configuration parseConfiguration(
            String content) throws DocumentException {

        return null;
    }

    @Data
    private static class Configuration {

    }
}
