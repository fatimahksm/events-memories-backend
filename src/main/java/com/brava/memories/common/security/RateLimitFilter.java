package com.brava.memories.common.security;
import com.brava.memories.config.RateLimitProperties;
import com.brava.memories.config.UploadProperties;
import jakarta.servlet.*;import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;import java.time.Instant;import java.util.Set;import java.util.concurrent.*;
@Component
public class RateLimitFilter extends OncePerRequestFilter {
 private static final Set<String> AUTH_PATHS=Set.of("/api/auth/login","/api/auth/register","/api/auth/owner-access","/api/auth/event-access","/api/auth/forgot-password","/api/auth/reset-password");
 private static final java.util.regex.Pattern UPLOAD_SESSION_PATH=java.util.regex.Pattern.compile("^/api/public/events/[^/]+/uploads/session$");
 private static final long UPLOAD_BATCH_WINDOW_SECONDS=600;
 private final RateLimitProperties props; private final UploadProperties uploadProps; private final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>();
 public RateLimitFilter(RateLimitProperties props,UploadProperties uploadProps){this.props=props;this.uploadProps=uploadProps;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
   if("OPTIONS".equals(req.getMethod())){chain.doFilter(req,res);return;}
   String path=req.getRequestURI();int limit=0;long window=60;
   if(AUTH_PATHS.contains(path)&&"POST".equals(req.getMethod())){limit=props.loginAttemptsPerFiveMinutes();window=300;}
   else if(UPLOAD_SESSION_PATH.matcher(path).matches()&&"POST".equals(req.getMethod())){limit=uploadProps.maxFilesPerRequest();window=UPLOAD_BATCH_WINDOW_SECONDS;}
   else if(path.startsWith("/api/public/")&&!"GET".equals(req.getMethod()))limit=props.publicMutationsPerMinute();
   if(limit>0&&!allow(key(req,path),limit,window)){res.setStatus(429);res.setContentType(MediaType.APPLICATION_JSON_VALUE);res.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests. Please try again later.\"}");return;}
   chain.doFilter(req,res);
 }
 private String key(HttpServletRequest req,String path){
   String ip=clientIp(req);
   if(AUTH_PATHS.contains(path))return ip+":auth";
   String visitor=req.getHeader("X-Visitor-Id");
   if(visitor!=null&&visitor.matches("[A-Za-z0-9_-]{8,128}"))return ip+":"+visitor+":"+(UPLOAD_SESSION_PATH.matcher(path).matches()?"upload":"public");
   return ip+":public";
 }
 private String clientIp(HttpServletRequest req){String cf=req.getHeader("CF-Connecting-IP");if(cf!=null&&!cf.isBlank())return cf.trim();String forwarded=req.getHeader("X-Forwarded-For");if(forwarded!=null&&!forwarded.isBlank())return forwarded.split(",")[0].trim();return req.getRemoteAddr();}
 private boolean allow(String key,int limit,long seconds){long now=Instant.now().getEpochSecond();Window w=windows.compute(key,(k,v)->v==null||now-v.started>=seconds?new Window(now,1):new Window(v.started,v.count+1));return w.count<=limit;}
 private record Window(long started,int count){}
}
