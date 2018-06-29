package net.ewant.sessionshare.support.spring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@ConditionalOnClass(value = {ShareSessionConfiguration.class})
@Import(value = {ShareSessionInitializerProcessor.class})
public @interface EnableShareSession {
}
