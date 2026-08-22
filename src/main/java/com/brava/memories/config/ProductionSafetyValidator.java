package com.brava.memories.config;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
@Component
public class ProductionSafetyValidator {
 private final Environment env; private final AppProperties props;
 public ProductionSafetyValidator(Environment env,AppProperties props){this.env=env;this.props=props;}
 @PostConstruct void validate(){boolean prod=Arrays.asList(env.getActiveProfiles()).contains("prod");if(!prod)return;if(!"r2".equalsIgnoreCase(props.storage().provider()))throw new IllegalStateException("Production requires STORAGE_PROVIDER=r2");if(!props.scanning().enabled())throw new IllegalStateException("Production requires malware scanning");if(props.security().jwtSecret().contains("change-this"))throw new IllegalStateException("Production JWT secret must be configured");if(!props.security().cookieSecure())throw new IllegalStateException("Production requires secure auth cookies");if(props.security().visitorHashSecret().contains("change-this"))throw new IllegalStateException("Production requires VISITOR_HASH_SECRET to be configured");if(props.bootstrap().adminPassword().equals("ChangeMe123!"))throw new IllegalStateException("Production requires BOOTSTRAP_ADMIN_PASSWORD to be configured");}
}
