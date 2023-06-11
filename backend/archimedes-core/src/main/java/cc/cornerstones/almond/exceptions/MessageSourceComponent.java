package cc.cornerstones.almond.exceptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MessageSourceComponent {
    @Autowired
    private MessageSource messageSource;

    public String getMessage(String code) {
        return getMessage(code, null);
    }

    public String getMessage(String code, Object[] args) {
        return getMessage(code, args, "");
    }

    public String getMessage(String code, Object[] args, String defaultMsg) {
        // 这里使用比较方便的方法，不依赖request.
        Locale locale = LocaleContextHolder.getLocale();
        return this.messageSource.getMessage(code, args, defaultMsg, locale);
    }

}
